package com.sdamashchuk.mathbubbles.core.ui.sound

/**
 * The device's own ringer state, independent of the in-app mute switch - both gate every sample
 * the same way, but this one the player never gets to override from inside the game.
 */
interface SilentModeChecker {
    fun isSilent(): Boolean
}
