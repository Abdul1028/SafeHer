package com.example.safeher;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.safeher.R;

public class OnboardingPageFragment extends Fragment {

    private static final String ARG_IMAGE_RES_ID = "imageResId";
    private static final String ARG_TITLE = "title"; // Added title argument
    private static final String ARG_DESCRIPTION = "description";

    public static OnboardingPageFragment newInstance(@DrawableRes int imageResId, String title, String description) {
        OnboardingPageFragment fragment = new OnboardingPageFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_IMAGE_RES_ID, imageResId);
        args.putString(ARG_TITLE, title); // Store title
        args.putString(ARG_DESCRIPTION, description);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_onboarding_page, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ImageView imageView = view.findViewById(R.id.imageViewOnboarding);
        TextView titleView = view.findViewById(R.id.textViewOnboardingTitle);
        TextView descriptionView = view.findViewById(R.id.textViewOnboardingDescription);

        if (getArguments() != null) {
            int imageResId = getArguments().getInt(ARG_IMAGE_RES_ID);
            String title = getArguments().getString(ARG_TITLE);
            String description = getArguments().getString(ARG_DESCRIPTION);

            // Use a placeholder if imageResId is 0 or invalid
            if (imageResId != 0) {
                imageView.setImageResource(imageResId);
            } else {
                 imageView.setImageResource(R.drawable.ic_launcher_background); // Default Placeholder
            }
            titleView.setText(title);
            descriptionView.setText(description);
        }
    }
}
