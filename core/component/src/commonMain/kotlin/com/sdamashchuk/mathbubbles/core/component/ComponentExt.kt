package com.sdamashchuk.mathbubbles.core.component

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.getOrCreate

/**
 * Get-or-create a [ComponentViewModel] from this component's instanceKeeper, keyed by its class
 * unless [key] is given. This is what makes a state holder survive configuration change now that
 * androidx.lifecycle.ViewModel is gone.
 */
inline fun <reified VM : ComponentViewModel> ComponentContext.viewModel(
    key: String? = null,
    crossinline factory: () -> VM,
): VM {
    val cacheKey: Any = key ?: VM::class
    return instanceKeeper.getOrCreate(cacheKey) { factory() }
}
