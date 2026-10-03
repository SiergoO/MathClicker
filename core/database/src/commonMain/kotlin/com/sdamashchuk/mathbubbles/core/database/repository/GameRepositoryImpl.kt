package com.sdamashchuk.mathbubbles.core.database.repository

import com.sdamashchuk.mathbubbles.core.database.dao.FieldDao
import com.sdamashchuk.mathbubbles.core.database.dao.TargetsDao
import com.sdamashchuk.mathbubbles.core.database.dao.insert
import com.sdamashchuk.mathbubbles.core.database.dao.update
import com.sdamashchuk.mathbubbles.core.database.local.FieldQueries
import com.sdamashchuk.mathbubbles.core.database.local.TargetsQueries
import com.sdamashchuk.mathbubbles.core.model.Field
import com.sdamashchuk.mathbubbles.core.model.Target
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class GameRepositoryImpl(
    private val fieldDao: FieldDao,
    private val targetsDao: TargetsDao,
    private val fieldQueries: FieldQueries,
    private val targetsQueries: TargetsQueries,
    private val dispatcher: CoroutineDispatcher,
) : GameRepository {
    override suspend fun insertField(field: Field) {
        persistence("insert field ${field.id}") { fieldDao.insertField(field) }
    }

    override suspend fun updateField(field: Field) {
        persistence("update field ${field.id}") { fieldDao.updateField(field) }
    }

    override suspend fun getUnfinishedField(): Field? =
        persistence("read the unfinished field") { fieldDao.getUnfinishedField() }

    override suspend fun getNextFieldId(): Int = persistence("read the next field id") { fieldDao.getNextFieldId() }

    override suspend fun getRecentClosedFields(): List<Field> =
        persistence("read recent results") { fieldDao.getRecentClosedFields() }

    override suspend fun getBestClosedField(): Field? =
        persistence("read the best result") { fieldDao.getBestClosedField() }

    override suspend fun updateTargets(targets: List<Target>) =
        persistence("update targets") { targetsDao.updateTargets(targets) }

    override suspend fun getTargets(): List<Target> = persistence("read targets") { targetsDao.getTargets() }

    override suspend fun refreshTargets(targets: List<Target>) =
        persistence("refresh targets") { targetsDao.refreshTargets(targets) }

    override suspend fun saveFieldAndTargets(
        field: Field,
        targets: List<Target>,
        replaceTargets: Boolean,
    ) = persistence("save field ${field.id} and its targets") {
        withContext(dispatcher) {
            fieldQueries.transaction {
                fieldQueries.update(field)
                if (replaceTargets) {
                    targetsQueries.deleteTargets()
                    targets.forEach { targetsQueries.insert(it) }
                } else {
                    targets.forEach { targetsQueries.update(it) }
                }
            }
        }
    }
}

// Driver failures are platform classes (SQLiteException on Android) that common code cannot name,
// so every non-cancellation failure is rethrown as the one type callers can catch.
@Suppress("TooGenericExceptionCaught")
private suspend fun <T> persistence(
    operation: String,
    block: suspend () -> T,
): T =
    try {
        block()
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (failure: Exception) {
        throw PersistenceException("Could not $operation", failure)
    }
