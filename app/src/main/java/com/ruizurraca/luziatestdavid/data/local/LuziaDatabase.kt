package com.ruizurraca.luziatestdavid.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ruizurraca.luziatestdavid.data.local.dao.ChatMessageDao
import com.ruizurraca.luziatestdavid.data.local.entity.ChatMessageEntity

@Database(entities = [ChatMessageEntity::class], version = 2, exportSchema = false)
abstract class LuziaDatabase : RoomDatabase() {
    abstract fun chatMessageDao(): ChatMessageDao
}
