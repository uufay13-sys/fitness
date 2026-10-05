package com.example.data.manager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.example.data.model.AdQuotation
import com.example.data.model.OwnerApprovalRequest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OwnerContactIntegration {

    const val DEFAULT_OWNER_PHONE: String = "8309596486"
    const val COUNTRY_CODE_PREFIX: String = "+91"

    /**
     * Enforce privacy protection: Never expose owner's phone number publicly
     * unless explicitly toggled by owner.
     */
    fun getDisplayPhone(ownerPhone: String, isPublic: Boolean): String {
        return if (isPublic) {
            ownerPhone
        } else {
            val last4 = if (ownerPhone.length >= 4) ownerPhone.takeLast(4) else ownerPhone
            "******$last4 (Private)"
        }
    }

    /**
     * Formats official owner approval dispatch payload containing all necessary campaign
     * breakdown items mandated by UMR AURA governance.
     */
    fun formatApprovalDispatchMessage(request: OwnerApprovalRequest): String {
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        val startFormatted = dateFormat.format(Date(request.startDate))
        val endFormatted = dateFormat.format(Date(request.endDate))

        return """
            [AURA ADS AGENT — OWNER APPROVAL DISPATCH]
            Destination Contact: 8309596486
            
            Client: ${request.clientName}
            Business: ${request.businessName}
            Objective: ${request.campaignObjective}
            Platforms: ${request.platforms}
            Duration: ${request.durationDays} Days ($startFormatted to $endFormatted)
            Audience: ${request.targetAudience}
            
            COMMERCIAL BREAKDOWN:
            • Media Ad Spend: ₹${request.adSpend.toInt()}
            • Service Fee: ₹${request.serviceFee.toInt()}
            • Creative Fee: ₹${request.creativeFee.toInt()}
            • Concession/Discount: ₹${request.discount.toInt()}
            • Final Client Price: ₹${request.finalPrice.toInt()}
            • Projected Profit: ₹${request.estimatedProfit.toInt()}
            
            Scope: ${request.clientRequirements}
            
            STATUS: PENDING OWNER APPROVAL
        """.trimIndent()
    }

    /**
     * Dispatches SMS approval proposal to 8309596486
     */
    fun dispatchSmsApproval(context: Context, request: OwnerApprovalRequest, ownerPhone: String = DEFAULT_OWNER_PHONE) {
        val message = formatApprovalDispatchMessage(request)
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$ownerPhone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open SMS app: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Dispatches WhatsApp proposal to 8309596486
     */
    fun dispatchWhatsAppApproval(context: Context, request: OwnerApprovalRequest, ownerPhone: String = DEFAULT_OWNER_PHONE) {
        val message = formatApprovalDispatchMessage(request)
        try {
            val fullNumber = if (ownerPhone.startsWith("+")) ownerPhone.removePrefix("+") else "91$ownerPhone"
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$fullNumber&text=" + Uri.encode(message))
            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Initiates phone call intent to owner 8309596486
     */
    fun callOwner(context: Context, ownerPhone: String = DEFAULT_OWNER_PHONE) {
        try {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:$ownerPhone")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open dialer: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
