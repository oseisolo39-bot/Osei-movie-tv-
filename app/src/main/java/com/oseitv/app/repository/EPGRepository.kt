package com.oseitv.app.repository

import com.oseitv.app.models.Program
import java.util.*

object EPGRepository {
    private val shows = listOf(
        "Morning News", "Breakfast Show", "Daily Debate", "Movie: Action Heroes",
        "Docuseries: Planet Earth", "Cooking with Chef", "Afternoon Drama", "Sports Highlights",
        "Evening News", "Primetime Movie", "Late Night Talk Show", "Music Videos",
        "Special Report", "Sitcom Re-runs", "Reality TV Hour", "Tech Review",
        "Cartoons", "Live Sports", "Documentary", "Standup Comedy"
    )

    fun generateSchedule(channelId: String): List<Program> {
        val seed = channelId.sumOf { it.code }
        val schedule = mutableListOf<Program>()
        
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        
        val currentTime = startOfDay.clone() as Calendar
        val endOfDay = startOfDay.clone() as Calendar
        endOfDay.add(Calendar.DAY_OF_YEAR, 1)

        var i = 0
        while (currentTime.before(endOfDay)) {
            val showSeed = (seed + i + currentTime.get(Calendar.HOUR_OF_DAY)) % shows.size
            val durationMinutes = when {
                showSeed % 3 == 0 -> 30
                showSeed % 2 == 0 -> 60
                else -> 120
            }

            val showEndTime = currentTime.clone() as Calendar
            showEndTime.add(Calendar.MINUTE, durationMinutes)

            schedule.add(
                Program(
                    title = shows[showSeed],
                    startTime = currentTime.clone() as Calendar,
                    endTime = showEndTime,
                    description = "Watch ${shows[showSeed]} live on Osei TV."
                )
            )

            currentTime.timeInMillis = showEndTime.timeInMillis
            i++
        }
        return schedule
    }

    fun getCurrentlyPlaying(schedule: List<Program>): Program? {
        val now = Calendar.getInstance()
        return schedule.find { now.after(it.startTime) && now.before(it.endTime) }
    }
}
