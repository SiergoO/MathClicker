package com.sdamashchuk.mathbubbles.feature.menu

import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.database.repository.PersistenceException
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target

class ThrowingGameRepository(
    var failReads: Boolean = false,
    var failWrites: Boolean = false,
    private val unfinishedField: Field? = null,
) : GameRepository {
    override suspend fun insertField(field: Field) = Unit

    override suspend fun updateField(field: Field) {
        if (failWrites) throw PersistenceException("update")
    }

    override suspend fun getUnfinishedField(): Field? {
        if (failReads) throw PersistenceException("read")
        return unfinishedField
    }

    override suspend fun getNextFieldId(): Int = 1

    override suspend fun getRecentClosedFields(): List<Field> = emptyList()

    override suspend fun getBestClosedField(): Field? = null

    override suspend fun updateTargets(targets: List<Target>) = Unit

    override suspend fun getTargets(): List<Target> = emptyList()

    override suspend fun refreshTargets(targets: List<Target>) = Unit

    override suspend fun saveFieldAndTargets(
        field: Field,
        targets: List<Target>,
        replaceTargets: Boolean,
    ) = Unit
}
