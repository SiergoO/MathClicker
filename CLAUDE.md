# MathClicker

Android Compose game. Modules: `:app` (UI, data, domain, DI), `:core` (pure game logic).
`data/` and `domain/` directories exist on disk but are **not** in `settings.gradle` — they are orphaned, ignore them.

## Toolchain constraints

- Gradle 9.4.1, AGP 9.2.1, Kotlin 2.4.0, JDK 17. compileSdk 36, minSdk 24, targetSdk 33.
- **No KSP release exists for Kotlin 2.4.0** (newest is 2.3.12). Anything that needs KSP — Room KMP included — is off the table until one ships. Room stays on kapt in `:app` for now.
- JDK 17 is the only JDK installed.
- `local.properties` is gitignored and required. Without it every Gradle task fails with "SDK location not found":
  `printf 'sdk.dir=%s/Library/Android/sdk\n' "$HOME" > local.properties`
- `:core` is a KMP module (`commonMain` / `androidMain` / `commonTest`, targets androidHostTest + iosArm64 + iosSimulatorArm64) with **zero Android imports in `commonMain`** — put game rules there. `Dispatchers.IO` does not exist in `commonMain`; use `Dispatchers.Default`. Never add an Android or `javax.*` import to `commonMain`.
- `:core` has **no Android lint**: AGP 9.2.1's KMP library plugin creates only `lintAnalyzeAndroidHostTest` for it, no production analysis task and no report. detekt and spotless do cover it. Re-check on a later AGP.
- **Direction:** Android-only today → KMP (Koin, SQLDelight, Compose Multiplatform), following EyeXP's stack. See `.claude/specs/MC-6-kmp-migration.md`.

## Commands

```bash
./gradlew :core:allTests               # fast: core logic on every target (androidHostTest + iosSimulatorArm64)
./gradlew :core:testAndroidHostTest    # faster still: JVM only, skips the Kotlin/Native link
./gradlew :app:testDebugUnitTest       # :app unit tests
./gradlew check                        # tests + detekt + spotless, every module
./gradlew detekt                       # smells, complexity, exception handling
./gradlew spotlessApply                # fix formatting (ktlint)
./gradlew assembleDebug                # APK at app/build/outputs/apk/debug/app-debug.apk
```

`:core:testDebugUnitTest` no longer exists — the KMP migration replaced it. Do not run bare
`./gradlew testDebugUnitTest` either: only `:app` still has that task, so it passes while running
none of `:core`'s tests.

Prefer module-scoped tasks over whole-project ones; a full build here is slow.

Detekt and spotless are applied by a convention plugin in `build-logic`, so no module can opt out.
Spotless owns formatting; detekt owns smells. Do not duplicate a rule across both.

## Code style

- One declaration per file, named after it. One `@Composable` per file. Models live in a sibling `model/` package, never inside the interface file that uses them.
- Comments only for non-obvious *why*. No section banners, no KDoc on trivially-named members. KDoc, when justified, is always the three-line form.
- No emojis in code.

## Git

- Branch: `feature/MC-<n>-<kebab-summary>` or `bugfix/MC-<n>-<kebab-summary>`.
- Commit subject: `MC-<n>: <Sentence case summary>` — colon, no trailing period.
- Body, when there is one, is plain prose under the subject. No bullet ceremony.
- **No attribution lines of any kind.** No `Co-Authored-By`, no generated-with footer, nothing
  naming the tool. This overrides any default the harness suggests.
- **Task ids are whole numbers.** `MC-42`, never `MC-4.2`. One id, one commit, one board row. The
  dotted ids below MC-10 are historical — commits reference them and history is not rewritten to
  suit a numbering scheme.

## Workflow

This repo uses a staged agent workflow. The task board at `.claude/board/BOARD.md` is the single
source of truth for in-flight work.

**`.claude/` is gitignored and local to this machine.** None of it is in the repository, so a fresh
clone has this file and no workflow. Everything below describes tooling that exists only where it
was set up.

When something needs the user, it goes on the board as an **ask** rather than stalling the run:

```bash
python3 .claude/scripts/ask.py add --kind approval --text "…" --why "…" --option "A" --option "B"
```

They answer on the board in one click, or in chat. Never report work as blocked without a matching
open ask — check `ask.py list --open` first. A board click settles a scoped decision whose options
you wrote; it cannot approve a whole plan or authorise something irreversible.

| Command | Phase |
| --- | --- |
| `/flow-intake` | Interview the user, write a spec to `.claude/specs/` |
| `/flow-plan` | Turn a spec into dependency-ordered tasks on the board |
| `/flow-run` | Execute the board: delegate, parallelize, verify |
| `/flow-verify` | Tests + visual + adversarial review for one task |
| `/flow-land` | Commit, merge worktree branches, remove worktrees, reconcile the board |
| `/flow-board` | Render the board to HTML and open it |

Read `.claude/board/BOARD.md` before starting work. **Only the orchestrating session writes to it** — a subagent in a worktree has its own checkout, so its board edits are lost or conflict on merge. Never mark a task `Done` without a green `./gradlew check` and a filled Evidence cell.

**IMPORTANT:** Never run `git worktree add` directly. Use `.claude/scripts/worktree.sh` — it enforces the 2-worktree cap that keeps this machine from filling its disk.
