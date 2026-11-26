package com.lafarguem.pumptracker.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lafarguem.pumptracker.data.Entry
import com.lafarguem.pumptracker.data.Exercise
import com.lafarguem.pumptracker.data.Repository
import com.lafarguem.pumptracker.data.Stats
import com.lafarguem.pumptracker.data.StatsRange
import com.lafarguem.pumptracker.data.computeStats
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = Repository.get(app)

    /** Re-read on every resume so the screen rolls over to the new day after midnight. */
    val today = MutableStateFlow(LocalDate.now())
    val statsExercise = MutableStateFlow(Exercise.PUSH_UPS)
    val statsRange = MutableStateFlow(StatsRange.WEEK)

    val todayEntries: StateFlow<List<Entry>> = today
        .flatMapLatest { repo.entriesFor(it.toEpochDay()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats: StateFlow<Stats?> = combine(
        statsExercise.flatMapLatest { repo.dailyTotals(it) },
        today,
        statsRange,
    ) { totals, day, range -> computeStats(totals, day, range) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun refreshToday() {
        today.value = LocalDate.now()
    }

    suspend fun add(exercise: Exercise, amount: Int): Long = repo.add(exercise, amount)

    fun delete(id: Long) {
        viewModelScope.launch { repo.delete(id) }
    }
}
