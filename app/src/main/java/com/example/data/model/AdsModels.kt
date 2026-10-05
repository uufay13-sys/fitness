package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AuraAdsStatus(val label: String) {
    READY("READY"),
    SEARCHING("SEARCHING PROSPECTS"),
    ANALYZING("ANALYZING REQUIREMENTS"),
    APPROACHING_CLIENT("APPROACHING CLIENT"),
    DISCUSSING("DISCUSSING"),
    NEGOTIATING("NEGOTIATING"),
    PREPARING_QUOTE("PREPARING QUOTE"),
    WAITING_FOR_OWNER_APPROVAL("WAITING FOR OWNER APPROVAL"),
    CAMPAIGN_READY("CAMPAIGN READY"),
    RUNNING("RUNNING CAMPAIGN"),
    COMPLETED("COMPLETED")
}

enum class AutonomousMode(val label: String, val description: String) {
    ASSISTED("Assisted", "AURA prepares everything but waits for owner approval before taking any external action."),
    SEMI_AUTONOMOUS("Semi-Autonomous", "AURA can communicate and negotiate within limits, but requires owner approval for final pricing, payment, and launch."),
    AUTONOMOUS("Autonomous", "AURA executes within hard spending, pricing, and communication limits configured by the owner.")
}

enum class CampaignState(val label: String, val description: String) {
    DRAFT("Draft", "Initial campaign workspace created with client requirements."),
    APPROVAL_REQUIRED("Approval Required", "Quotation prepared; pending owner approval."),
    APPROVED("Approved", "Owner approved quotation and campaign parameters."),
    PAYMENT_PENDING("Payment Pending", "Invoice issued to client; awaiting advance verification."),
    READY("Ready", "Payment verified; ready for platform launch and targeting."),
    RUNNING("Running", "Live campaign actively delivering impressions and clicks."),
    PAUSED("Paused", "Campaign temporarily halted for creative or budget adjustment."),
    COMPLETED("Completed", "Campaign duration completed and goals fulfilled."),
    CANCELLED("Cancelled", "Campaign cancelled with refund reconciliation.")
}

@Entity(tableName = "client_prospects")
data class ClientProspect(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val businessName: String,
    val category: String, // Gym, Restaurant, Hotel, Retail, Real Estate, Education, Startup, Local Brand
    val contactPerson: String,
    val contactPhone: String,
    val contactEmail: String,
    val cityLocation: String,
    val targetAudience: String,
    val adObjective: String, // Brand Awareness, Lead Generation, Store Footfall, App Installs, Website Traffic
    val preferredPlatforms: String, // Meta Ads, Google Ads, YouTube Ads
    val approxBudget: Double,
    val expectedDurationDays: Int,
    val status: String = "PROSPECT", // PROSPECT, CONTACTED, INTERESTED, NEGOTIATING, CONVERTED, LOST
    val hasCreatives: Boolean = false,
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ad_packages")
data class AdPackage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String, // BASIC, STANDARD, PREMIUM, CUSTOM
    val description: String,
    val baseAdSpend: Double,
    val serviceFee: Double,
    val creativeFee: Double,
    val platformCost: Double,
    val durationDays: Int,
    val featuresJson: String, // JSON array of features
    val isCustom: Boolean = false
)

@Entity(tableName = "pricing_rules")
data class PricingRuleEntity(
    @PrimaryKey val id: Int = 1,
    val ownerPhone: String = "8309596486", // Configured owner contact
    val isOwnerPhonePublic: Boolean = false, // Keep private unless explicitly enabled by owner
    val minCampaignPrice: Double = 4000.0,
    val baseStartingPrice: Double = 5000.0,
    val maxDiscountPercent: Double = 20.0,
    val serviceFeePercent: Double = 15.0,
    val creativeProductionFee: Double = 1200.0,
    val managementFeePercent: Double = 10.0,
    val minProfitMarginPercent: Double = 25.0,
    val autonomousMode: String = "ASSISTED"
)

@Entity(tableName = "ad_quotations")
data class AdQuotation(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quotationCode: String, // e.g. UMR-ADS-2026-0001
    val prospectId: Long,
    val clientName: String,
    val businessName: String,
    val campaignDescription: String,
    val includedServicesJson: String,
    val durationDays: Int,
    val adSpend: Double,
    val serviceFee: Double,
    val creativeFee: Double,
    val platformFee: Double,
    val discountAmount: Double,
    val taxAmount: Double,
    val finalAmount: Double,
    val estimatedProfit: Double,
    val paymentTerms: String = "50% Advance on approval, 50% on campaign launch verification",
    val refundTerms: String = "Ad spend refunded if unspent prior to ad approval. Service fees non-refundable once creative production begins.",
    val conditions: String = "All ad creatives subject to platform policy guidelines. No guaranteed sales figures.",
    val isApprovedByOwner: Boolean = false,
    val status: String = "PENDING_APPROVAL", // PENDING_APPROVAL, APPROVED, REJECTED, SENT_TO_CLIENT, ACCEPTED
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "owner_approval_requests")
data class OwnerApprovalRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val quotationId: Long,
    val ownerPhone: String = "8309596486",
    val clientName: String,
    val businessName: String,
    val campaignObjective: String,
    val platforms: String,
    val durationDays: Int,
    val targetAudience: String,
    val adSpend: Double,
    val serviceFee: Double,
    val creativeFee: Double,
    val discount: Double,
    val finalPrice: Double,
    val estimatedProfit: Double,
    val clientRequirements: String,
    val startDate: Long,
    val endDate: Long,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED, EDIT_REQUESTED, NEGOTIATE
    val ownerNotes: String = "",
    val requestedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "ad_campaigns")
data class AdCampaign(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val prospectId: Long,
    val quotationId: Long,
    val businessName: String,
    val status: String = "DRAFT", // DRAFT, APPROVAL_REQUIRED, APPROVED, PAYMENT_PENDING, READY, RUNNING, PAUSED, COMPLETED, CANCELLED
    val platform: String,
    val budget: Double,
    val spent: Double = 0.0,
    val revenue: Double,
    val profit: Double,
    val startDate: Long,
    val endDate: Long,
    val impressions: Long = 0,
    val clicks: Long = 0,
    val conversions: Long = 0,
    val ctr: Float = 0f,
    val cpc: Float = 0f,
    val apiIntegrationStatus: String = "MANUAL_SETUP_REQUIRED", // MANUAL_SETUP_REQUIRED, META_ADS_CONNECTED, GOOGLE_ADS_CONNECTED
    val optimizationRecommendation: String = "Campaign launch verified. Monitoring audience CTR & engagement."
)

@Entity(tableName = "ad_payment_records")
data class AdPaymentRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val campaignId: Long,
    val quotationCode: String,
    val clientName: String,
    val amount: Double,
    val paymentMethod: String = "UPI / Verified Bank",
    val status: String = "PAYMENT_PENDING", // PAYMENT_PENDING, VERIFIED, FAILED, REFUNDED
    val transactionReference: String = "",
    val verifiedAt: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "client_communications")
data class ClientCommunicationMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val prospectId: Long,
    val sender: String, // AURA_ADS_AGENT, CLIENT, OWNER
    val channel: String = "IN_APP", // IN_APP, SMS_8309596486, EMAIL, WHATSAPP
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "ai_ad_action_logs")
data class AiAdActionLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val actionType: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)
