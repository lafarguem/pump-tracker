package com.lafarguem.pumptracker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lafarguem.pumptracker.R
import com.lafarguem.pumptracker.data.Exercise
import kotlinx.coroutines.launch

@Composable
fun PumpApp(vm: MainViewModel = viewModel()) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LifecycleResumeEffect(Unit) {
        vm.refreshToday()
        onPauseOrDispose { }
    }

    fun add(exercise: Exercise, amount: Int) {
        scope.launch {
            val id = vm.add(exercise, amount)
            snackbar.currentSnackbarData?.dismiss()
            val result = snackbar.showSnackbar(
                message = "+$amount ${context.getString(exercise.label).lowercase()}",
                actionLabel = "Undo",
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) vm.delete(id)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Icon(painterResource(R.drawable.ic_today), contentDescription = null) },
                    label = { Text("Today") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Icon(painterResource(R.drawable.ic_stats), contentDescription = null) },
                    label = { Text("Stats") },
                )
            }
        },
    ) { padding ->
        if (tab == 0) {
            val date by vm.today.collectAsStateWithLifecycle()
            val entries by vm.todayEntries.collectAsStateWithLifecycle()
            TodayScreen(
                date = date,
                entries = entries,
                onAdd = ::add,
                onDelete = vm::delete,
                modifier = Modifier.padding(padding),
            )
        } else {
            val exercise by vm.statsExercise.collectAsStateWithLifecycle()
            val range by vm.statsRange.collectAsStateWithLifecycle()
            val stats by vm.stats.collectAsStateWithLifecycle()
            StatsScreen(
                exercise = exercise,
                range = range,
                stats = stats,
                onExercise = { vm.statsExercise.value = it },
                onRange = { vm.statsRange.value = it },
                modifier = Modifier.padding(padding),
            )
        }
    }
}
