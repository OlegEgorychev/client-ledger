package com.clientledger.app.data.dao

import androidx.room.*
import com.clientledger.app.data.entity.ClientEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientDao {
    @Query("SELECT * FROM clients ORDER BY lastName, firstName")
    fun getAllClients(): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE id = :id")
    suspend fun getClientById(id: Long): ClientEntity?

    @Query(
        """
        SELECT * FROM clients 
        WHERE LOWER(firstName) LIKE LOWER(:query) || '%' 
           OR LOWER(lastName) LIKE LOWER(:query) || '%'
        ORDER BY lastName, firstName
        """
    )
    fun searchClients(query: String): Flow<List<ClientEntity>>

    @Query("SELECT * FROM clients WHERE phone = :phone LIMIT 1")
    suspend fun getClientByPhone(phone: String): ClientEntity?

    @Query(
        """
        SELECT * FROM clients 
        WHERE LOWER(TRIM(firstName)) = LOWER(TRIM(:firstName))
          AND LOWER(TRIM(lastName)) = LOWER(TRIM(:lastName))
        LIMIT 1
        """
    )
    suspend fun findClientByName(firstName: String, lastName: String): ClientEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertClient(client: ClientEntity): Long

    @Update
    suspend fun updateClient(client: ClientEntity)

    @Delete
    suspend fun deleteClient(client: ClientEntity)
    
    // Test data management
    @Query("DELETE FROM clients WHERE isTestData = 1")
    suspend fun deleteAllTestClients()
}


