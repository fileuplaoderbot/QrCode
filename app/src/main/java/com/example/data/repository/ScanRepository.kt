package com.example.data.repository

import com.example.data.local.ScanDao
import com.example.data.local.ScanEntity
import kotlinx.coroutines.flow.Flow

class ScanRepository(private val scanDao: ScanDao) {
    val allScans: Flow<List<ScanEntity>> = scanDao.getAllScans()
    val favoriteScans: Flow<List<ScanEntity>> = scanDao.getFavoriteScans()

    suspend fun insertScan(scan: ScanEntity): Long = scanDao.insertScan(scan)

    suspend fun insertAll(scans: List<ScanEntity>) = scanDao.insertAll(scans)

    suspend fun findByContent(content: String): ScanEntity? = scanDao.findByContent(content)

    suspend fun toggleFavorite(scan: ScanEntity) {
        scanDao.updateScan(scan.copy(isFavorite = !scan.isFavorite))
    }

    suspend fun deleteScan(scan: ScanEntity) = scanDao.deleteScan(scan)

    suspend fun deleteById(id: Long) = scanDao.deleteById(id)

    suspend fun clearAll() = scanDao.clearAll()

    suspend fun getAllList(): List<ScanEntity> = scanDao.getAllList()
}
