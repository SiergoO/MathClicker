package com.sdamashchuk.mathbubbles.core.database.repository

import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target

interface GameRepository {
    suspend fun insertField(field: Field)

    suspend fun updateField(field: Field)

    suspend fun getUnfinishedField(): Field?

    suspend fun getFieldCount(): Int

    suspend fun getRecentClosedFields(): List<Field>

    suspend fun getBestClosedField(): Field?

    suspend fun updateTargets(targets: List<Target>)

    suspend fun getTargets(): List<Target>

    suspend fun refreshTargets(targets: List<Target>)

    // The field and its targets land in one transaction, so a kill between the two writes can
    // never leave a field paired with a target set it never actually had.
    suspend fun saveFieldAndTargets(
        field: Field,
        targets: List<Target>,
        replaceTargets: Boolean,
    )
}
