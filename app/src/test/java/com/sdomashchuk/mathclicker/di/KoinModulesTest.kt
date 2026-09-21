package com.sdomashchuk.mathclicker.di

import com.sdomashchuk.mathclicker.core.game.Game
import com.sdomashchuk.mathclicker.domain.repository.GameRepository
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.verify.verify
import java.io.File

class KoinModulesTest {
    // CoroutineScope is built inline in gameModule rather than declared, so verify() cannot resolve
    // it. Context is deliberately absent from this list: no definition's constructor takes one —
    // androidContext() is only read inside dataModule's lambda, which verify() never executes — so
    // whitelisting it would claim a check that does not happen.
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyConstructorDependencyHasADefinition() {
        module { includes(appModules) }.verify(extraTypes = listOf(CoroutineScope::class))
    }

    // verify() reflects on constructors and never asserts on definition kind, so it passes happily
    // if `single { Game(...).apply { start() } }` degrades to a factory. That degradation compiles,
    // renders, and silently kills level-up and life-loss, because each injection site would get its
    // own un-started collector. Only resolving twice catches it.
    @Test
    fun gameIsOneSharedInstanceAcrossResolutions() {
        val koin = koinApplication { modules(appModules) }.koin
        try {
            assertSame(koin.get<Game>(), koin.get<Game>())
        } finally {
            koin.get<Game>().stop()
            koin.close()
        }
    }

    // RootComponent is the only KoinComponent left now that viewModelModule is gone, and it is not
    // itself a Koin definition, so verify() above never looks at it — it resolves Game and
    // GameRepository with plain get() calls inside a private factory, not constructor injection,
    // and nothing else in appModules takes GameRepository as a constructor parameter to exercise
    // that binding by accident. RootComponentProbe exists only to give verify() a constructor
    // shaped like that factory's real dependency pull; verify() never runs a definition's lambda
    // body, so this stays JVM-safe even though GameRepository's real binding opens an
    // AndroidSqliteDriver. Delete `bind GameRepository::class` from dataModule and this fails;
    // today it would instead fail on first navigation to the game screen.
    @OptIn(KoinExperimentalAPI::class)
    @Test
    fun everyTypeRootComponentAsksKoinForHasADefinition() {
        module {
            includes(appModules)
            factory { RootComponentProbe(get(), get()) }
        }.verify(extraTypes = listOf(CoroutineScope::class))
    }

    // The probe only mirrors what someone remembered to put in it, so on its own the test above
    // promises a completeness it cannot deliver: a third get<>() added to RootComponent.child()
    // and left out of the probe would sail through and fail on first navigation instead. This
    // reads the factory's own source and fails the moment the two drift, which is what turns the
    // probe from a note asking to be maintained into one that says when it was not.
    @Test
    fun theProbeMirrorsEveryTypeRootComponentAsksKoinFor() {
        val asked =
            Regex("""\bget<(\w+)>\(\)""")
                .findAll(rootComponentSource())
                .map { it.groupValues[1] }
                .toSortedSet()
        val mirrored =
            RootComponentProbe::class.java.declaredConstructors
                .single()
                .parameterTypes
                .map { it.simpleName }
                .toSortedSet()
        assertEquals(
            "RootComponent asks Koin for types RootComponentProbe does not mirror, so the graph " +
                "test does not cover them. Add them to the probe.",
            asked,
            mirrored,
        )
    }

    private fun rootComponentSource(): String {
        val relative = "src/main/java/com/sdomashchuk/mathclicker/presentation/navigation/RootComponent.kt"
        val candidates = listOf(File(relative), File("app/$relative"))
        return candidates.firstOrNull { it.isFile }?.readText()
            ?: error("RootComponent.kt not found from ${File(".").absolutePath}; tried $candidates")
    }

    private class RootComponentProbe(
        game: Game,
        gameRepository: GameRepository,
    )
}
