# Math Bubbles

Android Compose game, heading for Kotlin Multiplatform. The app is called Math Bubbles (renamed in
MC-85) and has not been released yet. `applicationId` is still `com.sdamashchuk.matharcade`, sources
live under `com/sdamashchuk/matharcade/`, and code names such as `MathArcadeTheme` keep the older
Math Arcade name. The directory on disk is still `MathClicker`, because the tooling resolves through it.

Eight modules: `:app`, `:core:model`, `:core:game`, `:core:database`, `:core:ui`, `:core:component`,
`:feature:game`, `:feature:menu`. `data/` and `domain/` directories exist on disk but are **not** in
`settings.gradle` — they are orphaned, ignore them.

## Toolchain

Versions, the KSP/Room constraint, lint coverage and `commonMain` import rules live in
`.claude/rules/` and load with the files they govern. What they do not say:

- Direction: Android-only today, moving to KMP (Koin, SQLDelight, Compose Multiplatform) after
  EyeXP's stack. See `.orchestra/library/specs/MC-6-kmp-migration.md`.
- Missing `local.properties` is fixed with
  `printf 'sdk.dir=%s/Library/Android/sdk\n' "$HOME" > local.properties`.

## Commands

```bash
./gradlew :core:game:allTests            # game rules on every target (androidHostTest + iosSimulatorArm64)
./gradlew :core:game:testAndroidHostTest # faster: JVM only, skips the Kotlin/Native link
./gradlew :app:testDebugUnitTest         # :app unit tests
./gradlew check                          # tests + detekt + spotless + app launch, every module
./gradlew detekt                         # smells, complexity, exception handling
./gradlew spotlessApply                  # fix formatting (ktlint)
./gradlew assembleDebug                  # APK at app/build/outputs/apk/debug/app-debug.apk
```

**There is no `:core` module.** `:core:allTests`, `:core:testAndroidHostTest` and
`:core:testDebugUnitTest` all fail — the module was split into `:core:model` / `:core:game` /
`:core:database` / `:core:ui` / `:core:component` in MC-9. Scope to the one you mean.

Do not run bare `./gradlew testDebugUnitTest` either: only `:app` has that task, so it passes while
running none of the core tests.

`./gradlew check` includes `:app:checkAppLaunch`, which installs the APK on a connected device and
fails the build if it does not reach its first frame. It is **not concurrency-safe** — two
simultaneous runs force-stop each other and produce failures that look like app crashes or a missing
`TotalTime`. Run one at a time. Several runs have already been lost to this.

Prefer module-scoped tasks over whole-project ones; a full build here is slow.

Spotless owns formatting; detekt owns smells. Do not duplicate a rule across both.

## Git

Commit format and the no-attribution rule are in `.claude/rules/commits.md`. Beyond that:

- Branch: `feature/MC-<n>-<kebab-summary>` or `bugfix/MC-<n>-<kebab-summary>`.
- **Task ids are whole numbers.** `MC-42`, never `MC-4.2`. The dotted ids below MC-10 are
  historical: commits reference them and history is not rewritten to suit a numbering scheme.

## Workflow

Work runs through the orchestra plugin. Its board lives in `.orchestra/` and is served by
`orchestra serve` on port 8766; the CLI is `~/StudioProjects/MobileOrchestra/plugin/bin/orchestra`.
`.orchestra/` and `.claude/` are gitignored, so a fresh clone has this file and no workflow.

- Skills: `/orchestra:requirements` to write a requirement, `/orchestra:run` to take it to committed
  work, `/orchestra:review`, `/orchestra:health`, `/orchestra:rules`, `/orchestra:board`.
- Moving a card is only a status change. Work starts from the board's Run button, which arrives as a
  `RUN <id>` event.
- Anything that needs the owner goes on the board as an ask (`orchestra ask add`), not a stalled
  run. Never report work as blocked without a matching open ask.
- Never close a task without a green `./gradlew check`; `orchestra task done` asks for problem,
  solution, result and evidence.
- Pushing to origin or deleting a remote branch never happens from a board click.
- Never run `git worktree add` directly. `orchestra worktree create <task>` enforces the
  two-worktree cap and the disk floor.
