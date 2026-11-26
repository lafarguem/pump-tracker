package com.lafarguem.pumptracker.data

import androidx.annotation.StringRes
import com.lafarguem.pumptracker.R

/**
 * Constant names are persisted in the database: rename the label, never the constant.
 * [quickSteps] are the one-tap amounts offered in the app and on the widget.
 */
enum class Exercise(@StringRes val label: Int, val quickSteps: List<Int>) {
    PUSH_UPS(R.string.push_ups, listOf(10, 25, 50)),
    PULL_UPS(R.string.pull_ups, listOf(1, 5, 10)),
    SQUATS(R.string.squats, listOf(10, 25, 50)),
}
