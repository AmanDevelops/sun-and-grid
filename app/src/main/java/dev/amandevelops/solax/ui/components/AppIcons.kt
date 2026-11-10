package dev.amandevelops.solax.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Hand-built line icons (24dp grid). Keeping these local avoids pulling in the
 * large material-icons-extended artifact just for a handful of glyphs.
 */
object AppIcons {

    val Sun: ImageVector = strokeIcon("Sun") {
        stroke("M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8Z")
        stroke("M12 2.5V5 M12 19v2.5 M2.5 12H5 M19 12h2.5 M5.3 5.3 7 7 M17 17l1.7 1.7 M5.3 18.7 7 17 M17 7l1.7-1.7")
    }

    val Bolt: ImageVector = strokeIcon("Bolt") {
        stroke("M13.2 2.5 4.5 13.5H11l-.8 8L19.5 9.8h-6.7Z")
    }

    val Home: ImageVector = strokeIcon("Home") {
        stroke("M4 10.5 12 3.5l8 7V19.5a1.5 1.5 0 0 1-1.5 1.5h-13A1.5 1.5 0 0 1 4 19.5Z")
        stroke("M9.5 21v-6.5h5V21")
    }

    val Chart: ImageVector = strokeIcon("Chart") {
        stroke("M5 20V12 M12 20V4 M19 20V8")
    }

    val Refresh: ImageVector = strokeIcon("Refresh") {
        stroke("M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8")
        stroke("M21 3v5h-5")
    }

    val Settings: ImageVector = strokeIcon("Settings") {
        stroke("M21 4h-7 M10 4H3 M21 12h-9 M8 12H3 M21 20h-5 M12 20H3")
        stroke("M14 2v4 M8 10v4 M16 18v4")
    }

    val ChevronLeft: ImageVector = strokeIcon("ChevronLeft") {
        stroke("M15 5 8 12l7 7")
    }

    val ChevronRight: ImageVector = strokeIcon("ChevronRight") {
        stroke("M9 5l7 7-7 7")
    }

    val ChevronDown: ImageVector = strokeIcon("ChevronDown") {
        stroke("M5 9l7 7 7-7")
    }

    val Wifi: ImageVector = strokeIcon("Wifi") {
        stroke("M2 8.8a15 15 0 0 1 20 0 M5 12.9a10 10 0 0 1 14 0 M8.5 16.4a5 5 0 0 1 7 0")
        filled("M12 19.1a1 1 0 1 0 0 2a1 1 0 1 0 0-2Z")
    }

    val Thermometer: ImageVector = strokeIcon("Thermometer") {
        stroke("M14 14.8V3.5a2.5 2.5 0 0 0-5 0v11.3a4.5 4.5 0 1 0 5 0Z")
        stroke("M12 9v7")
    }

    val Calendar: ImageVector = strokeIcon("Calendar") {
        stroke("M8 2v4 M16 2v4 M3 10h18")
        stroke("M3 6a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2Z")
    }

    val Grid: ImageVector = strokeIcon("Grid") {
        stroke("M12 22v-5 M9 8V2 M15 8V2 M18 8v3a6 6 0 0 1-12 0V8Z")
    }

    val ArrowUpRight: ImageVector = strokeIcon("ArrowUpRight") {
        stroke("M7 17 17 7 M8 7h9v9")
    }

    val ArrowDownLeft: ImageVector = strokeIcon("ArrowDownLeft") {
        stroke("M17 7 7 17 M16 17H7V8")
    }

    val Check: ImageVector = strokeIcon("Check") {
        stroke("M20 6 9 17l-5-5")
    }

    val CheckCircle: ImageVector = strokeIcon("CheckCircle") {
        stroke("M21.5 12a9.5 9.5 0 1 1-19 0a9.5 9.5 0 0 1 19 0Z")
        stroke("M8 12.5l2.7 2.7L16 9.5")
    }

    val Alert: ImageVector = strokeIcon("Alert") {
        stroke("M10.3 3.9 2.4 18a2 2 0 0 0 1.7 3h15.8a2 2 0 0 0 1.7-3L13.7 3.9a2 2 0 0 0-3.4 0Z")
        stroke("M12 9v4")
        filled("M12 16.7a1 1 0 1 0 0 2a1 1 0 1 0 0-2Z")
    }

