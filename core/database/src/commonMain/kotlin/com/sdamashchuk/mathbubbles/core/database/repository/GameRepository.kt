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
}
