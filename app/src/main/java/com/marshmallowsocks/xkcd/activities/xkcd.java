package com.marshmallowsocks.xkcd.activities;

import android.animation.Animator;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.github.badoualy.morphytoolbar.MorphyToolbar;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.core.Constants;
import com.marshmallowsocks.xkcd.util.core.MSNewComicReceiver;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.http.MSRequestQueue;
import com.marshmallowsocks.xkcd.util.xkcd.XKCDComicBean;
import com.willowtreeapps.spruce.Spruce;
import com.willowtreeapps.spruce.animation.DefaultAnimations;
import com.willowtreeapps.spruce.sort.DefaultSort;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

public class xkcd extends AppCompatActivity {

    private MSRequestQueue msRequestQueue;
    private static XKCDComicBean currentComic;
    private static Integer which = -1;
    private static Integer max = -1;
    private boolean isLastComic = false;
    private boolean isFirstComic = false;
    private boolean isOverlayActive = false;
    private ColorStateList oldStates;
    private int[][] states;
    private int[] colors;

    private MSNewComicReceiver newComicReceiver;
    private MorphyToolbar specialToolbar;
    private LinearLayout extraButtonsLayout;

    private Button previousButton;
    private Button nextButton;
    private Button firstButton;
    private Button lastButton;
    private Button randomButton;

