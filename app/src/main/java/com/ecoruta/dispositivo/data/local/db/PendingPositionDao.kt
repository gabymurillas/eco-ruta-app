package com.ecoruta.dispositivo.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingPositionDao {

    @Insert
    suspend fun insert(position: PendingPositionEntity)

    /** Oldest first, so the backend receives positions in capture order. */
    @Query("SELECT * FROM pending_positions ORDER BY captured_at_ms ASC, id ASC LIMIT :limit")
    suspend fun oldest(limit: Int): List<PendingPositionEntity>

    @Query("DELETE FROM pending_positions WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM pending_positions WHERE captured_at_ms < :thresholdMs")
    suspend fun deleteCapturedBefore(thresholdMs: Long)

    @Query("SELECT COUNT(*) FROM pending_positions")
    fun observeCount(): Flow<Int>
}
