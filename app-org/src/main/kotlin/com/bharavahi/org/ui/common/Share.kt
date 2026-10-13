package com.bharavahi.org.ui.common

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

/** ORG-31: opens WhatsApp with [text] ready to send; the admin picks the group. Falls back to the share sheet. */
fun shareToWhatsApp(context: Context, text: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    try {
        context.startActivity(Intent(send).setPackage(WHATSAPP))
    } catch (e: ActivityNotFoundException) {
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}

private const val WHATSAPP = "com.whatsapp"
