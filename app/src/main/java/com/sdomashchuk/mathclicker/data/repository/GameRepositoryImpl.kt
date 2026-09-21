package com.sdomashchuk.mathclicker.data.repository

import com.sdomashchuk.mathclicker.core.model.Field
import com.sdomashchuk.mathclicker.core.model.Target
import com.sdomashchuk.mathclicker.data.database.dao.FieldDao
import com.sdomashchuk.mathclicker.data.database.dao.TargetsDao
import com.sdomashchuk.mathclicker.domain.repository.GameRepository

class GameRepositoryImpl(
    private val fieldDao: FieldDao,
    private val targetsDao: TargetsDao,
) : GameRepository {
    override suspend fun insertField(field: Field) {
        fieldDao.insertField(field)
    }

    override suspend fun updateField(field: Field) {
        fieldDao.updateField(field)
    }

    override suspend fun getFieldById(id: Int): Field = fieldDao.getFieldById(id)

    override suspend fun getUnfinishedField(): Field? = fieldDao.getUnfinishedField()

    override suspend fun getFieldCount(): Int = fieldDao.getFieldCount()

    override suspend fun insertTarget(target: Target) {
        targetsDao.insertTarget(target)
    }

    override suspend fun insertTargets(targets: List<Target>) {
        targetsDao.insertTargets(targets)
    }

    override suspend fun updateTarget(target: Target) {
        targetsDao.updateTarget(target)
    }

    override suspend fun updateTargets(targets: List<Target>) {
        targetsDao.updateTargets(targets)
    }

    override suspend fun getTargets(): List<Target> = targetsDao.getTargets()

    override suspend fun refreshTargets(targets: List<Target>) {
        targetsDao.deleteTargets()
        insertTargets(targets)
    }

    override suspend fun deleteAllTargets() {
        targetsDao.deleteTargets()
    }
}
