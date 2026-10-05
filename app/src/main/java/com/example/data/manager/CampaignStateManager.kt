package com.example.data.manager

import com.example.data.local.AdsDao
import com.example.data.model.AdCampaign
import com.example.data.model.AiAdActionLog
import com.example.data.model.CampaignState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CampaignStateManager(
    private val adsDao: AdsDao
) {

    /**
     * Determines whether a transition from [currentState] to [targetState] is legally permissible
     * within the AURA ADS governance framework.
     */
    fun canTransition(currentState: String, targetState: CampaignState): Boolean {
        return when (currentState) {
            CampaignState.DRAFT.name -> {
                targetState in listOf(CampaignState.APPROVAL_REQUIRED, CampaignState.CANCELLED)
            }
            CampaignState.APPROVAL_REQUIRED.name -> {
                targetState in listOf(CampaignState.APPROVED, CampaignState.DRAFT, CampaignState.CANCELLED)
            }
            CampaignState.APPROVED.name -> {
                targetState in listOf(CampaignState.PAYMENT_PENDING, CampaignState.READY, CampaignState.RUNNING, CampaignState.CANCELLED)
            }
            CampaignState.PAYMENT_PENDING.name -> {
                targetState in listOf(CampaignState.READY, CampaignState.CANCELLED)
            }
            CampaignState.READY.name -> {
                targetState in listOf(CampaignState.RUNNING, CampaignState.PAUSED, CampaignState.CANCELLED)
            }
            CampaignState.RUNNING.name -> {
                targetState in listOf(CampaignState.PAUSED, CampaignState.COMPLETED, CampaignState.CANCELLED)
            }
            CampaignState.PAUSED.name -> {
                targetState in listOf(CampaignState.RUNNING, CampaignState.CANCELLED)
            }
            CampaignState.COMPLETED.name -> false // Terminal
            CampaignState.CANCELLED.name -> false // Terminal
            else -> false
        }
    }

    suspend fun transitionState(
        campaignId: Long,
        targetState: CampaignState,
        reason: String = "",
        optimizationNote: String? = null
    ): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val campaign = adsDao.getCampaignById(campaignId)
            ?: return@withContext Result.failure(IllegalArgumentException("Campaign $campaignId not found"))

        if (!canTransition(campaign.status, targetState)) {
            val errorMsg = "Illegal campaign transition from ${campaign.status} to ${targetState.name}"
            logAction("TRANSITION_REJECTED", "Failed state transition on '${campaign.title}': $errorMsg")
            return@withContext Result.failure(IllegalStateException(errorMsg))
        }

        val updated = campaign.copy(
            status = targetState.name,
            optimizationRecommendation = optimizationNote ?: campaign.optimizationRecommendation
        )
        adsDao.updateCampaign(updated)

        logAction(
            "CAMPAIGN_STATE_${targetState.name}",
            "Transitioned '${campaign.title}' from ${campaign.status} to ${targetState.name}${if (reason.isNotBlank()) " (Reason: $reason)" else ""}"
        )

        Result.success(updated)
    }

    suspend fun launchCampaign(campaignId: Long): Result<AdCampaign> {
        return transitionState(
            campaignId = campaignId,
            targetState = CampaignState.RUNNING,
            reason = "Owner authorized campaign launch",
            optimizationNote = "Campaign actively running. Media bids and audience targeting delivering live impressions."
        )
    }

    suspend fun pauseCampaign(campaignId: Long, reason: String): Result<AdCampaign> {
        return transitionState(
            campaignId = campaignId,
            targetState = CampaignState.PAUSED,
            reason = reason,
            optimizationNote = "Campaign paused: $reason. Ad spend frozen."
        )
    }

    suspend fun resumeCampaign(campaignId: Long): Result<AdCampaign> {
        return transitionState(
            campaignId = campaignId,
            targetState = CampaignState.RUNNING,
            reason = "Campaign resumed by operator",
            optimizationNote = "Campaign resumed. Live performance optimization active."
        )
    }

    suspend fun completeCampaign(campaignId: Long): Result<AdCampaign> {
        return transitionState(
            campaignId = campaignId,
            targetState = CampaignState.COMPLETED,
            reason = "Target schedule and metrics achieved",
            optimizationNote = "Campaign completed successfully. Final ROI performance audit generated."
        )
    }

    suspend fun cancelCampaign(campaignId: Long, reason: String): Result<AdCampaign> {
        return transitionState(
            campaignId = campaignId,
            targetState = CampaignState.CANCELLED,
            reason = reason,
            optimizationNote = "Campaign cancelled: $reason. Remaining unspent ad spend credited."
        )
    }

    suspend fun recordMetricsUpdate(
        campaignId: Long,
        impressionsDelta: Long,
        clicksDelta: Long,
        conversionsDelta: Long,
        spendDelta: Double
    ): Result<AdCampaign> = withContext(Dispatchers.IO) {
        val campaign = adsDao.getCampaignById(campaignId)
            ?: return@withContext Result.failure(IllegalArgumentException("Campaign $campaignId not found"))

        val newImpressions = campaign.impressions + impressionsDelta
        val newClicks = campaign.clicks + clicksDelta
        val newConversions = campaign.conversions + conversionsDelta
        val newSpent = (campaign.spent + spendDelta).coerceAtMost(campaign.budget)

        val ctr = if (newImpressions > 0) (newClicks.toFloat() / newImpressions) * 100f else 0f
        val cpc = if (newClicks > 0) (newSpent / newClicks).toFloat() else 0f

        val updated = campaign.copy(
            impressions = newImpressions,
            clicks = newClicks,
            conversions = newConversions,
            spent = newSpent,
            ctr = ctr,
            cpc = cpc
        )
        adsDao.updateCampaign(updated)

        logAction(
            "METRICS_SYNC",
            "Updated '${campaign.title}' telemetry: +${impressionsDelta} impr, +${clicksDelta} clicks, spent: ₹${newSpent.toInt()}/₹${campaign.budget.toInt()}"
        )

        Result.success(updated)
    }

    private suspend fun logAction(actionType: String, description: String) {
        adsDao.insertActionLog(
            AiAdActionLog(
                actionType = actionType,
                description = description,
                timestamp = System.currentTimeMillis()
            )
        )
    }
}
