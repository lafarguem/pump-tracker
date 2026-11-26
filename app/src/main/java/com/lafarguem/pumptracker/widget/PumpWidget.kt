package com.lafarguem.pumptracker.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.lafarguem.pumptracker.MainActivity
import com.lafarguem.pumptracker.data.Exercise
import com.lafarguem.pumptracker.data.Repository
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.ZoneId

/** Home-screen widget: today's total per exercise, with one-tap "+N" buttons. */
class PumpWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val repo = Repository.get(context)
        val initial = repo.totalsOnce(Repository.today())
        provideContent {
            // Roll over to the new day if the widget session happens to be alive at midnight.
            val day by produceState(Repository.today()) {
                while (true) {
                    delay(nextMidnightMillis() - System.currentTimeMillis() + 500)
                    value = Repository.today()
                }
            }
            val totals by remember(day) { repo.totalsFor(day) }.collectAsState(initial)
            GlanceTheme { WidgetContent(totals) }
        }
    }
}

@Composable
private fun WidgetContent(totals: Map<Exercise, Int>) {
    val context = LocalContext.current
    val wide = LocalSize.current.width >= 280.dp
    val openApp = actionStartActivity<MainActivity>()

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(24.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Exercise.entries.forEach { exercise ->
            Row(
                modifier = GlanceModifier.fillMaxWidth().defaultWeight(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(GlanceModifier.defaultWeight().clickable(openApp)) {
                    Text(
                        context.getString(exercise.label),
                        style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 12.sp),
                        maxLines = 1,
                    )
                    Text(
                        (totals[exercise] ?: 0).toString(),
                        style = TextStyle(
                            color = GlanceTheme.colors.onSurface,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                        maxLines = 1,
                    )
                }
                // Narrow widgets drop the smallest step.
                val steps = if (wide) exercise.quickSteps else exercise.quickSteps.takeLast(2)
                steps.forEach { step ->
                    Spacer(GlanceModifier.width(6.dp))
                    Box(
                        modifier = GlanceModifier
                            .width(50.dp)
                            .height(40.dp)
                            .background(GlanceTheme.colors.primaryContainer)
                            .cornerRadius(20.dp)
                            .clickable(
                                actionRunCallback<AddRepsAction>(
                                    actionParametersOf(
                                        AddRepsAction.EXERCISE to exercise.name,
                                        AddRepsAction.AMOUNT to step,
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "+$step",
                            style = TextStyle(
                                color = GlanceTheme.colors.onPrimaryContainer,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                    }
                }
            }
        }
    }
}

class AddRepsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val exercise = parameters[EXERCISE]?.let(Exercise::valueOf) ?: return
        val amount = parameters[AMOUNT] ?: return
        Repository.get(context).add(exercise, amount)
    }

    companion object {
        val EXERCISE = ActionParameters.Key<String>("exercise")
        val AMOUNT = ActionParameters.Key<Int>("amount")
    }
}

class PumpWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PumpWidget()

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduleMidnightRefresh(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        context.getSystemService(AlarmManager::class.java).cancel(refreshIntent(context))
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            // The counts are per day, so redraw when the day (or the clock) changes.
            ACTION_REFRESH, Intent.ACTION_TIME_CHANGED, Intent.ACTION_TIMEZONE_CHANGED -> {
                val manager = AppWidgetManager.getInstance(context)
                val ids = manager.getAppWidgetIds(ComponentName(context, PumpWidgetReceiver::class.java))
                if (ids.isNotEmpty()) onUpdate(context, manager, ids)
            }

            else -> super.onReceive(context, intent)
        }
    }

    private fun scheduleMidnightRefresh(context: Context) {
        // Inexact and non-waking: the widget only has to be right by the time the screen is on.
        context.getSystemService(AlarmManager::class.java)
            .set(AlarmManager.RTC, nextMidnightMillis() + 1_000, refreshIntent(context))
    }

    private fun refreshIntent(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, PumpWidgetReceiver::class.java).setAction(ACTION_REFRESH),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private companion object {
        const val ACTION_REFRESH = "com.lafarguem.pumptracker.widget.REFRESH"
    }
}

private fun nextMidnightMillis(): Long =
    LocalDate.now().plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
