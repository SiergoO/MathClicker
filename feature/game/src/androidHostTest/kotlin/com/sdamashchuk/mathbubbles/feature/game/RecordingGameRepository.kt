package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target

private const val RECENT_RESULT_COUNT = 7

class RecordingGameRepository(
    initialFields: List<Field> = emptyList(),
    private var targets: List<Target> = emptyList(),
) : GameRepository {
    val fields = initialFields.associateBy { it.id }.toSortedMap()
    val fieldWrites = mutableListOf<Field>()
    var recentReads = 0
        private set

    override suspend fun insertField(field: Field) {
        fields[field.id] = field
        fieldWrites += field
    }

    override suspend fun updateField(field: Field) {
        if (field.id in fields) fields[field.id] = field
        fieldWrites += field
    }

    override suspend fun getUnfinishedField(): Field? = fields.values.lastOrNull { !it.isClosed }

    override suspend fun getNextFieldId(): Int = (fields.keys.maxOrNull() ?: 0) + 1

    override suspend fun getRecentClosedFields(): List<Field> {
        recentReads++
        return fields.values
            .filter { it.isClosed }
            .sortedByDescending { it.id }
            .take(RECENT_RESULT_COUNT)
    }

    override suspend fun getBestClosedField(): Field? = fields.values.filter { it.isClosed }.maxByOrNull { it.score }

    override suspend fun updateTargets(targets: List<Target>) {
        this.targets = targets
    }

    override suspend fun getTargets(): List<Target> = targets

    override suspend fun refreshTargets(targets: List<Target>) {
        this.targets = targets
    }

    override suspend fun saveFieldAndTargets(
        field: Field,
        targets: List<Target>,
        replaceTargets: Boolean,
    ) {
        updateField(field)
        if (replaceTargets) refreshTargets(targets) else updateTargets(targets)
    }
}
