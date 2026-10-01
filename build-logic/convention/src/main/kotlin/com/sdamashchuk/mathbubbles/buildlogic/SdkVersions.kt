package com.sdamashchuk.mathbubbles.buildlogic

/**
 * The single declaration of the platform levels - compileSdk and minSdk were written out twice
 * each, in the plain-Android path and the KMP one, so a bump could land in one and miss the other.
 */
internal object SdkVersions {
    const val COMPILE = 37
    const val MIN = 24
    const val TARGET = 33
}
