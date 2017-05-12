package com.marshmallowsocks.xkcd.activities;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import com.github.paolorotolo.appintro.AppIntro;
import com.github.paolorotolo.appintro.AppIntroFragment;
import com.marshmallowsocks.xkcd.R;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

/**
 * Created by marshmallowsocks on 5/11/2017.
 * first time tutorial
 */

public class MSXkcdIntro extends AppIntro {

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Note here that we DO NOT use setContentView();
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/xkcd.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
        // Instead of fragments, you can also use our default slide
        // Just set a title, description, background and image. AppIntro will do the rest.
        addSlide(AppIntroFragment.newInstance("MSXKCD", "A BEAUTIFUL XKCD VIEWER", R.mipmap.xkcd_icon_round, Color.parseColor("#6E7B91")));
        addSlide(AppIntroFragment.newInstance("TOOLBAR", "Click the heart to favorite comics\nClick the search icon to search for comics\n".toUpperCase(), R.mipmap.xkcd_icon_round, Color.parseColor("#6E7B91")));
        addSlide(AppIntroFragment.newInstance("SIDEBAR", "Click any relevant options to do things; toggle the navigation bar here".toUpperCase(), R.mipmap.xkcd_icon_round, Color.parseColor("#6E7B91")));
        addSlide(AppIntroFragment.newInstance("NAVIGATION BAR", "Use either a standard navigation bar or only a random button".toUpperCase(), R.mipmap.xkcd_icon_round, Color.parseColor("#6E7B91")));
        // OPTIONAL METHODS
        // Override bar/separator color.
        setBarColor(Color.parseColor("#6E7B91"));
        setSeparatorColor(Color.parseColor("#6E7B91"));

        // Hide Skip/Done button.
        showSkipButton(true);
        setProgressButtonEnabled(true);

        // Turn vibration on and set intensity.
        // NOTE: you will probably need to ask VIBRATE permission in Manifest.
        setVibrate(false);
        //setVibrateIntensity(30);
    }

    @Override
    public void onSkipPressed(Fragment currentFragment) {
        super.onSkipPressed(currentFragment);
        finish();
    }

    @Override
    public void onDonePressed(Fragment currentFragment) {
        super.onDonePressed(currentFragment);
        finish();
    }

    @Override
    public void onSlideChanged(@Nullable Fragment oldFragment, @Nullable Fragment newFragment) {
        super.onSlideChanged(oldFragment, newFragment);
        // Do something when the slide changes.
    }
}
