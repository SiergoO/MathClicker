package com.sdomashchuk.mathclicker.di

import com.sdomashchuk.mathclicker.game.Game
import kotlinx.coroutines.CoroutineScope
import org.junit.Assert.assertSame
import org.junit.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.test.verify.verify

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
}
