/*
 * Copyright (C) 2026 FedoraTube Contributors
 *
 * This file is part of Fedora Tube (https://github.com/muhammadmarowan/FedoraTube).
 */

package com.fedoratube.org.data.recommendation

import kotlinx.serialization.json.JsonElement

/** Everything a rank pass reads besides its candidates, as a Home snapshot stores it. */
internal data class NeuroDiagnosticState(
    val brain: JsonElement,
    val categories: NeuroCategoryStore.CategoryIndex,
    val coWatch: NeuroCoWatchStore.CoWatchIndex,
)
