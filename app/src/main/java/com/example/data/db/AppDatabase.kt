package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.BookingEntity
import com.example.data.model.CompetitionEntity
import com.example.data.model.CourseEntity
import com.example.data.model.FlightHistoryEntity
import com.example.data.model.LessonSlotEntity
import com.example.data.model.StudentEntity

@Database(
    entities = [
        CourseEntity::class,
        CompetitionEntity::class,
        FlightHistoryEntity::class,
        StudentEntity::class,
        LessonSlotEntity::class,
        BookingEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun paramoteurDao(): ParamoteurDao
    abstract fun planningDao(): PlanningDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "paramoteur_db"
                ).fallbackToDestructiveMigration()
                 .fallbackToDestructiveMigrationOnDowngrade()
                 .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
