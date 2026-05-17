package com.ottapp.moviestream.ui.onboarding

import androidx.annotation.DrawableRes

data class OnboardSlide(
    @DrawableRes val imageRes: Int,
    val title: String,
    val subtitle: String
)
