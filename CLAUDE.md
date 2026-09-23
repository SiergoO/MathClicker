# Math Arcade

Android Compose game, heading for Kotlin Multiplatform. Store title `Math Arcade: Tap to Zero`;
`applicationId` is `com.sdamashchuk.matharcade` and sources live under `com/sdamashchuk/matharcade/`.
It was called MathClicker until MC-44 — the *directory* on disk is still `MathClicker` and stays that
way, because the tooling resolves through it.

Eight modules: `:app`, `:core:model`, `:core:game`, `:core:database`, `:core:ui`, `:core:component`,
`:feature:game`, `:feature:menu`. `data/` and `domain/` directories exist on disk but are **not** in
`settings.gradle` — they are orphaned, ignore them.

## Toolchain constraints

- Gradle 9.4.1, AGP 9.2.1, Kotlin 2.4.0, JDK 17. compileSdk 36, minSdk 24, targetSdk 33.
- **No KSP release exists for Kotlin 2.4.0** (newest is 2.3.12). Anything that needs KSP — Room KMP included — is off the table until one ships. This is *why* persistence is SQLDelight: **Room and kapt are gone from this project entirely**, replaced in MC-6. Do not reintroduce either.
- JDK 17 is the only JDK installed.
- `local.properties` is gitignored and required. Without it every Gradle task fails with "SDK location not found":
  `printf 'sdk.dir=%s/Library/Android/sdk\n' "$HOME" > local.properties`
- The `:core:*` modules are KMP (`commonMain` / `androidMain` / `commonTest`, targets androidHostTest + iosArm64 + iosSimulatorArm64) with **zero Android imports in `commonMain`** — game rules go in `:core:game`. `Dispatchers.IO` does not exist in `commonMain`; use `Dispatchers.Default`. Never add an Android or `javax.*` import to `commonMain`.
- `:core:*` has **no Android lint**: AGP 9.2.1's KMP library plugin creates only `lintAnalyzeAndroidHostTest`, no production analysis task and no report. detekt and spotless do cover it. Re-check on a later AGP.
- **Direction:** Android-only today → KMP (Koin, SQLDelight, Compose Multiplatform), following EyeXP's stack. See `.claude/specs/MC-6-kmp-migration.md`.

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

The same trap applies to any module-scoped test task: on a module that has no tests yet it is
`NO-SOURCE` and reports success. `:feature:game:allTests` was MC-17's gate and ran nothing. A gate
cell naming a task that cannot fail is not a gate — use `./gradlew check` until the module has a test.

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
open ask — check `ask.py list --open` first.

**The board can approve a plan.** An unapproved epic shows an `approve & run` button that writes
`approved.txt` and queues the plan in one press. This was not always true — the board could start
an approved task but never approve one — and the old rule is gone because the owner asked for it
twice and the control is theirs. Do not re-introduce a chat-only gate for plans, and do not treat
an unapproved epic as `blocked`: it is new, not broken. What still never goes through a board
click is anything irreversible outside the repo — pushing to origin, deleting a remote branch.

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
