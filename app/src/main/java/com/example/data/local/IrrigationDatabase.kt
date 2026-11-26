package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        IrrigationZoneEntity::class,
        IrrigationScheduleEntity::class,
        WateringLogEntity::class,
        ControllerConfigEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class IrrigationDatabase : RoomDatabase() {
    abstract fun zoneDao(): ZoneDao
    abstract fun scheduleDao(): ScheduleDao
    abstract fun wateringLogDao(): WateringLogDao
    abstract fun controllerConfigDao(): ControllerConfigDao

    companion object {
        @Volatile
        private var INSTANCE: IrrigationDatabase? = null

        fun getInstance(context: Context): IrrigationDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    IrrigationDatabase::class.java,
                    "irigasiku_db"
                ).fallbackToDestructiveMigration(true).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
