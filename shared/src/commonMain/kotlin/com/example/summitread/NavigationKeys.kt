package com.example.summitread

import kotlinx.serialization.Serializable

@Serializable
data object HomeRoute

@Serializable
data class MountainRoute(val bookId: Long)

@Serializable
data object ProfileRoute
