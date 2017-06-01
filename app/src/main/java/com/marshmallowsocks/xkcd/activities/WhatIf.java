package com.marshmallowsocks.xkcd.activities;

import android.animation.Animator;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.like.LikeButton;
import com.like.OnLikeListener;
import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.marshmallowsocks.xkcd.util.core.MSMathView;
import com.marshmallowsocks.xkcd.util.core.MSNewComicReceiver;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.whatif.CitationSpan;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfBean;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfSearchBean;
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
import com.willowtreeapps.spruce.Spruce;
import com.willowtreeapps.spruce.animation.DefaultAnimations;
import com.willowtreeapps.spruce.sort.DefaultSort;

import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;
import uk.co.chrisjenx.calligraphy.CalligraphyTypefaceSpan;
import uk.co.chrisjenx.calligraphy.TypefaceUtils;


public class WhatIf extends AppCompatActivity {

    private Integer which;
    private static Integer maxWhich;
    private boolean shouldMaxBeSet;
    private boolean isPreviousAvailable;
    private boolean isNextAvailable;

    private Button firstButton;
    private Button lastButton;
    private Button nextButton;
    private Button previousButton;
    private Button randomButton;
    private LinearLayout buttonBar;
    private ProgressBar loadingSpinner;
    private FloatingActionButton randomFab;
    private MSNewComicReceiver newComicReceiver;
    private MSXkcdDatabase database;

    private String mathView = "";
    private static Random random;

    final String[] buttonNames = {
            "FAVORITES",
            "XKCD",
            "ALL",
            "TOGGLE NAVIGATION BAR"
    };

    final int[] buttonImages = {
            R.drawable.fa_heart_on,
            R.mipmap.xkcd_icon_circle,
            android.R.drawable.ic_menu_gallery,
            R.drawable.fa_toggle_off_white
    };

