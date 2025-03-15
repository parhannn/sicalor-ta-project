package com.example.sicalor.adapter

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.sicalor.R
import com.example.sicalor.ui.fragment.OnboardingFragment

class OnboardingAdapter(fragmentActivity: FragmentActivity) : FragmentStateAdapter(fragmentActivity) {
    override fun getItemCount(): Int = 3
    private val context: Context = fragmentActivity

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> OnboardingFragment.newInstance(
                context.getString(R.string.onboarding_title_1),
                context.getString(R.string.onboarding_desc_1),
                R.drawable.onboard_first
            )
            1 -> OnboardingFragment.newInstance(
                context.getString(R.string.onboarding_title_2),
                context.getString(R.string.onboarding_desc_2),
                R.drawable.onboard_second
            )
            else -> OnboardingFragment.newInstance(
                context.getString(R.string.onboarding_title_3),
                context.getString(R.string.onboarding_desc_3),
                R.drawable.onboard_third
            )
        }
    }
}