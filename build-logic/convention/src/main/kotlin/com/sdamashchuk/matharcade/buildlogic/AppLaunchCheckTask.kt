package com.sdamashchuk.matharcade.buildlogic

import com.android.build.api.variant.BuiltArtifactsLoader
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

/**
 * Installs the debug APK on a connected device or emulator, launches the app, and fails the
 * build if it never reaches its first rendered frame or dies shortly after — the gap
 * `./gradlew check` otherwise leaves open, since compiling and unit-testing never starts the app.
 *
 * Skips, loudly, when there is no usable device: no `adb devices` at all, or one that cannot be
 * woken. A developer without an emulator, or with one asleep from being left idle, must not be
 * blocked, and there is no CI here to run it instead.
 *
 * What a green run does and does not prove: `am start -W` confirms the splash's first frame was
 * displayed, and the post-launch window (sized past the splash's own ~4.9s runtime) confirms the
 * process stayed crash-free beyond that point. Compose screen identity (splash vs. menu) is not
 * observable from adb — both live inside the same Activity — so this is "launched, drew a frame,
 * and survived past where the splash hands off," not literal proof that Menu composed.
 */
abstract class AppLaunchCheckTask : DefaultTask() {
    @get:InputDirectory
    abstract val apkFolder: DirectoryProperty

    // The loader is a stateless AGP-provided accessor, not build input; it cannot be meaningfully
    // hashed and AGP's own samples mark the equivalent property this way.
    @get:Internal
    abstract val builtArtifactsLoader: Property<BuiltArtifactsLoader>

    @get:Input
    abstract val applicationId: Property<String>

    @get:InputFile
    abstract val adbExecutable: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val requestedSerial: Property<String>

    @get:Inject
    abstract val execOperations: ExecOperations

    init {
        // Every run depends on live device state (installed package, running process, logcat
        // buffers) that Gradle cannot see as a declared input, so a cached "up to date" result
        // would mean nothing. Run it every time check does.
        outputs.upToDateWhen { false }
        group = "verification"
        description =
            "Installs and launches the debug app on a connected device, failing if it does not reach its first frame."
    }

    @TaskAction
    fun run() {
        val adb = adbExecutable.get().asFile.absolutePath
        val online = onlineDevices(adb)
        if (online.isEmpty()) {
            skip(
                "no connected device or emulator (`adb devices` found none). Attach one and re-run to exercise this check.",
            )
            return
        }
        val serial = selectSerial(online)

        if (!ensureAwake(adb, serial)) {
            skip(
                "device $serial's screen could not be woken (still asleep after $WAKE_ATTEMPTS attempts). " +
                    "A sleeping screen reproduces the exact adb signature of a startup crash, so this fails " +
                    "open rather than blaming the app; wake the device manually and re-run.",
            )
            return
        }

        val appId = applicationId.get()
        val apk = loadApk()

        logger.lifecycle("$path: installing $apk on $serial")
        val install = adbOut(adb, "-s", serial, "install", "-r", "-d", apk)
        check("Success" in install) { "install failed on $serial:\n$install" }

        // Force-stop first: `am start` on an already-running app just brings its top task
        // forward and skips application/process creation entirely, so a crash at startup
        // would go unobserved on a warm process from a previous run.
        adbOut(adb, "-s", serial, "shell", "am", "force-stop", appId)
        adbOut(adb, "-s", serial, "logcat", "-b", "crash", "-c")

        val component = resolveLauncherComponent(adb, serial, appId)
        logger.lifecycle("$path: launching $component")
        val startOutput = adbOut(adb, "-s", serial, "shell", "am", "start", "-W", "-n", component)
        verifyRealColdLaunch(startOutput, adb, serial)

        awaitNoPostLaunchCrash(adb, serial, appId)
        logger.lifecycle(
            "$path: $appId displayed its first frame and stayed alive crash-free for " +
                "${OBSERVATION_WINDOW_MS / MILLIS_PER_SECOND}s past launch (through the splash) on $serial",
        )
    }

    /**
     * `Status: ok` plus a `TotalTime:` line is necessary but not sufficient: an already-running
     * app satisfies both without a real cold launch (`Warning: Activity not started…`,
     * `TotalTime: 0`), which force-stop above should already prevent — this is the second line of
     * defence for that same assumption.
     */
    private fun verifyRealColdLaunch(
        startOutput: String,
        adb: String,
        serial: String,
    ) {
        check(
            "Status: ok" in startOutput,
        ) { launchFailure("ActivityManager rejected the launch", startOutput, adb, serial) }
        check("Activity not started" !in startOutput) {
            launchFailure(
                "am start warned the activity was not started — a warm process was already on top " +
                    "instead of the cold launch force-stop should have produced",
                startOutput,
                adb,
                serial,
            )
        }
        // TotalTime is only printed once WindowManager reports the launched activity's first frame
        // as displayed — the same "Displayed" signal used for cold-start benchmarking. A
        // ClassCastException thrown during Compose's first composition never reaches that point:
        // Status still reads "ok" but TotalTime is missing, which is exactly what a manual
        // reproduction of MC-26's target bug showed.
        val totalTime =
            Regex("TotalTime:\\s*(\\d+)")
                .find(startOutput)
                ?.groupValues
                ?.get(1)
                ?.toIntOrNull()
        check(totalTime != null && totalTime > 0) {
            launchFailure("app never reported a nonzero TotalTime (first frame displayed)", startOutput, adb, serial)
        }
    }

