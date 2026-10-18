package be.launcher.niagara.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import be.launcher.niagara.AppEntry

fun openAppInfo(context: Context, app: AppEntry) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", app.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

fun uninstallApp(context: Context, app: AppEntry) {
    val intent = Intent(Intent.ACTION_DELETE)
        .setData(Uri.fromParts("package", app.packageName, null))
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}
