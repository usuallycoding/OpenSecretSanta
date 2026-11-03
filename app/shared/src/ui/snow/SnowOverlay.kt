package com.usuallycoding.opensecretsanta.ui.snow

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import kotlin.random.Random

/** White on the dark theme; on the light one white would not show, so a pale blue-grey. */
private val DARK_SNOW = Color.White
private val LIGHT_SNOW = Color(0xFF8FA6C2)

/** A step is never longer than this, so that a pause (a drag of the window, a slow frame) does not throw the snow forward. */
private const val LONGEST_STEP_SECONDS = 0.05f

/**
 * Snow falling over whatever is under it. It draws one canvas that takes no input (a tap, a scroll or a key reaches what is
 * below it) and says nothing to a screen reader. The frame clock moves it, and the time is only read while drawing, so a frame
 * redraws this canvas and recomposes nothing; with the app out of sight there are no frames, and so no work.
 */
@Composable
fun SnowOverlay(modifier: Modifier = Modifier) {
    val color = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) DARK_SNOW else LIGHT_SNOW
    var frameNanos by remember { mutableLongStateOf(0L) }
    val snow = remember { SnowState() }
    LaunchedEffect(Unit) {
        while (true) withFrameNanos { frameNanos = it }
    }
    Canvas(modifier.fillMaxSize().clearAndSetSemantics { }) {
        val now = frameNanos
        val dp = density
        val width = size.width / dp
        val height = size.height / dp
        snow.advance(now, width, height)
        for (flake in snow.flakes) {
            val center = Offset(flake.drawnX(width) * dp, flake.y * dp)
            // a soft edge: a larger, fainter flake behind the flake itself
            drawCircle(color.copy(alpha = flake.alpha * 0.35f), radius = flake.radius * 1.8f * dp, center = center)
            drawCircle(color.copy(alpha = flake.alpha), radius = flake.radius * dp, center = center)
        }
    }
}

/** The flakes and the time they were last moved; used only from drawing, which is one thread. */
private class SnowState {
    var flakes: List<Flake> = emptyList()
        private set
    private var last = 0L
    private val random = Random.Default

    fun advance(nanos: Long, width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        flakes = if (flakes.isEmpty()) Snowfall.start(Snowfall.flakeCount(width, height), width, height, random) else Snowfall.resize(flakes, width, height, random)
        val seconds = if (last == 0L) 0f else ((nanos - last) / 1_000_000_000f).coerceIn(0f, LONGEST_STEP_SECONDS)
        last = nanos
        flakes = Snowfall.step(flakes, seconds, width, height)
    }
}
