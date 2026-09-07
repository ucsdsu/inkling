package dev.inkling.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.RoomDatabase

@Database(
    entities = [Child::class, DeviceState::class, AppRule::class, UsageEvent::class, Settings::class, SpikeRow::class, ReadEvent::class, TutorAttempt::class, DiscoveryProgress::class],
    version = 4,
    exportSchema = false,
)
abstract class InklingDb : RoomDatabase() {
    abstract fun children(): ChildDao
    abstract fun deviceState(): DeviceStateDao
    abstract fun settings(): SettingsDao
    abstract fun rules(): AppRuleDao
    abstract fun usage(): UsageDao
    abstract fun spikes(): SpikeDao
    abstract fun reads(): ReadEventDao
    abstract fun attempts(): TutorAttemptDao
    abstract fun discoveries(): DiscoveryDao

    companion object {
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS DiscoveryProgress (childId INTEGER NOT NULL, lessonId TEXT NOT NULL, firstCompletedDay INTEGER NOT NULL, lastRecallDay INTEGER, PRIMARY KEY(childId, lessonId))")
            }
        }

        fun open(context: Context): InklingDb =
            Room.databaseBuilder(context.applicationContext, InklingDb::class.java, "inkling.db")
                // Pre-release: the only database on earth is Cove's test device. Remove before any public build.
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration()
                .build()
    }
}