    private fun launchFailure(
        reason: String,
        startOutput: String,
        adb: String,
        serial: String,
    ): String {
        // The crash log is read here, not just later in awaitNoPostLaunchCrash, because a crash
        // fast enough can already be in the buffer by the time `am start -W` returns, and an
        // exception without its stack trace tells nobody what actually happened.
        val crash = adbOut(adb, "-s", serial, "logcat", "-b", "crash", "-d").trim()
        val crashSection =
            if (crash.isEmpty()) "no crash captured in the crash log buffer yet" else "crash log:\n$crash"
        return "$reason on $serial:\n$startOutput\n$crashSection"
    }

    /**
     * `am start -W` only proves the launch call itself succeeded; the app can still crash a moment
     * later. The window is sized in wall-clock time via [System.currentTimeMillis], not poll count,
     * so it cannot shrink on a faster machine the way a fixed number of iterations would — a
     * reviewer reproduction showed a real post-splash crash escaping a poll-counted window once adb
     * round-trips got fast enough. [OBSERVATION_WINDOW_MS] covers the splash's own ~4.87s runtime
     * (app/src/main/res/raw/logo.lottie, ip=25 op=171 @ 29.97fps) plus real margin for a crash
     * shortly after the app hands off to its next screen.
     */
    private fun awaitNoPostLaunchCrash(
        adb: String,
        serial: String,
        appId: String,
    ) {
        val deadline = System.currentTimeMillis() + OBSERVATION_WINDOW_MS
        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(OBSERVATION_POLL_INTERVAL_MS)
            val crash = adbOut(adb, "-s", serial, "logcat", "-b", "crash", "-d")
            check(appId !in crash) { "app crashed after launch on $serial:\n$crash" }
            val pid = adbOut(adb, "-s", serial, "shell", "pidof", appId).trim()
            check(pid.isNotEmpty()) { "app process for $appId is no longer running on $serial after launch" }
        }
    }

    /**
     * A sleeping screen makes `am start -W` return `Status: ok` with no `TotalTime:` line on a
     * perfectly healthy app — byte-for-byte the same shape as a real startup crash — so this must
     * run, and succeed, before the launch is judged at all.
     */
    private fun ensureAwake(
        adb: String,
        serial: String,
    ): Boolean {
        repeat(WAKE_ATTEMPTS) {
            val power = adbOut(adb, "-s", serial, "shell", "dumpsys", "power")
            if ("mWakefulness=Awake" in power) {
                adbOut(adb, "-s", serial, "shell", "wm", "dismiss-keyguard")
                return true
            }
            adbOut(adb, "-s", serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP")
            Thread.sleep(WAKE_POLL_INTERVAL_MS)
        }
        return false
    }

    /**
     * Mirrors adb's own refusal to guess: a single online device is used automatically, but
     * several with no `ANDROID_SERIAL` set fails loudly rather than silently installing on
     * whichever `adb devices` happens to list first — a phone left on USB next to the emulator
     * must not receive a debug build from a check nobody thought was targeting it.
     */
    private fun selectSerial(online: List<String>): String {
        val requested = requestedSerial.orNull
        if (requested != null) {
            check(
                requested in online,
            ) { "ANDROID_SERIAL=$requested is set but is not among the connected devices: $online" }
            return requested
        }
        check(online.size == 1) {
            "multiple devices are connected ($online) and ANDROID_SERIAL is not set; " +
                "export ANDROID_SERIAL to pick one deliberately rather than installing on whichever adb lists first."
        }
        return online.single()
    }

    private fun resolveLauncherComponent(
        adb: String,
        serial: String,
        appId: String,
    ): String {
        // Resolved from the package rather than hardcoded, so this task stays correct for any
        // application module that applies matharcade.android.application, not just :app today.
        val resolved =
            adbOut(
                adb,
                "-s",
                serial,
                "shell",
                "cmd",
                "package",
                "resolve-activity",
                "--brief",
                "-c",
                "android.intent.category.LAUNCHER",
                appId,
            ).lines()
                .map { it.trim() }
                .lastOrNull { it.startsWith("$appId/") }
        return checkNotNull(resolved) { "could not resolve a launcher activity for $appId on $serial" }
    }

    private fun loadApk(): String {
        val builtArtifacts = builtArtifactsLoader.get().load(apkFolder.get())
        val element =
            checkNotNull(builtArtifacts?.elements?.firstOrNull()) { "no APK was produced in ${apkFolder.get()}" }
        return element.outputFile
    }

    private fun onlineDevices(adb: String): List<String> =
        adbOut(adb, "devices")
            .lines()
            .drop(1) // header line: "List of devices attached"
            .mapNotNull { line ->
                val parts = line.trim().split(Regex("\\s+"))
                parts.takeIf { it.size >= 2 && it[1] == "device" }?.get(0)
            }

    private fun skip(reason: String) {
        logger.lifecycle("$path SKIPPED: $reason")
    }

    private fun adbOut(vararg args: String): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            commandLine(*args)
            standardOutput = output
            errorOutput = output
            isIgnoreExitValue = true
        }
        return output.toString(Charsets.UTF_8)
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
        const val OBSERVATION_WINDOW_MS = 10_000L
        const val OBSERVATION_POLL_INTERVAL_MS = 500L
        const val WAKE_ATTEMPTS = 5
        const val WAKE_POLL_INTERVAL_MS = 400L
    }
}
