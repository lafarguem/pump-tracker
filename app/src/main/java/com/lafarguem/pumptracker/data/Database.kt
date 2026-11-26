package com.lafarguem.pumptracker.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

/** One "+N reps" tap. [day] is the local date as an epoch day, [timestamp] is epoch millis. */
@Entity(tableName = "entries", indices = [Index("day"), Index("exercise", "day")])
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exercise: Exercise,
    val amount: Int,
    val day: Long,
    val timestamp: Long,
)

data class ExerciseTotal(
    @ColumnInfo(name = "exercise") val exercise: Exercise,
    @ColumnInfo(name = "total") val total: Int,
)

data class DayTotal(
    @ColumnInfo(name = "day") val day: Long,
    @ColumnInfo(name = "total") val total: Int,
)

@Dao
interface EntryDao {
    @Insert
    suspend fun insert(entry: Entry): Long

    @Query("DELETE FROM entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM entries WHERE day = :day ORDER BY timestamp DESC, id DESC")
    fun observeDay(day: Long): Flow<List<Entry>>

    @Query("SELECT exercise, SUM(amount) AS total FROM entries WHERE day = :day GROUP BY exercise")
    fun observeTotals(day: Long): Flow<List<ExerciseTotal>>

    @Query("SELECT exercise, SUM(amount) AS total FROM entries WHERE day = :day GROUP BY exercise")
    suspend fun totals(day: Long): List<ExerciseTotal>

    @Query("SELECT day, SUM(amount) AS total FROM entries WHERE exercise = :exercise GROUP BY day")
    fun observeDailyTotals(exercise: Exercise): Flow<List<DayTotal>>
}

@Database(entities = [Entry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun entryDao(): EntryDao
}
