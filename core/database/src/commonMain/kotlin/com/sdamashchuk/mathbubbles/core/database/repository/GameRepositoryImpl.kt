package com.sdamashchuk.mathbubbles.core.database.repository

import com.sdamashchuk.mathbubbles.core.database.dao.FieldDao
import com.sdamashchuk.mathbubbles.core.database.dao.TargetsDao
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target

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

    override suspend fun getUnfinishedField(): Field? = fieldDao.getUnfinishedField()

    override suspend fun getFieldCount(): Int = fieldDao.getFieldCount()

    override suspend fun getRecentClosedFields(): List<Field> = fieldDao.getRecentClosedFields()

    override suspend fun getBestClosedField(): Field? = fieldDao.getBestClosedField()

    override suspend fun updateTargets(targets: List<Target>) {
        targetsDao.updateTargets(targets)
    }

    override suspend fun getTargets(): List<Target> = targetsDao.getTargets()

    override suspend fun refreshTargets(targets: List<Target>) {
        targetsDao.refreshTargets(targets)
    }
}
