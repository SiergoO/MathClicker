plugins {
    id("mathclicker.kmp.library")
    alias(libs.plugins.sqldelight)
}

sqldelight {
    databases {
        // "Database" is the file name; kept in the driver, not here, since this module never
        // builds the driver — see DatabaseModule.kt. The name, schema version and CREATE TABLE
        // text must not drift: SQLDelight opens Room's existing on-device file in place only
        // because they still match.
        create("MathClickerDatabase") {
            packageName.set("com.sdomashchuk.mathclicker.core.database.local")
        }
    }
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.model)
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
