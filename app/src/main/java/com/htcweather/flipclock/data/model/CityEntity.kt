package com.htcweather.flipclock.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "cities")
data class CityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val country: String?,
    val lat: Double,
    val lon: Double,
    val isHome: Boolean = false,
    val orderIndex: Int = 0
)
