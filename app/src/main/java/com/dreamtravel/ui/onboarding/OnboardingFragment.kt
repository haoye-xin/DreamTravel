package com.dreamtravel.ui.onboarding

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.dreamtravel.R
import com.dreamtravel.databinding.FragmentOnboardingBinding
import com.dreamtravel.util.PermissionUtils

@Suppress("DEPRECATION")
class OnboardingFragment : Fragment(R.layout.fragment_onboarding) {

    private var _binding: FragmentOnboardingBinding? = null
    private val binding get() = _binding!!
    private var currentPage = 0

    companion object {
        private const val PREFS_NAME = "dream_travel_prefs"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
    }

    private val onboardingPages = listOf(
        OnboardingPage(R.string.onboarding_welcome, R.drawable.ic_launcher_foreground),
        OnboardingPage(R.string.onboarding_location, R.drawable.ic_launcher_foreground),
        OnboardingPage(R.string.onboarding_permission, R.drawable.ic_launcher_foreground)
    )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentOnboardingBinding.bind(view)

        // Skip button
        binding.btnSkip.setOnClickListener { finishOnboarding() }

        // Next/Done button
        binding.btnNext.setOnClickListener {
            if (currentPage == onboardingPages.size - 1) {
                requestPermissions()
                finishOnboarding()
            } else {
                currentPage++
                binding.viewPager.currentItem = currentPage
            }
        }

        // ViewPager
        binding.viewPager.adapter = OnboardingPagerAdapter(onboardingPages)
        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                currentPage = position
                binding.btnNext.text = if (position == onboardingPages.size - 1) "允许" else "下一步"
            }
        })
    }

    private fun requestPermissions() {
        val permissions = PermissionUtils.getRequiredPermissions()
        requestPermissions(permissions, 100)
    }

    private fun finishOnboarding() {
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, 0)
        prefs.edit().putBoolean(KEY_ONBOARDING_DONE, true).apply()
        findNavController().navigate(R.id.action_onboarding_to_placeList)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class OnboardingPage(val textResId: Int, val imageResId: Int)
