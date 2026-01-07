package com.clientledger.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.clientledger.app.data.dao.AppointmentDao
import com.clientledger.app.data.dao.ClientDao
import com.clientledger.app.data.dao.ExpenseDao
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.entity.ExpenseEntity

@Database(
    entities = [ClientEntity::class, AppointmentEntity::class, ExpenseEntity::class],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun expenseDao(): ExpenseDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "client_ledger_database"
                )
                .fallbackToDestructiveMigration() // Разрешаем автоматическое пересоздание БД при изменении схемы
                .build()
                INSTANCE = instance
                
                instance
            }
        }
    }
}


