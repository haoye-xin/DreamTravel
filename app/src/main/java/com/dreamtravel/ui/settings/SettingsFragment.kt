package com.dreamtravel.ui.settings

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.dreamtravel.R
import com.dreamtravel.analytics.AnalyticsEvent
import com.dreamtravel.analytics.AnalyticsManager
import com.dreamtravel.databinding.FragmentSettingsBinding
import com.dreamtravel.util.HealthCheckItem
import com.dreamtravel.util.Severity
import com.dreamtravel.util.StatusManager
import com.dreamtravel.util.StatusMessage
import com.dreamtravel.util.StatusType
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: SettingsViewModel by viewModels()

    @Inject lateinit var statusManager: StatusManager
    @Inject lateinit var analytics: AnalyticsManager

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        analytics.logEvent(AnalyticsEvent.SETTINGS_OPENED)

        setupToolbar()
        setupClickListeners()
        setupStatusPanel()
        observeViewModel()
        displayAppInfo()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupClickListeners() {
        binding.btnUpgradeAccount.setOnClickListener {
            showEmailPasswordDialog(
                title = getString(R.string.settings_upgrade_account),
                positiveButtonText = getString(R.string.settings_upgrade_confirm),
                onConfirm = { email, password ->
                    viewModel.upgradeAccount(email, password)
                }
            )
        }

        binding.btnSignIn.setOnClickListener {
            showEmailPasswordDialog(
                title = getString(R.string.settings_sign_in),
                positiveButtonText = getString(R.string.settings_sign_in),
                onConfirm = { email, password ->
                    viewModel.signIn(email, password)
                }
            )
        }

        binding.btnSignOut.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.settings_sign_out_confirm_title))
                .setMessage(getString(R.string.settings_sign_out_confirm_message))
                .setPositiveButton(getString(R.string.settings_sign_out)) { _, _ ->
                    viewModel.signOut()
                }
                .setNegativeButton(getString(R.string.btn_later), null)
                .show()
        }

        binding.textPrivacyPolicy.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.privacy_policy_title))
                .setMessage(getString(R.string.privacy_policy_content))
                .setPositiveButton(android.R.string.ok, null)
                .show()
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    updateAccountUI(state)
                    updateLoadingState(state)
                    showErrorIfNeeded(state.error)
                }
            }
        }
    }

    private fun updateAccountUI(state: AccountState) {
        val statusText: String
        val emailText: String?

        when {
            state.isAnonymous -> {
                statusText = getString(R.string.settings_account_anonymous)
                emailText = null
                binding.btnUpgradeAccount.visibility = View.VISIBLE
                binding.btnSignIn.visibility = View.VISIBLE
                binding.btnSignOut.visibility = View.GONE
            }
            state.email != null -> {
                statusText = getString(R.string.settings_account_email)
                emailText = state.email
                binding.btnUpgradeAccount.visibility = View.GONE
                binding.btnSignIn.visibility = View.GONE
                binding.btnSignOut.visibility = View.VISIBLE
            }
            else -> {
                statusText = getString(R.string.settings_account_not_available)
                emailText = null
                binding.btnUpgradeAccount.visibility = View.VISIBLE
                binding.btnSignIn.visibility = View.VISIBLE
                binding.btnSignOut.visibility = View.GONE
            }
        }

        binding.textAccountStatus.text = statusText

        if (emailText != null) {
            binding.textAccountEmail.visibility = View.VISIBLE
            binding.textAccountEmail.text = emailText
        } else {
            binding.textAccountEmail.visibility = View.GONE
        }

        // Data status
        if (state.email != null) {
            binding.indicatorSync.setBackgroundResource(R.drawable.ic_circle_green)
            binding.textDataStatus.text = getString(R.string.settings_data_synced)
        } else if (state.isAnonymous) {
            binding.indicatorSync.setBackgroundResource(R.drawable.ic_circle_orange)
            binding.textDataStatus.text = getString(R.string.settings_data_local_anonymous)
        } else {
            binding.indicatorSync.setBackgroundResource(R.drawable.ic_circle_gray)
            binding.textDataStatus.text = getString(R.string.settings_data_local_only)
        }
    }

    private fun updateLoadingState(state: AccountState) {
        binding.progressAuth.visibility = if (state.isLoading) View.VISIBLE else View.GONE
        binding.btnUpgradeAccount.isEnabled = !state.isLoading
        binding.btnSignIn.isEnabled = !state.isLoading
        binding.btnSignOut.isEnabled = !state.isLoading
    }

    private fun showErrorIfNeeded(error: String?) {
        if (error != null) {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle(getString(R.string.settings_error_title))
                .setMessage(error)
                .setPositiveButton(android.R.string.ok) { _, _ -> viewModel.clearError() }
                .show()
        }
    }

    private fun displayAppInfo() {
        val packageInfo = requireContext().packageManager
            .getPackageInfo(requireContext().packageName, 0)
        val versionName = packageInfo.versionName ?: "1.0.0"
        binding.textAppVersion.text = getString(R.string.settings_app_version, versionName)
    }

    private fun showEmailPasswordDialog(
        title: String,
        positiveButtonText: String,
        onConfirm: (email: String, password: String) -> Unit
    ) {
        val container = FrameLayout(requireContext()).apply {
            setPadding(
                resources.getDimensionPixelSize(R.dimen.dialog_horizontal_padding),
                0,
                resources.getDimensionPixelSize(R.dimen.dialog_horizontal_padding),
                0
            )
        }

        val emailLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.settings_email_hint)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
            setPadding(0, 0, 0, 16)
        }
        val emailInput = TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }
        emailLayout.addView(emailInput)

        val passwordLayout = TextInputLayout(requireContext()).apply {
            hint = getString(R.string.settings_password_hint)
            boxBackgroundMode = TextInputLayout.BOX_BACKGROUND_OUTLINE
        }
        val passwordInput = TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or
                android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        passwordLayout.addView(passwordInput)

        val linearLayout = android.widget.LinearLayout(requireContext()).apply {
            orientation = android.widget.LinearLayout.VERTICAL
            addView(emailLayout)
            addView(passwordLayout)
        }
        container.addView(linearLayout)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(title)
            .setView(container)
            .setPositiveButton(positiveButtonText) { _, _ ->
                val email = emailInput.text?.toString().orEmpty().trim()
                val password = passwordInput.text?.toString().orEmpty()
                onConfirm(email, password)
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    // ── System Status Diagnostics ────────────────────────────

    private fun setupStatusPanel() {
        binding.btnRefreshStatus.setOnClickListener {
            refreshStatuses()
        }
        // Initial load
        refreshStatuses()
    }

    private fun refreshStatuses() {
        viewLifecycleOwner.lifecycleScope.launch {
            val items = withContext(Dispatchers.IO) {
                statusManager.checkAllDetailed()
            }
            populateStatusItems(items)
        }
    }

    private fun populateStatusItems(items: List<HealthCheckItem>) {
        val container = binding.statusItemsContainer
        container.removeAllViews()

        // Count only actionable issues (exclude INFO)
        val issues = items.filter { !it.passed && it.issue?.severity != Severity.INFO }
        val hasIssues = issues.isNotEmpty()

        // Summary
        if (hasIssues) {
            binding.textStatusSummary.text = getString(
                R.string.settings_status_count, issues.size
            )
            binding.textStatusSummary.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.accent)
            )
        } else {
            binding.textStatusSummary.text = getString(R.string.settings_status_all_ok)
            binding.textStatusSummary.setTextColor(
                ContextCompat.getColor(requireContext(), R.color.primary)
            )
        }

        // Label lookup: StatusType → label resource ID
        val labelMap = mapOf(
            StatusType.NETWORK to R.string.settings_diag_network,
            StatusType.LOCATION_PERMISSION to R.string.settings_diag_location_perm,
            StatusType.BACKGROUND_LOCATION to R.string.settings_diag_background_location,
            StatusType.LOCATION_GPS to R.string.settings_diag_gps,
            StatusType.NOTIFICATION to R.string.settings_diag_notification,
            StatusType.BATTERY_OPTIMIZATION to R.string.settings_diag_battery_optimization,
            StatusType.FOREGROUND_SERVICE to R.string.settings_diag_foreground_service,
            StatusType.SYNC to R.string.settings_diag_sync
        )

        for (item in items) {
            val diag = DiagnosticItem(
                type = item.type,
                labelResId = labelMap[item.type] ?: R.string.settings_diag_none,
                issue = item.issue
            )
            container.addView(createStatusRow(diag))
        }
    }

    private fun createStatusRow(item: DiagnosticItem): View {
        val ctx = requireContext()
        val row = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dpToPx(8), 0, dpToPx(8))
        }

        // Colored dot
        val dot = View(ctx).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(10), dpToPx(10)).apply {
                marginEnd = dpToPx(8)
            }
            background = ContextCompat.getDrawable(ctx, when {
                item.issue != null && item.issue.severity == Severity.ERROR ->
                    R.drawable.ic_circle_red
                item.issue != null && item.issue.severity == Severity.WARNING ->
                    R.drawable.ic_circle_orange
                else ->
                    R.drawable.ic_circle_green
            })
        }
        row.addView(dot)

        // Label + message text container
        val textContainer = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val labelText = TextView(ctx).apply {
            text = getString(item.labelResId)
            textSize = 14f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_primary))
        }
        textContainer.addView(labelText)

        val messageText = TextView(ctx).apply {
            text = if (item.issue != null) {
                getString(item.issue.messageResId)
            } else {
                getString(R.string.settings_diag_ok)
            }
            textSize = 12f
            setTextColor(ContextCompat.getColor(ctx, R.color.text_secondary))
        }
        textContainer.addView(messageText)

        row.addView(textContainer)

        // Fix action button (only when there's an action)
        item.issue?.action?.let { action ->
            val fixButton = MaterialButton(ctx).apply {
                text = getString(R.string.settings_diag_fix)
                textSize = 12f
                isAllCaps = false
                setPadding(dpToPx(12), dpToPx(4), dpToPx(12), dpToPx(4))
                strokeWidth = dpToPx(1)
                strokeColor = ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.primary)
                )
                setTextColor(ContextCompat.getColor(ctx, R.color.primary))
                setBackgroundColor(Color.TRANSPARENT)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = dpToPx(8)
                }

                setOnClickListener {
                    val intent = statusManager.resolveAction(action.actionType)
                    if (intent != null) {
                        try {
                            startActivity(intent)
                        } catch (_: Exception) {
                            // Ignore if settings activity unavailable
                        }
                    }
                }
            }
            row.addView(fixButton)
        }

        return row
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    /** Holds display state for a single diagnostic row. */
    private data class DiagnosticItem(
        val type: StatusType,
        val labelResId: Int,
        val issue: StatusMessage?
    )
}
