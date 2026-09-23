plugins {
    id("matharcade.kmp.library")
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        // "Database" is the file name; kept in the driver, not here, since this module never
        // builds the driver — see DatabaseModule.kt. The name, schema version and CREATE TABLE
        // text must not drift: SQLDelight opens Room's existing on-device file in place only
        // because they still match.
        create("MathArcadeDatabase") {
            packageName.set("com.sdamashchuk.matharcade.core.database.local")
            // .sqm files live next to the .sq files they migrate. SQLDelight numbers a migration
            // file by the version it migrates *from* — 1.sqm takes version 1 to 2 — and both Room
            // (`@Database(version = 1)`) and the pre-migration SQLDelight schema already wrote
            // version 1 to every existing install's `user_version`, so the first one is 1.sqm.
            // verifyMigrations replays the .sqm files on top of the committed databases/ snapshot
            // they migrate from and fails the build if the result drifts from what the .sq CREATE
            // TABLE statements declare for the current version.
            verifyMigrations.set(true)
            // Where `generateCommonMainMathArcadeDatabaseSchema` writes a `<version>.db` snapshot of
            // the schema as it stood at that version, and where verifyMigrations expects to find one
            // per released version to replay each later .sqm against.
            schemaOutputDirectory.set(layout.projectDirectory.dir("src/commonMain/sqldelight/databases"))
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            // api, not implementation: GameRepository and its DAOs expose Field, Target and
            // OperationSign on their own public API, so callers need :core:model resolvable
            // at their own compile time, not just this module's.
            api(projects.core.model)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.koin.core)
        }
        androidHostTest.dependencies {
            implementation(libs.junit)
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
