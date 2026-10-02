package com.sdamashchuk.mathbubbles.feature.menu

import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target

class FakeGameRepository(
    private var unfinishedField: Field? = null,
) : GameRepository {
    val updateFieldCalls = mutableListOf<Field>()

    override suspend fun insertField(field: Field) = Unit

    override suspend fun updateField(field: Field) {
        updateFieldCalls += field
        unfinishedField = if (field.isClosed) null else field
    }

    override suspend fun getUnfinishedField(): Field? = unfinishedField

    override suspend fun getFieldCount(): Int = 0

    override suspend fun getRecentClosedFields(): List<Field> = emptyList()

    override suspend fun getBestClosedField(): Field? = null

    override suspend fun updateTargets(targets: List<Target>) = Unit

    override suspend fun getTargets(): List<Target> = emptyList()

    override suspend fun refreshTargets(targets: List<Target>) = Unit

    override suspend fun saveFieldAndTargets(
        field: Field,
        targets: List<Target>,
        replaceTargets: Boolean,
    ) {
        updateField(field)
        if (replaceTargets) refreshTargets(targets) else updateTargets(targets)
    }
}