    private PhotoView comicHolder;
    private ConstraintLayout componentLayout;
    private LinearLayout buttonBar;
    private TextView altText;
    private TextView metadata;
    private Button closeButton;
    private Button explainButton;
    private LikeButton likeButton;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_xkcd);
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);


        extraButtonsLayout =  (LinearLayout) getLayoutInflater().inflate(R.layout.xkcd_expanded_toolbar, null);
        Spannable appName = new SpannableString(Constants.APP_NAME);
        CalligraphyTypefaceSpan typefaceSpan = new CalligraphyTypefaceSpan(TypefaceUtils.load(getAssets(), "fonts/xkcd.otf"));
        appName.setSpan(typefaceSpan, 0, Constants.APP_NAME.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        specialToolbar = MorphyToolbar.builder(this, toolbar)
                .withToolbarAsSupportActionBar()
                .withTitle(appName)
                .withPicture(R.mipmap.xkcd_icon_circle)
                .withHidePictureWhenCollapsed(false)
                .withContentExpandedMarginStart(5)
                .build();

        final ImageButton toggleButton = (ImageButton) specialToolbar.findViewById(R.id.toolbarToggle);

        toggleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleToolbar(specialToolbar.isCollapsed());
            }
        });

        setSupportActionBar(toolbar);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/xkcd.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);

        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.NEW_XKCD);
        newComicReceiver = new MSNewComicReceiver(viewGroup, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getComicData();
            }
        });
        IntentFilter newComicFilter = new IntentFilter();
        newComicFilter.addAction(Constants.NEW_COMIC_ADDED);

        LocalBroadcastManager.getInstance(this).registerReceiver(newComicReceiver, newComicFilter);
        if(getIntent() != null) {
            if(Constants.SEARCH_TO_PAGE_ACTION.equals(getIntent().getAction())) {
                which = getIntent().getIntExtra("newPage", -1);
            }
        }

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

        previousButton = (Button) findViewById(R.id.previousButton);
        nextButton = (Button) findViewById(R.id.nextButton);
        firstButton = (Button) findViewById(R.id.firstButton);
        lastButton = (Button) findViewById(R.id.lastButton);
        randomButton = (Button) findViewById(R.id.randomButton);

        comicHolder = (PhotoView) findViewById(R.id.comicHolder);
        componentLayout = (ConstraintLayout) findViewById(R.id.componentHolder);
        buttonBar = (LinearLayout) findViewById(R.id.buttonBar);
        altText = (TextView) findViewById(R.id.altText);
        metadata = (TextView) findViewById(R.id.metadata);
        closeButton = (Button) findViewById(R.id.closeOverlay);
        explainButton = (Button) findViewById(R.id.explainButton);
        likeButton = (LikeButton) findViewById(R.id.likeButton);

        new Spruce
                .SpruceBuilder(buttonBar)
                .sortWith(new DefaultSort(100L))
                .animateWith(new Animator[]{DefaultAnimations.shrinkAnimator(buttonBar, 1200)})
                .start();

        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.putString(String.format(Constants.FAVORITE_KEY, which.toString()), currentComic.jsonify());
                editor.apply();
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove(String.format(Constants.FAVORITE_KEY, which.toString()));
                editor.apply();
            }
        });

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    componentLayout.setBackgroundTintList(oldStates);
                    altText.setElevation(0.0f);
                }
                comicHolder.setAlpha(1.0f);
                buttonBar.setAlpha(1.0f);
                toggleButtonBar(true);
                altText.setVisibility(View.GONE);
                closeButton.setVisibility(View.GONE);
                explainButton.setVisibility(View.GONE);
                metadata.setVisibility(View.GONE);
                isOverlayActive = false;
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
                if (!(max.intValue() == which.intValue())) {
                    which++;
                }

                if (max.intValue() == which.intValue()) {
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
                } else if (max.intValue() == which.intValue()) {
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

        extraButtonsLayout.findViewById(R.id.altButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleToolbar(false);
                isOverlayActive = true;
                if(altText.getVisibility() == View.GONE) {
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        oldStates = componentLayout.getBackgroundTintList();
                        componentLayout.setBackgroundTintList(new ColorStateList(states, colors));
                    }
                    comicHolder.setAlpha(0.3f);
                    buttonBar.setAlpha(0.3f);
                    toggleButtonBar(false);
                    altText.setVisibility(View.VISIBLE);
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        altText.setElevation(15.0f);
                    }
                    closeButton.setVisibility(View.VISIBLE);
                    explainButton.setVisibility(View.VISIBLE);
                    metadata.setVisibility(View.VISIBLE);
                }
            }
        });

        extraButtonsLayout.findViewById(R.id.allButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleToolbar(false);
                Intent allComics = new Intent(xkcd.this, ComicSearchResults.class);
                allComics.setAction(Constants.ALL_COMICS);
                startActivity(allComics);
            }
        });

        extraButtonsLayout.findViewById(R.id.whatIfButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleToolbar(false);
                Intent whatIfIntent = new Intent(xkcd.this, WhatIf.class);
                startActivity(whatIfIntent);
            }
        });

        extraButtonsLayout.findViewById(R.id.searchButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleToolbar(false);
                onSearchRequested();
            }
        });

        extraButtonsLayout.findViewById(R.id.favoritesButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleToolbar(false);
                Intent favoriteIntent = new Intent(xkcd.this, ComicSearchResults.class);
                favoriteIntent.setAction(Constants.FAVORITE_KEY);
                startActivity(favoriteIntent);
            }
        });

        if (which != -1) {
            if(which.intValue() == max.intValue()) {
                nextButton.setEnabled(false);
                lastButton.setEnabled(false);
                nextButton.setAlpha(0.5f);
                lastButton.setAlpha(0.5f);
            }
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
        if(max.intValue() == which.intValue()) {
            for (int i = 0; i < buttonBar.getChildCount() - 2; i++) {
                buttonBar.getChildAt(i).setEnabled(toggle);
            }
        }

    }
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter newComicFilter = new IntentFilter();
        newComicFilter.addAction(Constants.NEW_COMIC_ADDED);

        LocalBroadcastManager.getInstance(this).registerReceiver(newComicReceiver, newComicFilter);
    }
    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(newComicReceiver);
    }
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_xkcd, menu);
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
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    oldStates = componentLayout.getBackgroundTintList();
                    componentLayout.setBackgroundTintList(new ColorStateList(states, colors));
                }
                comicHolder.setAlpha(0.3f);
                buttonBar.setAlpha(0.3f);
                toggleButtonBar(false);
                altText.setVisibility(View.VISIBLE);
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    altText.setElevation(15.0f);
                }
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

        if(id == R.id.action_favorites) {
            Intent favoriteIntent = new Intent(this, ComicSearchResults.class);
            favoriteIntent.setAction(Constants.FAVORITE_KEY);
            startActivity(favoriteIntent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void toggleToolbar(boolean toggle) {
        ImageButton toolbarToggle = (ImageButton)specialToolbar.findViewById(R.id.toolbarToggle);
        if(toggle) {
            if(isOverlayActive) {
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    componentLayout.setBackgroundTintList(oldStates);
                    altText.setElevation(0.0f);
                }
                comicHolder.setAlpha(1.0f);
                buttonBar.setAlpha(1.0f);
                toggleButtonBar(true);
                altText.setVisibility(View.GONE);
                closeButton.setVisibility(View.GONE);
                explainButton.setVisibility(View.GONE);
                metadata.setVisibility(View.GONE);
                isOverlayActive = false;
            }
            toolbarToggle.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            specialToolbar.expand();
            likeButton.setVisibility(View.VISIBLE);
            specialToolbar.addView(extraButtonsLayout);
        }
        else {
            toolbarToggle.setImageResource(android.R.drawable.ic_menu_add);
            specialToolbar.removeView(extraButtonsLayout);
            specialToolbar.collapse();
            likeButton.setVisibility(View.GONE);
        }
    }

    private void loadComic() {
        PhotoView comicHolder = (PhotoView) findViewById(R.id.comicHolder);
        TextView comicTitle = (TextView) findViewById(R.id.comicTitle);
        TextView altText = (TextView) findViewById(R.id.altText);
        TextView metadata = (TextView) findViewById(R.id.metadata);
        LikeButton likeButton = (LikeButton) findViewById(R.id.likeButton);

        likeButton.setLiked(isFavorite(which));

        if(currentComic.getImageUrl().endsWith(".gif")) {
            Glide.with(this).load(currentComic.getImageUrl()).asGif().into(comicHolder);
        }
        else {
            Glide.with(this).load(currentComic.getImageUrl()).thumbnail(Glide.with(this).load(Constants.LOADING_URL)).crossFade().into(comicHolder);
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
        final MSXkcdDatabase db = new MSXkcdDatabase(xkcd.this);
        if(!number.equals(Constants.LAST)) {
            if(db.contains(Integer.parseInt(number))) {
                currentComic = db.getComic(Integer.parseInt(number));
                JSONObject representation = new JSONObject();
                try {
                    representation.put(Constants.COMIC_TITLE, currentComic.getTitle());
                    representation.put(Constants.COMIC_EXTRA, currentComic.getAltText());
                    representation.put(Constants.COMIC_INDEX, currentComic.getNumber());
                    representation.put(Constants.COMIC_URL, currentComic.getImageUrl());
                    Integer day, month, year;
                    String date = currentComic.getDate();
                    day = Integer.parseInt(date.split("-")[1]);
                    month = Integer.parseInt(date.split("-")[0]);
                    year = Integer.parseInt(date.split("-")[2]);

                    representation.put(Constants.COMIC_DAY, day);
                    representation.put(Constants.COMIC_MONTH, month);
                    representation.put(Constants.COMIC_YEAR, year);
                }
                catch (JSONException e) {
                    e.printStackTrace();
                }
                currentComic.setJsonRepresentation(representation);
                loadComic();
                return;
            }
        }
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
                            String date;

                            currentComic.setTitle(result.getString(Constants.COMIC_TITLE));
                            currentComic.setAltText(result.getString(Constants.COMIC_EXTRA).toUpperCase());
                            currentComic.setImageUrl(result.getString(Constants.COMIC_URL));
                            currentComic.setNumber(result.getInt(Constants.COMIC_INDEX));
                            currentComic.setJsonRepresentation(result);

                            date = result.getString(Constants.COMIC_MONTH);
                            date += "-" + result.getString(Constants.COMIC_DAY);
                            date += "-" + result.getString(Constants.COMIC_YEAR);

                            currentComic.setDate(date);

                            which = result.getInt(Constants.COMIC_INDEX);
                            if(shouldMaxBeSet) {
                                max = result.getInt(Constants.COMIC_INDEX);
                                SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);

                                if(preferences.getInt(Constants.MAX, -1) < max) {
                                    SharedPreferences.Editor editor = preferences.edit();
                                    editor.putInt(Constants.MAX, max);
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

    private boolean isFavorite(Integer which) {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        return preferences.contains(String.format(Constants.FAVORITE_KEY, which.toString()));
    }
}