    final String[] buttonSubtitles = {
            "CHECK YOUR FAVORITES",
            "VIEW XKCD COMICS",
            "SEE ALL WHAT IF",
            "USE RANDOM BUTTON INSTEAD OF NAVIGATION BAR"
    };

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_what_if);
        random = new Random();
        final Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        LikeButton likeButton = (LikeButton) toolbar.findViewById(R.id.likeButton);
        likeButton.setOnLikeListener(new OnLikeListener() {
            @Override
            public void liked(LikeButton likeButton) {
                SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                WhatIfSearchBean data = database.getWhatIf(which);
                try {
                    editor.putString(String.format(Constants.WHAT_IF_FAVORITE_KEY, which.toString()), data.jsonify());
                    editor.apply();
                }
                catch(JSONException e) {
                    Toast.makeText(WhatIf.this, "Could not favorite", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void unLiked(LikeButton likeButton) {
                SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = preferences.edit();
                editor.remove(String.format(Constants.WHAT_IF_FAVORITE_KEY, which.toString()));
                editor.apply();
            }
        });
        ImageButton searchButton = (ImageButton) findViewById(R.id.searchButton);
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onSearchRequested();
            }
        });
        toolbar.post(new Runnable() {
            @Override
            public void run() {
                Spannable appName = new SpannableString(Constants.APP_NAME);
                Spannable subtitle = new SpannableString("WHAT IF?");
                CalligraphyTypefaceSpan typefaceSpan = new CalligraphyTypefaceSpan(TypefaceUtils.load(getAssets(), "fonts/xkcd.otf"));
                appName.setSpan(typefaceSpan, 0, Constants.APP_NAME.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                subtitle.setSpan(typefaceSpan, 0, subtitle.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                toolbar.setTitle(appName);
                toolbar.setSubtitle(subtitle);
            }
        });
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(getAssets().open("html/mathview.html")));

            // do reading, usually loop until end of file reading
            String mLine;
            while ((mLine = reader.readLine()) != null) {
                mathView += mLine;
            }
        } catch (IOException e) {
            //log the exception
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    //log the exception
                }
            }
        }
        setupBoomMenu(toolbar);
        setSupportActionBar(toolbar);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Verdana-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);
        loadingSpinner = (ProgressBar) findViewById(R.id.loadingSpinner);

        newComicReceiver = new MSNewComicReceiver(viewGroup, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent xkcdIntent = new Intent(WhatIf.this, msxkcd.class);
                startActivity(xkcdIntent);
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldMaxBeSet = true;
                new RetrieveWhatIfTask().execute(Constants.WHAT_IF_LATEST_URL);
            }
        });

        database = new MSXkcdDatabase(this);
        IntentFilter newComicFilter = new IntentFilter();
        newComicFilter.addAction(Constants.NEW_COMIC_ADDED);

        LocalBroadcastManager.getInstance(this).registerReceiver(newComicReceiver, newComicFilter);

        firstButton = (Button) findViewById(R.id.firstButton);
        previousButton = (Button) findViewById(R.id.previousButton);
        nextButton = (Button) findViewById(R.id.nextButton);
        randomButton = (Button) findViewById(R.id.randomButton);
        lastButton = (Button) findViewById(R.id.lastButton);
        buttonBar = (LinearLayout) findViewById(R.id.navigationBar);
        randomFab = (FloatingActionButton) findViewById(R.id.randomFab);
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);

        randomFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                randomAction();
            }
        });
        if(getIntent().getIntExtra("newPage", -1) != -1) {
            which = getIntent().getIntExtra("newPage", -1);
            new RetrieveWhatIfTask().execute(String.format(Constants.WHAT_IF_URL, Integer.toString(which)));
        }

        else {
            shouldMaxBeSet = true;
            new RetrieveWhatIfTask().execute(Constants.WHAT_IF_LATEST_URL);
        }
        firstButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldMaxBeSet = false;
                which = 1;
                new RetrieveWhatIfTask().execute(String.format(Constants.WHAT_IF_URL, which.toString()));
            }
        });
        lastButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldMaxBeSet = true;
                new RetrieveWhatIfTask().execute(Constants.WHAT_IF_LATEST_URL);
            }
        });
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldMaxBeSet = false;
                which--;
                new RetrieveWhatIfTask().execute(String.format(Constants.WHAT_IF_URL, which.toString()));
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldMaxBeSet = false;
                which++;
                new RetrieveWhatIfTask().execute(String.format(Constants.WHAT_IF_URL, which.toString()));
            }
        });

        randomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                randomAction();
            }
        });
    }
    private void disableButtons() {
        firstButton.setEnabled(false);
        lastButton.setEnabled(false);
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);
        randomButton.setEnabled(false);
        nextButton.setAlpha(0.5f);
        previousButton.setAlpha(0.5f);
        randomButton.setAlpha(0.5f);
        firstButton.setAlpha(0.5f);
        lastButton.setAlpha(0.5f);
    }
    private class RetrieveWhatIfTask extends AsyncTask<String, Void, List<WhatIfBean>> {
        @Override
        protected void onPreExecute() {
            final ScrollView body = (ScrollView) findViewById(R.id.whatIfScrollViewport);
            disableButtons();
            body.removeAllViews();
            loadingSpinner.setVisibility(View.VISIBLE);
        }
        @Override
        protected List<WhatIfBean> doInBackground(String... urls) {
            try {
                Document doc = Jsoup.connect(urls[0]).get();
                List<WhatIfBean> body = new ArrayList<>();

                //first set what we're on:
                String whichText = doc.getElementsByClass("entry").get(0).child(0).attr("href");
                which = Integer.parseInt(whichText.substring(19, whichText.lastIndexOf('/')));

                if(shouldMaxBeSet) {
                    maxWhich = which;
                }

                if(!database.containsWhatIf(which)) {
                    WhatIfSearchBean newData = new WhatIfSearchBean();
                    newData.setNumber(which);
                    newData.setTitle(doc.getElementsByTag("h1").get(0).text());
                    if(!database.addWhatIfMetadata(newData)) {
                        Toast.makeText(WhatIf.this, "A database error occurred", Toast.LENGTH_SHORT).show();
                    }
                }
                //check if previous available:
                isPreviousAvailable = doc.getElementsByClass("nav-prev").size() != 0;

                //check if next available:
                isNextAvailable = doc.getElementsByClass("nav-next").size() != 0;

                for(Element node : doc.getElementsByClass(Constants.WHAT_IF_ENTRY).get(0).children()) {
                    WhatIfBean newNode = new WhatIfBean();
                    boolean shouldAddNode = true;
                    switch(node.tagName()) {
                        case Constants.ANCHOR:
                            newNode.setType(Constants.WHAT_IF_TITLE);
                            newNode.setBody(node.text());
                            break;
                        case Constants.PARAGRAPH:
                            //check for question
                            //check for asker
                            //check for answer body
                            if(Constants.WHAT_IF_QUESTION.equals(node.id())) {
                                newNode.setType(Constants.WHAT_IF_QUESTION);
                                newNode.setImageUrl(null);
                                newNode.setBody(node.text());
                            }
                            //check for question asker
                            else if(Constants.WHAT_IF_ATTRIBUTE.equals(node.id())) {
                                newNode.setType(Constants.WHAT_IF_ATTRIBUTE);
                                newNode.setImageUrl(null);
                                newNode.setBody(node.text());
                            }
                            else {
                                //is not question
                                //check for nested links
                                newNode.setImageUrl(null);
                                if(node.children().size() == 0) {
                                    if(node.text().startsWith("\\[") && node.text().endsWith("\\]")) {
                                        //is pure latex
                                        String rawLatex = node.text();
                                        newNode.setType(Constants.WHAT_IF_LATEX_IMAGE);
                                        newNode.setBody(rawLatex);
                                    }
                                    else {
                                        //2nd worst case: could be nested latex within non citation
                                        //body
                                        String[][] containsConditions = {
                                                {"\\(", "\\)"},
                                                {"\\[", "\\]"}
                                        };
                                        if(node.text().contains(containsConditions[0][0]) || node.text().contains(containsConditions[1][0])) {
                                            int conditionIndex;
                                            if(node.text().contains(containsConditions[0][0])) {
                                                conditionIndex = 0;
                                            }
                                            else {
                                                conditionIndex = 1;
                                            }
                                            List<String> equationBodies = new ArrayList<>();
                                            shouldAddNode = false;
                                            for (String str : node.text().split(Pattern.quote(containsConditions[conditionIndex][0]))) {
                                                if (str.matches(".*" + Pattern.quote(containsConditions[conditionIndex][1]) + ".*")) {
                                                    Collections.addAll(equationBodies, str.split(Pattern.quote(containsConditions[conditionIndex][1])));

                                                } else {
                                                    equationBodies.add(str);
                                                }
                                            }


                                            //at this point, all odd indices contain equations.
                                            //TODO: consider 0 being an equation

                                            for(int i = 0; i < equationBodies.size(); i++) {
                                                WhatIfBean tempNode = new WhatIfBean();
                                                if(i % 2 == 0) {
                                                    tempNode.setType(Constants.WHAT_IF_ANSWER_BODY_TEXT);
                                                    tempNode.setBody(equationBodies.get(i));
                                                    tempNode.setImageUrl(null);
                                                }
                                                else {
                                                    tempNode.setType(Constants.WHAT_IF_LATEX_IMAGE);
                                                    tempNode.setBody(containsConditions[0][0] + equationBodies.get(i) + containsConditions[0][1]);
                                                    tempNode.setImageUrl(null);
                                                }
                                                body.add(tempNode);
                                            }
                                        }
                                        else {
                                            //phew. simplest case
                                            newNode.setType(Constants.WHAT_IF_ANSWER_BODY_TEXT);
                                            newNode.setBody(node.text());
                                            newNode.setImageUrl(null);
                                        }
                                    }
                                }
                                else {
                                    newNode.setType(Constants.WHAT_IF_ANSWER_BODY_HTML);

                                    for(Element citation : node.select(Constants.CLASS_REF)) {
                                        newNode.addCitation(citation.child(0).text(), citation.child(1).text());
                                        citation.child(1).remove();
                                        citation.child(0).wrap(String.format(Constants.ANCHOR_WRAP, citation.select(Constants.CLASS_REF_NUM).text()));
                                        citation.child(0).wrap(Constants.SUP_WRAP);
                                    }

                                    newNode.setBody(node.html());
                                    newNode.setImageUrl(null);
                                }
                            }
                            break;
                        case Constants.IMAGE:
                            newNode.setType(Constants.WHAT_IF_ILLUSTRATION);
                            newNode.setImageUrl(Constants.WHAT_IF_LATEST_URL + node.attr("src"));
                            newNode.setBody(null);
                            break;
                        case Constants.BLOCKQUOTE:
                            newNode.setType(Constants.WHAT_IF_BLOCKQUOTE);
                            newNode.setBody(node.text());
                            break;
                        case Constants.UNORDERED_LIST:
                        case Constants.ORDERED_LIST:
                        default:
                            newNode.setType(Constants.WHAT_IF_ANSWER_BODY_HTML);
                            newNode.setBody(node.html());
                    }
                    if(shouldAddNode) {
                        body.add(newNode);
                    }
                }
                return body;
            } catch (Exception e) {
                return null;
            }
        }
        @Override
        protected void onPostExecute(List<WhatIfBean> content) {
            final ScrollView body = (ScrollView) findViewById(R.id.whatIfScrollViewport);
            body.removeAllViews();
            LinearLayout bodyContents = new LinearLayout(WhatIf.this);
            bodyContents.setOrientation(LinearLayout.VERTICAL);
            for(WhatIfBean node : content) {
                TextView paragraphContainer = null;
                ImageView illustrationContainer = null;
                MSMathView equation = null;

                switch (node.getType()) {
                    case Constants.WHAT_IF_TITLE:
                        paragraphContainer = new TextView(WhatIf.this);
                        paragraphContainer.setTextSize(28.0f);
                        paragraphContainer.setTextColor(Color.parseColor("#000000"));
                        SpannableString spanString = new SpannableString(node.getBody());
                        spanString.setSpan(new UnderlineSpan(), 0, spanString.length(), 0);
                        spanString.setSpan(new StyleSpan(Typeface.BOLD), 0, spanString.length(), 0);
                        paragraphContainer.setText(spanString);
                        break;
                    case Constants.WHAT_IF_QUESTION:
                        paragraphContainer = new TextView(WhatIf.this);
                        paragraphContainer.setTextSize(20.0f);
                        paragraphContainer.setTextColor(Color.parseColor("#005994"));
                        paragraphContainer.setTypeface(paragraphContainer.getTypeface(), Typeface.BOLD_ITALIC);
                        paragraphContainer.setText(node.getBody());
                        break;
                    case Constants.WHAT_IF_ATTRIBUTE:
                        paragraphContainer = new TextView(WhatIf.this);
                        paragraphContainer.setTextSize(20.0f);
                        paragraphContainer.setTextColor(Color.parseColor("#005994"));
                        paragraphContainer.setTypeface(paragraphContainer.getTypeface(), Typeface.BOLD_ITALIC);
                        paragraphContainer.setGravity(Gravity.END);
                        paragraphContainer.setText(node.getBody());
                        break;
                    case Constants.WHAT_IF_ANSWER_BODY_TEXT:
                        paragraphContainer = new TextView(WhatIf.this);
                        paragraphContainer.setTextSize(20.0f);
                        paragraphContainer.setText(node.getBody());
                        break;
                    case Constants.WHAT_IF_ANSWER_BODY_HTML:
                        paragraphContainer = new TextView(WhatIf.this);
                        paragraphContainer.setTextSize(20.0f);
                        paragraphContainer.setLinksClickable(true);
                        paragraphContainer.setMovementMethod(LinkMovementMethod.getInstance());

                        if(node.hasCitations()) {
                            SpannableStringBuilder spannableStringBuilder = (SpannableStringBuilder) Html.fromHtml(node.getBody());
                            URLSpan[] spans = spannableStringBuilder.getSpans(0, spannableStringBuilder.length(), URLSpan.class);
                            for(URLSpan span : spans) {
                                for(WhatIfBean.Citation citation : node.getCitations()) {
                                    if (span.getURL().equals(citation.getCitationNumber())) {
                                        int start = spannableStringBuilder.getSpanStart(span);
                                        int end = spannableStringBuilder.getSpanEnd(span);
                                        spannableStringBuilder.removeSpan(span);
                                        span = new CitationSpan(citation.getCitationBody());
                                        spannableStringBuilder.setSpan(span, start, end, 0);
                                    }
                                }
                            }
                            paragraphContainer.setText(spannableStringBuilder);
                        }
                        else {
                            paragraphContainer.setText(Html.fromHtml(node.getBody()));
                        }
                        break;
                    case Constants.WHAT_IF_LATEX_IMAGE:
                        equation = new MSMathView(WhatIf.this);
                        equation.createFrom(mathView, node.getBody());
                        break;
                    case Constants.WHAT_IF_ILLUSTRATION:
                        illustrationContainer = new ImageView(WhatIf.this);
                        Glide.with(WhatIf.this).load(node.getImageUrl()).into(illustrationContainer);
                        break;
                    case Constants.WHAT_IF_BLOCKQUOTE:
                        paragraphContainer = new TextView(WhatIf.this);
                        paragraphContainer.setTextSize(22.0f);
                        paragraphContainer.setTypeface(paragraphContainer.getTypeface(), Typeface.ITALIC);
                        paragraphContainer.setGravity(Gravity.CENTER_HORIZONTAL);
                        paragraphContainer.setText(node.getBody());
                        break;
                    case Constants.WHAT_IF_ANSWER_BODY_LIST:
                        paragraphContainer = new TextView(WhatIf.this);
                        paragraphContainer.setTextSize(20.0f);
                        paragraphContainer.setGravity(Gravity.CENTER_HORIZONTAL);
                        paragraphContainer.setText(node.getBody());
                }
                if(paragraphContainer != null) {
                    bodyContents.addView(paragraphContainer);
                }
                else if(illustrationContainer != null) {
                    bodyContents.addView(illustrationContainer);

                }
                else {
                    bodyContents.addView(equation);
                }
            }

            previousButton.setEnabled(isPreviousAvailable);
            firstButton.setEnabled(isPreviousAvailable);
            nextButton.setEnabled(isNextAvailable);
            lastButton.setEnabled(isNextAvailable);
            randomButton.setEnabled(true);
            randomButton.setAlpha(1.0f);
            if(isPreviousAvailable) {
                previousButton.setAlpha(1.0f);
                firstButton.setAlpha(1.0f);
            }
            else {
                previousButton.setAlpha(0.5f);
                firstButton.setAlpha(0.5f);
            }
            if(isNextAvailable) {
                nextButton.setAlpha(1.0f);
                lastButton.setAlpha(1.0f);
            }
            else {
                nextButton.setAlpha(0.5f);
                lastButton.setAlpha(0.5f);
            }
            loadingSpinner.setVisibility(View.GONE);
            body.addView(bodyContents);
            body.postDelayed(new Runnable() {
                @Override
                public void run() {
                    body.fullScroll(ScrollView.FOCUS_UP);
                }
            }, 600);
            isFavorite(which);
        }
    }
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ImageButton searchButton = (ImageButton) findViewById(R.id.searchButton);
        BoomMenuButton oldBmb = (BoomMenuButton) toolbar.findViewById(R.id.bmb);
        ViewGroup.LayoutParams searchParams = searchButton.getLayoutParams();
        ViewGroup.LayoutParams oldBmbLayoutParams = oldBmb.getLayoutParams();
        toolbar.removeView(oldBmb);
        toolbar.removeView(searchButton);
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
        bmb.setId(R.id.bmb);
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
        setupBoomMenu(toolbar);
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
        //getMenuInflater().inflate(R.menu.menu_what_if, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if(id == R.id.action_xkcd) {
            Intent xkcdIntent = new Intent(this, msxkcd.class);
            startActivity(xkcdIntent);
            return true;
        }
        if(id == R.id.action_all_what_if) {
            Intent allWhatIfIntent = new Intent(this, WhatIfSearchResults.class);
            allWhatIfIntent.setAction(Constants.ALL_WHAT_IF);
            startActivity(allWhatIfIntent);
            return true;
        }
        if(id == R.id.action_search) {
            onSearchRequested();
        }

        return super.onOptionsItemSelected(item);
    }
    public void isFavorite(Integer which) {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        LikeButton likeButton= (LikeButton) toolbar.findViewById(R.id.likeButton);
        likeButton.setLiked(preferences.contains(
                String.format(Constants.WHAT_IF_FAVORITE_KEY, which.toString())
        ));
    }
    private void setupBoomMenu(Toolbar toolbar) {
        final BoomMenuButton bmb = (BoomMenuButton) toolbar.findViewById(R.id.bmb);
        final FloatingActionButton randomFab = (FloatingActionButton) findViewById(R.id.randomFab);
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
                        Intent favoriteIntent = new Intent(WhatIf.this, WhatIfSearchResults.class);
                        favoriteIntent.setAction(Constants.WHAT_IF_FAVORITE_KEY);
                        startActivity(favoriteIntent);
                        break;
                    case 1:
                        Intent msXkcdIntent = new Intent(WhatIf.this, msxkcd.class);
                        startActivity(msXkcdIntent);
                        break;
                    case 2:
                        Intent allWhatIf = new Intent(WhatIf.this, WhatIfSearchResults.class);
                        allWhatIf.setAction(Constants.ALL_WHAT_IF);
                        startActivity(allWhatIf);
                        break;
                    case 3:
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
                                            buttonSubtitles[3] = "USE NAVIGATION BAR INSTEAD OF RANDOM BUTTON";
                                            buttonImages[3] = R.drawable.fa_toggle_on_white;
                                            switch(getResources().getConfiguration().orientation) {
                                                case 1:
                                                    ((HamButton.Builder)bmb.getBuilder(3)).subNormalText(buttonSubtitles[3]);
                                                    ((HamButton.Builder)bmb.getBuilder(3)).normalImageRes(buttonImages[3]);
                                                    break;
                                                case 2:
                                                    ((TextOutsideCircleButton.Builder)bmb.getBuilder(3)).normalImageRes(buttonImages[3]);
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
                            buttonSubtitles[3] = "USE RANDOM BUTTON INSTEAD OF NAVIGATION BAR";
                            buttonImages[3] = R.drawable.fa_toggle_off_white;
                            switch(getResources().getConfiguration().orientation) {
                                case 1:
                                    ((HamButton.Builder)bmb.getBuilder(3)).subNormalText(buttonSubtitles[3]);
                                    ((HamButton.Builder)bmb.getBuilder(3)).normalImageRes(buttonImages[3]);
                                    break;
                                case 2:
                                    ((TextOutsideCircleButton.Builder)bmb.getBuilder(3)).normalImageRes(buttonImages[3]);
                                    break;
                            }
                            new Spruce
                                    .SpruceBuilder(buttonBar)
                                    .sortWith(new DefaultSort(50L))
                                    .animateWith(DefaultAnimations.fadeInAnimator(buttonBar, 400)).start();
                            randomFab.hide();
                        }
                        break;
                }
            }
        };

        switch(getResources().getConfiguration().orientation) {
            case 1:
                bmb.clearBuilders();
                bmb.setButtonEnum(ButtonEnum.Ham);
                bmb.setBoomEnum(BoomEnum.HORIZONTAL_THROW_1);
                bmb.setPiecePlaceEnum(PiecePlaceEnum.HAM_4);
                bmb.setButtonPlaceEnum(ButtonPlaceEnum.HAM_4);
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
                bmb.setPiecePlaceEnum(PiecePlaceEnum.DOT_4_2);
                bmb.setButtonPlaceEnum(ButtonPlaceEnum.Horizontal);
                for (int i = 0; i < 4; i++) {
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
    private void randomAction() {
        Integer temp;
        temp = random.nextInt(maxWhich) + 1;
        while(temp.intValue() == which.intValue()) {
            temp = random.nextInt(maxWhich) + 1;
        }
        which = temp;
        shouldMaxBeSet = false;
        new RetrieveWhatIfTask().execute(String.format(Constants.WHAT_IF_URL, which.toString()));
    }
}
