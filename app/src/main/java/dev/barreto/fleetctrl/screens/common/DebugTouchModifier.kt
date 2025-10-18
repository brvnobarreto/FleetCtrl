package dev.barreto.fleetctrl.screens.common

import android.util.Log
import android.view.MotionEvent
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInteropFilter

/**
 * Debug helper that logs MotionEvent callbacks so we can diagnose
 * pull-to-refresh gesture issues on empty screens.
 */
fun Modifier.debugTouchLogger(tag: String): Modifier = pointerInteropFilter { event ->
    when (event.actionMasked) {
        MotionEvent.ACTION_DOWN -> Log.d(tag, "ACTION_DOWN at x=${event.x}, y=${event.y}")
        MotionEvent.ACTION_MOVE -> Log.d(tag, "ACTION_MOVE at x=${event.x}, y=${event.y}")
        MotionEvent.ACTION_UP -> Log.d(tag, "ACTION_UP at x=${event.x}, y=${event.y}")
        MotionEvent.ACTION_CANCEL -> Log.d(tag, "ACTION_CANCEL")
    }
    false // Não consome o evento, deixa stream seguir normalmente
}

