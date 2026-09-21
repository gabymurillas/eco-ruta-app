package com.ecoruta.dispositivo.data.local.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** A position waiting to be delivered to the backend. Rows are deleted once acknowledged. */
@Entity(
    tableName = "pending_positions",
    indices = [Index(value = ["captured_at_ms"])],
)
data class PendingPositionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "unit_id") val unitId: String,
    @ColumnInfo(name = "trip_id") val tripId: String,
    val latitude: Double,
    val longitude: Double,
    @ColumnInfo(name = "speed_kmh") val speedKmh: Float,
    @ColumnInfo(name = "accuracy_m") val accuracyMeters: Float,
    @ColumnInfo(name = "captured_at_ms") val capturedAtMs: Long,
)
