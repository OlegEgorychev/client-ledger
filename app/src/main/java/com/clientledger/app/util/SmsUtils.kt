package com.clientledger.app.util

import android.content.Context
import android.content.Intent
import android.net.Uri

object SmsUtils {
    /**
     * Opens the system SMS composer with a pre-filled phone number and message body.
     * Uses ACTION_SENDTO with smsto: URI scheme (no SEND_SMS permission required).
     * User must explicitly confirm sending in the SMS app.
     * 
     * @param context Application context
     * @param phoneNumber Phone number (can be in any format, will be passed to SMS app)
     * @param messageBody Pre-filled message text
     * @return true if SMS composer was opened successfully, false otherwise
     */
    fun openSmsComposer(context: Context, phoneNumber: String, messageBody: String): Boolean {
        return try {
            val uri = Uri.parse("smsto:$phoneNumber")
            val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
                putExtra("sms_body", messageBody)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if a phone number is valid for sending SMS.
     * At minimum, the phone number should not be blank.
     * 
     * @param phoneNumber Phone number to validate
     * @return true if phone number is valid, false otherwise
     */
    fun isValidPhoneForSms(phoneNumber: String?): Boolean {
        return !phoneNumber.isNullOrBlank()
    }
}
