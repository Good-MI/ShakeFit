package com.example.shakefit.ui.theme

import androidx.compose.ui.graphics.Color
import com.example.shakefit.model.ExerciseType

// Primary (Coral/Orange - Energetic & Sporty)
val PrimaryLight = Color(0xFFFF5722)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFFFDBD0)
val OnPrimaryContainerLight = Color(0xFF3E1000)

val PrimaryDark = Color(0xFFFF8A65)
val OnPrimaryDark = Color(0xFF3E1000)
val PrimaryContainerDark = Color(0xFF662009)
val OnPrimaryContainerDark = Color(0xFFFFDBD0)

// Secondary (Teal - Vitality)
val SecondaryLight = Color(0xFF00897B)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFB2DFDB)
val OnSecondaryContainerLight = Color(0xFF00332C)

val SecondaryDark = Color(0xFF4DB6AC)
val OnSecondaryDark = Color(0xFF003731)
val SecondaryContainerDark = Color(0xFF004D40)
val OnSecondaryContainerDark = Color(0xFFB2DFDB)

// Tertiary (Gold/Amber - Streak, Gamifikasi, Badge)
val TertiaryLight = Color(0xFFFFB300)
val OnTertiaryLight = Color(0xFF000000)
val TertiaryContainerLight = Color(0xFFFFF3CD)
val OnTertiaryContainerLight = Color(0xFF4D3800)

val TertiaryDark = Color(0xFFFFD54F)
val OnTertiaryDark = Color(0xFF4D3800)
val TertiaryContainerDark = Color(0xFF4D3800)
val OnTertiaryContainerDark = Color(0xFFFFE082)

// Background & Surface
val BackgroundLight = Color(0xFFF8F9FA)
val OnBackgroundLight = Color(0xFF191C1E)
val SurfaceLight = Color(0xFFFFFFFF)
val OnSurfaceLight = Color(0xFF191C1E)
val SurfaceVariantLight = Color(0xFFEEF1F5)
val OnSurfaceVariantLight = Color(0xFF42474E)
val OutlineLight = Color(0xFFD0D7DE)

val BackgroundDark = Color(0xFF0F1115)
val OnBackgroundDark = Color(0xFFE2E2E6)
val SurfaceDark = Color(0xFF171A21)
val OnSurfaceDark = Color(0xFFE2E2E6)
val SurfaceVariantDark = Color(0xFF232834)
val OnSurfaceVariantDark = Color(0xFFC3C7D0)
val OutlineDark = Color(0xFF323949)

// Semantic Status
val ErrorLight = Color(0xFFD32F2F)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorDark = Color(0xFFEF5350)
val OnErrorDark = Color(0xFF490002)

val SuccessLight = Color(0xFF2E7D32)
val SuccessDark = Color(0xFF66BB6A)
val WarningLight = Color(0xFFED6C02)
val WarningDark = Color(0xFFFFA726)

// Exercise Type Accents (DesignShakefit.md Bagian 4.1)
val SquatAccentLight = Color(0xFF5C6BC0)
val SquatAccentDark = Color(0xFF9FA8DA)
val PushupAccentLight = Color(0xFFFF5722)
val PushupAccentDark = Color(0xFFFF8A65)
val RunningAccentLight = Color(0xFF00897B)
val RunningAccentDark = Color(0xFF4DB6AC)

fun getExerciseAccentColor(type: ExerciseType, isDark: Boolean): Color {
    return when (type) {
        ExerciseType.SQUAT -> if (isDark) SquatAccentDark else SquatAccentLight
        ExerciseType.PUSHUP -> if (isDark) PushupAccentDark else PushupAccentLight
        ExerciseType.RUNNING -> if (isDark) RunningAccentDark else RunningAccentLight
    }
}
