package com.dreamtravel.ui.onboarding

import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.dreamtravel.R
import com.dreamtravel.analytics.AnalyticsEvent
import com.dreamtravel.analytics.AnalyticsManager
import com.dreamtravel.databinding.FragmentOnboardingBinding
import com.dreamtravel.util.PermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    @Inject
    lateinit var analytics: AnalyticsManager

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private var currentPage = 0

    companion object {
        private const val PREFS_NAME = "dream_travel_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
        private const val KEY_SAVED_PAGE = "onboarding_current_page"

        private const val REQUEST_CODE_FOREGROUND = 100
        private const val REQUEST_CODE_BACKGROUND = 101
        private const val REQUEST_CODE_NOTIFICATION = 102
    }

    // 4 pages on API 29+ (with background explanation), 3 otherwise
    private val onboardingPages: List<OnboardingPage> by lazy {
        if (hasBackgroundPage()) {
            listOf(
                OnboardingPage(R.string.onboarding_welcome, R.drawable.ic_launcher_foreground),
                OnboardingPage(R.string.onboarding_location, R.drawable.ic_launcher_foreground),
                OnboardingPage(R.string.onboarding_permission, R.drawable.ic_launcher_foreground),
                OnboardingPage(R.string.onboarding_background, R.drawable.ic_launcher_foreground)
            )
        } else {
            listOf(
                OnboardingPage(R.string.onboarding_welcome, R.drawable.ic_launcher_foreground),
                OnboardingPage(R.string.onboarding_location, R.drawable.ic_launcher_foreground),
                OnboardingPage(R.string.onboarding_permission, R.drawable.ic_launcher_foreground)
            )
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        if (savedInstanceState != null) {
            currentPage = savedInstanceState.getInt(KEY_SAVED_PAGE, 0)
        }

        // Skip button — always completes onboarding, no permissions required
        binding.btnSkip.setOnClickListener { skipAndFinish() }

        // Next / Allow button
        binding.btnNext.setOnClickListener { handleNextClick() }

        // ViewPager
        binding.viewPager.apply {
            adapter = OnboardingPagerAdapter(onboardingPages)
            setCurrentItem(currentPage, false)
            registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                override fun onPageSelected(position: Int) {
                    currentPage = position
                    updateButtonText()
                }
            })
        }

        updateButtonText()

        // Privacy policy link
        binding.tvPrivacy.setOnClickListener { showPrivacyDialog() }
    }

    // ── Button handlers ────────────────────────────────────────────

    private fun handleNextClick() {
        if (isForegroundRequestPage()) {
            requestPermissions(
                PermissionUtils.getForegroundPermissions(),
                REQUEST_CODE_FOREGROUND
            )
        } else if (isBackgroundRequestPage()) {
            requestPermissions(
                PermissionUtils.getBackgroundPermissions(),
                REQUEST_CODE_BACKGROUND
            )
        } else {
            currentPage++
            binding.viewPager.currentItem = currentPage
        }
    }

    private fun skipAndFinish() {
        analytics.logEvent(AnalyticsEvent.ONBOARDING_SKIPPED)
        finishOnboarding()
    }

    // ── Page helpers ───────────────────────────────────────────────

    private fun hasBackgroundPage(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    private fun isForegroundRequestPage(): Boolean {
        return currentPage == if (hasBackgroundPage()) {
            onboardingPages.size - 2 // 4 pages: index 2 is foreground
        } else {
            onboardingPages.size - 1 // 3 pages: index 2 (last) is foreground
        }
    }

    private fun isBackgroundRequestPage(): Boolean {
        return hasBackgroundPage() && currentPage == onboardingPages.size - 1
    }

    private fun updateButtonText() {
        binding.btnNext.text = if (isForegroundRequestPage() || isBackgroundRequestPage()) {
            "允许"
        } else {
            "下一步"
        }
    }

    // ── Permission result handling ─────────────────────────────────

    @Suppress("DEPRECATION")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CODE_FOREGROUND -> handleForegroundResult(permissions, grantResults)
            REQUEST_CODE_BACKGROUND -> {
                if (permissions.isNotEmpty()) {
                    handleBackgroundResult(permissions, grantResults)
                }
            }
            REQUEST_CODE_NOTIFICATION -> handleNotificationResult(grantResults)
        }
    }

    // ── Foreground location result ─────────────────────────────────

    private fun handleForegroundResult(
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = allGranted(grantResults)
        analytics.logEvent(
            AnalyticsEvent.PERMISSION_FOREGROUND,
            mapOf(AnalyticsEvent.Param.GRANTED to granted)
        )
        PermissionUtils.savePermissionResult(
            requireContext(),
            PermissionUtils.KEY_PERMISSION_FOREGROUND,
            granted
        )

        if (granted) {
            advanceAfterPermission()
        } else {
            showPermissionDeniedDialog(
                permissions = permissions,
                rationaleResId = R.string.permission_location_rationale,
                retryAction = {
                    requestPermissions(
                        PermissionUtils.getForegroundPermissions(),
                        REQUEST_CODE_FOREGROUND
                    )
                },
                skipAction = { advanceAfterPermission() }
            )
        }
    }

    // ── Background location result ─────────────────────────────────

    private fun handleBackgroundResult(
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        val granted = allGranted(grantResults)
        analytics.logEvent(
            AnalyticsEvent.PERMISSION_BACKGROUND,
            mapOf(AnalyticsEvent.Param.GRANTED to granted)
        )
        PermissionUtils.savePermissionResult(
            requireContext(),
            PermissionUtils.KEY_PERMISSION_BACKGROUND,
            granted
        )

        if (granted) {
            finishOnboarding()
        } else {
            showPermissionDeniedDialog(
                permissions = permissions,
                rationaleResId = R.string.permission_background_rationale,
                retryAction = {
                    requestPermissions(
                        PermissionUtils.getBackgroundPermissions(),
                        REQUEST_CODE_BACKGROUND
                    )
                },
                skipAction = { finishOnboarding() }
            )
        }
    }

    // ── Notification result ────────────────────────────────────────

    private fun handleNotificationResult(grantResults: IntArray) {
        val granted = allGranted(grantResults)
        analytics.logEvent(
            AnalyticsEvent.PERMISSION_NOTIFICATION,
            mapOf(AnalyticsEvent.Param.GRANTED to granted)
        )
        PermissionUtils.savePermissionResult(
            requireContext(),
            PermissionUtils.KEY_PERMISSION_NOTIFICATION,
            granted
        )
    }

    // ── Dialog helpers ─────────────────────────────────────────────

    /**
     * Shows the appropriate dialog after a permission denial:
     * - If permanently denied → "前往设置" (open app settings)
     * - If denied with rationale still available → "重试" / "跳过"
     */
    private fun showPermissionDeniedDialog(
        permissions: Array<out String>,
        rationaleResId: Int,
        retryAction: () -> Unit,
        skipAction: () -> Unit
    ) {
        val firstPermission = permissions.firstOrNull() ?: return
        val permanentlyDenied = PermissionUtils.isPermanentlyDenied(this, firstPermission)

        if (permanentlyDenied) {
            AlertDialog.Builder(requireContext())
                .setTitle("需要权限")
                .setMessage(R.string.permission_permanently_denied)
                .setPositiveButton(R.string.permission_settings) { _, _ ->
                    PermissionUtils.openAppSettings(requireContext())
                    skipAction()
                }
                .setNegativeButton(R.string.btn_skip) { _, _ -> skipAction() }
                .setCancelable(false)
                .show()
        } else {
            AlertDialog.Builder(requireContext())
                .setTitle("需要权限")
                .setMessage(rationaleResId)
                .setPositiveButton(R.string.permission_retry) { _, _ -> retryAction() }
                .setNegativeButton(R.string.btn_skip) { _, _ -> skipAction() }
                .setCancelable(false)
                .show()
        }
    }

    // ── Flow control ───────────────────────────────────────────────

    /** Advance to the next page, or finish if no more pages remain */
    private fun advanceAfterPermission() {
        if (hasBackgroundPage() && !isBackgroundRequestPage()) {
            // Foreground granted, move to background explanation page
            currentPage++
            binding.viewPager.currentItem = currentPage
        } else {
            // No background page needed → request notification then finish
            requestNotificationPermission()
            finishOnboarding()
        }
    }

    private fun requestNotificationPermission() {
        val perms = PermissionUtils.getNotificationPermission()
        if (perms.isNotEmpty()) {
            requestPermissions(perms, REQUEST_CODE_NOTIFICATION)
        }
    }

    private fun finishOnboarding() {
        analytics.logEvent(AnalyticsEvent.ONBOARDING_COMPLETED)
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        findNavController().navigate(R.id.action_onboarding_to_placeList)
    }

    // ── Util ───────────────────────────────────────────────────────

    private fun allGranted(grantResults: IntArray): Boolean {
        return grantResults.isNotEmpty() &&
                grantResults.all { it == PackageManager.PERMISSION_GRANTED }
    }

    private fun showPrivacyDialog() {
        val content = Html.fromHtml(
            getString(R.string.privacy_policy_content),
            Html.FROM_HTML_MODE_LEGACY
        )
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.privacy_policy_title)
            .setMessage(content)
            .setPositiveButton(android.R.string.ok, null)
            .show()
    }

    // ── Lifecycle ──────────────────────────────────────────────────

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt(KEY_SAVED_PAGE, currentPage)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class OnboardingPage(val textResId: Int, val imageResId: Int)
