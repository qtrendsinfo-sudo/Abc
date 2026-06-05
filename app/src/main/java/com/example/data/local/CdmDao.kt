package com.example.data.local

import androidx.room.*
import com.example.data.model.CdmMachine
import kotlinx.coroutines.flow.Flow

@Dao
interface CdmDao {

    @Query("SELECT * FROM cdm_machines ORDER BY id ASC")
    fun getAllCdmMachinesFlow(): Flow<List<CdmMachine>>

    @Query("SELECT * FROM cdm_machines WHERE id = :id")
    suspend fun getCdmMachineById(id: Int): CdmMachine?

    @Query("SELECT * FROM cdm_machines WHERE isFavorite = 1")
    fun getFavoriteMachinesFlow(): Flow<List<CdmMachine>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCdmMachines(machines: List<CdmMachine>)

    @Update
    suspend fun updateCdmMachine(machine: CdmMachine)

    @Query("SELECT COUNT(*) FROM cdm_machines")
    suspend fun countMachines(): Int
}
