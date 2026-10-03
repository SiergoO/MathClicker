package com.sdamashchuk.mathbubbles.feature.game

import com.sdamashchuk.mathbubbles.core.database.repository.GameRepository
import com.sdamashchuk.mathbubbles.core.database.repository.PersistenceException
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target

class FlakyGameRepository : GameRepository {
    val fields = sortedMapOf<Int, Field>()
    var failUnfinishedFieldRead = false
    var failTargetsRead = false
    var failNextIdRead = false
    var failInsert = false
    var failWrites = false

    var insertedFields = 0
    var updatedFields = 0

    override suspend fun insertField(field: Field) {
        if (failInsert) throw PersistenceException("insert")
        if (field.id in fields) throw PersistenceException("duplicate id ${field.id}")
        fields[field.id] = field
        insertedFields++
    }

    override suspend fun updateField(field: Field) {
        if (failWrites) throw PersistenceException("update")
        if (field.id in fields) fields[field.id] = field
        updatedFields++
    }

    override suspend fun getUnfinishedField(): Field? {
        if (failUnfinishedFieldRead) throw PersistenceException("read field")
        return fields.values.lastOrNull { !it.isClosed }
    }

    override suspend fun getNextFieldId(): Int {
        if (failNextIdRead) throw PersistenceException("read id")
        return (fields.keys.maxOrNull() ?: 0) + 1
    }

    override suspend fun getRecentClosedFields(): List<Field> = emptyList()

    override suspend fun getBestClosedField(): Field? = null

    override suspend fun updateTargets(targets: List<Target>) {
        if (failWrites) throw PersistenceException("update targets")
    }

    override suspend fun getTargets(): List<Target> {
        if (failTargetsRead) throw PersistenceException("read targets")
        return emptyList()
    }

    override suspend fun refreshTargets(targets: List<Target>) {
        if (failWrites) throw PersistenceException("refresh targets")
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
