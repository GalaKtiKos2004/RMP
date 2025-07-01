package ru.fefu.activitiesfefu.data

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromCoordinatesList(value: List<Coordinates>): String {
        val gson = Gson()
        val type = object : TypeToken<List<Coordinates>>() {}.type
        return gson.toJson(value, type)
    }

    @TypeConverter
    fun toCoordinatesList(value: String): List<Coordinates> {
        val gson = Gson()
        val type = object : TypeToken<List<Coordinates>>() {}.type
        return gson.fromJson(value, type)
    }
} 