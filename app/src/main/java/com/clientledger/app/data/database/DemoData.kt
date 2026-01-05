package com.clientledger.app.data.database

import com.clientledger.app.data.dao.AppointmentDao
import com.clientledger.app.data.dao.ClientDao
import com.clientledger.app.data.dao.ExpenseDao
import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.ClientEntity
import com.clientledger.app.data.entity.ExpenseEntity
import com.clientledger.app.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

suspend fun insertDemoData(
    clientDao: ClientDao,
    appointmentDao: AppointmentDao,
    expenseDao: ExpenseDao
) = withContext(Dispatchers.IO) {
    // Проверяем, есть ли уже данные (простая проверка через count)
    val existingClient = clientDao.getClientByPhone("+79001234567")
    if (existingClient != null) return@withContext
    
    val now = System.currentTimeMillis()
    val today = LocalDate.now()
    
    // Создаём демо-клиентов
    val client1Id = clientDao.insertClient(
        ClientEntity(
            firstName = "Иван",
            lastName = "Петров",
            gender = "male",
            birthDate = "1990-05-15",
            phone = "+79001234567",
            telegram = "@ivan_petrov",
            notes = "Постоянный клиент",
            createdAt = now,
            updatedAt = now
        )
    )
    
    val client2Id = clientDao.insertClient(
        ClientEntity(
            firstName = "Мария",
            lastName = "Иванова",
            gender = "female",
            birthDate = "1985-08-20",
            phone = "+79009876543",
            telegram = "@maria_ivanova",
            notes = null,
            createdAt = now,
            updatedAt = now
        )
    )
    
    // Создаём демо-записи
    val date1 = today.minusDays(2)
    val date2 = today.minusDays(1)
    
    appointmentDao.insertAppointment(
        AppointmentEntity(
            clientId = client1Id,
            title = "Стрижка",
            startsAt = date1.atTime(LocalTime.of(10, 0)).toMillis(),
            dateKey = date1.toDateKey(),
            durationMinutes = 60, // 1 час
            incomeCents = 150000, // 1500 ₽
            isPaid = true,
            createdAt = now
        )
    )
    
    appointmentDao.insertAppointment(
        AppointmentEntity(
            clientId = client2Id,
            title = "Окрашивание",
            startsAt = date1.atTime(LocalTime.of(14, 0)).toMillis(),
            dateKey = date1.toDateKey(),
            durationMinutes = 120, // 2 часа
            incomeCents = 300000, // 3000 ₽
            isPaid = true,
            createdAt = now
        )
    )
    
    appointmentDao.insertAppointment(
        AppointmentEntity(
            clientId = client1Id,
            title = "Стрижка",
            startsAt = date2.atTime(LocalTime.of(11, 0)).toMillis(),
            dateKey = date2.toDateKey(),
            durationMinutes = 60, // 1 час
            incomeCents = 150000, // 1500 ₽
            isPaid = true,
            createdAt = now
        )
    )
    
    // Создаём демо-расходы
    expenseDao.insertExpense(
        ExpenseEntity(
            title = "Такси",
            spentAt = date1.atTime(LocalTime.of(8, 30)).toMillis(),
            dateKey = date1.toDateKey(),
            amountCents = 50000, // 500 ₽
            createdAt = now
        )
    )
    
    expenseDao.insertExpense(
        ExpenseEntity(
            title = "Материалы",
            spentAt = date1.atTime(LocalTime.of(9, 0)).toMillis(),
            dateKey = date1.toDateKey(),
            amountCents = 200000, // 2000 ₽
            createdAt = now
        )
    )
}


