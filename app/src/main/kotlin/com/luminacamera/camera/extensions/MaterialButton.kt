package com.luminacamera.camera.extensions

import androidx.annotation.DrawableRes
import com.google.android.material.button.MaterialButton
import com.luminacamera.camera.R
import com.luminacamera.camera.views.ShadowDrawable

fun MaterialButton.setShadowIcon(@DrawableRes drawableResId: Int) {
    icon = ShadowDrawable(context, drawableResId, R.style.TopIconShadow)
}
