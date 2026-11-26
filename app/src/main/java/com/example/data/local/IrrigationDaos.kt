package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ZoneDao {
    @Query("SELECT * FROM irrigation_zones ORDER BY channelNumber ASC, id ASC")
    fun observeAllZones(): Flow<List<IrrigationZoneEntity>>

    @Query("SELECT * FROM irrigation_zones ORDER BY channelNumber ASC, id ASC")
    suspend fun getAllZonesOnce(): List<IrrigationZoneEntity>

    @Query("SELECT * FROM irrigation_zones WHERE id = :id")
    suspend fun getZoneById(id: Int): IrrigationZoneEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZone(zone: IrrigationZoneEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertZones(zones: List<IrrigationZoneEntity>)

    @Update
    suspend fun updateZone(zone: IrrigationZoneEntity)

    @Delete
    suspend fun deleteZone(zone: IrrigationZoneEntity)
}

@Dao
interface ScheduleDao {
    @Query("SELECT * FROM irrigation_schedules ORDER BY hour ASC, minute ASC")
    fun observeAllSchedules(): Flow<List<IrrigationScheduleEntity>>

    @Query("SELECT * FROM irrigation_schedules ORDER BY hour ASC, minute ASC")
    suspend fun getAllSchedulesOnce(): List<IrrigationScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedule(schedule: IrrigationScheduleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(schedules: List<IrrigationScheduleEntity>)

    @Update
    suspend fun updateSchedule(schedule: IrrigationScheduleEntity)

    @Delete
    suspend fun deleteSchedule(schedule: IrrigationScheduleEntity)

    @Query("DELETE FROM irrigation_schedules WHERE zoneId = :zoneId")
    suspend fun deleteByZoneId(zoneId: Int)
}

@Dao
interface WateringLogDao {
    @Query("SELECT * FROM watering_logs ORDER BY startedAt DESC LIMIT 150")
    fun observeRecentLogs(): Flow<List<WateringLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: WateringLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLogs(logs: List<WateringLogEntity>)

    @Query("DELETE FROM watering_logs")
    suspend fun clearAllLogs()
}

@Dao
interface ControllerConfigDao {
    @Query("SELECT * FROM controller_config WHERE id = 1")
    fun observeConfig(): Flow<ControllerConfigEntity?>

    @Query("SELECT * FROM controller_config WHERE id = 1")
    suspend fun getConfigOnce(): ControllerConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertConfig(config: ControllerConfigEntity)
}
