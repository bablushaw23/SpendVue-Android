package com.spendvue.ui.accounts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditAccountScreen(
    onNavigateBack: () -> Unit,
    viewModel: AddEditAccountViewModel = hiltViewModel()
) {
    val accountName by viewModel.accountName.collectAsState()
    val bankName by viewModel.bankName.collectAsState()
    val accountType by viewModel.accountType.collectAsState()
    val last4Digits by viewModel.last4Digits.collectAsState()
    val currentBalance by viewModel.currentBalance.collectAsState()
    val uiEvent by viewModel.uiEvent.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val formErrors by viewModel.formErrors.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showArchiveDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiEvent) {
        when (val ev = uiEvent) {
            is UiEvent.NavigateBack -> {
                onNavigateBack()
                viewModel.onEventConsumed()
            }
            is UiEvent.ShowSnackbar -> {
                snackbarHostState.showSnackbar(ev.message)
                viewModel.onEventConsumed()
            }
            null -> Unit
        }
    }

    // Archive confirmation dialog (shown only in edit mode)
    if (showArchiveDialog) {
        AlertDialog(
            onDismissRequest = { showArchiveDialog = false },
            icon = { Text("🗄️", fontSize = 28.sp) },
            title = { Text("Archive \"$accountName\"?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("This account will be hidden from your active list.", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(8.dp))
                    Text("✓ Past transactions remain visible", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("✓ You can restore anytime", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.archiveCurrentAccount()
                        showArchiveDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Archive") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showArchiveDialog = false }) { Text("Cancel") }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(if (viewModel.isEditing) "Edit Account" else "Add Account")
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Account Name ──────────────────────────────────────────────
            SpendSenseTextField(
                value = accountName,
                onValueChange = { viewModel.onEvent(AccountFormEvent.AccountNameChanged(it)) },
                label = "Account Name",
                placeholder = "e.g. Primary Salary",
                errorText = formErrors["accountName"],
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                enabled = true
            )

            // ── Bank Name Dropdown ────────────────────────────────────────
            BankNameDropdown(
                selected = bankName,
                onSelect = { viewModel.onEvent(AccountFormEvent.BankNameChanged(it)) },
                errorText = formErrors["bankName"]
            )

            // ── Account Type (radio, read-only on edit) ───────────────────
            AccountTypeSelector(
                selected = accountType,
                onSelect = { viewModel.onEvent(AccountFormEvent.AccountTypeChanged(it)) },
                readOnly = viewModel.isEditing
            )

            // ── Last 4 Digits (read-only on edit) ─────────────────────────
            SpendSenseTextField(
                value = last4Digits,
                onValueChange = { viewModel.onEvent(AccountFormEvent.Last4DigitsChanged(it)) },
                label = "Last 4 Digits",
                placeholder = "e.g. 6789",
                errorText = formErrors["last4Digits"],
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                enabled = !viewModel.isEditing,
                supportingText = if (viewModel.isEditing)
                    "Last 4 digits cannot be changed" else "Helps match SMS transactions"
            )

            // ── Current Balance ───────────────────────────────────────────
            SpendSenseTextField(
                value = currentBalance,
                onValueChange = { viewModel.onEvent(AccountFormEvent.CurrentBalanceChanged(it)) },
                label = "Current Balance (₹)",
                placeholder = "e.g. 50000",
                errorText = formErrors["balance"],
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                enabled = !viewModel.isEditing,
                supportingText = if (viewModel.isEditing)
                    "Update balance during Daily Sync (Feature 9)" else "Check your bank app for accuracy"
            )

            Spacer(Modifier.height(8.dp))

            // ── Save button ───────────────────────────────────────────────
            Button(
                onClick = { viewModel.onEvent(AccountFormEvent.SaveAccount) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                enabled = !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        if (viewModel.isEditing) "Save Changes" else "Add Account",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // ── Archive button (edit only) ────────────────────────────────
            if (viewModel.isEditing) {
                Spacer(Modifier.height(4.dp))
                OutlinedButton(
                    onClick = { showArchiveDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
                        )
                    )
                ) {
                    Text("🗄️  Archive This Account", fontWeight = FontWeight.Medium)
                }
            }

            // ── Privacy footer ────────────────────────────────────────────
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("🔒 ", fontSize = 12.sp)
                Text(
                    "Saved on your device only · Never sent to any server",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Reusable TextField
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SpendSenseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    errorText: String? = null,
    supportingText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    enabled: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
        isError = errorText != null,
        supportingText = {
            when {
                errorText != null -> Text(errorText, color = MaterialTheme.colorScheme.error)
                supportingText != null -> Text(supportingText, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = keyboardOptions,
        enabled = enabled,
        shape = RoundedCornerShape(12.dp)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Bank name dropdown
// ─────────────────────────────────────────────────────────────────────────────

private val POPULAR_BANKS = listOf(
    "HDFC Bank", "ICICI Bank", "SBI", "Axis Bank", "Kotak Mahindra Bank",
    "IDFC First Bank", "Yes Bank", "IndusInd Bank", "Bank of Baroda",
    "Punjab National Bank", "Canara Bank", "Other"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BankNameDropdown(
    selected: String,
    onSelect: (String) -> Unit,
    errorText: String? = null
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            label = { Text("Bank Name") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            isError = errorText != null,
            supportingText = errorText?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(12.dp),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            POPULAR_BANKS.forEach { bank ->
                DropdownMenuItem(
                    text = { Text(bank) },
                    onClick = { onSelect(bank); expanded = false }
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Account type radio selector
// ─────────────────────────────────────────────────────────────────────────────

private val ACCOUNT_TYPES = listOf(
    Triple("SALARY", "💼", "Salary Account"),
    Triple("SAVINGS", "🏦", "Savings Account"),
    Triple("CREDIT_CARD", "💳", "Credit Card")
)

@Composable
private fun AccountTypeSelector(
    selected: String,
    onSelect: (String) -> Unit,
    readOnly: Boolean
) {
    Column {
        Text(
            "Account Type${if (readOnly) " (cannot be changed)" else ""}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        ACCOUNT_TYPES.forEach { (type, emoji, label) ->
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selected == type,
                    onClick = { if (!readOnly) onSelect(type) },
                    enabled = !readOnly
                )
                Spacer(Modifier.width(6.dp))
                Text("$emoji  $label", style = MaterialTheme.typography.bodyMedium)
            }
        }
        if (readOnly) {
            Text(
                "💡 To change type, archive this account and create a new one",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
    }
}
