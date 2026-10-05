package com.example.data.repository

import com.example.data.api.AuraAdsAiService
import com.example.data.local.AdsDao
import com.example.data.manager.CampaignStateManager
import com.example.data.model.AdCampaign
import com.example.data.model.AdPackage
import com.example.data.model.AdPaymentRecord
import com.example.data.model.AdQuotation
import com.example.data.model.AiAdActionLog
import com.example.data.model.CampaignState
import com.example.data.model.ClientCommunicationMessage
import com.example.data.model.ClientProspect
import com.example.data.model.OwnerApprovalRequest
import com.example.data.model.PricingRuleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdsRepository(
    private val adsDao: AdsDao,
    val aiService: AuraAdsAiService = AuraAdsAiService()
) {

    val prospects: Flow<List<ClientProspect>> = adsDao.getAllProspects()
    val packages: Flow<List<AdPackage>> = adsDao.getAllPackages()
    val pricingRules: Flow<PricingRuleEntity?> = adsDao.getPricingRules()
    val quotations: Flow<List<AdQuotation>> = adsDao.getAllQuotations()
    val approvalRequests: Flow<List<OwnerApprovalRequest>> = adsDao.getAllApprovalRequests()
    val pendingApprovals: Flow<List<OwnerApprovalRequest>> = adsDao.getPendingApprovalRequests()
    val campaigns: Flow<List<AdCampaign>> = adsDao.getAllCampaigns()
    val payments: Flow<List<AdPaymentRecord>> = adsDao.getAllPayments()
    val recentCommunications: Flow<List<ClientCommunicationMessage>> = adsDao.getRecentCommunications()
    val actionLogs: Flow<List<AiAdActionLog>> = adsDao.getRecentActionLogs()

    val stateManager: CampaignStateManager = CampaignStateManager(adsDao)

    suspend fun getPricingRulesOnce(): PricingRuleEntity {
        return adsDao.getPricingRulesOnce() ?: PricingRuleEntity()
    }

    suspend fun savePricingRules(rules: PricingRuleEntity) {
        adsDao.savePricingRules(rules)
        logAiAction("RULES_UPDATED", "Owner updated pricing rules (Min: ₹${rules.minCampaignPrice.toInt()}, Max Discount: ${rules.maxDiscountPercent.toInt()}%, Mode: ${rules.autonomousMode})")
    }

    suspend fun addProspect(prospect: ClientProspect): Long {
        val id = adsDao.insertProspect(prospect)
        logAiAction("PROSPECT_IDENTIFIED", "Identified potential client: ${prospect.businessName} (${prospect.category}) in ${prospect.cityLocation}")
        return id
    }

    suspend fun updateProspectStatus(id: Long, status: String) {
        val existing = adsDao.getProspectById(id)
        if (existing != null) {
            adsDao.updateProspect(existing.copy(status = status))
            logAiAction("PROSPECT_STATUS_UPDATED", "${existing.businessName} status transitioned to $status")
        }
    }

    suspend fun logAiAction(actionType: String, description: String): Long {
        return adsDao.insertActionLog(
            AiAdActionLog(
                actionType = actionType,
                description = description,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    suspend fun calculateAndGenerateQuotation(
        prospect: ClientProspect,
        adSpend: Double,
        durationDays: Int,
        requiresCreative: Boolean,
        customDiscountPercent: Double
    ): AdQuotation {
        val rules = getPricingRulesOnce()
        val timestamp = System.currentTimeMillis()
        val count = quotations.firstOrNull()?.size ?: 0
        val quoteIdStr = "UMR-ADS-2026-${String.format(Locale.US, "%04d", count + 1)}"

        // Cost Calculation Formula:
        // Total Client Price = Ad Spend + Service Fee + Creative Fee + Platform/Processing Costs + Optional Services
        val serviceFee = adSpend * (rules.serviceFeePercent / 100.0)
        val creativeFee = if (requiresCreative) rules.creativeProductionFee else 0.0
        val platformFee = adSpend * (rules.managementFeePercent / 100.0)
        val gross = adSpend + serviceFee + creativeFee + platformFee

        val maxAllowedDiscount = rules.maxDiscountPercent
        val actualDiscountPercent = customDiscountPercent.coerceIn(0.0, maxAllowedDiscount)
        val discountAmount = gross * (actualDiscountPercent / 100.0)

        // Hard minimum constraint enforced
        val discountedSubtotal = (gross - discountAmount).coerceAtLeast(rules.minCampaignPrice)
        val taxAmount = discountedSubtotal * 0.18 // 18% GST
        val finalAmount = discountedSubtotal + taxAmount
        val estimatedProfit = (serviceFee + creativeFee + platformFee - discountAmount).coerceAtLeast(0.0)

        val servicesList = """["Targeted ${prospect.preferredPlatforms} Media Buy","Audience Calibration: ${prospect.targetAudience}","Creative Production: ${if (requiresCreative) "Custom Video/Banner" else "Client Provided Assets"}","Live A/B Campaign Optimization","Bi-weekly Performance Analytics Dashboard"]"""

        val quotation = AdQuotation(
            quotationCode = quoteIdStr,
            prospectId = prospect.id,
            clientName = prospect.contactPerson,
            businessName = prospect.businessName,
            campaignDescription = "Targeted ${prospect.adObjective} campaign for ${prospect.businessName} across ${prospect.preferredPlatforms}",
            includedServicesJson = servicesList,
            durationDays = durationDays,
            adSpend = adSpend,
            serviceFee = serviceFee,
            creativeFee = creativeFee,
            platformFee = platformFee,
            discountAmount = discountAmount,
            taxAmount = taxAmount,
            finalAmount = finalAmount,
            estimatedProfit = estimatedProfit,
            isApprovedByOwner = false,
            status = "PENDING_APPROVAL",
            createdAt = timestamp
        )

        val id = adsDao.insertQuotation(quotation)
        logAiAction("QUOTATION_PREPARED", "Generated quotation $quoteIdStr for ${prospect.businessName} totaling ₹${finalAmount.toInt()}")

        // Create owner approval request directly
        val approval = OwnerApprovalRequest(
            quotationId = id,
            ownerPhone = rules.ownerPhone,
            clientName = prospect.contactPerson,
            businessName = prospect.businessName,
            campaignObjective = prospect.adObjective,
            platforms = prospect.preferredPlatforms,
            durationDays = durationDays,
            targetAudience = prospect.targetAudience,
            adSpend = adSpend,
            serviceFee = serviceFee,
            creativeFee = creativeFee,
            discount = discountAmount,
            finalPrice = finalAmount,
            estimatedProfit = estimatedProfit,
            clientRequirements = "Goal: ${prospect.adObjective}, Loc: ${prospect.cityLocation}, Duration: ${durationDays}d",
            startDate = timestamp,
            endDate = timestamp + (durationDays.toLong() * 86400000L),
            status = "PENDING",
            requestedAt = timestamp
        )
        adsDao.insertApprovalRequest(approval)
        logAiAction("APPROVAL_REQUEST_SENT", "Dispatched campaign approval proposal to owner contact ${rules.ownerPhone}")

        return quotation.copy(id = id)
    }

    suspend fun approveOwnerQuotation(approvalId: Long, quotationId: Long) {
        val approval = adsDao.getApprovalRequestById(approvalId)
        val quote = adsDao.getQuotationById(quotationId)

        if (approval != null && quote != null) {
            adsDao.updateApprovalRequest(approval.copy(status = "APPROVED"))
            adsDao.updateQuotation(quote.copy(isApprovedByOwner = true, status = "APPROVED"))

            // Create campaign workspace in DRAFT / PAYMENT_PENDING state
            val campaign = AdCampaign(
                title = "${quote.businessName} - ${approval.campaignObjective}",
                prospectId = quote.prospectId,
                quotationId = quote.id,
                businessName = quote.businessName,
                status = "PAYMENT_PENDING",
                platform = approval.platforms,
                budget = quote.adSpend,
                spent = 0.0,
                revenue = quote.finalAmount,
                profit = quote.estimatedProfit,
                startDate = approval.startDate,
                endDate = approval.endDate,
                impressions = 0,
                clicks = 0,
                conversions = 0,
                ctr = 0f,
                cpc = 0f,
                apiIntegrationStatus = "MANUAL_SETUP_REQUIRED"
            )
            val campaignId = adsDao.insertCampaign(campaign)

            // Create pending payment record
            adsDao.insertPayment(
                AdPaymentRecord(
                    campaignId = campaignId,
                    quotationCode = quote.quotationCode,
                    clientName = quote.clientName,
                    amount = quote.finalAmount,
                    paymentMethod = "Verified UPI / Bank Transfer",
                    status = "PAYMENT_PENDING",
                    createdAt = System.currentTimeMillis()
                )
            )

            logAiAction("OWNER_APPROVED", "Owner ${approval.ownerPhone} approved quotation ${quote.quotationCode}. Campaign workspace initialized.")
        }
    }

    suspend fun rejectOwnerQuotation(approvalId: Long, quotationId: Long, reason: String) {
        val approval = adsDao.getApprovalRequestById(approvalId)
        val quote = adsDao.getQuotationById(quotationId)

        if (approval != null && quote != null) {
            adsDao.updateApprovalRequest(approval.copy(status = "REJECTED", ownerNotes = reason))
            adsDao.updateQuotation(quote.copy(status = "REJECTED"))
            logAiAction("OWNER_REJECTED", "Owner rejected quotation ${quote.quotationCode}. Reason: $reason")
        }
    }

    suspend fun verifyPayment(payment: AdPaymentRecord, transactionRef: String) {
        val updatedPayment = payment.copy(
            status = "VERIFIED",
            transactionReference = transactionRef,
            verifiedAt = System.currentTimeMillis()
        )
        adsDao.updatePayment(updatedPayment)

        val campaign = adsDao.getCampaignById(payment.campaignId)
        if (campaign != null) {
            adsDao.updateCampaign(
                campaign.copy(
                    status = "READY",
                    optimizationRecommendation = "Payment verified. Ready for launch and audience targeting calibration."
                )
            )
        }

        logAiAction("PAYMENT_VERIFIED", "Payment of ₹${payment.amount.toInt()} for ${payment.quotationCode} verified (Ref: $transactionRef). Campaign marked READY.")
    }

    suspend fun launchCampaign(campaignId: Long): Result<AdCampaign> {
        return stateManager.launchCampaign(campaignId)
    }

    suspend fun pauseCampaign(campaignId: Long, reason: String = "Manual pause by operator"): Result<AdCampaign> {
        return stateManager.pauseCampaign(campaignId, reason)
    }

    suspend fun resumeCampaign(campaignId: Long): Result<AdCampaign> {
        return stateManager.resumeCampaign(campaignId)
    }

    suspend fun completeCampaign(campaignId: Long): Result<AdCampaign> {
        return stateManager.completeCampaign(campaignId)
    }

    suspend fun cancelCampaign(campaignId: Long, reason: String): Result<AdCampaign> {
        return stateManager.cancelCampaign(campaignId, reason)
    }

    suspend fun recordCampaignMetrics(
        campaignId: Long,
        impressionsDelta: Long,
        clicksDelta: Long,
        conversionsDelta: Long,
        spendDelta: Double
    ): Result<AdCampaign> {
        return stateManager.recordMetricsUpdate(campaignId, impressionsDelta, clicksDelta, conversionsDelta, spendDelta)
    }

    suspend fun updateCampaignStatus(campaignId: Long, status: String) {
        val campaign = adsDao.getCampaignById(campaignId)
        if (campaign != null) {
            adsDao.updateCampaign(campaign.copy(status = status))
            logAiAction("CAMPAIGN_STATUS_CHANGE", "${campaign.title} status changed to $status")
        }
    }

    suspend fun sendCommunication(prospectId: Long, sender: String, channel: String, message: String) {
        adsDao.insertCommunication(
            ClientCommunicationMessage(
                prospectId = prospectId,
                sender = sender,
                channel = channel,
                message = message,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    fun getCommunicationsForProspect(prospectId: Long): Flow<List<ClientCommunicationMessage>> {
        return adsDao.getCommunicationsForProspect(prospectId)
    }
}
