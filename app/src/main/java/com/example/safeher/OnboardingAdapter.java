package com.example.safeher;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class OnboardingAdapter extends FragmentStateAdapter {

    // Define placeholder images and descriptions
    // IMPORTANT: Replace R.drawable.placeholder_X with your actual image resource IDs later
    private static final int[] IMAGE_RES_IDS = {
            R.drawable.onboarding1, // Replace with actual image 1
            R.drawable.dashboard_banner, // Replace with actual image 2
            R.drawable.dashboard_banner, // Replace with actual image 3
            R.drawable.register_banner  // Replace with actual image 4
    };

    private static final String[] TITLES = {
            "SafeHer", // Title for Page 1
            "Enhanced SOS", // Title for Page 2
            "Community Connection", // Title for Page 3
            "Your Privacy Matters" // Title for Page 4
    };

    private static final String[] DESCRIPTIONS = {
            "\"Your safety, your strength, your companion.\"", // Page 1 Desc (Example Quote)
            "Notify nearby users alongside your emergency contacts during an SOS.", // Page 2 Desc
            "Connect with the SafeHer community for support and shared experiences.", // Page 3 Desc
            "\"Your location and data are shared only when you choose, putting you in control.\"" // Page 4 Desc (Example Quote)
    };

    public OnboardingAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position >= 0 && position < IMAGE_RES_IDS.length) {
            return OnboardingPageFragment.newInstance(IMAGE_RES_IDS[position], TITLES[position], DESCRIPTIONS[position]);
        }
        // Fallback for safety, though should not happen with getItemCount
        return OnboardingPageFragment.newInstance(R.drawable.ic_launcher_background, "Error", "Invalid page.");
    }

    @Override
    public int getItemCount() {
        return IMAGE_RES_IDS.length; // Number of onboarding pages
    }
}
