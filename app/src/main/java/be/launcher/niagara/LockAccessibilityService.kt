package be.launcher.niagara

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Build
import android.view.accessibility.AccessibilityEvent

/**
 * Doet maar één ding: het scherm vergrendelen als je dubbeltikt op de letterbalk.
 * Het leest geen schermen of tekst (zie res/xml/lock_service.xml).
 */
class LockAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onUnbind(intent: Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    companion object {
        @Volatile
        var instance: LockAccessibilityService? = null
            private set

        val isEnabled: Boolean get() = instance != null

        /** Geeft false terug als de service uit staat of Android te oud is (voor Android 9). */
        fun lockScreen(): Boolean {
            val service = instance ?: return false
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            } else {
                false
            }
        }
    }
}
