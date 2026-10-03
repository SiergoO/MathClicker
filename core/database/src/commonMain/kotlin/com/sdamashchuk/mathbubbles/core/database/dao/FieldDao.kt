package com.sdamashchuk.mathbubbles.core.database.dao

import com.sdamashchuk.mathbubbles.core.database.local.FieldQueries
import com.sdamashchuk.mathbubbles.core.model.Booster
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.INITIAL_LIFE_COUNT
import com.sdamashchuk.mathbubbles.core.model.OperationSign
import com.sdamashchuk.mathbubbles.core.model.logging.Logger
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import com.sdamashchuk.mathbubbles.core.database.local.Field_ as LocalField

private const val BOOSTER_STASH_SEPARATOR = ","

// Mirrors core/game's own MAX_COMBO_MULTIPLIER - 1: applyCombo never writes past it, so this only
// guards a row a future migration or external edit left out of range.
private const val MAX_BONUS_MULTIPLIER = 9

// Core/game's SessionHelperImpl owns the level range; this module cannot depend on it.
private const val MIN_LEVEL = 1
private const val MAX_LEVEL = 999

// Rewind runs the clock at -1 and neutral is 1, so no valid rate lies outside this span.
private const val MIN_EFFECT_RATE = -1.0
private const val MAX_EFFECT_RATE = 1.0

class FieldDao(
    private val queries: FieldQueries,
    private val dispatcher: CoroutineDispatcher,
    private val logger: Logger,
) {
    // id == 0 is the domain model's "not yet persisted" default. Room bound NULL for it on an
    // autoGenerate primary key so SQLite would assign the next rowid; the generated insertField
    // takes a nullable id for the same reason, so the mapping here is a direct translation.
    suspend fun insertField(field: Field) = withContext(dispatcher) { queries.insert(field) }

    suspend fun updateField(field: Field) = withContext(dispatcher) { queries.update(field) }

    suspend fun getFieldById(id: Int): Field =
        withContext(dispatcher) {
            checkNotNull(queries.getFieldById(id.toLong()).executeAsOne().toDomainModel(logger)) {
                "Field $id cannot be decoded"
            }
        }

    suspend fun getUnfinishedField(): Field? =
        withContext(dispatcher) {
            var decoded: Field? = null
            while (decoded == null) {
                val row = queries.getUnfinishedField().executeAsOneOrNull() ?: break
                decoded = row.toDomainModel(logger)
                if (decoded == null) queries.closeFieldById(row.id)
            }
            decoded
        }

    suspend fun getNextFieldId(): Int =
        withContext(dispatcher) { queries.getMaxFieldId().executeAsOne().toIntClamped() + 1 }

    // Newest first, capped at 7 (see Field.sq); the best among them is a separate query below
    // because it has to read past this window, not just this page.
    suspend fun getRecentClosedFields(): List<Field> =
        withContext(
            dispatcher,
        ) { queries.getRecentClosedFields().executeAsList().mapNotNull { it.toDomainModel(logger) } }

    suspend fun getBestClosedField(): Field? =
        withContext(dispatcher) {
            queries
                .getBestClosedField(
                    OperationSign.entries.map { it.sign },
                ).executeAsOneOrNull()
                ?.toDomainModel(logger)
        }
}

// Shared with GameRepositoryImpl's transactional save, which calls these synchronously inside a
// queries.transaction block rather than through the suspend wrappers above.
internal fun FieldQueries.insert(field: Field) =
    insertField(
        id = field.id.takeIf { it != 0 }?.toLong(),
        level = field.level.toLong(),
        score = field.score.toLong(),
        lifeCount = field.lifeCount.toLong(),
        bonusMultiplier = field.bonusMultiplier.toLong(),
        currentOperationSign = field.currentOperationSign.sign,
        currentOperationDigit = field.currentOperationDigit.toLong(),
        nextOperationSign = field.nextOperationSign.sign,
        nextOperationDigit = field.nextOperationDigit.toLong(),
        isClosed = field.isClosed,
        finishedAt = field.finishedAt,
        gameTimeMs = field.gameTimeMs,
        currentBooster = field.currentBooster?.name,
        nextBooster = field.nextBooster?.name,
        boosterStash = field.boosterStash.joinToString(BOOSTER_STASH_SEPARATOR) { it.name },
        boosterDropCounter = field.boosterDropCounter.toLong(),
        hasDroppedBoosterThisSession = field.hasDroppedBoosterThisSession,
        timedEffectBooster = field.timedEffectBooster?.name,
        timedEffectRemainingMs = field.timedEffectRemainingMs.toLong(),
        timedEffectRate = field.timedEffectRate,
        freezeTintEnvelope = field.freezeTintEnvelope,
        icePickArmedFireButton = field.icePickArmedFireButton,
        icePickArmedStashIndex = field.icePickArmedStashIndex?.toLong(),
        shieldActive = field.shieldActive,
    )

