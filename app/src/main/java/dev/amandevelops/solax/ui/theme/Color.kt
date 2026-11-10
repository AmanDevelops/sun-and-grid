package dev.amandevelops.solax.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The Sun & Grid palette. Colours mirror the reference design exactly for both
 * light and dark mode.
 */
@Immutable
data class AppColors(
    val background: Color,
    val card: Color,
    val ink: Color,
    val mute: Color,
    val line: Color,
    val sun: Color,
    val grid: Color,
    val ok: Color,
    val bad: Color,
    val sky1: Color,
    val sky2: Color,
    val onSun: Color,
    val white: Color,
)

val LightColors = AppColors(
    background = Color(0xFFE9E7F7),
    card = Color(0xFFFFFFFF),
    ink = Color(0xFF17173A),
    mute = Color(0xFF6D6D92),
    line = Color(0xFFE4E1F4),
    sun = Color(0xFFFF9A1F),
    grid = Color(0xFF5146E5),
    ok = Color(0xFF10B39F),
    bad = Color(0xFFFF5D73),
    sky1 = Color(0xFFA9CCFF),
    sky2 = Color(0xFFFFE6B8),
    onSun = Color(0xFF1A1200),
    white = Color(0xFFFFFFFF),
)

val DarkColors = AppColors(
    background = Color(0xFF0D0E22),
    card = Color(0xFF171936),
    ink = Color(0xFFF1F0FF),
    mute = Color(0xFF9A9BC4),
    line = Color(0xFF272A52),
    sun = Color(0xFFFFB13D),
    grid = Color(0xFF8B84FF),
    ok = Color(0xFF3FD6C0),
    bad = Color(0xFFFF5D73),
    sky1 = Color(0xFF202C6B),
    sky2 = Color(0xFFE8895A),
    onSun = Color(0xFF1A1200),
    white = Color(0xFFFFFFFF),
)
