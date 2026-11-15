package com.clientledger.app.data.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context
import com.clientledger.app.data.dao.AppointmentDao
import com.clientledger.app.data.dao.AppointmentServiceDao
import com.clientledger.app.data.dao.ClientDao
import com.clientledger.app.data.dao.ExpenseDao
import com.clientledger.app.data.dao.ExpenseItemDao
import com.clientledger.app.data.dao.PersonalEventDao
import com.clientledger.app.data.dao.ServiceTagDao
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.AppointmentServiceEntity
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.data.entity.ExpenseItemEntity
import com.clientledger.app.data.entity.PersonalEventEntity
import com.clientledger.app.data.entity.ServiceTagEntity

@Database(
    entities = [
        ClientEntity::class, 
        AppointmentEntity::class, 
        ExpenseEntity::class,
        ExpenseItemEntity::class,
        ServiceTagEntity::class,
        AppointmentServiceEntity::class,
        PersonalEventEntity::class
    ],
    version = 10,
    exportSchema = true,
    autoMigrations = [
        // 9 -> 10: новая таблица personal_events (личные дела). Данные не трогаются.
        AutoMigration(from = 9, to = 10)
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun clientDao(): ClientDao
    abstract fun appointmentDao(): AppointmentDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun expenseItemDao(): ExpenseItemDao
    abstract fun serviceTagDao(): ServiceTagDao
    abstract fun appointmentServiceDao(): AppointmentServiceDao
    abstract fun personalEventDao(): PersonalEventDao

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
                // ВАЖНО: начиная с версии 9 база больше НЕ пересоздаётся при изменении схемы.
                // Любое изменение сущностей = повысить version и добавить Migration(N, N+1)
                // через .addMigrations(...). Схемы каждой версии сохраняются в app/schemas.
                // Пересоздание разрешено только для старых версий 1–8 (схемы которых не сохранились).
                .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8)
                .build()
                INSTANCE = instance
                
                instance
            }
        }
    }
}


