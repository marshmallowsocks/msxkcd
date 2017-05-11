package com.marshmallowsocks.xkcd.activities;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.method.LinkMovementMethod;
import android.text.style.StyleSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.marshmallowsocks.xkcd.util.core.MSNewComicReceiver;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.whatif.CitationSpan;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfBean;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfSearchBean;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import io.github.kexanie.library.MathView;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;


public class WhatIf extends AppCompatActivity {

    private Integer which;
    private static Integer maxWhich;
    private boolean shouldMaxBeSet;
    private boolean isPreviousAvailable;
    private boolean isNextAvailable;

    private Button nextButton;
    private Button previousButton;
    private Button randomButton;

    private MSNewComicReceiver newComicReceiver;
    private MSXkcdDatabase database;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_what_if);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Verdana-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        final ViewGroup viewGroup = (ViewGroup) ((ViewGroup) this
                .findViewById(android.R.id.content)).getChildAt(0);

        newComicReceiver = new MSNewComicReceiver(viewGroup, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent xkcdIntent = new Intent(WhatIf.this, msxkcd.class);
                startActivity(xkcdIntent);
            }
        });

        database = new MSXkcdDatabase(this);
        IntentFilter newComicFilter = new IntentFilter();
        newComicFilter.addAction(Constants.NEW_COMIC_ADDED);

        LocalBroadcastManager.getInstance(this).registerReceiver(newComicReceiver, newComicFilter);
        previousButton = (Button) findViewById(R.id.previousButton);
        nextButton = (Button) findViewById(R.id.nextButton);
        randomButton = (Button) findViewById(R.id.randomButton);

        nextButton.setEnabled(false);
        previousButton.setEnabled(false);

        if(getIntent().getIntExtra("newPage", -1) != -1) {
            which = getIntent().getIntExtra("newPage", -1);
            new RetrieveWhatIfTask().execute(String.format(Constants.WHAT_IF_URL, Integer.toString(which)));
        }

        else {
            shouldMaxBeSet = true;
            new RetrieveWhatIfTask().execute(Constants.WHAT_IF_LATEST_URL);
        }
        previousButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldMaxBeSet = false;
                disableButtons();
                which--;
                new RetrieveWhatIfTask().execute(String.format(Constants.WHAT_IF_URL, which.toString()));
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                shouldMaxBeSet = false;
                disableButtons();
                which++;
                new RetrieveWhatIfTask().execute(String.format(Constants.WHAT_IF_URL, which.toString()));
            }
        });

        randomButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Random random = new Random();
                which = random.nextInt(maxWhich) + 1;
                disableButtons();
                new RetrieveWhatIfTask().execute(String.format(Constants.WHAT_IF_URL, which.toString()));

            }
        });
    }

    private void disableButtons() {
        nextButton.setEnabled(false);
        previousButton.setEnabled(false);
        randomButton.setEnabled(false);
        nextButton.setAlpha(0.5f);
        previousButton.setAlpha(0.5f);
        randomButton.setAlpha(0.5f);
    }

    private class RetrieveWhatIfTask extends AsyncTask<String, Void, List<WhatIfBean>> {

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
                        Toast.makeText(WhatIf.this, "A database error occured", Toast.LENGTH_SHORT);
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
                                        if(node.text().contains("\\(")) {

                                            List<String> equationBodies = new ArrayList<>();
                                            shouldAddNode = false;
                                            for (String str : node.text().split(Pattern.quote("\\("))) {
                                                if (str.matches(".*" + Pattern.quote("\\)") + ".*")) {
                                                    Collections.addAll(equationBodies, str.split(Pattern.quote("\\)")));

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
                                                    tempNode.setBody("\\(" + equationBodies.get(i) + "\\)");
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
                            newNode.setType(Constants.WHAT_IF_ANSWER_BODY_LIST);
                            StringBuilder listText = new StringBuilder();
                            for(Element li : node.children()) {
                                listText.append(li.text()).append("\n");
                            }
                            newNode.setBody(listText.toString());
                            break;
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
                NestedScrollView equationContainer = null;
                if(node == null) {
                    Toast.makeText(WhatIf.this, "A node was null", Toast.LENGTH_SHORT).show();
                    continue;
                }
                switch (node.getType()) {
                    case Constants.WHAT_IF_TITLE:
                        paragraphContainer = new TextView(WhatIf.this);
                        paragraphContainer.setTextSize(28.0f);
                        paragraphContainer.setTextColor(getResources().getColor(android.R.color.black));
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
                                        span = new CitationSpan("", citation.getCitationBody());
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
                        MathView equation = new MathView(WhatIf.this, null);
                        equationContainer = new NestedScrollView(WhatIf.this);
                        equation.setText(node.getBody());
                        equationContainer.addView(equation);
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
                    bodyContents.addView(equationContainer);
                }
            }

            previousButton.setEnabled(isPreviousAvailable);
            nextButton.setEnabled(isNextAvailable);
            randomButton.setEnabled(true);
            randomButton.setAlpha(1.0f);
            if(isPreviousAvailable) {
                previousButton.setAlpha(1.0f);
            }
            else {
                previousButton.setAlpha(0.5f);
            }
            if(isNextAvailable) {
                nextButton.setAlpha(1.0f);
            }
            else {
                nextButton.setAlpha(0.5f);
            }

            body.addView(bodyContents);
            body.postDelayed(new Runnable() {
                @Override
                public void run() {
                    body.fullScroll(ScrollView.FOCUS_UP);
                }
            }, 600);
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
        getMenuInflater().inflate(R.menu.menu_what_if, menu);
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
}
