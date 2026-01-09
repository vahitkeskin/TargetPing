package com.vahitkeskin.targetping.domain.usecase

import android.location.Location
import com.vahitkeskin.targetping.domain.model.TargetLocation
import javax.inject.Inject

class CheckProximityUseCase @Inject constructor() {
    // Returns list of targets that are within radius and haven't been triggered recently (e.g. 5 mins)
    operator fun invoke(currentLocation: Location, targets: List<TargetLocation>): List<TargetLocation> {
        return targets.filter { target ->
            val results = FloatArray(1)
            Location.distanceBetween(
                currentLocation.latitude, currentLocation.longitude,
                target.latitude, target.longitude,
                results
            )
            val distance = results[0]

            // Logic: Inside radius AND (never triggered OR triggered > 5 mins ago)
            distance <= target.radiusMeters &&
                    (System.currentTimeMillis() - target.lastTriggered > 5 * 60 * 1000)
        }
    }
}