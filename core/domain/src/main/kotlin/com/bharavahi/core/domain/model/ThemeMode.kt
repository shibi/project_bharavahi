package com.bharavahi.core.domain.model

enum class ThemeMode {
    SYSTEM, LIGHT, DARK;

    companion object {
        /** UI-5: used until the admin picks a mode in Profile. */
        val DEFAULT = LIGHT
    }
}
