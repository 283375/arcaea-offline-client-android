package xyz.sevive.arcaeaoffline.core.database.daos

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import xyz.sevive.arcaeaoffline.core.constants.ArcaeaScoreRatingClass
import xyz.sevive.arcaeaoffline.core.database.entities.Chart

@Dao
interface ChartDao {
    @Query("SELECT * FROM charts WHERE song_id = :songId AND rating_class = :ratingClass")
    fun find(songId: String, ratingClass: ArcaeaScoreRatingClass): Flow<Chart>

    @Query("SELECT * FROM charts")
    fun findAll(): Flow<List<Chart>>

    @Query("SELECT * FROM charts WHERE song_id = :songId")
    fun findAllBySongId(songId: String): Flow<List<Chart>>
}
