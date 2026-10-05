package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.AdCampaign
import com.example.data.model.AdPackage
import com.example.data.model.AdPaymentRecord
import com.example.data.model.AdQuotation
import com.example.data.model.AiAdActionLog
import com.example.data.model.ClientCommunicationMessage
import com.example.data.model.ClientProspect
import com.example.data.model.OwnerApprovalRequest
import com.example.data.model.PricingRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AdsDao {

    // Prospects
    @Query("SELECT * FROM client_prospects ORDER BY createdAt DESC")
    fun getAllProspects(): Flow<List<ClientProspect>>

    @Query("SELECT * FROM client_prospects WHERE id = :id")
    suspend fun getProspectById(id: Long): ClientProspect?

    @Query("SELECT * FROM client_prospects WHERE status = :status ORDER BY createdAt DESC")
    fun getProspectsByStatus(status: String): Flow<List<ClientProspect>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProspect(prospect: ClientProspect): Long

    @Update
    suspend fun updateProspect(prospect: ClientProspect)

    @Delete
    suspend fun deleteProspect(prospect: ClientProspect)

    // Packages
    @Query("SELECT * FROM ad_packages")
    fun getAllPackages(): Flow<List<AdPackage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPackage(pkg: AdPackage): Long

    // Pricing Rules
    @Query("SELECT * FROM pricing_rules WHERE id = 1")
    fun getPricingRules(): Flow<PricingRuleEntity?>

    @Query("SELECT * FROM pricing_rules WHERE id = 1")
    suspend fun getPricingRulesOnce(): PricingRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePricingRules(rule: PricingRuleEntity)

    // Quotations
    @Query("SELECT * FROM ad_quotations ORDER BY createdAt DESC")
    fun getAllQuotations(): Flow<List<AdQuotation>>

    @Query("SELECT * FROM ad_quotations WHERE id = :id")
    suspend fun getQuotationById(id: Long): AdQuotation?

    @Query("SELECT * FROM ad_quotations WHERE prospectId = :prospectId ORDER BY createdAt DESC")
    fun getQuotationsForProspect(prospectId: Long): Flow<List<AdQuotation>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuotation(quotation: AdQuotation): Long

    @Update
    suspend fun updateQuotation(quotation: AdQuotation)

    // Owner Approvals
    @Query("SELECT * FROM owner_approval_requests ORDER BY requestedAt DESC")
    fun getAllApprovalRequests(): Flow<List<OwnerApprovalRequest>>

    @Query("SELECT * FROM owner_approval_requests WHERE status = 'PENDING' ORDER BY requestedAt DESC")
    fun getPendingApprovalRequests(): Flow<List<OwnerApprovalRequest>>

    @Query("SELECT * FROM owner_approval_requests WHERE id = :id")
    suspend fun getApprovalRequestById(id: Long): OwnerApprovalRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApprovalRequest(approval: OwnerApprovalRequest): Long

    @Update
    suspend fun updateApprovalRequest(approval: OwnerApprovalRequest)

    // Campaigns
    @Query("SELECT * FROM ad_campaigns ORDER BY id DESC")
    fun getAllCampaigns(): Flow<List<AdCampaign>>

    @Query("SELECT * FROM ad_campaigns WHERE status = :status ORDER BY id DESC")
    fun getCampaignsByStatus(status: String): Flow<List<AdCampaign>>

    @Query("SELECT * FROM ad_campaigns WHERE id = :id")
    suspend fun getCampaignById(id: Long): AdCampaign?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCampaign(campaign: AdCampaign): Long

    @Update
    suspend fun updateCampaign(campaign: AdCampaign)

    @Delete
    suspend fun deleteCampaign(campaign: AdCampaign)

    // Payments
    @Query("SELECT * FROM ad_payment_records ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<AdPaymentRecord>>

    @Query("SELECT * FROM ad_payment_records WHERE id = :id")
    suspend fun getPaymentById(id: Long): AdPaymentRecord?

    @Query("SELECT * FROM ad_payment_records WHERE campaignId = :campaignId ORDER BY createdAt DESC")
    fun getPaymentsForCampaign(campaignId: Long): Flow<List<AdPaymentRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: AdPaymentRecord): Long

    @Update
    suspend fun updatePayment(payment: AdPaymentRecord)

    // Communications
    @Query("SELECT * FROM client_communications WHERE prospectId = :prospectId ORDER BY timestamp ASC")
    fun getCommunicationsForProspect(prospectId: Long): Flow<List<ClientCommunicationMessage>>

    @Query("SELECT * FROM client_communications ORDER BY timestamp DESC LIMIT 50")
    fun getRecentCommunications(): Flow<List<ClientCommunicationMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCommunication(comm: ClientCommunicationMessage): Long

    // Action Logs
    @Query("SELECT * FROM ai_ad_action_logs ORDER BY timestamp DESC LIMIT 100")
    fun getRecentActionLogs(): Flow<List<AiAdActionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActionLog(log: AiAdActionLog): Long
}
