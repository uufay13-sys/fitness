package com.example.ui.screens

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.manager.OwnerContactIntegration
import com.example.data.model.AdCampaign
import com.example.data.model.AdPackage
import com.example.data.model.AdPaymentRecord
import com.example.data.model.AdQuotation
import com.example.data.model.AuraAdsStatus
import com.example.data.model.AutonomousMode
import com.example.data.model.CampaignState
import com.example.data.model.ClientProspect
import com.example.data.model.OwnerApprovalRequest
import com.example.data.model.PricingRuleEntity
import com.example.ui.AuraViewModel
import com.example.ui.components.AuraAvatar
import com.example.ui.components.AuraVoiceHelper
import com.example.ui.theme.AuraCyan
import com.example.ui.theme.AuraLime
import com.example.ui.theme.AuraViolet
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun AuraAdsScreen(viewModel: AuraViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val auraAdsStatus by viewModel.auraAdsStatus.collectAsState()
    val prospects by viewModel.clientProspects.collectAsState()
    val packages by viewModel.adPackages.collectAsState()
    val pricingRules by viewModel.pricingRules.collectAsState()
    val quotations by viewModel.adQuotations.collectAsState()
    val approvals by viewModel.approvalRequests.collectAsState()
    val pendingApprovals by viewModel.pendingApprovals.collectAsState()
    val campaigns by viewModel.adCampaigns.collectAsState()
    val payments by viewModel.adPayments.collectAsState()
    val actionLogs by viewModel.adActionLogs.collectAsState()
    val communications by viewModel.recentCommunications.collectAsState()

    val currentRules = pricingRules ?: PricingRuleEntity()

    // Modals state
    var showAddProspectDialog by remember { mutableStateOf(false) }
    var showProposalDialog by remember { mutableStateOf<Pair<ClientProspect, String>?>(null) }
    var showQuoteCalculatorForProspect by remember { mutableStateOf<ClientProspect?>(null) }
    var showNegotiationDialog by remember { mutableStateOf<ClientProspect?>(null) }
    var showPaymentVerifyDialog by remember { mutableStateOf<AdPaymentRecord?>(null) }
    var showPricingSettingsDialog by remember { mutableStateOf(false) }

    // Voice helper
    val voiceHelper = remember {
        AuraVoiceHelper(
            context = context,
            onSpeechResult = { cmd ->
                viewModel.executeAdsCommand(cmd) { }
            },
            onListeningStateChanged = { isListening ->
                if (isListening) viewModel.setAuraAdsStatus(AuraAdsStatus.DISCUSSING)
                else viewModel.setAuraAdsStatus(AuraAdsStatus.READY)
            }
        )
    }

    val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) voiceHelper.startListening()
    }

    val tabs = listOf(
        "Control Center",
        "Clients (${prospects.size})",
        "Packages & Pricing",
        "Approvals (${pendingApprovals.size})",
        "Campaigns (${campaigns.size})",
        "Activity Log"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Executive Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AURA ADS",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp
                                ),
                                color = AuraCyan
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = AuraLime.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "AI BUSINESS AGENT",
                                    color = AuraLime,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = "Advertising Sales & Client Acquisition Intelligence",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showPricingSettingsDialog = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Rules & Settings", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Real-time Agent Status & Mode Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Agent Status Badge
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = when (auraAdsStatus) {
                            AuraAdsStatus.RUNNING, AuraAdsStatus.CAMPAIGN_READY -> AuraLime.copy(alpha = 0.2f)
                            AuraAdsStatus.NEGOTIATING, AuraAdsStatus.WAITING_FOR_OWNER_APPROVAL -> Color(0xFFFF9900).copy(alpha = 0.2f)
                            AuraAdsStatus.SEARCHING, AuraAdsStatus.ANALYZING, AuraAdsStatus.PREPARING_QUOTE -> AuraCyan.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surfaceVariant
                        },
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            when (auraAdsStatus) {
                                AuraAdsStatus.RUNNING, AuraAdsStatus.CAMPAIGN_READY -> AuraLime
                                AuraAdsStatus.NEGOTIATING, AuraAdsStatus.WAITING_FOR_OWNER_APPROVAL -> Color(0xFFFF9900)
                                AuraAdsStatus.SEARCHING, AuraAdsStatus.ANALYZING -> AuraCyan
                                else -> MaterialTheme.colorScheme.outline
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (auraAdsStatus) {
                                            AuraAdsStatus.RUNNING, AuraAdsStatus.CAMPAIGN_READY -> AuraLime
                                            AuraAdsStatus.NEGOTIATING, AuraAdsStatus.WAITING_FOR_OWNER_APPROVAL -> Color(0xFFFF9900)
                                            else -> AuraCyan
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AURA: ${auraAdsStatus.label}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }

                    // Mode Tag
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Text(
                            text = "MODE: ${currentRules.autonomousMode.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // Scrollable Tabs
        ScrollableTabRow(
            selectedTabIndex = selectedTab,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = AuraCyan,
            edgePadding = 16.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = AuraCyan
                )
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = {
                        Text(
                            text = title,
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                )
            }
        }

        // Tab Content
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> AdsControlCenterTab(
                    viewModel = viewModel,
                    status = auraAdsStatus,
                    rules = currentRules,
                    prospects = prospects,
                    campaigns = campaigns,
                    pendingApprovals = pendingApprovals,
                    onVoiceCommand = { micLauncher.launch(Manifest.permission.RECORD_AUDIO) },
                    onNavigateToTab = { selectedTab = it },
                    onOpenAddProspect = { showAddProspectDialog = true }
                )
                1 -> ClientsProspectsTab(
                    prospects = prospects,
                    onAddProspect = { showAddProspectDialog = true },
                    onApproachClient = { prospect ->
                        viewModel.generateClientProposal(prospect) { proposal ->
                            showProposalDialog = Pair(prospect, proposal)
                        }
                    },
                    onPrepareQuote = { prospect ->
                        showQuoteCalculatorForProspect = prospect
                    },
                    onNegotiate = { prospect ->
                        showNegotiationDialog = prospect
                    },
                    onUpdateStatus = { id, status ->
                        viewModel.updateProspectStatus(id, status)
                    }
                )
                2 -> PackagesAndCostTab(
                    packages = packages,
                    rules = currentRules,
                    onCalculateCustom = { prospect ->
                        showQuoteCalculatorForProspect = prospect
                    },
                    onOpenSettings = { showPricingSettingsDialog = true }
                )
                3 -> ApprovalsAndQuotationsTab(
                    approvals = approvals,
                    quotations = quotations,
                    onApprove = { app ->
                        viewModel.approveQuotationByOwner(app.id, app.quotationId)
                    },
                    onReject = { app, reason ->
                        viewModel.rejectQuotationByOwner(app.id, app.quotationId, reason)
                    }
                )
                4 -> CampaignsAndFinanceTab(
                    campaigns = campaigns,
                    payments = payments,
                    onVerifyPayment = { payment ->
                        showPaymentVerifyDialog = payment
                    },
                    onLaunchCampaign = { camp -> viewModel.launchCampaign(camp.id) },
                    onPauseCampaign = { camp -> viewModel.pauseCampaign(camp.id) },
                    onResumeCampaign = { camp -> viewModel.resumeCampaign(camp.id) },
                    onCompleteCampaign = { camp -> viewModel.completeCampaign(camp.id) },
                    onCancelCampaign = { camp -> viewModel.cancelCampaign(camp.id, "Owner cancellation") },
                    onSimulateMetrics = { camp -> viewModel.simulateCampaignMetrics(camp.id) }
                )
                5 -> ActivityLogTab(logs = actionLogs)
            }
        }
    }

    // Modal: 10-Point Client Requirement & Prospect Entry Dialog
    if (showAddProspectDialog) {
        AddClientRequirementDialog(
            onDismiss = { showAddProspectDialog = false },
            onSubmit = { bName, cat, person, phone, email, city, audience, obj, plats, budget, dur, creative ->
                viewModel.addClientProspect(bName, cat, person, phone, email, city, audience, obj, plats, budget, dur, creative)
                showAddProspectDialog = false
            }
        )
    }

    // Modal: Generated AI Proposal
    showProposalDialog?.let { (prospect, proposalText) ->
        AlertDialog(
            onDismissRequest = { showProposalDialog = null },
            title = {
                Text(
                    text = "Personalized Proposal for ${prospect.businessName}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            },
            text = {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    Text(
                        text = "Prepared by AURA ADS AGENT (AI Assistant)",
                        style = MaterialTheme.typography.labelSmall,
                        color = AuraCyan,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Text(
                            text = proposalText,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.sendClientMessage(prospect.id, proposalText, "IN_APP")
                        viewModel.updateProspectStatus(prospect.id, "CONTACTED")
                        showProposalDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
                ) {
                    Text("SEND TO CLIENT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showProposalDialog = null }) {
                    Text("CLOSE")
                }
            }
        )
    }

    // Modal: Cost Calculator & Quotation Creator for a Prospect
    showQuoteCalculatorForProspect?.let { prospect ->
        QuotationCalculatorModal(
            prospect = prospect,
            rules = currentRules,
            onDismiss = { showQuoteCalculatorForProspect = null },
            onGenerateQuote = { spend, days, creative, discount ->
                viewModel.calculateAndCreateQuotation(prospect, spend, days, creative, discount)
                showQuoteCalculatorForProspect = null
                selectedTab = 3 // Jump to approvals tab
            }
        )
    }

    // Modal: AI Negotiation Dialog
    showNegotiationDialog?.let { prospect ->
        NegotiationSimulationModal(
            prospect = prospect,
            rules = currentRules,
            onDismiss = { showNegotiationDialog = null },
            onRequestApproval = { offerAmount ->
                viewModel.calculateAndCreateQuotation(
                    prospect = prospect,
                    adSpend = offerAmount * 0.7,
                    durationDays = prospect.expectedDurationDays,
                    requiresCreative = false,
                    discountPercent = 10.0
                )
                showNegotiationDialog = null
                selectedTab = 3
            }
        )
    }

    // Modal: Payment Verification
    showPaymentVerifyDialog?.let { payment ->
        var refText by remember { mutableStateOf("UPI-TXN-" + System.currentTimeMillis().toString().takeLast(6)) }
        AlertDialog(
            onDismissRequest = { showPaymentVerifyDialog = null },
            title = { Text("Verify Client Payment", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Client: ${payment.clientName}")
                    Text("Quotation Ref: ${payment.quotationCode}")
                    Text("Amount Due: ₹${payment.amount.toInt()}", fontWeight = FontWeight.Bold, color = AuraLime)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = refText,
                        onValueChange = { refText = it },
                        label = { Text("Transaction Reference ID / UTR") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.verifyPayment(payment, refText)
                        showPaymentVerifyDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraLime, contentColor = Color.Black)
                ) {
                    Text("VERIFY & ADVANCE TO READY", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaymentVerifyDialog = null }) { Text("CANCEL") }
            }
        )
    }

    // Modal: Pricing Rules & Autonomous Mode
    if (showPricingSettingsDialog) {
        PricingSettingsModal(
            currentRules = currentRules,
            onDismiss = { showPricingSettingsDialog = false },
            onSave = { updated ->
                viewModel.savePricingRules(updated)
                showPricingSettingsDialog = false
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 0: CONTROL CENTER
// -------------------------------------------------------------
@Composable
fun AdsControlCenterTab(
    viewModel: AuraViewModel,
    status: AuraAdsStatus,
    rules: PricingRuleEntity,
    prospects: List<ClientProspect>,
    campaigns: List<AdCampaign>,
    pendingApprovals: List<OwnerApprovalRequest>,
    onVoiceCommand: () -> Unit,
    onNavigateToTab: (Int) -> Unit,
    onOpenAddProspect: () -> Unit
) {
    var commandInput by remember { mutableStateOf("") }
    var lastAiReply by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val quickCommands = listOf(
        "Find new advertising clients.",
        "Show interested clients.",
        "Calculate lowest acceptable price.",
        "How much profit did we make?",
        "Show running campaigns."
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Agent Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Brush.horizontalGradient(listOf(AuraCyan.copy(alpha = 0.6f), AuraLime.copy(alpha = 0.6f)))),
                modifier = Modifier.fillMaxWidth().testTag("ads_agent_hero_card")
            ) {
                Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    AuraAvatar(
                        status = com.example.data.api.AuraAiStatus.READY,
                        size = 76.dp,
                        showLabel = false
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "AURA ADS AGENT",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "\"I discover prospective clients, formulate compliant quotations, negotiate within configured bounds, and request owner authorization.\"",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Voice / Command Input Bar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = commandInput,
                            onValueChange = { commandInput = it },
                            placeholder = { Text("Ask AURA or give command...") },
                            singleLine = true,
                            modifier = Modifier.weight(1f).testTag("ads_command_input")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onVoiceCommand,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AuraCyan.copy(alpha = 0.2f))
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice", tint = AuraCyan)
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(
                            onClick = {
                                if (commandInput.isNotBlank()) {
                                    viewModel.executeAdsCommand(commandInput) { reply ->
                                        lastAiReply = reply
                                        commandInput = ""
                                    }
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AuraLime)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.Black)
                        }
                    }

                    // Suggested Command Chips
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickCommands.forEach { cmd ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier.clickable {
                                    viewModel.executeAdsCommand(cmd) { reply ->
                                        lastAiReply = reply
                                    }
                                }
                            ) {
                                Text(
                                    text = cmd,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AuraCyan,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Live AI Response Bubble (if available)
        lastAiReply?.let { reply ->
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AuraCyan),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("AURA RESPONSE", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AuraCyan)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(reply, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }

        // Pending Owner Approvals Banner (Section 9)
        if (pendingApprovals.isNotEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFF9900).copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9900)),
                    modifier = Modifier.fillMaxWidth().clickable { onNavigateToTab(3) }
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFF9900), modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${pendingApprovals.size} Campaign Proposal(s) Pending Owner Approval",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Dispatched to owner contact ${rules.ownerPhone}. Tap to review line items.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Button(
                            onClick = { onNavigateToTab(3) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9900), contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("REVIEW", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Quick Stats Metrics
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                AdsMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Prospects",
                    value = "${prospects.size}",
                    sub = "${prospects.count { it.status == "INTERESTED" }} interested",
                    icon = Icons.Default.Storefront,
                    color = AuraCyan
                )
                AdsMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Campaigns",
                    value = "${campaigns.size}",
                    sub = "${campaigns.count { it.status == "RUNNING" }} running",
                    icon = Icons.Default.Campaign,
                    color = AuraLime
                )
                AdsMetricCard(
                    modifier = Modifier.weight(1f),
                    title = "Net Profit",
                    value = "₹${campaigns.sumOf { it.profit }.toInt()}",
                    sub = "verified",
                    icon = Icons.Default.Paid,
                    color = AuraViolet
                )
            }
        }

        // Quick Discovery Actions
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("CLIENT DISCOVERY & PIPELINE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "Add prospective local businesses or generate AI personalized outreach proposals.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onOpenAddProspect,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("ADD PROSPECT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = { onNavigateToTab(1) },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("VIEW PIPELINE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Business Owner Contact & Governance Card (Section 2 & 9)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth().testTag("owner_governance_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Security, contentDescription = null, tint = AuraLime, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "BUSINESS OWNER GOVERNANCE",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (rules.isOwnerPhonePublic) Color(0xFFFF9900).copy(alpha = 0.15f) else AuraLime.copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = if (rules.isOwnerPhonePublic) "PUBLIC NUMBER" else "PRIVATE & PROTECTED",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (rules.isOwnerPhonePublic) Color(0xFFFF9900) else AuraLime,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = "Configured Owner Contact: ${OwnerContactIntegration.getDisplayPhone(rules.ownerPhone, rules.isOwnerPhonePublic)}",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (!rules.isOwnerPhonePublic)
                            "Owner contact is strictly protected and hidden from client-facing proposals."
                        else
                            "Owner contact number is visible on public proposals.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { OwnerContactIntegration.callOwner(context, rules.ownerPhone) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("CALL OWNER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = {
                                val dummyReq = OwnerApprovalRequest(
                                    quotationId = 0L,
                                    ownerPhone = rules.ownerPhone,
                                    clientName = "Governance Ping",
                                    businessName = "UMR AURA",
                                    campaignObjective = "Status Check",
                                    platforms = "Omni-Channel",
                                    durationDays = 7,
                                    targetAudience = "Owner",
                                    adSpend = 0.0,
                                    serviceFee = 0.0,
                                    creativeFee = 0.0,
                                    discount = 0.0,
                                    finalPrice = 0.0,
                                    estimatedProfit = 0.0,
                                    clientRequirements = "Direct test dispatch",
                                    startDate = System.currentTimeMillis(),
                                    endDate = System.currentTimeMillis() + 86400000L
                                )
                                OwnerContactIntegration.dispatchSmsApproval(context, dummyReq, rules.ownerPhone)
                            },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("SMS OWNER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 1: CLIENTS & PROSPECTS
// -------------------------------------------------------------
@Composable
fun ClientsProspectsTab(
    prospects: List<ClientProspect>,
    onAddProspect: () -> Unit,
    onApproachClient: (ClientProspect) -> Unit,
    onPrepareQuote: (ClientProspect) -> Unit,
    onNegotiate: (ClientProspect) -> Unit,
    onUpdateStatus: (Long, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PROSPECTIVE CLIENTS", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("${prospects.size} identified businesses", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(
                    onClick = onAddProspect,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NEW CLIENT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                }
            }
        }

        if (prospects.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No prospective clients added yet.", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Tap '+ NEW CLIENT' to analyze requirements and generate tailored proposals.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(prospects) { prospect ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth().testTag("prospect_card_${prospect.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prospect.businessName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("${prospect.category} • ${prospect.cityLocation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = when (prospect.status) {
                                    "CONVERTED" -> AuraLime.copy(alpha = 0.2f)
                                    "INTERESTED", "NEGOTIATING" -> AuraCyan.copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = prospect.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (prospect.status) {
                                        "CONVERTED" -> AuraLime
                                        "INTERESTED", "NEGOTIATING" -> AuraCyan
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Contact: ${prospect.contactPerson} (${prospect.contactPhone})", style = MaterialTheme.typography.bodySmall)
                        Text("Target: ${prospect.targetAudience}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Objective: ${prospect.adObjective} • Budget: ₹${prospect.approxBudget.toInt()} (${prospect.expectedDurationDays}d)", style = MaterialTheme.typography.bodySmall, color = AuraCyan)

                        Spacer(modifier = Modifier.height(12.dp))

                        // Action Buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onApproachClient(prospect) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("APPROACH", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { onPrepareQuote(prospect) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("QUOTE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = { onNegotiate(prospect) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("NEGOTIATE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: PACKAGES & COST CALCULATOR
// -------------------------------------------------------------
@Composable
fun PackagesAndCostTab(
    packages: List<AdPackage>,
    rules: PricingRuleEntity,
    onCalculateCustom: (ClientProspect) -> Unit,
    onOpenSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ADVERTISING PACKAGES", fontWeight = FontWeight.Black, fontSize = 16.sp)
                    Text("Verified pricing formulas & deliverables", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                OutlinedButton(onClick = onOpenSettings, shape = RoundedCornerShape(8.dp)) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("RULES", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(packages) { pkg ->
            val total = pkg.baseAdSpend + pkg.serviceFee + pkg.creativeFee + pkg.platformCost
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (pkg.name == "STANDARD") AuraCyan else MaterialTheme.colorScheme.outline
                ),
                modifier = Modifier.fillMaxWidth().testTag("package_card_${pkg.name}")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(pkg.name, fontWeight = FontWeight.Black, fontSize = 16.sp, color = AuraCyan)
                            Text("${pkg.durationDays} Days Duration", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("₹${total.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AuraLime)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(pkg.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    Spacer(modifier = Modifier.height(10.dp))
                    // Price Breakdown
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Media Ad Spend", style = MaterialTheme.typography.labelSmall)
                                Text("₹${pkg.baseAdSpend.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("AURA Service Fee", style = MaterialTheme.typography.labelSmall)
                                Text("₹${pkg.serviceFee.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Creative Production", style = MaterialTheme.typography.labelSmall)
                                Text("₹${pkg.creativeFee.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    // Feature List
                    val features = try {
                        val arr = JSONArray(pkg.featuresJson)
                        (0 until arr.length()).map { arr.getString(it) }
                    } catch (e: Exception) {
                        emptyList()
                    }
                    features.forEach { feat ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = AuraLime, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(feat, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Dynamic Pricing Formula Explainer (Section 7)
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("VERIFIED PRICING FORMULA", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AuraCyan)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Total Client Price = Ad Spend + Service Fee (${rules.serviceFeePercent.toInt()}%) + Creative Fee (₹${rules.creativeProductionFee.toInt()}) + Platform/Processing (${rules.managementFeePercent.toInt()}%) + GST (18%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        "• Hard minimum floor: ₹${rules.minCampaignPrice.toInt()}\n• Maximum discount limit: ${rules.maxDiscountPercent.toInt()}%\n• Target minimum profit margin: ${rules.minProfitMarginPercent.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: APPROVALS & QUOTATIONS
// -------------------------------------------------------------
@Composable
fun ApprovalsAndQuotationsTab(
    approvals: List<OwnerApprovalRequest>,
    quotations: List<AdQuotation>,
    onApprove: (OwnerApprovalRequest) -> Unit,
    onReject: (OwnerApprovalRequest, String) -> Unit
) {
    val context = LocalContext.current
    val pending = approvals.filter { it.status == "PENDING" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Pending Owner Approvals Header
        item {
            Column {
                Text("OWNER APPROVAL QUEUE", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Commercial approval required before agreements become binding", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (pending.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = AuraLime, modifier = Modifier.size(32.dp))
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("No pending owner approval requests.", fontWeight = FontWeight.Bold)
                        Text("New quotations prepared by AURA ADS AGENT will appear here.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(pending) { app ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9900)),
                    modifier = Modifier.fillMaxWidth().testTag("approval_card_${app.id}")
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column {
                                Text(app.businessName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Text("Client: ${app.clientName} • Goal: ${app.campaignObjective}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(0xFFFF9900).copy(alpha = 0.2f)
                            ) {
                                Text("APPROVAL REQUIRED", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFF9900), modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Platform & Duration", style = MaterialTheme.typography.bodySmall)
                                    Text("${app.platforms} (${app.durationDays} days)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Media Ad Spend", style = MaterialTheme.typography.bodySmall)
                                    Text("₹${app.adSpend.toInt()}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("AURA Service + Creative", style = MaterialTheme.typography.bodySmall)
                                    Text("₹${(app.serviceFee + app.creativeFee).toInt()}", style = MaterialTheme.typography.bodySmall)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Discount Applied", style = MaterialTheme.typography.bodySmall)
                                    Text("-₹${app.discount.toInt()}", style = MaterialTheme.typography.bodySmall, color = Color(0xFFFF4444))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Final Client Price (incl. tax)", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("₹${app.finalPrice.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Black, color = AuraLime)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("Estimated Profit", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                                    Text("₹${app.estimatedProfit.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = AuraCyan)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(
                                onClick = { OwnerContactIntegration.dispatchSmsApproval(context, app, app.ownerPhone) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("SMS 8309596486", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                            OutlinedButton(
                                onClick = { OwnerContactIntegration.dispatchWhatsAppApproval(context, app, app.ownerPhone) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("WhatsApp", fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApprove(app) },
                                colors = ButtonDefaults.buttonColors(containerColor = AuraLime, contentColor = Color.Black),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("APPROVE", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                            OutlinedButton(
                                onClick = { onReject(app, "Owner declined commercial terms") },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("REJECT", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }

        // Generated Quotations List (Section 10)
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text("FORMAL QUOTATIONS ARCHIVE", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }

        items(quotations) { quote ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(quote.quotationCode, fontWeight = FontWeight.Black, fontSize = 14.sp, color = AuraCyan)
                            Text(quote.businessName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = when (quote.status) {
                                "APPROVED" -> AuraLime.copy(alpha = 0.2f)
                                "PENDING_APPROVAL" -> Color(0xFFFF9900).copy(alpha = 0.2f)
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = quote.status,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = when (quote.status) {
                                    "APPROVED" -> AuraLime
                                    "PENDING_APPROVAL" -> Color(0xFFFF9900)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(quote.campaignDescription, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Total Amount: ₹${quote.finalAmount.toInt()} (Ad Spend: ₹${quote.adSpend.toInt()}, Service Fee: ₹${quote.serviceFee.toInt()})", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = AuraLime)
                    Text("Payment Terms: ${quote.paymentTerms}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: CAMPAIGNS & FINANCE
// -------------------------------------------------------------
@Composable
fun CampaignsAndFinanceTab(
    campaigns: List<AdCampaign>,
    payments: List<AdPaymentRecord>,
    onVerifyPayment: (AdPaymentRecord) -> Unit,
    onLaunchCampaign: (AdCampaign) -> Unit,
    onPauseCampaign: (AdCampaign) -> Unit,
    onResumeCampaign: (AdCampaign) -> Unit,
    onCompleteCampaign: (AdCampaign) -> Unit,
    onCancelCampaign: (AdCampaign) -> Unit,
    onSimulateMetrics: (AdCampaign) -> Unit
) {
    val totalRevenue = campaigns.sumOf { it.revenue }
    val totalProfit = campaigns.sumOf { it.profit }
    val totalSpend = campaigns.sumOf { it.spent }
    val pendingPayments = payments.filter { it.status == "PAYMENT_PENDING" }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Finance Header
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(18.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("COMMERCIAL PERFORMANCE & REVENUE", fontWeight = FontWeight.Black, fontSize = 14.sp, color = AuraCyan)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Total Revenue", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${totalRevenue.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AuraLime)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Media Spend", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${totalSpend.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Net Profit", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("₹${totalProfit.toInt()}", fontWeight = FontWeight.Black, fontSize = 18.sp, color = AuraViolet)
                        }
                    }
                }
            }
        }

        // Pending Client Payments (Section 12)
        if (pendingPayments.isNotEmpty()) {
            item {
                Text("PENDING CLIENT PAYMENTS", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            items(pendingPayments) { p ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFF9900)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Quote: ${p.quotationCode}", fontWeight = FontWeight.Bold, color = AuraCyan, fontSize = 13.sp)
                            Text("Client: ${p.clientName} • ₹${p.amount.toInt()}", style = MaterialTheme.typography.bodySmall)
                            Text("Status: PAYMENT PENDING", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFF9900), fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onVerifyPayment(p) },
                            colors = ButtonDefaults.buttonColors(containerColor = AuraLime, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("VERIFY", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Campaigns List
        item {
            Text("CAMPAIGN WORKSPACE", fontWeight = FontWeight.Black, fontSize = 15.sp)
        }

        if (campaigns.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("No campaigns in workspace.", fontWeight = FontWeight.Bold)
                        Text("Campaigns initialize automatically once an owner approves a quotation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(campaigns) { camp ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(camp.title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text("${camp.businessName} • ${camp.platform}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = when (camp.status) {
                                    "RUNNING" -> AuraLime.copy(alpha = 0.2f)
                                    "READY" -> AuraCyan.copy(alpha = 0.2f)
                                    "PAYMENT_PENDING" -> Color(0xFFFF9900).copy(alpha = 0.2f)
                                    else -> MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = camp.status,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when (camp.status) {
                                        "RUNNING" -> AuraLime
                                        "READY" -> AuraCyan
                                        "PAYMENT_PENDING" -> Color(0xFFFF9900)
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Budget: ₹${camp.budget.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                            Text("Spent: ₹${camp.spent.toInt()}", style = MaterialTheme.typography.bodySmall)
                            Text("Profit: ₹${camp.profit.toInt()}", style = MaterialTheme.typography.bodySmall, color = AuraLime, fontWeight = FontWeight.Bold)
                        }

                        // Notice for manual platform requirement (Section 14)
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (camp.status == "RUNNING") "Active live monitoring. AI optimization calibrated."
                                    else "Campaign launch requires manual platform setup.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Telemetry Metrics (Impressions, Clicks, CTR, CPC)
                        if (camp.status == "RUNNING" || camp.impressions > 0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("Impr", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        Text(String.format(Locale.US, "%,d", camp.impressions), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("Clicks", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        Text(String.format(Locale.US, "%,d", camp.clicks), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AuraCyan)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("CTR", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        Text("${String.format(Locale.US, "%.1f", camp.ctr)}%", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AuraLime)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Column(modifier = Modifier.padding(6.dp)) {
                                        Text("CPC", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.sp)
                                        Text("₹${String.format(Locale.US, "%.1f", camp.cpc)}", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // State Machine Lifecycle Actions
                        Spacer(modifier = Modifier.height(12.dp))
                        when (camp.status) {
                            "READY" -> {
                                Button(
                                    onClick = { onLaunchCampaign(camp) },
                                    colors = ButtonDefaults.buttonColors(containerColor = AuraLime, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("LAUNCH CAMPAIGN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                            "RUNNING" -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = { onPauseCampaign(camp) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Pause, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("PAUSE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onCompleteCampaign(camp) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuraCyan, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("COMPLETE", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onSimulateMetrics(camp) },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("+ TRAFFIC", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            "PAUSED" -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { onResumeCampaign(camp) },
                                        colors = ButtonDefaults.buttonColors(containerColor = AuraLime, contentColor = Color.Black),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("RESUME", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onCancelCampaign(camp) },
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("CANCEL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            "PAYMENT_PENDING" -> {
                                Text(
                                    "Awaiting client advance payment verification before launch.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFF9900)
                                )
                            }
                            "COMPLETED" -> {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = AuraLime.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Campaign successfully completed. Final performance archived.",
                                        color = AuraLime,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                            "CANCELLED" -> {
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "Campaign cancelled. Media spend reconciliation recorded.",
                                        color = MaterialTheme.colorScheme.error,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 5: ACTIVITY LOG
// -------------------------------------------------------------
@Composable
fun ActivityLogTab(logs: List<com.example.data.model.AiAdActionLog>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Column {
                Text("AI ACTIVITY AUDIT TRAIL", fontWeight = FontWeight.Black, fontSize = 16.sp)
                Text("Chronological record of automated decisions & client events", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        items(logs) { log ->
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(AuraCyan.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.History, contentDescription = null, tint = AuraCyan, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(log.actionType, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = AuraCyan)
                            Text(
                                SimpleDateFormat("hh:mm:ss a", Locale.US).format(Date(log.timestamp)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(log.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// SUPPORTING MODALS & DIALOGS
// -------------------------------------------------------------
@Composable
fun AddClientRequirementDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, String, String, String, String, String, String, String, Double, Int, Boolean) -> Unit
) {
    var businessName by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Gym") }
    var contactPerson by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("Bengaluru") }
    var targetAudience by remember { mutableStateOf("Adults 20-45 yrs within 5km radius") }
    var objective by remember { mutableStateOf("Lead Generation") }
    var platforms by remember { mutableStateOf("Meta Ads & Google Ads") }
    var budgetStr by remember { mutableStateOf("10000") }
    var durationDaysStr by remember { mutableStateOf("14") }
    var hasCreatives by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("10-Point Client Requirement Analysis", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value = businessName, onValueChange = { businessName = it }, label = { Text("1. Business / Brand Name") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("2. Category (Gym, Restaurant, Hotel...)") }, singleLine = true)
                OutlinedTextField(value = contactPerson, onValueChange = { contactPerson = it }, label = { Text("3. Contact Person") }, singleLine = true)
                OutlinedTextField(value = phone, onValueChange = { phone = it }, label = { Text("4. Phone Number") }, singleLine = true)
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("5. Email Address") }, singleLine = true)
                OutlinedTextField(value = city, onValueChange = { city = it }, label = { Text("6. Location / Target City") }, singleLine = true)
                OutlinedTextField(value = targetAudience, onValueChange = { targetAudience = it }, label = { Text("7. Target Audience Profile") }, singleLine = true)
                OutlinedTextField(value = objective, onValueChange = { objective = it }, label = { Text("8. Campaign Objective") }, singleLine = true)
                OutlinedTextField(value = budgetStr, onValueChange = { budgetStr = it }, label = { Text("9. Approximate Budget (₹)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                OutlinedTextField(value = durationDaysStr, onValueChange = { durationDaysStr = it }, label = { Text("10. Duration (Days)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = hasCreatives, onCheckedChange = { hasCreatives = it })
                    Text("Client already has ready ad creatives", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (businessName.isNotBlank()) {
                        val budget = budgetStr.toDoubleOrNull() ?: 5000.0
                        val days = durationDaysStr.toIntOrNull() ?: 7
                        onSubmit(businessName, category, contactPerson, phone, email, city, targetAudience, objective, platforms, budget, days, hasCreatives)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
            ) {
                Text("SAVE & ANALYZE", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun QuotationCalculatorModal(
    prospect: ClientProspect,
    rules: PricingRuleEntity,
    onDismiss: () -> Unit,
    onGenerateQuote: (Double, Int, Boolean, Double) -> Unit
) {
    var adSpend by remember { mutableDoubleStateOf(prospect.approxBudget.coerceAtLeast(3000.0)) }
    var durationDays by remember { mutableIntStateOf(prospect.expectedDurationDays.coerceAtLeast(7)) }
    var requiresCreative by remember { mutableStateOf(!prospect.hasCreatives) }
    var discountPercent by remember { mutableDoubleStateOf(0.0) }

    val serviceFee = adSpend * (rules.serviceFeePercent / 100.0)
    val creativeFee = if (requiresCreative) rules.creativeProductionFee else 0.0
    val platformFee = adSpend * (rules.managementFeePercent / 100.0)
    val gross = adSpend + serviceFee + creativeFee + platformFee
    val discount = gross * (discountPercent / 100.0)
    val subtotal = (gross - discount).coerceAtLeast(rules.minCampaignPrice)
    val tax = subtotal * 0.18
    val finalTotal = subtotal + tax
    val estimatedProfit = (serviceFee + creativeFee + platformFee - discount).coerceAtLeast(0.0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quotation Cost Calculator", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Client: ${prospect.businessName}", fontWeight = FontWeight.Bold, color = AuraCyan)
                Spacer(modifier = Modifier.height(10.dp))

                Text("Ad Spend: ₹${adSpend.toInt()}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Slider(
                    value = adSpend.toFloat(),
                    onValueChange = { adSpend = it.toDouble() },
                    valueRange = 2000f..50000f,
                    steps = 48
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text("Campaign Duration: $durationDays Days", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(7, 14, 21, 30).forEach { d ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (durationDays == d) AuraCyan else MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.clickable { durationDays = d }
                        ) {
                            Text(
                                text = "${d}d",
                                color = if (durationDays == d) Color.Black else MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = requiresCreative, onCheckedChange = { requiresCreative = it })
                    Text("Requires Creative Production (+₹${rules.creativeProductionFee.toInt()})", style = MaterialTheme.typography.bodySmall)
                }

                Spacer(modifier = Modifier.height(6.dp))
                Text("Concession / Discount: ${discountPercent.toInt()}% (Max ${rules.maxDiscountPercent.toInt()}%)", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = discountPercent.toFloat(),
                    onValueChange = { discountPercent = it.toDouble() },
                    valueRange = 0f..rules.maxDiscountPercent.toFloat()
                )

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Service Fee (${rules.serviceFeePercent.toInt()}%)", style = MaterialTheme.typography.labelSmall)
                            Text("₹${serviceFee.toInt()}", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Platform & Monitoring (${rules.managementFeePercent.toInt()}%)", style = MaterialTheme.typography.labelSmall)
                            Text("₹${platformFee.toInt()}", style = MaterialTheme.typography.labelSmall)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("GST (18%)", style = MaterialTheme.typography.labelSmall)
                            Text("₹${tax.toInt()}", style = MaterialTheme.typography.labelSmall)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Total Client Price", fontWeight = FontWeight.Black)
                            Text("₹${finalTotal.toInt()}", fontWeight = FontWeight.Black, color = AuraLime)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Estimated Profit", style = MaterialTheme.typography.labelSmall)
                            Text("₹${estimatedProfit.toInt()}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = AuraCyan)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onGenerateQuote(adSpend, durationDays, requiresCreative, discountPercent) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
            ) {
                Text("DISPATCH FOR APPROVAL", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun NegotiationSimulationModal(
    prospect: ClientProspect,
    rules: PricingRuleEntity,
    onDismiss: () -> Unit,
    onRequestApproval: (Double) -> Unit
) {
    var offerText by remember { mutableStateOf("4500") }
    var negotiationReply by remember { mutableStateOf<String?>(null) }
    var allowEscalation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AURA Negotiation Chamber", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Client: ${prospect.businessName}", fontWeight = FontWeight.Bold, color = AuraCyan)
                Text("Rule Limits: Starting ₹${rules.baseStartingPrice.toInt()} • Hard Floor ₹${rules.minCampaignPrice.toInt()}", style = MaterialTheme.typography.labelSmall)
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = offerText,
                    onValueChange = { offerText = it },
                    label = { Text("Client Counter Offer Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = {
                        val offer = offerText.toDoubleOrNull() ?: 0.0
                        if (offer < rules.minCampaignPrice) {
                            negotiationReply = "AURA: \"Our configured package starts at ₹${rules.baseStartingPrice.toInt()}. While ₹${offer.toInt()} is below our minimum threshold of ₹${rules.minCampaignPrice.toInt()}, I can check whether a smaller package can fit your ₹${offer.toInt()} budget, or request owner approval at ${rules.ownerPhone}.\""
                            allowEscalation = false
                        } else {
                            val maxAllowed = rules.baseStartingPrice * (1 - (rules.maxDiscountPercent / 100.0))
                            if (offer >= maxAllowed) {
                                negotiationReply = "AURA: \"I can calibrate our package to match ₹${offer.toInt()} under our verified commercial terms. Submitting to owner (${rules.ownerPhone}) for final sign-off.\""
                                allowEscalation = true
                            } else {
                                negotiationReply = "AURA: \"The lowest optimized rate I can approve for this scope is ₹${maxAllowed.toInt()} (${rules.maxDiscountPercent.toInt()}% concession). Would you like me to request owner review?\""
                                allowEscalation = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("EVALUATE OFFER")
                }

                negotiationReply?.let { rep ->
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                        Text(rep, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(10.dp))
                    }
                }
            }
        },
        confirmButton = {
            if (allowEscalation) {
                Button(
                    onClick = {
                        val offer = offerText.toDoubleOrNull() ?: rules.minCampaignPrice
                        onRequestApproval(offer)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AuraLime, contentColor = Color.Black)
                ) {
                    Text("SUBMIT FOR OWNER APPROVAL")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CLOSE") }
        }
    )
}

@Composable
fun PricingSettingsModal(
    currentRules: PricingRuleEntity,
    onDismiss: () -> Unit,
    onSave: (PricingRuleEntity) -> Unit
) {
    var ownerPhone by remember { mutableStateOf(currentRules.ownerPhone) }
    var isPhonePublic by remember { mutableStateOf(currentRules.isOwnerPhonePublic) }
    var minPriceStr by remember { mutableStateOf(currentRules.minCampaignPrice.toInt().toString()) }
    var startPriceStr by remember { mutableStateOf(currentRules.baseStartingPrice.toInt().toString()) }
    var maxDiscountStr by remember { mutableStateOf(currentRules.maxDiscountPercent.toInt().toString()) }
    var serviceFeeStr by remember { mutableStateOf(currentRules.serviceFeePercent.toInt().toString()) }
    var creativeFeeStr by remember { mutableStateOf(currentRules.creativeProductionFee.toInt().toString()) }
    var mode by remember { mutableStateOf(currentRules.autonomousMode) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Owner Pricing & Safety Rules", fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = ownerPhone, onValueChange = { ownerPhone = it }, label = { Text("Owner Phone Number (Approvals)") }, singleLine = true)
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPhonePublic = !isPhonePublic }
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isPhonePublic,
                        onCheckedChange = { isPhonePublic = it }
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text("Display Contact Publicly", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Text(
                            text = if (isPhonePublic) "Owner phone is visible on client proposals." else "Private: Owner phone is masked (******6486).",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedTextField(value = startPriceStr, onValueChange = { startPriceStr = it }, label = { Text("Base Starting Price (₹)") }, singleLine = true)
                OutlinedTextField(value = minPriceStr, onValueChange = { minPriceStr = it }, label = { Text("Absolute Minimum Price Floor (₹)") }, singleLine = true)
                OutlinedTextField(value = maxDiscountStr, onValueChange = { maxDiscountStr = it }, label = { Text("Maximum Concession / Discount (%)") }, singleLine = true)
                OutlinedTextField(value = serviceFeeStr, onValueChange = { serviceFeeStr = it }, label = { Text("Service Fee (%)") }, singleLine = true)
                OutlinedTextField(value = creativeFeeStr, onValueChange = { creativeFeeStr = it }, label = { Text("Creative Production Fee (₹)") }, singleLine = true)

                Spacer(modifier = Modifier.height(4.dp))
                Text("Autonomous Mode (Section 21)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                listOf("ASSISTED", "SEMI-AUTONOMOUS", "AUTONOMOUS").forEach { m ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { mode = m }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = if (mode == m) AuraCyan else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(14.dp)
                        ) {}
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(m, fontSize = 12.sp, fontWeight = if (mode == m) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        currentRules.copy(
                            ownerPhone = ownerPhone,
                            isOwnerPhonePublic = isPhonePublic,
                            minCampaignPrice = minPriceStr.toDoubleOrNull() ?: 4000.0,
                            baseStartingPrice = startPriceStr.toDoubleOrNull() ?: 5000.0,
                            maxDiscountPercent = maxDiscountStr.toDoubleOrNull() ?: 20.0,
                            serviceFeePercent = serviceFeeStr.toDoubleOrNull() ?: 15.0,
                            creativeProductionFee = creativeFeeStr.toDoubleOrNull() ?: 1200.0,
                            autonomousMode = mode
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = Color.Black)
            ) {
                Text("SAVE RULES", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCEL") }
        }
    )
}

@Composable
fun AdsMetricCard(
    title: String,
    value: String,
    sub: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Black, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(sub, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
