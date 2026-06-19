package hr.kotwave.gameslibrary.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** Line icons as 24×24 [ImageVector]s. Tint via `Icon`. */
object AppIcons {

    val Grid: ImageVector = strokeIcon(
        "Grid",
        roundedRectD(3f, 3f, 7f, 7f, 1.5f),
        roundedRectD(14f, 3f, 7f, 7f, 1.5f),
        roundedRectD(3f, 14f, 7f, 7f, 1.5f),
        roundedRectD(14f, 14f, 7f, 7f, 1.5f),
    )

    val Heart: ImageVector = strokeIcon("Heart", HEART_PATH)

    val HeartFilled: ImageVector = fillIcon("HeartFilled", HEART_PATH)

    val Import: ImageVector = strokeIcon(
        "Import",
        "M21 12a9 9 0 1 1-9-9",
        "M12 7v5l3 2",
    )

    val Export: ImageVector = strokeIcon("Export", "M12 3v12M8 11l4 4 4-4M5 21h14")

    val ImportFile: ImageVector = strokeIcon("ImportFile", "M12 15V3M8 7l4-4 4 4M5 21h14")

    val Settings: ImageVector = strokeIcon(
        "Settings",
        circleD(12f, 12f, 3f),
        "M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-1.8-.3 1.6 1.6 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.6 1.6 0 0 0-1-1.5 1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0 .3-1.8 1.6 1.6 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.6 1.6 0 0 0 1.5-1 1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H9a1.6 1.6 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V9a1.6 1.6 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z",
    )

    val Plus: ImageVector = strokeIcon("Plus", "M12 5v14M5 12h14", strokeWidth = 2.4f)

    val Search: ImageVector = strokeIcon(
        "Search",
        circleD(11f, 11f, 7f),
        "M21 21l-4-4",
    )

    val Sliders: ImageVector = strokeIcon("Sliders", "M4 6h16M7 12h10M10 18h4")

    val ChevronDown: ImageVector = strokeIcon("ChevronDown", "M6 9l6 6 6-6")

    val ChevronLeft: ImageVector = strokeIcon("ChevronLeft", "M15 6l-6 6 6 6", strokeWidth = 2.2f)

    val ChevronRight: ImageVector = strokeIcon("ChevronRight", "M9 6l6 6-6 6")

    val Close: ImageVector = strokeIcon("Close", "M6 6l12 12M18 6L6 18", strokeWidth = 2.2f)

    val Check: ImageVector = strokeIcon("Check", "M5 13l4 4L19 7", strokeWidth = 2.3f)

    val Trash: ImageVector = strokeIcon("Trash", "M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14")

    val Edit: ImageVector = strokeIcon(
        "Edit",
        "M12 20h9",
        "M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z",
    )

    val Sync: ImageVector = strokeIcon("Sync", "M21 12a9 9 0 1 1-2.6-6.4M21 4v5h-5", strokeWidth = 2.2f)

    val Star: ImageVector = fillIcon("Star", STAR_PATH)

    val Minus: ImageVector = strokeIcon("Minus", "M5 12h14", strokeWidth = 2.4f)

    val Steam: ImageVector = fillIcon("Steam", STEAM_PATH)
}

private const val STEAM_PATH =
    "M12 2a10 10 0 0 0-10 10l5.5 2.3a2.8 2.8 0 0 1 1.6-.5l2.4-3.5v-.05a3.75 3.75 0 1 1 3.75 3.75h-.08" +
        "l-3.45 2.46a2.8 2.8 0 0 1-5.55.62L2.3 15.4A10 10 0 1 0 12 2zM8.2 17.7l-1.27-.53a2.1 2.1 0 0 0 " +
        "3.85-1.1 2.1 2.1 0 0 0-2.95-1.92l1.3.54a1.55 1.55 0 1 1-.93 2.95zM18 9.75a2.5 2.5 0 1 0-2.5 2.5 " +
        "2.5 2.5 0 0 0 2.5-2.5zm-4.37 0a1.88 1.88 0 1 1 1.87 1.87 1.87 1.87 0 0 1-1.87-1.87z"

private const val STAR_PATH =
    "M12 2l2.9 6.3 6.9.6-5.2 4.6 1.6 6.8L12 17.3 5.8 20.9l1.6-6.8L2.2 8.9l6.9-.6z"

private const val HEART_PATH =
    "M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"

private fun strokeIcon(name: String, vararg pathData: String, strokeWidth: Float = 2f): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    for (d in pathData) {
        builder.addPath(
            pathData = PathParser().parsePathString(d).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

private fun fillIcon(name: String, pathData: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).addPath(
        pathData = PathParser().parsePathString(pathData).toNodes(),
        fill = SolidColor(Color.Black),
    ).build()

/** SVG `d` for a stroke circle, as two relative arcs. */
private fun circleD(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} $cy a$r $r 0 1 0 ${2 * r} 0 a$r $r 0 1 0 ${-2 * r} 0"

/** SVG `d` for a rounded rectangle outline. */
private fun roundedRectD(x: Float, y: Float, w: Float, h: Float, r: Float): String {
    val hMid = w - 2 * r
    val vMid = h - 2 * r
    return "M${x + r} $y h$hMid a$r $r 0 0 1 $r $r v$vMid a$r $r 0 0 1 ${-r} $r " +
        "h${-hMid} a$r $r 0 0 1 ${-r} ${-r} v${-vMid} a$r $r 0 0 1 $r ${-r} z"
}
