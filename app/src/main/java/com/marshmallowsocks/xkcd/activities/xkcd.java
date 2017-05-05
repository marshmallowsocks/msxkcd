package com.marshmallowsocks.xkcd.activities;

import android.animation.Animator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;
import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.core.Constants;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.http.MSRequestQueue;
import com.marshmallowsocks.xkcd.util.xkcd.XKCDComicBean;
import com.willowtreeapps.spruce.Spruce;
import com.willowtreeapps.spruce.animation.DefaultAnimations;
import com.willowtreeapps.spruce.sort.DefaultSort;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;
import java.util.Random;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class xkcd extends AppCompatActivity {

    private MSRequestQueue msRequestQueue;
    private static XKCDComicBean currentComic;
    private static Integer which = -1;
    private static Integer max = -1;
    private boolean isLastComic = false;
    private boolean isFirstComic = false;
    private ColorStateList oldStates;
    private int[][] states;
    private int[] colors;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xkcd);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/xkcd.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        if(getIntent() != null) {
            if(Constants.SEARCH_TO_PAGE_ACTION.equals(getIntent().getAction())) {
                which = getIntent().getIntExtra("newPage", -1);
            }
        }

        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);

        new Spruce
                .SpruceBuilder(viewGroup)
                .sortWith(new DefaultSort(50L))
                .animateWith(new Animator[]{DefaultAnimations.growAnimator(viewGroup, 800)})
                .start();

        states = new int[][]{
                new int[]{android.R.attr.state_enabled}, // enabled
                new int[]{-android.R.attr.state_enabled}, // disabled
                new int[]{-android.R.attr.state_checked}, // unchecked
                new int[]{android.R.attr.state_pressed}  // pressed
        };

        colors = new int[]{
                Color.BLACK,
                Color.RED,
                Color.GREEN,
                Color.BLUE
        };

        msRequestQueue = MSRequestQueue.getInstance(this);
        currentComic = new XKCDComicBean();

        final Button previousButton = (Button) findViewById(R.id.previousButton);
        final Button nextButton = (Button) findViewById(R.id.nextButton);
        final Button firstButton = (Button) findViewById(R.id.firstButton);
        final Button lastButton = (Button) findViewById(R.id.lastButton);
        final Button randomButton = (Button) findViewById(R.id.randomButton);

        final PhotoView comicHolder = (PhotoView) findViewById(R.id.comicHolder);
        final ConstraintLayout componentLayout = (ConstraintLayout) findViewById(R.id.componentHolder);
        final LinearLayout buttonBar = (LinearLayout) findViewById(R.id.buttonBar);
        final TextView altText = (TextView) findViewById(R.id.altText);
        final TextView metadata = (TextView) findViewById(R.id.metadata);
        final Button closeButton = (Button) findViewById(R.id.closeOverlay);
        final Button explainButton = (Button) findViewById(R.id.explainButton);

        new Spruce
                .SpruceBuilder(buttonBar)
                .sortWith(new DefaultSort(100L))
                .animateWith(new Animator[]{DefaultAnimations.shrinkAnimator(buttonBar, 1200)})
                .start();

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                componentLayout.setBackgroundTintList(oldStates);
                comicHolder.setAlpha(1.0f);
                buttonBar.setAlpha(1.0f);
                toggleButtonBar(true);
                altText.setVisibility(View.GONE);
                closeButton.setVisibility(View.GONE);
                explainButton.setVisibility(View.GONE);
                metadata.setVisibility(View.GONE);
                altText.setElevation(0.0f);
            }
        });

        explainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (which == -1) {
                    which = max;
                }
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Constants.EXPLAIN_URL, which)));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setPackage("com.android.chrome");
                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    // Chrome is probably not installed
                    // Try with the default browser
                    i.setPackage(null);
                    startActivity(i);
                }
            }
        });

        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (which == -1) {
                    which = max - 1;
                } else {
                    which--;
                }

                if (which == 1) {
                    isFirstComic = true;
                    isLastComic = false;
                } else {
                    isFirstComic = false;
                    isLastComic = false;
                }
                if (isFirstComic) {
                    previousButton.setEnabled(false);
                    firstButton.setEnabled(false);

                    previousButton.setAlpha(0.5f);
                    firstButton.setAlpha(0.5f);
                }

                nextButton.setEnabled(true);
                lastButton.setEnabled(true);

                nextButton.setAlpha(1f);
                lastButton.setAlpha(1f);

                getComicData(which.toString());
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!Objects.equals(which, max)) {
                    which++;
                }

                if (Objects.equals(which, max)) {
                    isLastComic = true;
                    isFirstComic = false;
                } else {
                    isLastComic = false;
                    isFirstComic = false;
                }
                if (isLastComic) {
                    nextButton.setEnabled(false);
                    lastButton.setEnabled(false);
                    nextButton.setAlpha(0.5f);
                    lastButton.setAlpha(0.5f);
                }

                previousButton.setEnabled(true);
                firstButton.setEnabled(true);
                previousButton.setAlpha(1f);
                firstButton.setAlpha(1f);

                getComicData(which.toString());
            }
        });

        firstButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousButton.setEnabled(false);
                firstButton.setEnabled(false);
                previousButton.setAlpha(0.5f);
                firstButton.setAlpha(0.5f);

                nextButton.setEnabled(true);
                lastButton.setEnabled(true);
                nextButton.setAlpha(1f);
                lastButton.setAlpha(1f);

                which = 1;
                getComicData(which.toString());
            }
        });

        lastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                previousButton.setEnabled(true);
                firstButton.setEnabled(true);
                previousButton.setAlpha(1f);
                firstButton.setAlpha(1f);

                nextButton.setEnabled(false);
                lastButton.setEnabled(false);
                nextButton.setAlpha(0.5f);
                lastButton.setAlpha(0.5f);

                which = -1;
                getComicData();
            }
        });

        randomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random randomNumberGenerator = new Random();

                which = randomNumberGenerator.nextInt(max) + 1;

                if (which == 1) {
                    previousButton.setEnabled(false);
                    firstButton.setEnabled(false);

                    previousButton.setAlpha(0.5f);
                    firstButton.setAlpha(0.5f);

                    nextButton.setEnabled(true);
                    lastButton.setEnabled(true);

                    nextButton.setAlpha(1f);
                    lastButton.setAlpha(1f);
                } else if (Objects.equals(which, max)) {
                    previousButton.setEnabled(true);
                    firstButton.setEnabled(true);

                    previousButton.setAlpha(1f);
                    firstButton.setAlpha(1f);

                    nextButton.setEnabled(false);
                    lastButton.setEnabled(false);

                    nextButton.setAlpha(0.5f);
                    lastButton.setAlpha(0.5f);
                } else {
                    previousButton.setEnabled(true);
                    firstButton.setEnabled(true);

                    nextButton.setEnabled(true);
                    lastButton.setEnabled(true);

                    previousButton.setAlpha(1f);
                    firstButton.setAlpha(1f);

                    nextButton.setAlpha(1f);
                    lastButton.setAlpha(1f);
                }

                getComicData(which.toString());
            }
        });

        if (which != -1) {
            getComicData(which.toString());
        }
        else {
            nextButton.setEnabled(false);
            lastButton.setEnabled(false);
            nextButton.setAlpha(0.5f);
            lastButton.setAlpha(0.5f);
            getComicData();
        }
    }

    private void toggleButtonBar(boolean toggle) {
        LinearLayout buttonBar = (LinearLayout) findViewById(R.id.buttonBar);
        if(which > 1 && which < max) {
            for (int i = 0; i < buttonBar.getChildCount(); i++) {
                buttonBar.getChildAt(i).setEnabled(toggle);
            }
        }
        if(which == 1) {
            for (int i = 2; i < buttonBar.getChildCount(); i++) {
                buttonBar.getChildAt(i).setEnabled(toggle);
            }
        }
        if(Objects.equals(which, max)) {
            for (int i = 0; i < buttonBar.getChildCount() - 2; i++) {
                buttonBar.getChildAt(i).setEnabled(toggle);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_xkcd, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_alt_text) {

            final PhotoView comicHolder = (PhotoView) findViewById(R.id.comicHolder);
            final ConstraintLayout componentLayout = (ConstraintLayout) findViewById(R.id.componentHolder);
            final LinearLayout buttonBar = (LinearLayout) findViewById(R.id.buttonBar);
            final TextView altText = (TextView) findViewById(R.id.altText);
            final TextView metadata = (TextView) findViewById(R.id.metadata);
            final Button closeButton = (Button) findViewById(R.id.closeOverlay);
            final Button explainButton = (Button) findViewById(R.id.explainButton);

            if(altText.getVisibility() == View.GONE) {
                oldStates = componentLayout.getBackgroundTintList();
                componentLayout.setBackgroundTintList(new ColorStateList(states, colors));
                comicHolder.setAlpha(0.3f);
                buttonBar.setAlpha(0.3f);
                toggleButtonBar(false);
                altText.setVisibility(View.VISIBLE);
                altText.setElevation(15.0f);
                closeButton.setVisibility(View.VISIBLE);
                explainButton.setVisibility(View.VISIBLE);
                metadata.setVisibility(View.VISIBLE);
            }
            return true;
        }

        if(id == R.id.action_what_if) {
            Intent whatIfIntent = new Intent(this, WhatIf.class);
            startActivity(whatIfIntent);
            return true;
        }
        if(id == R.id.action_search) {
            onSearchRequested();
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadComic() {
        PhotoView comicHolder = (PhotoView) findViewById(R.id.comicHolder);
        TextView comicTitle = (TextView) findViewById(R.id.comicTitle);
        TextView altText = (TextView) findViewById(R.id.altText);
        TextView metadata = (TextView) findViewById(R.id.metadata);
        if(currentComic.getImageUrl().endsWith(".gif")) {
            Glide.with(this).load(currentComic.getImageUrl()).asGif().into(comicHolder);
        }
        else {
            Glide.with(this).load(currentComic.getImageUrl()).into(comicHolder);
        }
        comicTitle.setText(currentComic.getTitle());
        altText.setText(currentComic.getAltText());
        metadata.setText("#" + currentComic.getNumber() + "\n" + currentComic.getDate());

    }
    private void getComicData() {
        getComicData(Constants.LAST);
    }
    private void getComicData(String number) {
        String url;
        final boolean shouldMaxBeSet;
        if(number.equals(Constants.LAST)) {
            url = Constants.LATEST_URL;
            shouldMaxBeSet = true;
        }
        else {
            shouldMaxBeSet = false;
            url = String.format(Constants.URL_PATTERN, number);
        }
        StringRequest strRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        try {
                            JSONObject result = new JSONObject(response);
                            MSXkcdDatabase db = new MSXkcdDatabase(xkcd.this);
                            String date;

                            currentComic.setTitle(result.getString(Constants.COMIC_TITLE));
                            currentComic.setAltText(result.getString(Constants.COMIC_EXTRA).toUpperCase());
                            currentComic.setImageUrl(result.getString(Constants.COMIC_URL));
                            currentComic.setNumber(result.getInt(Constants.COMIC_INDEX));

                            date = result.getString(Constants.COMIC_MONTH);
                            date += "-" + result.getString(Constants.COMIC_DAY);
                            date += "-" + result.getString(Constants.COMIC_YEAR);

                            currentComic.setDate(date);

                            which = result.getInt(Constants.COMIC_INDEX);
                            if(shouldMaxBeSet) {
                                max = result.getInt(Constants.COMIC_INDEX);
                                SharedPreferences preferences = getSharedPreferences("com.marshmallowsocks.xkcd", Context.MODE_PRIVATE);

                                if(preferences.getInt("max", -1) < max) {
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putInt("max", max);
                                    editor.apply();

                                    if(db.contains(currentComic.getNumber())) {
                                        if (!(db.addNewMetadata(currentComic))) {
                                            Toast.makeText(xkcd.this, "An error occurred with the database", Toast.LENGTH_SHORT);
                                        }
                                    }
                                }
                            }

                            if(!db.contains(currentComic.getNumber())) {
                                if (!(db.addNewMetadata(currentComic))) {
                                    Toast.makeText(xkcd.this, "An error occurred with the database", Toast.LENGTH_SHORT);
                                }
                            }
                            loadComic();
                        }
                        catch(JSONException e) {
                            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Toast.makeText(getApplicationContext(), error.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
        msRequestQueue.addToRequestQueue(strRequest, this);
    }
}
