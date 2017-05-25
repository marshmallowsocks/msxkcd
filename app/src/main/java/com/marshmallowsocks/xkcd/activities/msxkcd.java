package com.marshmallowsocks.xkcd.activities;

import android.Manifest;
import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.Log;
import android.util.SparseArray;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.ToxicBakery.viewpager.transforms.TabletTransformer;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.fragments.ComicFragment;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.marshmallowsocks.xkcd.util.core.MSNewComicReceiver;
import com.marshmallowsocks.xkcd.util.core.MSShakeDetector;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.core.MSXkcdViewPager;
import com.marshmallowsocks.xkcd.util.http.MSBackgroundDownloader;
import com.marshmallowsocks.xkcd.util.http.MSRequestQueue;
import com.marshmallowsocks.xkcd.util.msxkcd.XKCDComicBean;
import com.nightonke.boommenu.Animation.BoomEnum;
import com.nightonke.boommenu.BoomButtons.BoomButton;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceAlignmentEnum;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomButtons.HamButton;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomButtons.TextOutsideCircleButton;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.OnBoomListener;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import com.willowtreeapps.spruce.Spruce;
import com.willowtreeapps.spruce.animation.DefaultAnimations;
import com.willowtreeapps.spruce.sort.DefaultSort;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Random;
import java.util.Stack;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;

public class msxkcd extends AppCompatActivity {

    private static Integer which = -1;
    private static Integer max = -1;
    private static Integer downloaded = -1;
    private boolean isLastComic = false;
    private boolean isFirstComic = false;
    private XKCDComicBean currentComic;

    private Stack<Integer> randomHistory;
    private MSNewComicReceiver newComicReceiver;
    private MSRequestQueue msRequestQueue;

    private Button previousButton;
    private Button nextButton;
    private Button firstButton;
    private Button lastButton;
    private Button randomButton;
    private LinearLayout buttonBar;

    private FloatingActionButton randomFab;
    private MSXkcdViewPager mViewPager;
    private SparseArray<ComicFragment> tagMap;

    private boolean isFromSearch;

    //for shaking
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private MSShakeDetector msShakeDetector;
    //end shaking
    @SuppressWarnings("FieldCanBeLocal")
    private SharedPreferences.OnSharedPreferenceChangeListener downloadListener;

    //drag and drop fab
    float dX;
    float dY;
    int lastAction;
    //end drag and drop fab
    private static Random randomNumberGenerator;
    //for boom menu
    final String[] buttonNames = {
            "ALT",
            "FAVORITES",
            "WHAT IF?",
            "ALL",
            "TOGGLE NAVIGATION BAR",
            "OFFLINE MODE"
    };

    final int[] buttonImages = {
            android.R.drawable.ic_menu_info_details,
            R.drawable.fa_heart_on,
            R.mipmap.what_if_logo,
            android.R.drawable.ic_menu_gallery,
            R.drawable.fa_toggle_off_white,
            android.R.drawable.ic_popup_sync
    };

