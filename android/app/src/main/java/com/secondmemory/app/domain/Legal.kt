package com.secondmemory.app.domain

object Legal {
    const val privacyPolicyUrl = "https://rentclock.com/second-memory/privacy"

    const val onDeviceConsent =
        "Pins, photos and files stay on this phone. Nothing is uploaded. That is user consent: share, pin and backup only happen when you tap them. There are no ads and no advertising ID to opt-out of. This is the prominent disclosure."

    fun showPermissionRationale(): String =
        "Pinned items live in the notification shade. Android needs a permission explanation before it can show them. You can decline; items still save in the app."
}
