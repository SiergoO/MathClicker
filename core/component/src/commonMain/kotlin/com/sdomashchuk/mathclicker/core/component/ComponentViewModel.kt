package com.sdomashchuk.mathclicker.core.component

import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Minimal InstanceKeeper-backed stand-in for androidx.lifecycle.ViewModel: a coroutine scope that
 * survives configuration change (retained by a component's instanceKeeper) and is cancelled only
 * once the instance is actually destroyed.
 */
abstract class ComponentViewModel : InstanceKeeper.Instance {
    protected val viewModelScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onDestroy() {
        viewModelScope.cancel()
    }
}
