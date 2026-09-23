package com.sdomashchuk.mathclicker.core.database.repository

import com.sdomashchuk.mathclicker.core.model.Field
import com.sdomashchuk.mathclicker.core.model.Target

interface GameRepository {
    suspend fun insertField(field: Field)

    suspend fun updateField(field: Field)

    suspend fun getUnfinishedField(): Field?

    suspend fun getFieldCount(): Int

    suspend fun updateTargets(targets: List<Target>)

    suspend fun getTargets(): List<Target>

    suspend fun refreshTargets(targets: List<Target>)
}
