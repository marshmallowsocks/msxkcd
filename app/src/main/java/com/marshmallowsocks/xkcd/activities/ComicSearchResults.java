package com.marshmallowsocks.xkcd.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.marshmallowsocks.xkcd.util.core.MSNewComicReceiver;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.http.MSRequestQueue;
import com.marshmallowsocks.xkcd.util.msxkcd.ComicSearchResultAdapter;
import com.marshmallowsocks.xkcd.util.msxkcd.XKCDComicBean;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ComicSearchResults extends AppCompatActivity {

    protected RecyclerView fullList;
    protected LinearLayoutManager layoutManager;
    protected MSXkcdDatabase db;
    protected MSRequestQueue msRequestQueue;
    protected Toolbar toolbar;
    private MSNewComicReceiver newComicReceiver;
    private ImageButton sortButton;
    private SortMode mode;
    private enum SortMode {
        ASC,
        DESC
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_search_results);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        sortButton = (ImageButton) toolbar.findViewById(R.id.sortButton);
        sortButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == SortMode.ASC) {
                    mode = SortMode.DESC;
                    sortButton.setScaleY(-1f);
                    layoutManager.setStackFromEnd(true);
                    layoutManager.setReverseLayout(true);
                }
                else {
                    mode = SortMode.ASC;
                    sortButton.setScaleX(1f);
                    sortButton.setScaleY(1f);
                    layoutManager.setStackFromEnd(false);
                    layoutManager.setReverseLayout(false);
                }
            }
        });
        setSupportActionBar(toolbar);

        if(getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/xkcd.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        db = new MSXkcdDatabase(this);
        msRequestQueue = MSRequestQueue.getInstance(this);

        newComicReceiver = new MSNewComicReceiver(findViewById(R.id.searchHolder), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ComicSearchResults.this, msxkcd.class));
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(ComicSearchResults.this, WhatIf.class));
            }
        });
        IntentFilter newComicFilter = new IntentFilter();
        newComicFilter.addAction(Constants.NEW_COMIC_ADDED);

        LocalBroadcastManager.getInstance(this).registerReceiver(newComicReceiver, newComicFilter);

        fullList = (RecyclerView) findViewById(R.id.searchResults);
        fullList.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        mode = SortMode.ASC;
        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            getSearchData(query);
        }
        else if(Constants.FAVORITE_KEY.equals(intent.getAction())) {
            try {
                toolbar.setTitle("FAVORITES");
                setSupportActionBar(toolbar);
                getFavoritesData();
            }
            catch(JSONException e) {
                //TODO
            }
        }

        else if(Constants.ALL_COMICS.equals(intent.getAction())) {
            toolbar.setTitle("COMICS");
            setSupportActionBar(toolbar);
            getAllData();
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
        LocalBroadcastManager.getInstance(this).unregisterReceiver(newComicReceiver);
        super.onPause();
    }

    protected void getSearchData(String query) {
        getRelevantComicData(query);
    }

    private void getAllData() {
        setAdapter(db.getAllComics());
    }

    private void getFavoritesData() throws JSONException {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        List<XKCDComicBean> results = new ArrayList<>();

        for(Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if(entry.getKey().startsWith("xkcd_favorite_")) {
                JSONObject result = new JSONObject((String)entry.getValue());
                XKCDComicBean comic = new XKCDComicBean();
                comic.setTitle(result.getString(Constants.COMIC_TITLE));
                comic.setAltText(result.getString(Constants.COMIC_EXTRA).toUpperCase());
                comic.setImageUrl(result.getString(Constants.COMIC_URL));
                comic.setNumber(result.getInt(Constants.COMIC_INDEX));

                results.add(comic);
            }
        }
        setAdapter(results);
    }

    private void getRelevantComicData(String query) {
        setAdapter(db.searchComic(query));
    }

    private void setAdapter(List<XKCDComicBean> results) {
        TextView noData = (TextView) findViewById(R.id.noData);

        if(results.size() > 0 && fullList != null) {
            fullList.setAdapter(new ComicSearchResultAdapter(this, results));
            fullList.setLayoutManager(layoutManager);
            noData.setVisibility(View.GONE);
        }
        else {
            noData.setVisibility(View.VISIBLE);
        }
    }
}
