package com.example.data.repository

import com.example.data.local.AdsDao
import com.example.data.manager.CampaignStateManager
import com.example.data.manager.OwnerContactIntegration
import com.example.data.model.AdCampaign
import com.example.data.model.AdPaymentRecord
import com.example.data.model.AdQuotation
import com.example.data.model.AiAdActionLog
import com.example.data.model.CampaignState
import com.example.data.model.OwnerApprovalRequest
import com.example.data.model.PricingRuleEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.withContext

/**
 * Commercial summary aggregate for the business owner dashboard and telemetry views.
 */
data class OwnerCommercialSummary(
    val totalAdSpend: Double = 0.0,
    val totalClientRevenue: Double = 0.0,
    val totalGrossProfit: Double = 0.0,
    val pendingPaymentsAmount: Double = 0.0,
    val totalCampaignsCount: Int = 0,
    val draftCount: Int = 0,
    val pendingApprovalCount: Int = 0,
    val activeRunningCount: Int = 0,
    val pausedCount: Int = 0,
    val completedCount: Int = 0,
    val cancelledCount: Int = 0
)

/**
 * AdsCampaignRepository provides the authoritative repository and persistence interface
 * for the business owner to govern campaign lifecycles and state transitions:
 *
 *     DRAFT  ──>  APPROVAL_REQUIRED  ──>  APPROVED / PAYMENT_PENDING  ──>  READY  ──>  RUNNING  ──>  COMPLETED
 *       │                 │                          │                       │            │             │
 *       └──[CANCEL]───────┴──────[CANCEL]────────────┴────────[CANCEL]───────┴─[PAUSE]───┴──[CANCEL]───┘
 */
