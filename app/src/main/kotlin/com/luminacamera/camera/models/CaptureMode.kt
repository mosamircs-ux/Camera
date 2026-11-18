package com.luminacamera.camera.models

import androidx.annotation.StringRes
import com.luminacamera.camera.R

enum class CaptureMode(@StringRes val stringResId: Int) {
    MINIMIZE_LATENCY(R.string.minimize_latency),
    MAXIMIZE_QUALITY(R.string.maximize_quality)
}
