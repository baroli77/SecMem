package com.secondmemory.app.domain

object PinStyle {
    val colors = listOf("forest", "clay", "slate", "rose", "navy")

    fun argb(name: String): Int = when (name) {
        "clay" -> 0xFF8C5A2B.toInt()
        "slate" -> 0xFF3D4A54.toInt()
        "rose" -> 0xFF7A3E4A.toInt()
        "navy" -> 0xFF2C3E6B.toInt()
        else -> 0xFF2C5C4F.toInt()
    }

    fun label(name: String): String = name.replaceFirstChar { it.uppercase() }

    fun restoredDuplicate(existing: Thing): Thing = existing.copy(
        isPinned = true,
        isFavourite = false,
        status = ThingStatus.ACTIVE,
        completedAt = null,
        archivedAt = null,
        resurfaceAt = null,
        reasonForResurface = null,
        updatedAt = System.currentTimeMillis(),
    )
}
