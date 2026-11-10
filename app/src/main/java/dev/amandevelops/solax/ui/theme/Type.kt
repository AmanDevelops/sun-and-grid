package dev.amandevelops.solax.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.amandevelops.solax.R

/**
 * Bricolage Grotesque, the typeface used by the Sun & Grid reference.
 * Static weights keep text rendering cheap on low-end devices.
 */
val Bricolage = FontFamily(
    Font(R.font.bricolage_regular, FontWeight.Normal),
    Font(R.font.bricolage_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_bold, FontWeight.Bold),
    Font(R.font.bricolage_extrabold, FontWeight.ExtraBold),
)

/**
 * Text styles taken directly from the reference CSS. Keeping them here avoids
 * scattered magic numbers and makes the whole app feel like one design.
 */
object AppText {
    val body = TextStyle(fontFamily = Bricolage, fontSize = 15.sp, lineHeight = 21.sp)
    val bodyMedium = TextStyle(fontFamily = Bricolage, fontSize = 14.sp, lineHeight = 20.sp)
    val sub = TextStyle(fontFamily = Bricolage, fontSize = 13.sp, lineHeight = 18.sp)
    val small = TextStyle(fontFamily = Bricolage, fontSize = 12.sp, lineHeight = 17.sp)
    val micro = TextStyle(fontFamily = Bricolage, fontSize = 11.sp, lineHeight = 12.sp)
    val tiny = TextStyle(fontFamily = Bricolage, fontSize = 10.sp, lineHeight = 14.sp)

    val header = TextStyle(fontFamily = Bricolage,
        fontSize = 30.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.9).sp,
    )

    val h2 = TextStyle(fontFamily = Bricolage,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.17).sp,
    )

    val hero = TextStyle(fontFamily = Bricolage,
        fontSize = 58.sp,
        lineHeight = 52.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-2.9).sp,
    )

    val number40 = TextStyle(fontFamily = Bricolage,
        fontSize = 40.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1.6).sp,
    )

    val number30 = TextStyle(fontFamily = Bricolage,
        fontSize = 30.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-1.2).sp,
    )

    val value22 = TextStyle(fontFamily = Bricolage,
        fontSize = 22.sp,
        lineHeight = 31.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.66).sp,
    )

    val value20 = TextStyle(fontFamily = Bricolage,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.6).sp,
    )

    val value19 = TextStyle(fontFamily = Bricolage,
        fontSize = 19.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.57).sp,
    )

    val value15 = TextStyle(fontFamily = Bricolage,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.ExtraBold,
    )

    val chip = TextStyle(fontFamily = Bricolage,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
    )

    val labelBold = TextStyle(fontFamily = Bricolage,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Bold,
    )

    val dock = TextStyle(fontFamily = Bricolage,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val button = TextStyle(fontFamily = Bricolage,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
    )

    val reading = TextStyle(fontFamily = Bricolage, fontSize = 14.sp, lineHeight = 20.sp)
    val readingSmall = TextStyle(fontFamily = Bricolage, fontSize = 11.sp, lineHeight = 15.sp)
}

val AppTypography = Typography(
    displayLarge = AppText.hero,
    displayMedium = AppText.number40,
    displaySmall = AppText.number30,
    headlineLarge = AppText.header,
    headlineMedium = AppText.value22,
    headlineSmall = AppText.value20,
    titleLarge = AppText.h2,
    titleMedium = TextStyle(fontFamily = Bricolage,
        fontSize = 15.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(fontFamily = Bricolage,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = AppText.body,
    bodyMedium = AppText.bodyMedium,
    bodySmall = AppText.sub,
    labelLarge = AppText.button,
    labelMedium = AppText.chip,
    labelSmall = AppText.micro,
)
