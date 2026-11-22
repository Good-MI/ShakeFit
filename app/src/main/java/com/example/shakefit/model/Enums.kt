package com.example.shakefit.model

enum class ExerciseType { SQUAT, PUSHUP, RUNNING }
enum class Gender { MALE, FEMALE }
enum class RunEnvironment { OUTDOOR, TREADMILL }
enum class DistanceSource { GPS, STEPS_ESTIMATE }
enum class SessionPhase { IDLE, COUNTDOWN, ACTIVE, PAUSED, FINISHED }
enum class SessionMode { FREE, CHALLENGE_60S, GOAL }
enum class GpsStatus { OFF, SEARCHING, READY, WEAK, PERMISSION_DENIED, DISABLED }

data class UiError(val code: String, val message: String)
