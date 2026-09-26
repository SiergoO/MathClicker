package com.sdamashchuk.matharcade.core.game.helper

// MC-52 (kept by MC-73): base minus a linear step, floored - that ordering is what keeps the curve
// from ever going empty at any level in 1..999 (SessionHelperImplTest pins this as a property, not a
// number: the pre-MC-52 curve crossed at level 1334, which is why SessionHelperImpl's own LEVEL_MAX
// exists). MC-73 dropped the "add a fixed spread on top" half: the spread now lives on
// getTargetFlightTimeMs as a speed multiplier, not on this base curve directly.
// MC-94: base 9500 -> 7600 and step 110 -> 90. A 9.5-second fall was built for level-1 values of
// 1..23; against MC-93's 1..11 it left the player waiting rather than playing, which is the "warm-up
// should be more dynamic" note. The step comes down with the base so the curve reaches its floor at
// about the same level it always did (46, was 55) rather than getting there a third faster.
internal const val FLIGHT_BASE_MS = 7600
private const val FLIGHT_STEP_MS = 90
private const val FLIGHT_FLOOR_MS = 3500

// The base (un-spread) flight time a level's speed curve implies - floored so it, and everything
// derived from it below, can never go empty or cross at any level in 1..999 (see FLIGHT_BASE_MS's own
// comment). Top-level rather than a SessionHelperImpl member (MC-70): the class was already at
// detekt's TooManyFunctions ceiling before getSubtractionTargetValueByLevel, and this reads none of
// that class's state - not even its Random - so it moved out rather than raising the threshold.
internal fun baseFlightMsByLevel(level: Int): Int =
    (FLIGHT_BASE_MS - level * FLIGHT_STEP_MS).coerceAtLeast(FLIGHT_FLOOR_MS)
