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
    private val tabOrder = listOf("home", "subscriptions", "music", "downloads", "library")

    private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTabForward(): Boolean? {
        val fromRoute = initialState.destination.route
        val toRoute = targetState.destination.route
        val fromIndex = tabOrder.indexOf(fromRoute)
        val toIndex = tabOrder.indexOf(toRoute)
        return if (fromIndex != -1 && toIndex != -1 && fromIndex != toIndex) fromIndex < toIndex else null
    }

    // YouTube-style: fluid directional horizontal slide between Home, Subs, and Music tabs,
    // combined with seamless fade.
    val enter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (isTabForward()) {
            true ->
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))

            false ->
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))

            null -> fadeIn(animationSpec = tween(150, easing = FastOutSlowInEasing))
        }
    }

    val exit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (isTabForward()) {
            true ->
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(200, easing = FastOutLinearInEasing),
                ) + fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing))

            false ->
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(200, easing = FastOutLinearInEasing),
                ) + fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing))

            null -> fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
        }
    }

    val popEnter: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
        when (isTabForward()) {
            true ->
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))

            false ->
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(220, easing = FastOutSlowInEasing),
                ) + fadeIn(animationSpec = tween(180, easing = FastOutSlowInEasing))

            null -> fadeIn(animationSpec = tween(150, easing = FastOutSlowInEasing))
        }
    }

    val popExit: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
        when (isTabForward()) {
            true ->
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(200, easing = FastOutLinearInEasing),
                ) + fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing))

            false ->
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(200, easing = FastOutLinearInEasing),
                ) + fadeOut(animationSpec = tween(160, easing = FastOutLinearInEasing))

            null -> fadeOut(animationSpec = tween(120, easing = FastOutLinearInEasing))
        }
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
