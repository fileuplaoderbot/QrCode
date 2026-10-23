package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ScanDao {
    @Query("SELECT * FROM scans ORDER BY timestamp DESC")
    fun getAllScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE isFavorite = 1 ORDER BY timestamp DESC")
    fun getFavoriteScans(): Flow<List<ScanEntity>>

    @Query("SELECT * FROM scans WHERE content = :content LIMIT 1")
    suspend fun findByContent(content: String): ScanEntity?

    @Query("SELECT * FROM scans WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): ScanEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScan(scan: ScanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(scans: List<ScanEntity>)

    @Update
    suspend fun updateScan(scan: ScanEntity)

    @Delete
    suspend fun deleteScan(scan: ScanEntity)

    @Query("DELETE FROM scans WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scans")
    suspend fun clearAll()

    @Query("SELECT * FROM scans")
    suspend fun getAllList(): List<ScanEntity>
}
