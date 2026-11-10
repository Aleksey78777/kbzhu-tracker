package ru.kbzhu.tracker.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** Выпитая вода за день (одна строка на дату). */
@Entity(tableName = "water")
data class WaterEntity(
    @PrimaryKey val date: Long,
    @ColumnInfo(name = "amount_ml") val amountMl: Int
)

/** Отметка веса (одна на дату — повторная отметка за день перезаписывает). */
@Entity(tableName = "weight")
data class WeightEntity(
    @PrimaryKey val date: Long,
    @ColumnInfo(name = "weight_kg") val weightKg: Double
)
