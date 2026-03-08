package com.spendvue.ui.navigation

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Welcome : Screen("welcome")
    object Accounts : Screen("accounts_list")
    object AddEditAccount : Screen("add_edit_account?accountId={accountId}") {
        fun passId(accountId: Int? = null): String {
            return "add_edit_account?accountId=${accountId ?: -1}"
        }
    }
}
