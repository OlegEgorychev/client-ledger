package com.clientledger.app.data.testdata

import com.clientledger.app.data.entity.AppointmentEntity
import com.clientledger.app.data.entity.AppointmentStatus
import com.clientledger.app.data.entity.ClientEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/**
 * Test Data Generator for Statistics Validation
 * 
 * INTERNAL TEST DATA MODE (DEBUG ONLY)
 * =====================================
 * 
 * This module provides deterministic test data for validating statistics calculations.
 * Test data is ONLY available in DEBUG builds and is NOT visible to end users.
 * 
 * HOW TO USE:
 * -----------
 * 1. Build the app in DEBUG mode
 * 2. Navigate to Calendar screen
 * 3. Scroll to bottom and tap on "версия X.Y.Z" text (long press)
 * 4. Dialog will appear with options:
 *    - "Generate" - Populates database with test data
 *    - "Clear All" - Removes all test data
 * 
 * TEST DATA STRUCTURE:
 * --------------------
 * - 30 clients: "Client 1" through "Client 30"
 * - December 1-31, 2025 (full month)
 * - January 1-7, 2026
 * 
 * DISTRIBUTION:
 * -------------
 * December:
 * - Weekdays: 4 visits/day
 * - Weekends: 2 visits/day
 * - Base price: 2,000₽
 * - Every 5th visit: Premium = 3,000₽
 * - Every 10th visit: CANCELED (status = CANCELED, income = 0)
 * 
 * January 1-7:
 * - 2 visits/day
 * - Base price: 2,500₽
 * - Jan 3, first visit: CANCELED
 * 
 * EXPECTED STATISTICS (Reference Values):
 * ----------------------------------------
 * December (Month):
 * - Total income: Highest month
 * - Best day: Any weekday with 4 visits + premium
 * - Best client: Client 1 (most frequent)
 * - Cancellation rate: ~10%
 * - Top 8 clients: ~65-75% of total income
 * 
 * January 1-7:
 * - Total income: Lower than December
 * - Cancellations: Exactly 1 (Jan 3)
 * - Visits per day: Flat 2/day
 * 
 * Day mode (December weekday):
 * - 4 visits
 * - Income: ~8,000-9,000₽ (depending on premium visits)
 * 
 * Day mode (December weekend):
 * - 2 visits
 * - Income: ~4,000-6,000₽
 * 
 * Day mode (Jan 3):
 * - 1 canceled + 1 completed
 * - Income: 2,500₽ (only completed visit)
 * 
 * CLEANUP:
 * --------
 * All test data is marked with isTestData = true flag.
 * Use clearAllTestData() to remove all test data in one action.
 * This restores a clean app state.
 */
object TestDataGenerator {
    
    private const val BASE_PRICE_DECEMBER = 2000_00L // 2000₽ in cents
    private const val PREMIUM_PRICE_DECEMBER = 3000_00L // 3000₽ in cents
    private const val BASE_PRICE_JANUARY = 2500_00L // 2500₽ in cents
    
    private val DECEMBER_START = LocalDate.of(2025, 12, 1)
    private val DECEMBER_END = LocalDate.of(2025, 12, 31)
    private val JANUARY_START = LocalDate.of(2026, 1, 1)
    private val JANUARY_END = LocalDate.of(2026, 1, 7)
    
    /**
     * Generate all test clients (30 clients)
     */
    fun generateClients(): List<ClientEntity> {
        val now = System.currentTimeMillis()
        return (1..30).map { i ->
            ClientEntity(
                firstName = "Client",
                lastName = i.toString(),
                gender = if (i % 2 == 0) "female" else "male",
                birthDate = null,
                phone = "+7999123456$i".take(12), // Valid phone format
                telegram = null,
                notes = if (i <= 3) "Top client" else if (i <= 15) "Mid client" else "Long tail",
                isTestData = true,
                createdAt = now - (30 - i) * 86400000L, // Staggered creation dates
                updatedAt = now
            )
        }
    }
    
