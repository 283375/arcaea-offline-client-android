package xyz.sevive.arcaeaoffline.core.database.repositories

import kotlinx.coroutines.flow.Flow
import xyz.sevive.arcaeaoffline.core.database.daos.ScoreCalculatedDao
import xyz.sevive.arcaeaoffline.core.database.entities.ScoreCalculated

interface ScoreCalculatedRepository {
    fun find(songId: String, ratingClass: Int): Flow<ScoreCalculated?>
    fun findAll(): Flow<List<ScoreCalculated>>
    fun findAllBySongId(songId: String): Flow<List<ScoreCalculated>>
}

class ScoreCalculatedRepositoryImpl(private val dao: ScoreCalculatedDao) :
    ScoreCalculatedRepository {
    override fun find(songId: String, ratingClass: Int): Flow<ScoreCalculated?> =
        dao.find(songId, ratingClass)

    override fun findAll(): Flow<List<ScoreCalculated>> = dao.findAll()

    override fun findAllBySongId(songId: String): Flow<List<ScoreCalculated>> =
        dao.findAllBySongId(songId)
}

