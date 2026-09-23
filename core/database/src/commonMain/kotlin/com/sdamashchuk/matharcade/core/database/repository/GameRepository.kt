package com.sdamashchuk.matharcade.core.database.repository

import com.sdamashchuk.matharcade.core.model.Field
import com.sdamashchuk.matharcade.core.model.Target

interface GameRepository {
    suspend fun insertField(field: Field)

    suspend fun updateField(field: Field)

    suspend fun getUnfinishedField(): Field?

    suspend fun getFieldCount(): Int

    suspend fun updateTargets(targets: List<Target>)

    suspend fun getTargets(): List<Target>

    suspend fun refreshTargets(targets: List<Target>)
}