internal fun FieldQueries.update(field: Field) =
    updateField(
        level = field.level.toLong(),
        score = field.score.toLong(),
        lifeCount = field.lifeCount.toLong(),
        bonusMultiplier = field.bonusMultiplier.toLong(),
        currentOperationSign = field.currentOperationSign.sign,
        currentOperationDigit = field.currentOperationDigit.toLong(),
        nextOperationSign = field.nextOperationSign.sign,
        nextOperationDigit = field.nextOperationDigit.toLong(),
        isClosed = field.isClosed,
        finishedAt = field.finishedAt,
        gameTimeMs = field.gameTimeMs,
        currentBooster = field.currentBooster?.name,
        nextBooster = field.nextBooster?.name,
        boosterStash = field.boosterStash.joinToString(BOOSTER_STASH_SEPARATOR) { it.name },
        boosterDropCounter = field.boosterDropCounter.toLong(),
        hasDroppedBoosterThisSession = field.hasDroppedBoosterThisSession,
        timedEffectBooster = field.timedEffectBooster?.name,
        timedEffectRemainingMs = field.timedEffectRemainingMs.toLong(),
        timedEffectRate = field.timedEffectRate,
        freezeTintEnvelope = field.freezeTintEnvelope,
        icePickArmedFireButton = field.icePickArmedFireButton,
        icePickArmedStashIndex = field.icePickArmedStashIndex?.toLong(),
        shieldActive = field.shieldActive,
        id = field.id.toLong(),
    )

private fun String.toOperationSignOrNull(): OperationSign? = OperationSign.entries.firstOrNull { it.sign == this }

private fun String.toBoosterOrNull(): Booster? = Booster.entries.firstOrNull { it.name == this }

private fun LocalField.toDomainModel(logger: Logger): Field? {
    val currentSign = currentOperationSign.toOperationSignOrNull()
    val nextSign = nextOperationSign.toOperationSignOrNull()
    if (currentSign == null || nextSign == null) {
        logger.warn("Field $id has an unknown operation sign and is ignored")
        return null
    }
    val stash = boosterStash.split(BOOSTER_STASH_SEPARATOR).mapNotNull { it.toBoosterOrNull() }
    return Field(
        id = id.toIntClamped(),
        level = level.toIntClamped().coerceIn(MIN_LEVEL, MAX_LEVEL),
        score = score.toIntClamped().coerceAtLeast(0),
        lifeCount = lifeCount.toIntClamped().coerceIn(0, INITIAL_LIFE_COUNT),
        bonusMultiplier = bonusMultiplier.toIntClamped().coerceIn(0, MAX_BONUS_MULTIPLIER),
        currentOperationSign = currentSign,
        currentOperationDigit = currentOperationDigit.toIntClamped(),
        nextOperationSign = nextSign,
        nextOperationDigit = nextOperationDigit.toIntClamped(),
        isClosed = isClosed,
        finishedAt = finishedAt,
        gameTimeMs = gameTimeMs,
        currentBooster = currentBooster?.toBoosterOrNull(),
        nextBooster = nextBooster?.toBoosterOrNull(),
        boosterStash = stash,
        boosterDropCounter = boosterDropCounter.toIntClamped().coerceAtLeast(0),
        hasDroppedBoosterThisSession = hasDroppedBoosterThisSession,
        timedEffectBooster = timedEffectBooster?.toBoosterOrNull(),
        timedEffectRemainingMs = timedEffectRemainingMs.toIntClamped().coerceAtLeast(0),
        timedEffectRate = timedEffectRate.coerceIn(MIN_EFFECT_RATE, MAX_EFFECT_RATE),
        freezeTintEnvelope = freezeTintEnvelope.coerceIn(0.0, 1.0),
        icePickArmedFireButton = icePickArmedFireButton,
        icePickArmedStashIndex = icePickArmedStashIndex?.toIntClamped()?.takeIf { it in stash.indices },
        shieldActive = shieldActive,
    )
}
