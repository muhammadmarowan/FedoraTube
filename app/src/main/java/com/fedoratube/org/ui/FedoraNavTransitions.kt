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
    // YouTube-style: seamless fade only, no scale or slide. This ensures switching between Home,
    // Subscriptions, and Music tabs has ZERO screen shake or jitter.
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(150, easing = FastOutSlowInEasing))
    }
    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
    }
    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        fadeIn(animationSpec = tween(150, easing = FastOutSlowInEasing))
    }
    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
    }

    // A back gesture seeks these by its progress, so every part shares one linear duration.
    val predictivePopEnter: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> EnterTransition = {
        fadeIn(animationSpec = tween(PREDICTIVE_BACK_DURATION_MS, easing = LinearEasing))
    }
    val predictivePopExit: AnimatedContentTransitionScope<NavBackStackEntry>.(Int) -> ExitTransition = {
        fadeOut(animationSpec = tween(PREDICTIVE_BACK_DURATION_MS, easing = LinearEasing))
    }
}

internal typealias FedoraNavTransitions = FlowNavTransitions

private const val PREDICTIVE_BACK_DURATION_MS = 300