class AdsCampaignRepository(
    private val adsDao: AdsDao,
    val stateManager: CampaignStateManager = CampaignStateManager(adsDao)
) {

    // ─────────────────────────────────────────────────────────────────────────────
    // PERSISTENT FLOWS FOR BUSINESS OWNER
    // ─────────────────────────────────────────────────────────────────────────────

    val allCampaigns: Flow<List<AdCampaign>> = adsDao.getAllCampaigns()
    val draftCampaigns: Flow<List<AdCampaign>> = adsDao.getCampaignsByStatus(CampaignState.DRAFT.name)
    val pendingApprovalCampaigns: Flow<List<AdCampaign>> = adsDao.getCampaignsByStatus(CampaignState.APPROVAL_REQUIRED.name)
    val approvedCampaigns: Flow<List<AdCampaign>> = adsDao.getCampaignsByStatus(CampaignState.APPROVED.name)
    val runningCampaigns: Flow<List<AdCampaign>> = adsDao.getCampaignsByStatus(CampaignState.RUNNING.name)
    val pausedCampaigns: Flow<List<AdCampaign>> = adsDao.getCampaignsByStatus(CampaignState.PAUSED.name)
    val completedCampaigns: Flow<List<AdCampaign>> = adsDao.getCampaignsByStatus(CampaignState.COMPLETED.name)
    val cancelledCampaigns: Flow<List<AdCampaign>> = adsDao.getCampaignsByStatus(CampaignState.CANCELLED.name)

    val pendingApprovals: Flow<List<OwnerApprovalRequest>> = adsDao.getPendingApprovalRequests()
    val allApprovals: Flow<List<OwnerApprovalRequest>> = adsDao.getAllApprovalRequests()
    val allPayments: Flow<List<AdPaymentRecord>> = adsDao.getAllPayments()
    val pricingRules: Flow<PricingRuleEntity?> = adsDao.getPricingRules()
    val actionLogs: Flow<List<AiAdActionLog>> = adsDao.getRecentActionLogs()

    /**
     * Real-time commercial telemetry aggregate for the business owner.
     */
    val ownerCommercialSummary: Flow<OwnerCommercialSummary> = combine(
        allCampaigns,
        allPayments,
        pendingApprovals
    ) { campaigns, payments, approvals ->
        val totalAdSpend = campaigns.sumOf { it.spent }
        val totalRevenue = campaigns.filter { it.status != CampaignState.CANCELLED.name }.sumOf { it.revenue }
        val totalProfit = campaigns.filter { it.status != CampaignState.CANCELLED.name }.sumOf { it.profit }
        val pendingPaymentsAmount = payments.filter { it.status == "PAYMENT_PENDING" }.sumOf { it.amount }

        OwnerCommercialSummary(
            totalAdSpend = totalAdSpend,
            totalClientRevenue = totalRevenue,
            totalGrossProfit = totalProfit,
            pendingPaymentsAmount = pendingPaymentsAmount,
            totalCampaignsCount = campaigns.size,
            draftCount = campaigns.count { it.status == CampaignState.DRAFT.name },
            pendingApprovalCount = approvals.count { it.status == "PENDING" },
            activeRunningCount = campaigns.count { it.status == CampaignState.RUNNING.name },
            pausedCount = campaigns.count { it.status == CampaignState.PAUSED.name },
            completedCount = campaigns.count { it.status == CampaignState.COMPLETED.name },
            cancelledCount = campaigns.count { it.status == CampaignState.CANCELLED.name }
        )
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // CAMPAIGN LIFECYCLE & STATE TRANSITIONS (Draft -> Approval -> Running -> Completed)
    // ─────────────────────────────────────────────────────────────────────────────

    /**
     * 1. DRAFT STAGE:
     * Creates a new campaign workspace in DRAFT state with client requirements and commercial terms.
     */
    suspend fun createDraftCampaign(
        prospectId: Long,
        quotationId: Long,
        title: String,
        businessName: String,
        platform: String,
        budget: Double,
        revenue: Double,
        profit: Double,
        durationDays: Int,
        startDate: Long = System.currentTimeMillis()
    ): Long = withContext(Dispatchers.IO) {
        val endDate = startDate + (durationDays.toLong() * 86_400_000L)
        val campaign = AdCampaign(
            title = title,
            prospectId = prospectId,
            quotationId = quotationId,
            businessName = businessName,
            status = CampaignState.DRAFT.name,
            platform = platform,
            budget = budget,
            spent = 0.0,
            revenue = revenue,
            profit = profit,
            startDate = startDate,
            endDate = endDate,
            impressions = 0,
            clicks = 0,
            conversions = 0,
            ctr = 0f,
            cpc = 0f,
            apiIntegrationStatus = "MANUAL_SETUP_REQUIRED",
            optimizationRecommendation = "Campaign draft created. Awaiting owner commercial signoff."
        )
        val id = adsDao.insertCampaign(campaign)
        logOwnerAction(
            "CAMPAIGN_DRAFT_CREATED",
            "Created DRAFT campaign '$title' for $businessName (Budget: ₹${budget.toInt()}, Revenue: ₹${revenue.toInt()})"
        )
        id
    }

    /**
     * 2. APPROVAL STAGE:
     * Transitions campaign from DRAFT to APPROVAL_REQUIRED, ensuring an official proposal
     * is dispatched to the configured owner contact (8309596486).
     */
    suspend fun submitForApproval(
        campaignId: Long,
        quotation: AdQuotation? = null
    ): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val campaign = adsDao.getCampaignById(campaignId)
            ?: return@withContext Result.failure(IllegalArgumentException("Campaign $campaignId not found"))

        val rules = getPricingRulesOnce()

        // Create approval request if not already present
        val quote = quotation ?: adsDao.getQuotationById(campaign.quotationId)
        if (quote != null) {
            val approval = OwnerApprovalRequest(
                quotationId = quote.id,
                ownerPhone = rules.ownerPhone,
                clientName = quote.clientName,
                businessName = quote.businessName,
                campaignObjective = campaign.title,
                platforms = campaign.platform,
                durationDays = quote.durationDays,
                targetAudience = "Targeted segment for ${quote.businessName}",
                adSpend = quote.adSpend,
                serviceFee = quote.serviceFee,
                creativeFee = quote.creativeFee,
                discount = quote.discountAmount,
                finalPrice = quote.finalAmount,
                estimatedProfit = quote.estimatedProfit,
                clientRequirements = quote.campaignDescription,
                startDate = campaign.startDate,
                endDate = campaign.endDate,
                status = "PENDING",
                requestedAt = System.currentTimeMillis()
            )
            adsDao.insertApprovalRequest(approval)
        }

        val result = stateManager.transitionState(
            campaignId = campaignId,
            targetState = CampaignState.APPROVAL_REQUIRED,
            reason = "Submitted for owner approval dispatch to ${rules.ownerPhone}",
            optimizationNote = "Pending owner commercial signoff on price ₹${campaign.revenue.toInt()} (Contact: ${rules.ownerPhone})."
        )

        if (result.isSuccess) {
            logOwnerAction(
                "APPROVAL_SUBMITTED",
                "Dispatched approval proposal for '${campaign.title}' to owner contact ${rules.ownerPhone}"
            )
        }
        result
    }

    /**
     * 3. OWNER APPROVAL:
     * Business owner approves the campaign proposal.
     * Transitions state from APPROVAL_REQUIRED to APPROVED and automatically provisions
     * a PAYMENT_PENDING invoice record.
     */
    suspend fun approveCampaignByOwner(
        approvalId: Long,
        campaignId: Long,
        ownerNotes: String = ""
    ): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val approval = adsDao.getApprovalRequestById(approvalId)
        val campaign = adsDao.getCampaignById(campaignId)
            ?: return@withContext Result.failure(IllegalArgumentException("Campaign $campaignId not found"))

        if (approval != null) {
            adsDao.updateApprovalRequest(approval.copy(status = "APPROVED", ownerNotes = ownerNotes))
        }

        // Mark quotation approved
        if (campaign.quotationId > 0) {
            adsDao.getQuotationById(campaign.quotationId)?.let { quote ->
                adsDao.updateQuotation(quote.copy(isApprovedByOwner = true, status = "APPROVED"))
            }
        }

        // Transition from APPROVAL_REQUIRED -> APPROVED
        val approvedResult = stateManager.transitionState(
            campaignId = campaignId,
            targetState = CampaignState.APPROVED,
            reason = "Owner authorized campaign parameters${if (ownerNotes.isNotBlank()) ": $ownerNotes" else ""}",
            optimizationNote = "Approved by owner (8309596486). Awaiting client advance verification."
        )

        if (approvedResult.isFailure) {
            return@withContext approvedResult
        }

        // Immediately move into PAYMENT_PENDING and register invoice
        val quoteCode = adsDao.getQuotationById(campaign.quotationId)?.quotationCode ?: "UMR-ADS-${campaign.id}"
        adsDao.insertPayment(
            AdPaymentRecord(
                campaignId = campaign.id,
                quotationCode = quoteCode,
                clientName = approval?.clientName ?: campaign.businessName,
                amount = campaign.revenue,
                paymentMethod = "Verified UPI / Bank Transfer",
                status = "PAYMENT_PENDING",
                createdAt = System.currentTimeMillis()
            )
        )

        val paymentPendingResult = stateManager.transitionState(
            campaignId = campaignId,
            targetState = CampaignState.PAYMENT_PENDING,
            reason = "Owner approved. Advance payment invoice generated.",
            optimizationNote = "Awaiting payment verification before live deployment."
        )

        logOwnerAction(
            "OWNER_APPROVED",
            "Owner approved '${campaign.title}' (Quotation: $quoteCode, Value: ₹${campaign.revenue.toInt()})"
        )

        paymentPendingResult
    }

    /**
     * 3b. OWNER REJECTION:
     * Owner rejects the proposal or requests alterations.
     */
    suspend fun rejectCampaignByOwner(
        approvalId: Long,
        campaignId: Long,
        reason: String
    ): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val approval = adsDao.getApprovalRequestById(approvalId)
        if (approval != null) {
            adsDao.updateApprovalRequest(approval.copy(status = "REJECTED", ownerNotes = reason))
        }

        if (campaignId > 0) {
            val campaign = adsDao.getCampaignById(campaignId)
            if (campaign != null && campaign.quotationId > 0) {
                adsDao.getQuotationById(campaign.quotationId)?.let { quote ->
                    adsDao.updateQuotation(quote.copy(status = "REJECTED"))
                }
            }
        }

        val result = stateManager.transitionState(
            campaignId = campaignId,
            targetState = CampaignState.DRAFT,
            reason = "Owner rejected with notes: $reason",
            optimizationNote = "Quotation revised. Owner notes: $reason"
        )

        logOwnerAction("OWNER_REJECTED", "Owner rejected campaign $campaignId: $reason")
        result
    }

    /**
     * 4. PAYMENT VERIFICATION -> READY:
     * Validates that payment has been received and moves campaign from PAYMENT_PENDING to READY.
     */
    suspend fun verifyPaymentAndMarkReady(
        paymentId: Long,
        transactionRef: String
    ): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val payment = adsDao.getPaymentById(paymentId)
            ?: return@withContext Result.failure(IllegalArgumentException("Payment record $paymentId not found"))

        val verified = payment.copy(
            status = "VERIFIED",
            transactionReference = transactionRef,
            verifiedAt = System.currentTimeMillis()
        )
        adsDao.updatePayment(verified)

        val result = stateManager.transitionState(
            campaignId = payment.campaignId,
            targetState = CampaignState.READY,
            reason = "Payment verified with reference $transactionRef",
            optimizationNote = "Payment verified (Ref: $transactionRef). Campaign is ready for launch."
        )

        logOwnerAction(
            "PAYMENT_VERIFIED",
            "Verified payment of ₹${payment.amount.toInt()} for ${payment.quotationCode} (Ref: $transactionRef). Marked READY."
        )
        result
    }

    /**
     * 5. RUNNING STAGE:
     * Business owner launches campaign into live delivery.
     * Transitions state from READY to RUNNING.
     */
    suspend fun launchCampaign(campaignId: Long): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val result = stateManager.launchCampaign(campaignId)
        if (result.isSuccess) {
            val camp = result.getOrNull()
            logOwnerAction(
                "CAMPAIGN_LAUNCHED",
                "Launched campaign '${camp?.title}' on ${camp?.platform} (Ad spend: ₹${camp?.budget?.toInt()})"
            )
        }
        result
    }

    /**
     * Temporary halt for creative, budget, or policy reasons.
     */
    suspend fun pauseCampaign(campaignId: Long, reason: String = "Operator paused"): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val result = stateManager.pauseCampaign(campaignId, reason)
        logOwnerAction("CAMPAIGN_PAUSED", "Paused campaign $campaignId: $reason")
        result
    }

    /**
     * Resumes a paused campaign back to RUNNING.
     */
    suspend fun resumeCampaign(campaignId: Long): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val result = stateManager.resumeCampaign(campaignId)
        logOwnerAction("CAMPAIGN_RESUMED", "Resumed campaign $campaignId to live delivery")
        result
    }

    /**
     * 6. COMPLETED STAGE:
     * Campaign schedule and deliverables successfully completed.
     * Transitions state from RUNNING to COMPLETED.
     */
    suspend fun completeCampaign(
        campaignId: Long,
        completionSummary: String = "All campaign objectives achieved"
    ): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val result = stateManager.completeCampaign(campaignId)
        if (result.isSuccess) {
            val camp = result.getOrNull()
            logOwnerAction(
                "CAMPAIGN_COMPLETED",
                "Completed campaign '${camp?.title}': Delivered ${camp?.impressions} impr, ${camp?.clicks} clicks, ROI profit ₹${camp?.profit?.toInt()}"
            )
        }
        result
    }

    /**
     * Cancels an active or pending campaign.
     */
    suspend fun cancelCampaign(campaignId: Long, reason: String): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val result = stateManager.cancelCampaign(campaignId, reason)
        logOwnerAction("CAMPAIGN_CANCELLED", "Cancelled campaign $campaignId: $reason")
        result
    }

    /**
     * Generic safe state transition adhering to the transition matrix.
     */
    suspend fun transitionState(
        campaignId: Long,
        targetState: CampaignState,
        reason: String = "",
        optimizationNote: String? = null
    ): Result<AdCampaign> = withContext(Dispatchers.IO) {
        stateManager.transitionState(campaignId, targetState, reason, optimizationNote)
    }

    /**
     * Records real-time ad performance metrics (Impressions, Clicks, Conversions, Spend).
     */
    suspend fun recordCampaignTelemetry(
        campaignId: Long,
        impressionsDelta: Long,
        clicksDelta: Long,
        conversionsDelta: Long,
        spendDelta: Double
    ): Result<AdCampaign> = withContext(Dispatchers.IO) {
        stateManager.recordMetricsUpdate(campaignId, impressionsDelta, clicksDelta, conversionsDelta, spendDelta)
    }

    // ─────────────────────────────────────────────────────────────────────────────
    // PERSISTENT OWNER INTERFACE & GOVERNANCE RULES
    // ─────────────────────────────────────────────────────────────────────────────

    suspend fun getCampaignById(id: Long): AdCampaign? = withContext(Dispatchers.IO) {
        adsDao.getCampaignById(id)
    }

    suspend fun getApprovalById(id: Long): OwnerApprovalRequest? = withContext(Dispatchers.IO) {
        adsDao.getApprovalRequestById(id)
    }

    suspend fun getPricingRulesOnce(): PricingRuleEntity = withContext(Dispatchers.IO) {
        adsDao.getPricingRulesOnce() ?: PricingRuleEntity()
    }

    suspend fun savePricingRules(rules: PricingRuleEntity) = withContext(Dispatchers.IO) {
        adsDao.savePricingRules(rules)
        logOwnerAction(
            "RULES_SAVED",
            "Updated pricing rules: Min ₹${rules.minCampaignPrice.toInt()}, Max Discount ${rules.maxDiscountPercent.toInt()}%, Owner Phone ${rules.ownerPhone}, Mode: ${rules.autonomousMode}"
        )
    }

    suspend fun deleteCampaign(campaign: AdCampaign) = withContext(Dispatchers.IO) {
        adsDao.deleteCampaign(campaign)
        logOwnerAction("CAMPAIGN_DELETED", "Deleted campaign record '${campaign.title}' (ID: ${campaign.id})")
    }

    suspend fun logOwnerAction(actionType: String, description: String): Long = withContext(Dispatchers.IO) {
        adsDao.insertActionLog(
            AiAdActionLog(
                actionType = actionType,
                description = description,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun getOwnerPhone(): String = OwnerContactIntegration.DEFAULT_OWNER_PHONE
}
