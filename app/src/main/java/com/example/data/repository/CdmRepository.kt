package com.example.data.repository

import android.util.Log
import com.example.data.local.CdmDao
import com.example.data.model.CdmDataProvider
import com.example.data.model.CdmMachine
import com.example.data.pref.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CdmRepository(
    private val cdmDao: CdmDao,
    private val settingsManager: SettingsManager
) {
    companion object {
        private const val TAG = "CdmRepository"
    }

    val allCdmMachines: Flow<List<CdmMachine>> = cdmDao.getAllCdmMachinesFlow()
    val favoriteMachines: Flow<List<CdmMachine>> = cdmDao.getFavoriteMachinesFlow()

    suspend fun initializeDatabaseIfNeeded() = withContext(Dispatchers.IO) {
        try {
            val count = cdmDao.countMachines()
            if (count == 0) {
                Log.d(TAG, "Pre-populating database with 131 Talabat Cash Deposit Machines...")
                val initialList = CdmDataProvider.getInitialMachines()
                cdmDao.insertCdmMachines(initialList)
                Log.d(TAG, "Successfully populated ${initialList.size} machines.")
            } else {
                Log.d(TAG, "Database already contains $count machines. Skipping pre-population.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error pre-populating database", e)
        }
    }

    suspend fun toggleFavorite(machineId: Int) = withContext(Dispatchers.IO) {
        val machine = cdmDao.getCdmMachineById(machineId) ?: return@withContext
        val updated = machine.copy(isFavorite = !machine.isFavorite)
        cdmDao.updateCdmMachine(updated)
    }

    suspend fun updateNotes(machineId: Int, notes: String) = withContext(Dispatchers.IO) {
        val machine = cdmDao.getCdmMachineById(machineId) ?: return@withContext
        val updated = machine.copy(notes = notes)
        cdmDao.updateCdmMachine(updated)
    }

    suspend fun updateMachineStatus(machineId: Int, status: String, reportType: String) = withContext(Dispatchers.IO) {
        val machine = cdmDao.getCdmMachineById(machineId) ?: return@withContext
        val updated = machine.copy(
            status = status,
            lastReportType = reportType,
            lastReportTime = System.currentTimeMillis()
        )
        cdmDao.updateCdmMachine(updated)
    }

    suspend fun searchMachines(query: String): List<CdmMachine> = withContext(Dispatchers.IO) {
        val all = allCdmMachines.first()
        if (query.isBlank()) return@withContext all
        val trimmed = query.lowercase().trim()
        return@withContext all.filter {
            it.merchantName.lowercase().contains(trimmed) ||
            it.branchName.lowercase().contains(trimmed) ||
            it.terminalId.lowercase().contains(trimmed) ||
            it.id.toString() == trimmed
        }
    }
}
