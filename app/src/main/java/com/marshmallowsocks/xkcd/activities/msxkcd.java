package com.marshmallowsocks.xkcd.activities;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.Build;
import android.support.constraint.ConstraintLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import android.view.animation.AlphaAnimation;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.github.chrisbanes.photoview.PhotoView;
import com.google.firebase.FirebaseApp;
import com.google.firebase.messaging.FirebaseMessaging;

import com.like.LikeButton;
import com.like.OnLikeListener;
import com.marshmallowsocks.xkcd.fragments.ComicFragment;
import com.marshmallowsocks.xkcd.fragments.ComicScrollCarouselLayout;
import com.marshmallowsocks.xkcd.util.core.MSShakeDetector;
import com.marshmallowsocks.xkcd.util.core.MSXkcdViewPager;
import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.marshmallowsocks.xkcd.util.core.MSNewComicReceiver;

import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.http.MSRequestQueue;
import com.marshmallowsocks.xkcd.util.msxkcd.XKCDComicBean;

import com.nightonke.boommenu.Animation.BoomEnum;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceAlignmentEnum;
import com.nightonke.boommenu.BoomButtons.ButtonPlaceEnum;
import com.nightonke.boommenu.BoomButtons.HamButton;
import com.nightonke.boommenu.BoomButtons.OnBMClickListener;
import com.nightonke.boommenu.BoomMenuButton;
import com.nightonke.boommenu.ButtonEnum;
import com.nightonke.boommenu.Piece.PiecePlaceEnum;

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

public class msxkcd extends AppCompatActivity {

    private static Integer which = -1;
    private static Integer max = -1;
    private boolean isLastComic = false;
    private boolean isFirstComic = false;
    private XKCDComicBean currentComic;

    private MSNewComicReceiver newComicReceiver;
    private MSRequestQueue msRequestQueue;

    private Button previousButton;
    private Button nextButton;
    private Button firstButton;
    private Button lastButton;
    private Button randomButton;
    private LinearLayout buttonBar;

    private MSXkcdViewPager mViewPager;
    private SparseArray<ComicFragment> tagMap;

    private boolean isFromSearch;

    public final static float BIG_SCALE = 1.0f;
    public final static float SMALL_SCALE = 0.7f;
    public final static float DIFF_SCALE = BIG_SCALE - SMALL_SCALE;

    //for shaking
    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private MSShakeDetector msShakeDetector;
    //end shaking

