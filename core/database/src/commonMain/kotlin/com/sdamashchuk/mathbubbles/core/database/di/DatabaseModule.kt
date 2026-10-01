package com.sdamashchuk.mathbubbles.core.database.di

import app.cash.sqldelight.db.SqlDriver
import com.sdamashchuk.mathbubbles.core.database.dao.FieldDao
import com.sdamashchuk.mathbubbles.core.database.dao.TargetsDao
import com.sdamashchuk.mathbubbles.core.database.local.FieldQueries
import com.sdamashchuk.mathbubbles.core.database.local.MathBubblesDatabase
import com.sdamashchuk.mathbubbles.core.database.local.TargetsQueries
import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.database.repository.GameRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module

// Neither the SqlDriver nor the dispatcher is built here. AndroidSqliteDriver and Context are
// Android-only, and Dispatchers.IO does not exist in commonMain — it is internal on Native. The
// consumer supplies both, which also keeps the disk writes off whatever pool the game loop runs
// on: the field and every target are persisted on each state change.
// The SqlDriver is not built here: AndroidSqliteDriver and Context are Android-only, and this
// module has no iOS app to justify an expect/actual for a driver nothing would use. The consumer
// (:app today) builds the platform driver and binds it; this module only consumes SqlDriver.
val databaseModule =
    module {
        single<MathBubblesDatabase> { MathBubblesDatabase(get<SqlDriver>()) }
        single<FieldQueries> { get<MathBubblesDatabase>().fieldQueries }
        single<TargetsQueries> { get<MathBubblesDatabase>().targetsQueries }
        single { FieldDao(get(), get()) }
        single { TargetsDao(get(), get()) }
        single { GameRepositoryImpl(get(), get()) } bind GameRepository::class
    }
