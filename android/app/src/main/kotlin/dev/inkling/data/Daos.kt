package dev.inkling.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ChildDao {
    @Query("SELECT * FROM Child ORDER BY id LIMIT 1") suspend fun first(): Child?
    @Insert suspend fun insert(c: Child): Long
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM Settings WHERE childId = :childId") fun flow(childId: Long): Flow<Settings?>
    @Query("SELECT * FROM Settings WHERE childId = :childId") suspend fun get(childId: Long): Settings?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsert(s: Settings)
}

@Dao
interface AppRuleDao {
    @Query("SELECT * FROM AppRule WHERE childId = :childId ORDER BY label") fun flow(childId: Long): Flow<List<AppRule>>
    @Query("SELECT * FROM AppRule WHERE childId = :childId ORDER BY label") suspend fun list(childId: Long): List<AppRule>
    @Query("SELECT * FROM AppRule WHERE childId = :childId AND packageName = :pkg") suspend fun find(childId: Long, pkg: String): AppRule?
    @Insert suspend fun insert(r: AppRule): Long
    @Update suspend fun update(r: AppRule)
}

@Dao
interface UsageDao {
    @Query("SELECT * FROM UsageEvent WHERE endedAt IS NULL") suspend fun open(): List<UsageEvent>
    @Query("SELECT * FROM UsageEvent WHERE childId = :childId AND (endedAt IS NULL OR endedAt >= :dayStart)") suspend fun since(childId: Long, dayStart: Long): List<UsageEvent>
    @Insert suspend fun insert(e: UsageEvent): Long
    @Update suspend fun update(e: UsageEvent)
}

@Dao
interface SpikeDao {
    @Insert suspend fun insert(r: SpikeRow)
    @Query("SELECT * FROM SpikeRow ORDER BY at") suspend fun all(): List<SpikeRow>
}
