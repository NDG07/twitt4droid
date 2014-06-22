package com.twitt4droid.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.widget.Toast;

import com.twitt4droid.R;

import twitter4j.TwitterException;

public abstract class BaseTimelineFragment extends Fragment {

    protected static final String ENABLE_DARK_THEME_ARG = "ENABLE_DARK_THEME";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    protected void onTwitterError(TwitterException ex) { 
        Toast.makeText(getActivity().getApplicationContext(), 
                R.string.twitt4droid_error_message, 
                Toast.LENGTH_LONG).show();
    }

    public abstract int getResourceTitle();
    public abstract int getResourceHoloLightIcon();
    public abstract int getResourceHoloDarkIcon();

    protected boolean isDarkThemeEnabled() {
        return getArguments().getBoolean(ENABLE_DARK_THEME_ARG, false);
    }
}