package com.lafarguem.pumptracker.data

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.room.Room
import com.lafarguem.pumptracker.widget.PumpWidget
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/** Single entry point for reads and writes, shared by the app and the widget. */
class Repository private constructor(private val context: Context, private val dao: EntryDao) {

    fun entriesFor(day: Long): Flow<List<Entry>> = dao.observeDay(day)

    fun totalsFor(day: Long): Flow<Map<Exercise, Int>> =
        dao.observeTotals(day).map { rows -> rows.associate { it.exercise to it.total } }

    suspend fun totalsOnce(day: Long): Map<Exercise, Int> =
        dao.totals(day).associate { it.exercise to it.total }

    /** Total reps per day (epoch day) for one exercise, over all time. */
    fun dailyTotals(exercise: Exercise): Flow<Map<Long, Int>> =
        dao.observeDailyTotals(exercise).map { rows -> rows.associate { it.day to it.total } }

    /** Logs [amount] reps for today and returns the new entry id (usable for undo). */
    suspend fun add(exercise: Exercise, amount: Int): Long {
        val id = dao.insert(
            Entry(
                exercise = exercise,
                amount = amount,
                day = today(),
                timestamp = System.currentTimeMillis(),
            )
        )
        PumpWidget().updateAll(context)
        return id
    }

    suspend fun delete(id: Long) {
        dao.delete(id)
        PumpWidget().updateAll(context)
    }

    companion object {
        @Volatile
        private var instance: Repository? = null

        fun get(context: Context): Repository = instance ?: synchronized(this) {
            instance ?: run {
                val app = context.applicationContext
                val db = Room.databaseBuilder(app, AppDatabase::class.java, "pump-tracker.db").build()
                Repository(app, db.entryDao()).also { instance = it }
            }
        }

        fun today(): Long = LocalDate.now().toEpochDay()
    }
}
