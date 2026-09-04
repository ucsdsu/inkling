package dev.inkling.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Child::class, AppRule::class, UsageEvent::class, Settings::class, SpikeRow::class], version = 1, exportSchema = false)
abstract class InklingDb : RoomDatabase() {
    abstract fun children(): ChildDao
    abstract fun settings(): SettingsDao
    abstract fun rules(): AppRuleDao
    abstract fun usage(): UsageDao
    abstract fun spikes(): SpikeDao

    companion object {
        fun open(context: Context): InklingDb =
            Room.databaseBuilder(context.applicationContext, InklingDb::class.java, "inkling.db").build()
    }
}
