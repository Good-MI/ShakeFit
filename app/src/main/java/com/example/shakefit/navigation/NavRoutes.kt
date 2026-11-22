package com.example.shakefit.navigation

/**
 * Definisi rute navigasi untuk ShakeFit.
 * Sesuai DesignShakefit.md bagian 5 (4-Tab Bottom Navigation + layar full-screen).
 */
object NavRoutes {
    // Tab utama (Bottom Navigation)
    const val HOME = "home"
    const val GOALS = "goals"
    const val ACTIVITY = "activity"
    const val PROFILE = "profile"

    // Layar full-screen (tanpa Bottom Bar)
    const val REP_SESSION = "rep_session/{exerciseType}"
    const val RUN_SESSION = "run_session"
    const val SETTINGS = "settings"
    const val SENSOR_DEBUG = "sensor_debug"

    // Helper untuk navigasi dengan argumen
    fun repSession(exerciseType: String): String = "rep_session/$exerciseType"
}
