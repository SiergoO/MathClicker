package com.sdamashchuk.matharcade.core.game.objectmapper

import com.sdamashchuk.matharcade.core.model.Target

internal fun List<Target>.changeActiveness(
    id: Int,
    isActive: Boolean,
): List<Target> =
    this.map {
        if (id == it.id) {
            it.copy(
                isActive = isActive,
            )
        } else {
            it
        }
    }

internal fun List<Target>.decrementValue(
    id: Int,
    decrement: Int,
): List<Target> =
    this.map {
        if (id == it.id) {
            val targetValue = it.value - decrement
            it.copy(
                value = targetValue,
            )
        } else {
            it
        }
    }

// The floor a hold's clamp below coerces finishesAtMs against - strictly after appearsAtMs, never
// equal to it, so the result can never reopen Target.position's own zero-span guard.
private const val MIN_TARGET_SPAN_MS = 1L

// The two single-target axis effects from MC-71's design doc collapse into one function: a positive
// byMs holds the target (finishesAtMs += x), a negative one speeds it up (finishesAtMs -= x).
// appearsAtMs never moves - shifting both ends would slide the whole flight rather than hold or
// hurry it. A no-op on every other id, and on this id too once it is inactive or has already broken
// out against gameTimeMs (checked independently of isActive, not assumed redundant with it, since a
// restored row could in principle carry a stale isActive against a finishesAtMs already in the
// past). The coerceAtLeast floor is what stops a large enough speed-up from inverting the span.
internal fun List<Target>.shiftFinish(
    id: Int,
    byMs: Long,
    gameTimeMs: Long,
): List<Target> =
    this.map {
        if (id == it.id && it.isActive && !it.hasBrokenOut(gameTimeMs)) {
            it.copy(finishesAtMs = (it.finishesAtMs + byMs).coerceAtLeast(it.appearsAtMs + MIN_TARGET_SPAN_MS))
        } else {
            it
        }
    }

internal fun List<Target>.ensureAlive(): List<Target> =
    this.map {
        it.copy(
            isActive = it.value > 0 && it.isActive,
        )
    }

internal fun List<Target>.ensureAlive(id: Int): List<Target> =
    this.map {
        if (id == it.id) {
            it.copy(
                isActive = it.value > 0 && it.isActive,
            )
        } else {
            it
        }
    }

// MC-71's design doc claims this dies once finishes are assigned directly - that claim is wrong.
// Consecutive targets' visible windows always overlap while the board is merely idling: MC-73's own
// invariant (finishSpacing < minFlightTime, since (INITIAL_LIFE_COUNT - 1) * spacing only needs to
// clear maxFlightTime, not spacing alone clearing minFlightTime with room to spare) means target i+1
// always appears before target i finishes, so waiting alone never empties the board. But tick()
// fires this on the edge "a visible active target existed, and now none does" - and a player
// clearing the last visible target *early*, before its slower not-yet-visible successors have
// caught up, produces exactly that edge. So this stays reachable through good play, not idling.
//
// MC-72's version of this function had the defect MC-73 exists to close a second time: it snapped
// however many "closest" targets it chose to exactly gameTimeMs, individually - which collapsed
// their finishes back to within a flight-spread of each other, recreating the triple-death this
// whole epic exists to kill, just triggered by clearing the board instead of idling. The fix is a
// single uniform shift of the whole remaining schedule: one shiftMs, computed from the closest
// waiting target, applied to every waiting target's appearsAtMs and finishesAtMs alike. Relative
// spacing between every pair of finishes is preserved exactly, because subtracting one constant
// from both ends of every interval cannot change any interval's length.
//
// This also removes the only Random draw shortenAppearanceDelay used to make (how many targets to
// reveal) - a strictly better outcome, not a loss: the uniform shift needs no choice to make, and
// tick()'s own determinism guarantee (the draw count must depend on game events, not frame rate) is
// trivially satisfied by a function that draws nothing at all.
internal fun List<Target>.shortenAppearanceDelay(gameTimeMs: Long): List<Target> {
    val waitingTargets = this.filter { it.isActive && !it.isVisible(gameTimeMs) }
    val closestAppearsAtMs = waitingTargets.minOfOrNull { it.appearsAtMs } ?: return this
    val shiftMs = closestAppearsAtMs - gameTimeMs
    return this.map { target ->
        if (waitingTargets.contains(target)) {
            target.copy(
                appearsAtMs = target.appearsAtMs - shiftMs,
                finishesAtMs = target.finishesAtMs - shiftMs,
            )
        } else {
            target
        }
    }
}
