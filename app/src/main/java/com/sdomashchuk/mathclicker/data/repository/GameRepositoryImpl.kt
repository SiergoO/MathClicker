package com.sdomashchuk.mathclicker.data.repository

import com.sdomashchuk.mathclicker.data.database.dao.FieldDao
import com.sdomashchuk.mathclicker.data.database.dao.TargetsDao
import com.sdomashchuk.mathclicker.data.database.entity.toDataModel
import com.sdomashchuk.mathclicker.data.database.entity.toDomainModel
import com.sdomashchuk.mathclicker.domain.model.game.session.Field
import com.sdomashchuk.mathclicker.domain.model.game.session.Target
import com.sdomashchuk.mathclicker.domain.repository.GameRepository

class GameRepositoryImpl(
    private val fieldDao: FieldDao,
    private val targetsDao: TargetsDao
) : GameRepository {

    override suspend fun insertField(field: Field) {
        fieldDao.insertField(field.toDataModel())
    }

    override suspend fun updateField(field: Field) {
        fieldDao.updateField(field.toDataModel())
    }

    override suspend fun getFieldById(id: Int): Field {
        return fieldDao.getFieldById(id).toDomainModel()
    }

    override suspend fun getUnfinishedField(): Field? {
        return fieldDao.getUnfinishedField()?.toDomainModel()
    }

    override suspend fun getFieldCount(): Int {
        return fieldDao.getFieldCount()
    }

    override suspend fun insertTarget(target: Target) {
        targetsDao.insertTarget(target.toDataModel())
    }

    override suspend fun insertTargets(targets: List<Target>) {
        targetsDao.insertTargets(targets.map { it.toDataModel() })
    }

    override suspend fun updateTarget(target: Target) {
        targetsDao.updateTarget(target.toDataModel())
    }

    override suspend fun updateTargets(targets: List<Target>) {
        targetsDao.updateTargets(targets.map { it.toDataModel() })
    }

    override suspend fun getTargets(): List<Target> {
        return targetsDao.getTargets().map { it.toDomainModel() }
    }

    override suspend fun refreshTargets(targets: List<Target>) {
        targetsDao.deleteTargets()
        insertTargets(targets)
    }

    override suspend fun deleteAllTargets() {
        targetsDao.deleteTargets()
    }
}

