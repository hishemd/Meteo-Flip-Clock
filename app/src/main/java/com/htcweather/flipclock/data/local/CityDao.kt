package com.htcweather.flipclock.data.local

import androidx.room.*
import com.htcweather.flipclock.data.model.CityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CityDao {

    @Query("SELECT * FROM cities ORDER BY orderIndex ASC, id ASC")
    fun getAllCities(): Flow<List<CityEntity>>

    @Query("SELECT * FROM cities WHERE isHome = 1 LIMIT 1")
    suspend fun getHomeCity(): CityEntity?

    @Query("SELECT * FROM cities WHERE id = :id")
    suspend fun getCityById(id: Long): CityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(city: CityEntity): Long

    @Update
    suspend fun update(city: CityEntity)

    @Delete
    suspend fun delete(city: CityEntity)

    @Query("DELETE FROM cities WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM cities")
    suspend fun count(): Int
}
