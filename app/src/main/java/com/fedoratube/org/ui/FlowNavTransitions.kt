package com.fedoratube.org.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavBackStackEntry

internal object FlowNavTransitions {
    // YouTube-style: fade only, no horizontal slide. A slide here reads as a screen
    // shake when switching top categories or going back.
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(200, easing = FastOutSlowInEasing))
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(180, easing = FastOutLinearInEasing))
    }

    // A back gesture seeks these by its progress, so every part shares one linear duration.
    val predictivePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
        fadeIn(animationSpec = tween(PREDICTIVE_BACK_DURATION_MS, easing = LinearEasing))
    }
    val predictivePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = {
        fadeOut(animationSpec = tween(PREDICTIVE_BACK_DURATION_MS, easing = LinearEasing))
    }
}

private const val PREDICTIVE_BACK_DURATION_MS = 300
