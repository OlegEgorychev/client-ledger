package com.clientledger.app

import android.app.Application
import com.clientledger.app.data.database.AppDatabase
import com.clientledger.app.data.repository.LedgerRepository

class LedgerApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        LedgerRepository(
            database.clientDao(),
            database.appointmentDao(),
            database.expenseDao()
        )
    }
}
