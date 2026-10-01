package com.sdamashchuk.mathbubbles.core.ui.component

private const val HASH_INDEX_PRIME = 374_761_393
private const val HASH_SALT_PRIME = 668_265_263
private const val HASH_MIX_PRIME = 1_274_126_177
private const val HASH_SHIFT = 13
private const val HASH_DROP_LOW_BITS = 8
private const val HASH_MASK = 0xFFFF
private const val HASH_SCALE = 65_535f

/**
 * A deterministic value in `[0, 1]` for a mote's `(index, salt)` pair, so a drifting or static
 * field needs no remembered state to keep a mote's placement stable across frames or recompositions.
 */
fun moteScatter(
    index: Int,
    salt: Int,
): Float {
    var h = index * HASH_INDEX_PRIME + salt * HASH_SALT_PRIME
    h = (h xor (h ushr HASH_SHIFT)) * HASH_MIX_PRIME
    return ((h ushr HASH_DROP_LOW_BITS) and HASH_MASK).toFloat() / HASH_SCALE
}