    //drag and drop fab
    float dX;
    float dY;
    int lastAction;
    //end drag and drop fab

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        final FloatingActionButton randomFab = (FloatingActionButton) findViewById(R.id.randomFab);
        randomFab.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                randomButtonAction();
            }
        });
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
                        if (lastAction == MotionEvent.ACTION_DOWN){
                            randomFab.performClick();
                        }
                        break;

                    default:
                        return false;
                }
                return true;
            }
        });
        String[] buttonNames = {
                "ALT",
                "FAVORITES",
                "WHAT IF?",
                "ALL",
                "TOGGLE NAVIGATION BAR",
                //"SETTINGS"
        };

        int[] buttonImages = {
             android.R.drawable.ic_menu_info_details,
             R.drawable.fa_heart_on,
             android.R.drawable.ic_menu_help,
             android.R.drawable.ic_menu_gallery,
             android.R.drawable.ic_input_add,
             //android.R.drawable.ic_menu_preferences
        };
        BoomMenuButton bmb = (BoomMenuButton) toolbar.findViewById(R.id.action_bar_left_bmb);
        bmb.setButtonEnum(ButtonEnum.Ham);
        bmb.setBoomEnum(BoomEnum.HORIZONTAL_THROW_1);
        bmb.setPiecePlaceEnum(PiecePlaceEnum.HAM_5);
        bmb.setButtonPlaceEnum(ButtonPlaceEnum.HAM_5);
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
                    .normalImageRes(buttonImages[i])
                    //set up actions
                    .listener(new OnBMClickListener() {
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
                                                        AlphaAnimation fabFadeIn = new AlphaAnimation(0, 1);
                                                        fabFadeIn.setDuration(400);
                                                        fabFadeIn.setFillAfter(true);
                                                        randomFab.setAnimation(fabFadeIn);
                                                        randomFab.setVisibility(View.VISIBLE);
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
                                        new Spruce
                                            .SpruceBuilder(buttonBar)
                                            .sortWith(new DefaultSort(50L))
                                            .animateWith(DefaultAnimations.fadeInAnimator(buttonBar, 400)).start();
                                        AlphaAnimation fabFadeOut = new AlphaAnimation(1, 0);
                                        fabFadeOut.setDuration(400);
                                        fabFadeOut.setFillAfter(true);
                                        randomFab.setAnimation(fabFadeOut);
                                        randomFab.setVisibility(View.GONE);
                                    }
                                    break;
                            }
                        }
                    })
            );
        }

        bmb.setButtonPlaceAlignmentEnum(ButtonPlaceAlignmentEnum.TR);
        LikeButton likeButton = (LikeButton) toolbar.findViewById(R.id.likeButton);
        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                MSXkcdDatabase db = new MSXkcdDatabase(msxkcd.this);

                //noinspection StatementWithEmptyBody
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
                    //TODO: network request
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
        setSupportActionBar(toolbar);
        //END TOOLBAR INIT

        //Comic init
        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);

        tagMap = new SparseArray<>();
        FirebaseApp.initializeApp(this);
        FirebaseMessaging.getInstance().subscribeToTopic(Constants.NEW_XKCD);
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
        });
        IntentFilter newComicFilter = new IntentFilter();
        newComicFilter.addAction(Constants.NEW_COMIC_ADDED);

        LocalBroadcastManager.getInstance(this).registerReceiver(newComicReceiver, newComicFilter);
        isFromSearch = false;
        if(getIntent() != null) {
            if(Constants.SEARCH_TO_PAGE_ACTION.equals(getIntent().getAction())) {
                which = getIntent().getIntExtra("newPage", -1);
                isFromSearch = true;
            }
        }

        new Spruce
                .SpruceBuilder(viewGroup)
                .sortWith(new DefaultSort(50L))
                .animateWith(new Animator[]{DefaultAnimations.growAnimator(viewGroup, 800)})
                .start();

        buttonBar = (LinearLayout) findViewById(R.id.buttonBar);
        previousButton = (Button) findViewById(R.id.previousButton);
        nextButton = (Button) findViewById(R.id.nextButton);
        firstButton = (Button) findViewById(R.id.firstButton);
        lastButton = (Button) findViewById(R.id.lastButton);
        randomButton = (Button) findViewById(R.id.randomButton);

        new Spruce
                .SpruceBuilder(buttonBar)
                .sortWith(new DefaultSort(100L))
                .animateWith(new Animator[]{DefaultAnimations.shrinkAnimator(buttonBar, 1200)})
                .start();
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

    private void setFavoriteCheckListener() {
        mViewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                try {
                    if (positionOffset >= 0f && positionOffset <= 1f) {
                        ComicScrollCarouselLayout cur = (ComicScrollCarouselLayout) tagMap.get(position).getView();
                        ComicScrollCarouselLayout next = (ComicScrollCarouselLayout) tagMap.get(position + 1).getView();
                        if (cur != null && next != null) {
                            cur.setScaleBoth(BIG_SCALE - DIFF_SCALE * positionOffset);
                            next.setScaleBoth(SMALL_SCALE + DIFF_SCALE * positionOffset);
                        }
                    }
                }
                catch(Exception e) {
                    //dont care;
                }
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
        mViewPager.setOffscreenPageLimit(3);

        if(!isFromSearch) {
            mViewPager.setCurrentItem(max - 1);
            isFavorite(max);
        }
        else {
            mViewPager.setCurrentItem(which - 1);
            isFavorite(which);
            isFromSearch = false;
        }

        mComicFragmentAdapter.notifyDataSetChanged();
        setFavoriteCheckListener();
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

                        setAdapter();
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

        mViewPager.setCurrentItem(which, false);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_msxkcd, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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

    private class ComicFragmentAdapter extends FragmentStatePagerAdapter
                    implements ViewPager.PageTransformer {
        final static float BIG_SCALE = 1.0f;
        final static float SMALL_SCALE = 0.7f;
        final static float DIFF_SCALE = BIG_SCALE - SMALL_SCALE;

        ComicFragmentAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            ComicFragment comicFragment = new ComicFragment();

            Bundle bundle = new Bundle();
            bundle.putInt("position", position);
            float scale;
            if (position == max - 1) {
                scale = BIG_SCALE;
            }
            else {
                scale = SMALL_SCALE;
            }
            bundle.putFloat("scale", scale);
            comicFragment.setArguments(bundle);
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

        @Override
        public void transformPage(View page, float position) {
            ComicScrollCarouselLayout layout = (ComicScrollCarouselLayout) page.findViewById(R.id.componentHolder);
            float scale = BIG_SCALE;
            if (position > 0) {
                scale = scale - position * DIFF_SCALE;
            } else {
                scale = scale + position * DIFF_SCALE;
            }
            if (scale < 0) scale = 0;
            layout.setScaleBoth(scale);
        }
    }
}