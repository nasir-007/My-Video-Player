package com.example.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object LibraryRoute

@Serializable
data class PlayerRoute(
    val uriString: String,
    val title: String,
    val subtitleUri: String? = null,
    val folderUri: String? = null
)