    /**
     * Generate appointments for December (full month)
     * @param clientIdMap: Map from client index (1-30) to actual client ID
     */
    fun generateDecemberAppointments(clientIdMap: Map<Int, Long>): List<AppointmentEntity> {
        val appointments = mutableListOf<AppointmentEntity>()
        var visitCounter = 0
        val now = System.currentTimeMillis()
        
        var currentDate = DECEMBER_START
        while (!currentDate.isAfter(DECEMBER_END)) {
            val isWeekend = currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY
            val visitsPerDay = if (isWeekend) 2 else 4
            
            repeat(visitsPerDay) { visitIndex ->
                visitCounter++
                val clientIndex = ((visitCounter - 1) % 30) + 1 // 1-30
                val clientId = clientIdMap[clientIndex] ?: 1L
                
                // Every 5th visit is premium
                val isPremium = visitCounter % 5 == 0
                // Every 10th visit is canceled
                val isCanceled = visitCounter % 10 == 0
                
                val price = if (isPremium && !isCanceled) {
                    PREMIUM_PRICE_DECEMBER
                } else if (!isCanceled) {
                    BASE_PRICE_DECEMBER
                } else {
                    0L // Canceled visits don't contribute to income
                }
                
                val status = if (isCanceled) {
                    AppointmentStatus.CANCELED.name
                } else {
                    AppointmentStatus.COMPLETED.name
                }
                
                val startTime = LocalTime.of(9 + visitIndex * 2, 0) // Staggered times: 9:00, 11:00, 13:00, 15:00
                val startsAt = currentDate.atTime(startTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                appointments.add(
                    AppointmentEntity(
                        clientId = clientId,
                        title = if (isPremium) "Premium Service" else "Standard Service",
                        startsAt = startsAt,
                        dateKey = currentDate.toString(),
                        durationMinutes = 60,
                        incomeCents = price,
                        isPaid = !isCanceled,
                        status = status,
                        canceledAt = if (isCanceled) startsAt else null,
                        cancelReason = if (isCanceled) "Test cancellation" else null,
                        isTestData = true,
                        createdAt = now - (DECEMBER_END.toEpochDay() - currentDate.toEpochDay()) * 86400000L
                    )
                )
            }
            
            currentDate = currentDate.plusDays(1)
        }
        
        return appointments
    }
    
    /**
     * Generate appointments for January 1-7
     * @param clientIdMap: Map from client index (1-30) to actual client ID
     */
    fun generateJanuaryAppointments(clientIdMap: Map<Int, Long>): List<AppointmentEntity> {
        val appointments = mutableListOf<AppointmentEntity>()
        var visitCounter = 0
        val now = System.currentTimeMillis()
        
        var currentDate = JANUARY_START
        while (!currentDate.isAfter(JANUARY_END)) {
            val visitsPerDay = 2
            
            repeat(visitsPerDay) { visitIndex ->
                visitCounter++
                val clientIndex = ((visitCounter - 1) % 30) + 1 // 1-30
                val clientId = clientIdMap[clientIndex] ?: 1L
                
                // Jan 3: first visit is canceled
                val isCanceled = currentDate == LocalDate.of(2026, 1, 3) && visitIndex == 0
                
                val price = if (!isCanceled) {
                    BASE_PRICE_JANUARY
                } else {
                    0L
                }
                
                val status = if (isCanceled) {
                    AppointmentStatus.CANCELED.name
                } else {
                    AppointmentStatus.COMPLETED.name
                }
                
                val startTime = LocalTime.of(10 + visitIndex * 3, 0) // 10:00, 13:00
                val startsAt = currentDate.atTime(startTime)
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                appointments.add(
                    AppointmentEntity(
                        clientId = clientId,
                        title = "Standard Service",
                        startsAt = startsAt,
                        dateKey = currentDate.toString(),
                        durationMinutes = 60,
                        incomeCents = price,
                        isPaid = !isCanceled,
                        status = status,
                        canceledAt = if (isCanceled) startsAt else null,
                        cancelReason = if (isCanceled) "Test cancellation" else null,
                        isTestData = true,
                        createdAt = now - (JANUARY_END.toEpochDay() - currentDate.toEpochDay()) * 86400000L
                    )
                )
            }
            
            currentDate = currentDate.plusDays(1)
        }
        
        return appointments
    }
    
    /**
     * Generate all test data (clients + appointments)
     * Note: Appointments will need client IDs to be updated after clients are inserted
     */
    fun generateAllTestData(): TestDataResult {
        val clients = generateClients()
        
        // Generate appointments with placeholder client IDs (1-30)
        // These will be mapped to actual IDs after clients are inserted
        val placeholderClientIdMap = (1..30).associateWith { it.toLong() }
        val decemberAppointments = generateDecemberAppointments(placeholderClientIdMap)
        val januaryAppointments = generateJanuaryAppointments(placeholderClientIdMap)
        
        return TestDataResult(
            clients = clients,
            decemberAppointments = decemberAppointments,
            januaryAppointments = januaryAppointments
        )
    }
}

data class TestDataResult(
    val clients: List<ClientEntity>,
    val decemberAppointments: List<AppointmentEntity>,
    val januaryAppointments: List<AppointmentEntity>
)