    final String[] buttonSubtitles = {
            "SHOW ALT TEXT",
            "CHECK YOUR FAVORITES",
            "VIEW WHAT IF",
            "SEE ALL XKCD COMICS",
            "USE RANDOM BUTTON INSTEAD OF NAVIGATION BAR",
            "DOWNLOADED -/?"
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        randomNumberGenerator = new Random();
        randomHistory = new Stack<>();
        if(!getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).contains(Constants.FIRST_TIME_FLAG)) {
            SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(Constants.FIRST_TIME_FLAG, true);
            editor.apply();
            startActivity(new Intent(this, MSXkcdIntro.class));
        }
        if(!getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).contains(Constants.OFFLINE_COUNT)) {
            SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(Constants.OFFLINE_COUNT, 0);
            editor.apply();
            downloaded = 0;
        }
        else {
            SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
            downloaded = preferences.getInt(Constants.OFFLINE_COUNT, 0);
        }
        downloadListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            @Override
            public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
                if(Constants.OFFLINE_COUNT.equals(key)) {
                    downloaded = sharedPreferences.getInt(Constants.OFFLINE_COUNT, 0);
                }
                if(Constants.SYNC_IN_PROGRESS.equals(key)) {
                    if(sharedPreferences.getBoolean(Constants.SYNC_IN_PROGRESS, false)) {
                        buttonSubtitles[5] = "SYNC_IN_PROGRESS";
                    }
                    else {
                        buttonSubtitles[5] = "DOWNLOADED -/?";
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.remove(Constants.SYNC_IN_PROGRESS);
                        editor.apply();
                    }
                }
            }
        };

        getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE).registerOnSharedPreferenceChangeListener(downloadListener);
        setContentView(R.layout.activity_msxkcd);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/xkcd.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );
        //set up shakes
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager
                .getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        msShakeDetector = new MSShakeDetector();
        msShakeDetector.setOnShakeListener(new MSShakeDetector.OnShakeListener() {
            @Override
            public void onShake(int count) {
                randomButton.callOnClick();
            }
        });
        //end shakes

        //TOOLBAR INIT
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        Spannable appName = new SpannableString(Constants.APP_NAME);
        CalligraphyTypefaceSpan typefaceSpan = new CalligraphyTypefaceSpan(TypefaceUtils.load(getAssets(), "fonts/xkcd.otf"));
        appName.setSpan(typefaceSpan, 0, Constants.APP_NAME.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        toolbar.setTitle(appName);

        randomFab = (FloatingActionButton) findViewById(R.id.randomFab);
        View.OnLongClickListener enableDrag = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                randomFab.setOnTouchListener(new View.OnTouchListener() {
                    @Override
                    public boolean onTouch(View view, MotionEvent event) {
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                dX = view.getX() - event.getRawX();
                                dY = view.getY() - event.getRawY();
                                lastAction = MotionEvent.ACTION_DOWN;
                                break;

                            case MotionEvent.ACTION_MOVE:
                                view.setY(event.getRawY() + dY);
                                view.setX(event.getRawX() + dX);
                                lastAction = MotionEvent.ACTION_MOVE;
                                break;

                            case MotionEvent.ACTION_UP:
                                randomFab.setOnTouchListener(null);
                                break;

                            default:
                                return false;
                        }
                        return true;
                    }
                });
                return false;
            }
        };
        randomFab.setOnLongClickListener(enableDrag);
        randomFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                randomButtonAction();
            }
        });

        setupBoomMenu(toolbar);
        LikeButton likeButton = (LikeButton) toolbar.findViewById(R.id.likeButton);
        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                MSXkcdDatabase db = new MSXkcdDatabase(msxkcd.this);

                if(db.contains(which)) {
                    currentComic = db.getComic(which);
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
                }
                else {
                    getComicData(which);
                    //TODO: Have to figure out how to commit this.
                }
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
        ImageButton searchButton = (ImageButton) toolbar.findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchRequested();
            }
        });
        ImageButton saveButton = (ImageButton) toolbar.findViewById(R.id.saveToGalleryButton);
        saveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int permissionCheck = ContextCompat.checkSelfPermission(msxkcd.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            msxkcd.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.WRITE_EXTERNAL_STORAGE);
                }
                else {
                    saveImageToGallery();
                }
            }
        });
        /*new Spruce
                .SpruceBuilder(likeButton)
                .sortWith(new DefaultSort(50L))
                .animateWith(new Animator[]{DefaultAnimations.growAnimator(likeButton, 800)})
                .start();*/
        setSupportActionBar(toolbar);
        //END TOOLBAR INIT

        //Comic init
        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);

        tagMap = new SparseArray<>();
        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.NEW_XKCD);
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.NEW_WHAT_IF);
        newComicReceiver = new MSNewComicReceiver(viewGroup, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView altText = (TextView) findViewById(R.id.altText);
                if(altText.getVisibility() == View.VISIBLE) {
                    ComicFragment current = tagMap.get(mViewPager.getCurrentItem());
                    current.toggleAltText(current.getView());
                }
                mViewPager.setCurrentItem(max);
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView altText = (TextView) findViewById(R.id.altText);
                if(altText.getVisibility() == View.VISIBLE) {
                    ComicFragment current = tagMap.get(mViewPager.getCurrentItem());
                    current.toggleAltText(current.getView());
                }
                startActivity(new Intent(msxkcd.this, WhatIf.class));
            }
        });
        IntentFilter newComicFilter = new IntentFilter();
        newComicFilter.addAction(Constants.NEW_COMIC_ADDED);
        newComicFilter.addAction(Constants.NEW_WHAT_IF_ADDED);
        LocalBroadcastManager.getInstance(this).registerReceiver(newComicReceiver, newComicFilter);
        isFromSearch = false;
        if(getIntent() != null) {
            if(Constants.SEARCH_TO_PAGE_ACTION.equals(getIntent().getAction())) {
                which = getIntent().getIntExtra("newPage", -1);
                isFromSearch = true;
            }
        }

        buttonBar = (LinearLayout) findViewById(R.id.buttonBar);
        previousButton = (Button) findViewById(R.id.previousButton);
        nextButton = (Button) findViewById(R.id.nextButton);
        firstButton = (Button) findViewById(R.id.firstButton);
        lastButton = (Button) findViewById(R.id.lastButton);
        randomButton = (Button) findViewById(R.id.randomButton);

        /*new Spruce
                .SpruceBuilder(buttonBar)
                .sortWith(new DefaultSort(100L))
                .animateWith(new Animator[]{DefaultAnimations.shrinkAnimator(buttonBar, 1200)})
                .start();*/
        //End comic init

        setupBottomNavigationBar();
        if(max == -1) {
            msRequestQueue = MSRequestQueue.getInstance(msxkcd.this);
            getLatestData();
        }
        // Set up the ViewPager with the sections adapter.
        else {
            setAdapter();
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ImageButton searchButton = (ImageButton) findViewById(R.id.searchButton);
        ImageButton saveButton = (ImageButton) findViewById(R.id.saveToGalleryButton);
        BoomMenuButton oldBmb = (BoomMenuButton) toolbar.findViewById(R.id.action_bar_left_bmb);
        Toolbar.LayoutParams searchParams = (Toolbar.LayoutParams) searchButton.getLayoutParams();
        Toolbar.LayoutParams saveParams = (Toolbar.LayoutParams) saveButton.getLayoutParams();
        Toolbar.LayoutParams oldBmbLayoutParams = (Toolbar.LayoutParams) oldBmb.getLayoutParams();
        toolbar.removeView(oldBmb);
        toolbar.removeView(searchButton);
        toolbar.removeView(saveButton);
        BoomMenuButton bmb = new BoomMenuButton(this);
        bmb.setLayoutParams(oldBmbLayoutParams);
        bmb.setTop(0);
        bmb.setBackgroundEffect(false);
        bmb.setBackgroundColor(Color.parseColor("#6e7b91"));
        bmb.setShadowColor(Color.parseColor("#6e7b91"));
        bmb.setFrames(120);
        bmb.setHideDelay(0);
        bmb.setHideDuration(100);
        bmb.setRotateDegree(1080);
        bmb.setShowDelay(0);
        bmb.setShowDuration(200);
        bmb.setId(R.id.action_bar_left_bmb);
        toolbar.addView(bmb);
        ImageButton newSearchButton = new ImageButton(this);
        newSearchButton.setId(R.id.searchButton);
        newSearchButton.setBackgroundResource(R.drawable.button_menu);
        newSearchButton.setImageResource(android.R.drawable.ic_menu_search);
        newSearchButton.setLayoutParams(searchParams);
        newSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchRequested();
            }
        });
        toolbar.addView(newSearchButton);
        ImageButton newSaveButton = new ImageButton(this);
        newSaveButton.setId(R.id.saveToGalleryButton);
        newSaveButton.setBackgroundResource(R.drawable.button_menu);
        newSaveButton.setImageResource(android.R.drawable.ic_menu_save);
        newSaveButton.setLayoutParams(saveParams);
        newSaveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int permissionCheck = ContextCompat.checkSelfPermission(msxkcd.this,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE);
                if(permissionCheck != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(
                            msxkcd.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, Constants.WRITE_EXTERNAL_STORAGE);
                }
                else {
                    saveImageToGallery();
                }
            }
        });
        toolbar.addView(newSaveButton);
        setupBoomMenu(toolbar);
        mViewPager.invalidate();
    }
    @Override
    protected void onResume() {
        super.onResume();
        IntentFilter newComicFilter = new IntentFilter();
        newComicFilter.addAction(Constants.NEW_COMIC_ADDED);

        LocalBroadcastManager.getInstance(this).registerReceiver(newComicReceiver, newComicFilter);
        mSensorManager.registerListener(msShakeDetector,
                mAccelerometer,
                SensorManager.SENSOR_DELAY_UI
        );

    }
    @Override
    protected void onPause() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(newComicReceiver);
        mSensorManager.unregisterListener(msShakeDetector);
        super.onPause();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case Constants.WRITE_EXTERNAL_STORAGE:
                if ((grantResults.length > 0) && (grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    saveImageToGallery();
                }
                break;
            default:
                break;
        }
    }
    private void setFavoriteCheckListener() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //don't care
            }

            @Override
            public void onPageSelected(int position) {
                which = position + 1;
                isFavorite(position + 1);
                if(which.intValue() == max.intValue()) {
                    toggleNavigationState("next");
                }
                else if(which == 1) {
                    toggleNavigationState("prev");
                }
                else {
                    toggleNavigationState("all");
                }
            }

            @Override
            public void onPageScrollStateChanged(int state) {
                //definitely don't care
            }
        });
    }
    private void setAdapter() {
        if(!isFromSearch) {
            which = max - 1;
        }
        ComicFragmentAdapter mComicFragmentAdapter = new ComicFragmentAdapter(getSupportFragmentManager());
        mViewPager = (MSXkcdViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mComicFragmentAdapter);
        mViewPager.setPageTransformer(true, new TabletTransformer());
        mViewPager.setOffscreenPageLimit(3);

        if(!isFromSearch) {
            mViewPager.setCurrentItem(max - 1);
            isFavorite(max);
        }
        else {
            mViewPager.setCurrentItem(which - 1);
            isFavorite(which);
            toggleButtonBar(true);
            isFromSearch = false;
        }

        mComicFragmentAdapter.notifyDataSetChanged();
        setFavoriteCheckListener();
    }
    public void toggleAltText() {
        View rootView = tagMap.get(mViewPager.getCurrentItem()).getView();
        if(rootView == null) {
            return;
        }
        ConstraintLayout componentLayout = (ConstraintLayout) rootView.findViewById(R.id.componentHolder);
        TextView altText = (TextView) rootView.findViewById(R.id.altText);
        TextView metadata = (TextView) rootView.findViewById(R.id.metadata);
        PhotoView comicHolder = (PhotoView) rootView.findViewById(R.id.comicHolder);
        Button closeButton = (Button) rootView.findViewById(R.id.closeOverlay);
        Button explainButton = (Button) rootView.findViewById(R.id.explainButton);

        if(altText.getVisibility() == View.GONE) {
            componentLayout.setBackgroundResource(R.color.cardview_dark_background);
            comicHolder.setAlpha(0.3f);
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
    private void getLatestData() {
        StringRequest strRequest = new StringRequest(Request.Method.GET, Constants.LATEST_URL,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        MSXkcdDatabase db = new MSXkcdDatabase(msxkcd.this);
                        try {
                            JSONObject result = new JSONObject(response);
                            String date;
                            currentComic = new XKCDComicBean();
                            currentComic.setTitle(result.getString(Constants.COMIC_TITLE));
                            currentComic.setAltText(result.getString(Constants.COMIC_EXTRA).toUpperCase());
                            currentComic.setImageUrl(result.getString(Constants.COMIC_URL));
                            currentComic.setNumber(result.getInt(Constants.COMIC_INDEX));
                            currentComic.setJsonRepresentation(result);

                            date = result.getString(Constants.COMIC_MONTH);
                            date += "-" + result.getString(Constants.COMIC_DAY);
                            date += "-" + result.getString(Constants.COMIC_YEAR);

                            currentComic.setDate(date);

                            max = result.getInt(Constants.COMIC_INDEX);
                            setupBoomMenu(null);
                            SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);

                            if(preferences.getInt(Constants.MAX, -1) < max) {
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putInt(Constants.MAX, max);
                                editor.apply();

                                if(db.contains(currentComic.getNumber())) {
                                    if (!(db.addNewMetadata(currentComic))) {
                                        Toast.makeText(msxkcd.this, "An error occurred with the database", Toast.LENGTH_SHORT);
                                    }
                                }
                            }

                            if(!db.contains(currentComic.getNumber())) {
                                if (!(db.addNewMetadata(currentComic))) {
                                    Toast.makeText(msxkcd.this, "An error occurred with the database", Toast.LENGTH_SHORT);
                                }
                            }
                        }
                        catch(JSONException e) {
                            Toast.makeText(msxkcd.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        finally {
                            db.close();
                        }

                        setAdapter();

                        int previous = currentComic.getNumber() - 1;
                        while(!db.contains(previous)) {
                            Log.d("PREVIOUS LOOP", previous + "");
                            getComicData(previous);
                            previous--;
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Toast.makeText(msxkcd.this, error.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
        msRequestQueue.addToRequestQueue(strRequest, msxkcd.this);
    }
    private void getComicData(Integer comicIndex) {
        StringRequest strRequest = new StringRequest(Request.Method.GET, String.format(Constants.URL_PATTERN, comicIndex),
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        MSXkcdDatabase db = new MSXkcdDatabase(msxkcd.this);
                        try {
                            JSONObject result = new JSONObject(response);
                            String date;
                            currentComic = new XKCDComicBean();
                            currentComic.setTitle(result.getString(Constants.COMIC_TITLE));
                            currentComic.setAltText(result.getString(Constants.COMIC_EXTRA).toUpperCase());
                            currentComic.setImageUrl(result.getString(Constants.COMIC_URL));
                            currentComic.setNumber(result.getInt(Constants.COMIC_INDEX));
                            currentComic.setJsonRepresentation(result);

                            date = result.getString(Constants.COMIC_MONTH);
                            date += "-" + result.getString(Constants.COMIC_DAY);
                            date += "-" + result.getString(Constants.COMIC_YEAR);

                            currentComic.setDate(date);

                            if(!db.contains(currentComic.getNumber())) {
                                if (!(db.addNewMetadata(currentComic))) {
                                    Toast.makeText(msxkcd.this, "An error occurred with the database", Toast.LENGTH_SHORT);
                                }
                            }
                        }
                        catch(JSONException e) {
                            Toast.makeText(msxkcd.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        finally {
                            db.close();
                        }
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Toast.makeText(msxkcd.this, error.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
        msRequestQueue.addToRequestQueue(strRequest, msxkcd.this);
    }
    private void toggleNavigationState(String type) {
        float prevAlpha;
        float nextAlpha;
        boolean nextEnabled;
        boolean prevEnabled;
        switch (type) {
            case "next":
                nextAlpha = 0.5f;
                prevAlpha = 1f;
                prevEnabled = true;
                nextEnabled = false;
                break;
            case "prev":
                prevAlpha = 0.5f;
                nextAlpha = 1f;
                prevEnabled = false;
                nextEnabled = true;
                break;
            default:
                prevAlpha = 1f;
                nextAlpha = 1f;
                prevEnabled = true;
                nextEnabled = true;
                break;
        }
        firstButton.setAlpha(prevAlpha);
        previousButton.setAlpha(prevAlpha);
        nextButton.setAlpha(nextAlpha);
        lastButton.setAlpha(nextAlpha);
        firstButton.setEnabled(prevEnabled);
        previousButton.setEnabled(prevEnabled);
        nextButton.setEnabled(nextEnabled);
        lastButton.setEnabled(nextEnabled);
    }
    private void setupBoomMenu(Toolbar toolbar) {
        if (toolbar == null) {
            toolbar = (Toolbar) findViewById(R.id.toolbar);
        }
        final BoomMenuButton bmb = (BoomMenuButton) toolbar.findViewById(R.id.action_bar_left_bmb);
        final FloatingActionButton randomFab = (FloatingActionButton) findViewById(R.id.randomFab);

        if(!"SYNC IN PROGRESS".equals(buttonSubtitles[5])) {
            buttonSubtitles[5] = "DOWNLOADED -/?";
            buttonSubtitles[5] = buttonSubtitles[5].replace("-", downloaded.toString());
            buttonSubtitles[5] = buttonSubtitles[5].replace("?", max.toString());
        }

        //lock orientation on boom
        bmb.setOnBoomListener(new OnBoomListener() {
            @Override
            public void onClicked(int index, BoomButton boomButton) {

            }

            @Override
            public void onBackgroundClick() {

            }

            @Override
            public void onBoomWillHide() {

            }

            @Override
            public void onBoomDidHide() {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
            }

            @Override
            public void onBoomWillShow() {

            }

            @Override
            public void onBoomDidShow() {
                switch (getWindowManager().getDefaultDisplay().getRotation()) {
                    case Surface.ROTATION_0:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                        break;
                    case Surface.ROTATION_90:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                        break;
                    case Surface.ROTATION_180:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
                        break;
                    case Surface.ROTATION_270:
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                        break;
                }
            }
        });
        OnBMClickListener listener = new OnBMClickListener() {
            @Override
            public void onBoomButtonClick(int index) {
                switch(index) {
                    case 0:
                        toggleAltText();
                        break;
                    case 1:
                        Intent favoriteIntent = new Intent(msxkcd.this, ComicSearchResults.class);
                        favoriteIntent.setAction(Constants.FAVORITE_KEY);
                        startActivity(favoriteIntent);
                        break;
                    case 2:
                        Intent whatIfIntent = new Intent(msxkcd.this, WhatIf.class);
                        startActivity(whatIfIntent);
                        break;
                    case 3:
                        Intent allComics = new Intent(msxkcd.this, ComicSearchResults.class);
                        allComics.setAction(Constants.ALL_COMICS);
                        startActivity(allComics);
                        break;
                    case 4:
                        if(buttonBar.getVisibility() == View.VISIBLE) {
                            new Spruce
                                    .SpruceBuilder(buttonBar)
                                    .sortWith(new DefaultSort(50L))
                                    .animateWith(new Animator[]{DefaultAnimations.fadeAwayAnimator(buttonBar, 400)})
                                    .start()
                                    .addListener(new Animator.AnimatorListener() {
                                        @Override
                                        public void onAnimationStart(Animator animation) {

                                        }

                                        @Override
                                        public void onAnimationEnd(Animator animation) {
                                            buttonBar.setVisibility(View.GONE);
                                            buttonSubtitles[4] = "USE NAVIGATION BAR INSTEAD OF RANDOM BUTTON";
                                            buttonImages[4] = R.drawable.fa_toggle_on_white;
                                            switch(getResources().getConfiguration().orientation) {
                                                case 1:
                                                    ((HamButton.Builder)bmb.getBuilder(4)).subNormalText(buttonSubtitles[4]);
                                                    ((HamButton.Builder)bmb.getBuilder(4)).normalImageRes(buttonImages[4]);
                                                    break;
                                                case 2:
                                                    ((TextOutsideCircleButton.Builder)bmb.getBuilder(4)).normalImageRes(buttonImages[4]);
                                                    break;
                                            }
                                            randomFab.show();
                                        }

                                        @Override
                                        public void onAnimationCancel(Animator animation) {

                                        }

                                        @Override
                                        public void onAnimationRepeat(Animator animation) {

                                        }
                                    });
                        }
                        else {
                            buttonBar.setVisibility(View.VISIBLE);
                            buttonSubtitles[4] = "USE RANDOM BUTTON INSTEAD OF NAVIGATION BAR";
                            buttonImages[4] = R.drawable.fa_toggle_off_white;
                            switch(getResources().getConfiguration().orientation) {
                                case 1:
                                    ((HamButton.Builder)bmb.getBuilder(4)).subNormalText(buttonSubtitles[4]);
                                    ((HamButton.Builder)bmb.getBuilder(4)).normalImageRes(buttonImages[4]);
                                    break;
                                case 2:
                                    ((TextOutsideCircleButton.Builder)bmb.getBuilder(4)).normalImageRes(buttonImages[4]);
                                    break;
                            }
                            new Spruce
                                    .SpruceBuilder(buttonBar)
                                    .sortWith(new DefaultSort(50L))
                                    .animateWith(DefaultAnimations.fadeInAnimator(buttonBar, 400)).start();
                            randomFab.hide();
                        }
                        break;
                    case 5:
                        startService(new Intent(msxkcd.this, MSBackgroundDownloader.class));
                        break;
                }
            }
        };

        switch(getResources().getConfiguration().orientation) {
            case 1:
                bmb.clearBuilders();
                bmb.setButtonEnum(ButtonEnum.Ham);
                bmb.setBoomEnum(BoomEnum.HORIZONTAL_THROW_1);
                bmb.setPiecePlaceEnum(PiecePlaceEnum.HAM_6);
                bmb.setButtonPlaceEnum(ButtonPlaceEnum.HAM_6);
                for (int i = 0; i < bmb.getButtonPlaceEnum().buttonNumber(); i++) {
                    bmb.addBuilder(new HamButton.Builder()
                            //button attributes
                            .pieceColorRes(R.color.colorPrimaryLight)
                            .normalColorRes(R.color.colorPrimary)
                            .highlightedColorRes(android.R.color.white)
                            //text attributes
                            .normalText(buttonNames[i])
                            .typeface(TypefaceUtils.load(getAssets(), "fonts/xkcd.otf"))
                            .highlightedColorRes(R.color.colorPrimaryDark)
                            .subNormalText(buttonSubtitles[i])
                            .subTypeface(TypefaceUtils.load(getAssets(), "fonts/xkcd.otf"))
                            .normalImageRes(buttonImages[i])
                            //set up actions
                            .listener(listener)
                    );
                }
                bmb.setButtonPlaceAlignmentEnum(ButtonPlaceAlignmentEnum.TR);
                break;
            case 2:
                bmb.clearBuilders();
                bmb.setButtonEnum(ButtonEnum.TextOutsideCircle);
                bmb.setBoomEnum(BoomEnum.HORIZONTAL_THROW_1);
                bmb.setPiecePlaceEnum(PiecePlaceEnum.DOT_6_1);
                bmb.setButtonPlaceEnum(ButtonPlaceEnum.Horizontal);
                for (int i = 0; i < 6; i++) {
                    bmb.addBuilder(new TextOutsideCircleButton.Builder()
                            //button attributes
                            .pieceColorRes(R.color.colorPrimaryLight)
                            .normalColorRes(R.color.colorPrimary)
                            .highlightedColorRes(android.R.color.white)
                            //text attributes
                            .normalText(buttonNames[i])
                            .typeface(TypefaceUtils.load(getAssets(), "fonts/xkcd.otf"))
                            .highlightedColorRes(R.color.colorPrimaryDark)
                            .normalImageRes(buttonImages[i])
                            //set up actions
                            .listener(listener)
                    );
                }
                bmb.setButtonPlaceAlignmentEnum(ButtonPlaceAlignmentEnum.Top);
                break;
        }
    }
    private void setupBottomNavigationBar() {
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

                mViewPager.setCurrentItem(which - 1);
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

                mViewPager.setCurrentItem(which - 1);
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
                mViewPager.setCurrentItem(which - 1);
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
                mViewPager.setCurrentItem(max - 1);
            }
        });

        randomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                randomButtonAction();
            }
        });

        nextButton.setEnabled(false);
        nextButton.setAlpha(0.5f);
        lastButton.setEnabled(false);
        lastButton.setAlpha(0.5f);
    }
    private void randomButtonAction() {
        Integer temp;
        temp = randomNumberGenerator.nextInt(max) + 1;
        while(temp.intValue() == which.intValue()) {
            temp = randomNumberGenerator.nextInt(max) + 1;
        }
        randomHistory.push(which);
        which = temp;
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
        View.OnLongClickListener goToLastRandomComic = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(!randomHistory.isEmpty()) {
                    mViewPager.setPagingEnabled(true);
                    mViewPager.setCurrentItem(randomHistory.pop() - 1);
                }
                return true;
            }
        };
        randomButton.setOnLongClickListener(goToLastRandomComic);
        mViewPager.setPagingEnabled(true);
        mViewPager.setCurrentItem(which);
    }
    public void toggleViewPager(boolean toggle) {
        mViewPager.setPagingEnabled(toggle);
    }
    public void isFavorite(Integer which) {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        LikeButton likeButton= (LikeButton) toolbar.findViewById(R.id.likeButton);
        likeButton.setLiked(preferences.contains(
                String.format(Constants.FAVORITE_KEY, which.toString())
        ));
    }
    public void toggleButtonBar(boolean toggle) {
        LinearLayout buttonBar = (LinearLayout) findViewById(R.id.buttonBar);
        RelativeLayout mainContainer = (RelativeLayout) findViewById(R.id.mainContainer);

        if(toggle) {
            mainContainer.setBackgroundResource(R.color.colorPrimaryLight);
            buttonBar.setAlpha(1.0f);
            mViewPager.setPagingEnabled(true);
        }
        else {
            buttonBar.setAlpha(0.3f);
            mainContainer.setBackgroundResource(R.color.cardview_dark_background);
            mViewPager.setPagingEnabled(false);
        }
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
    private void saveImageToGallery() {
        MSXkcdDatabase db = new MSXkcdDatabase(msxkcd.this);
        final XKCDComicBean comic = db.getComic(which);
        Target target = new Target() {
            @Override
            public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
                MediaStore.Images.Media.insertImage(getContentResolver(), bitmap, comic.getTitle(), null);
                Snackbar.make(findViewById(R.id.mainContainer), "Comic saved to gallery".toUpperCase(), Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onBitmapFailed(Drawable errorDrawable) {
                Snackbar.make(findViewById(R.id.mainContainer), "Could not save comic to gallery".toUpperCase(), Snackbar.LENGTH_SHORT).show();
            }

            @Override
            public void onPrepareLoad(Drawable placeHolderDrawable) {

            }
        };
        Picasso.with(msxkcd.this).load(comic.getImageUrl()).into(target);
        db.close();
    }
    private class ComicFragmentAdapter extends FragmentStatePagerAdapter {

        ComicFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            ComicFragment comicFragment = new ComicFragment();

            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            tagMap.put(position, comicFragment);
            comicFragment.setComicContext(position + 1);
            return comicFragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return max;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return Constants.APP_NAME;
        }
    }
}