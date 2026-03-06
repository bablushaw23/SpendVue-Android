package com.spendsense.ui.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.spendsense.data.local.entity.Account
import com.spendsense.ui.theme.CreditRed
import com.spendsense.ui.theme.PrimaryIndigo
import com.spendsense.ui.theme.SalaryGold
import com.spendsense.ui.theme.SavingsGreen
import java.text.NumberFormat
import java.util.Currency

// ─────────────────────────────────────────────────────────────────────────────
// Accounts Screen
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    onNavigateToAddAccount: () -> Unit,
    onNavigateToEditAccount: (Int) -> Unit,
    viewModel: AccountsViewModel = hiltViewModel()
) {
    val accounts by viewModel.accounts.collectAsState()
    val archivedAccounts by viewModel.archivedAccounts.collectAsState()
    val totalBalance by viewModel.totalBalance.collectAsState()
    val activeCount by viewModel.activeCount.collectAsState()
    val showArchived by viewModel.showArchived.collectAsState()
    val uiEvent by viewModel.uiEvent.collectAsState()

    var pendingArchiveId by remember { mutableStateOf<Int?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiEvent) {
        when (val ev = uiEvent) {
            is AccountsUiEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(ev.message)
                viewModel.clearEvent()
            }
            null -> Unit
        }
    }

    // Archive confirmation dialog
    if (pendingArchiveId != null) {
        val account = accounts.find { it.id == pendingArchiveId }
        AlertDialog(
            onDismissRequest = { pendingArchiveId = null },
            icon = { Text("🗄️", fontSize = 28.sp) },
            title = { Text("Archive Account?") },
            text = {
                Column {
                    Text(
                        "This will hide \"${account?.accountName ?: ""}\" from your active accounts.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(12.dp))
                    Text("✓ Past transactions remain visible", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("✓ You can restore it anytime", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("✗ Cannot add new transactions to it", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        pendingArchiveId?.let { viewModel.archiveAccount(it) }
                        pendingArchiveId = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Archive") }
            },
            dismissButton = {
                OutlinedButton(onClick = { pendingArchiveId = null }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onNavigateToAddAccount,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Account") },
                containerColor = PrimaryIndigo,
                contentColor = Color.White
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // ── Balance Summary Header ──────────────────────────────────────
            item {
                AccountsHeader(
                    activeCount = activeCount,
                    totalBalance = totalBalance
                )
            }

            // ── Grouped Account Sections ───────────────────────────────────
            val grouped = accounts.groupBy { it.accountType }
            val typeOrder = listOf("SALARY", "SAVINGS", "CREDIT_CARD")

            typeOrder.forEach { type ->
                val group = grouped[type]
                if (!group.isNullOrEmpty()) {
                    item(key = "header_$type") {
                        AccountSectionHeader(type = type, count = group.size)
                    }
                    items(group, key = { it.id }) { account ->
                        AccountCard(
                            account = account,
                            onEditClick = { onNavigateToEditAccount(account.id) },
                            onArchiveClick = { pendingArchiveId = account.id }
                        )
                    }
                }
            }

            // ── Archived Toggle ─────────────────────────────────────────────
            if (archivedAccounts.isNotEmpty()) {
                item {
                    ArchivedToggleRow(
                        count = archivedAccounts.size,
                        expanded = showArchived,
                        onToggle = { viewModel.toggleShowArchived() }
                    )
                }
                if (showArchived) {
                    items(archivedAccounts, key = { "archived_${it.id}" }) { account ->
                        ArchivedAccountCard(
                            account = account,
                            onRestoreClick = { viewModel.unarchiveAccount(account.id) }
                        )
                    }
                }
            }

            // ── Privacy Footer ──────────────────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🔒 ", fontSize = 13.sp)
                    Text(
                        "All data stored on your device only",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountsHeader(activeCount: Int, totalBalance: Double) {
    val fmt = rememberRupeeFormat()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(PrimaryIndigo.copy(alpha = 0.85f), PrimaryIndigo.copy(alpha = 0.5f))
                )
            )
            .padding(horizontal = 20.dp, vertical = 28.dp)
    ) {
        Column {
            Text(
                "My Accounts ($activeCount of 11)",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                fmt.format(totalBalance),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(Modifier.height(8.dp))
            if (activeCount == 0) {
                Text(
                    "Tap + to add your first account",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            } else {
                Text(
                    "Total net balance across all accounts",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Section header
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun AccountSectionHeader(type: String, count: Int) {
    val (emoji, label) = when (type) {
        "SALARY"      -> "💼" to "SALARY ACCOUNTS"
        "SAVINGS"     -> "🏦" to "SAVINGS ACCOUNTS"
        "CREDIT_CARD" -> "💳" to "CREDIT CARDS"
        else          -> "📁" to type
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 16.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "$label ($count)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Account card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AccountCard(
    account: Account,
    onEditClick: () -> Unit,
    onArchiveClick: () -> Unit
) {
    val fmt = rememberRupeeFormat()
    val isCredit = account.accountType == "CREDIT_CARD"
    val balanceColor = when {
        isCredit && account.currentBalance < 0 -> CreditRed
        !isCredit && account.currentBalance < 0 -> MaterialTheme.colorScheme.error
        else -> SavingsGreen
    }
    val typeColor = when (account.accountType) {
        "SALARY"      -> SalaryGold
        "CREDIT_CARD" -> CreditRed
        else          -> SavingsGreen
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Type dot indicator
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                val emoji = when (account.accountType) {
                    "SALARY"      -> "💼"
                    "CREDIT_CARD" -> "💳"
                    else          -> "🏦"
                }
                Text(emoji, fontSize = 20.sp)
            }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    account.accountName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "${account.bankName}  •  ••••${account.last4Digits}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    if (isCredit && account.currentBalance < 0)
                        "${fmt.format(-account.currentBalance)} due"
                    else
                        fmt.format(account.currentBalance),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = balanceColor
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                IconButton(onClick = onEditClick, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Archived toggle row
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArchivedToggleRow(count: Int, expanded: Boolean, onToggle: () -> Unit) {
    Spacer(Modifier.height(8.dp))
    TextButton(
        onClick = onToggle,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            if (expanded) "Hide Archived ($count) ▲" else "Show Archived ($count) ▼",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Archived account card  (muted)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ArchivedAccountCard(account: Account, onRestoreClick: () -> Unit) {
    val fmt = rememberRupeeFormat()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🗄️", fontSize = 20.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    account.accountName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${account.bankName} ••••${account.last4Digits} · Archived",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
            TextButton(onClick = onRestoreClick) {
                Text("Restore", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun rememberRupeeFormat(): NumberFormat = remember {
    NumberFormat.getCurrencyInstance().apply {
        currency = Currency.getInstance("INR")
        maximumFractionDigits = 2
    }
}