    val Info: ImageVector = strokeIcon("Info") {
        stroke("M21.5 12a9.5 9.5 0 1 1-19 0a9.5 9.5 0 0 1 19 0Z")
        stroke("M12 16v-4")
        filled("M12 7.5a1 1 0 1 0 0 2a1 1 0 1 0 0-2Z")
    }

    val Clock: ImageVector = strokeIcon("Clock") {
        stroke("M21.5 12a9.5 9.5 0 1 1-19 0a9.5 9.5 0 0 1 19 0Z")
        stroke("M12 6v6l4 2")
    }

    val Sparkle: ImageVector = strokeIcon("Sparkle") {
        stroke("M12 3l-1.9 5.8a2 2 0 0 1-1.3 1.3L3 12l5.8 1.9a2 2 0 0 1 1.3 1.3L12 21l1.9-5.8a2 2 0 0 1 1.3-1.3L21 12l-5.8-1.9a2 2 0 0 1-1.3-1.3Z")
        stroke("M5 3v3 M3.5 4.5h3 M19 18v3 M17.5 19.5h3")
    }

    val Leaf: ImageVector = strokeIcon("Leaf") {
        stroke("M11 20A7 7 0 0 1 9.8 6.1C15.5 5 17 4.5 19 2c1 2 2 4.2 2 8 0 5.5-4.8 10-10 10Z")
        stroke("M2 21c0-3 1.85-5.36 5.08-6C9.5 14.5 12 13 13 12")
    }

    val Shield: ImageVector = strokeIcon("Shield") {
        stroke("M20 13c0 5-3.5 7.5-8 9-4.5-1.5-8-4-8-9V5l8-3 8 3Z")
        stroke("M9 12l2 2 4-4")
    }

    val Trash: ImageVector = strokeIcon("Trash") {
        stroke("M3 6h18 M8 6V4h8v2 M19 6l-1 15H6L5 6 M10 11v6 M14 11v6")
    }

    val Close: ImageVector = strokeIcon("Close") {
        stroke("M6 6l12 12 M18 6 6 18")
    }

    val Back: ImageVector = strokeIcon("Back") {
        stroke("M19 12H5 M12 19l-7-7 7-7")
    }

    val House: ImageVector = Home

    val DockToday: ImageVector = strokeIcon("DockToday", 1.9f) {
        stroke("M3 11l9-8 9 8M5 10v10h14V10")
    }

    val DockSolar: ImageVector = strokeIcon("DockSolar", 1.9f) {
        stroke("M12 8a4 4 0 1 0 0 8a4 4 0 1 0 0-8Z")
        stroke("M12 2v3 M12 19v3 M2 12h3 M19 12h3 M5 5l2 2 M17 17l2 2 M19 5l-2 2 M7 17l-2 2")
    }

    val DockUsage: ImageVector = strokeIcon("DockUsage", 1.9f) {
        stroke("M13 2L4 14h7l-1 8 9-12h-7z")
    }

    val DockDevice: ImageVector = strokeIcon("DockDevice", 1.9f) {
        stroke("M8 3h8a4 4 0 0 1 4 4v10a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4V7a4 4 0 0 1 4-4Z")
        stroke("M8 8h8 M8 12h8 M8 16h4")
    }

    private fun strokeIcon(
        name: String,
        strokeWidth: Float = 1.8f,
        block: IconBuilder.() -> Unit,
    ): ImageVector = IconBuilder(name, strokeWidth).apply(block).build()
}

private class IconBuilder(
    private val name: String,
    private val strokeWidth: Float,
) {
    private val strokes = mutableListOf<String>()
    private val fills = mutableListOf<String>()

    fun stroke(data: String) {
        strokes += data
    }

    fun filled(data: String) {
        fills += data
    }

    fun build(): ImageVector {
        val builder = ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        )
        strokes.forEach { data ->
            builder.addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = strokeWidth,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
        fills.forEach { data ->
            builder.addPath(
                pathData = PathParser().parsePathString(data).toNodes(),
                fill = SolidColor(Color.Black),
            )
        }
        return builder.build()
    }
}
