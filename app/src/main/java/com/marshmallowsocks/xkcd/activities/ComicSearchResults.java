package com.marshmallowsocks.xkcd.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.core.Constants;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfSearchBean;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfSearchResultAdapter;
import com.marshmallowsocks.xkcd.util.xkcd.XKCDComicBean;
import com.marshmallowsocks.xkcd.util.xkcd.search.ComicSearchResultAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ComicSearchResults extends AppCompatActivity {

    private RecyclerView fullList;
    private LinearLayoutManager layoutManager;
    private MSXkcdDatabase db;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comic_search_results);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/xkcd.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        db = new MSXkcdDatabase(this);
        fullList = (RecyclerView) findViewById(R.id.searchResults);
        fullList.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

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
        else if(Constants.ALL_WHAT_IF.equals(intent.getAction())) {
            toolbar.setTitle("WHAT IF");
            setSupportActionBar(toolbar);
            getAllWhatIfData();
        }
    }

    private void getSearchData(String query) {
        List<XKCDComicBean> results = db.searchComic(query);
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

    private void getAllData() {
        List<XKCDComicBean> results = db.getAllComics();
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

    private void getAllWhatIfData() {
        List<WhatIfSearchBean> results = db.getAllWhatIf();
        TextView noData = (TextView) findViewById(R.id.noData);

        if(results.size() > 0 && fullList != null) {
            fullList.setAdapter(new WhatIfSearchResultAdapter(this, results));
            fullList.setLayoutManager(layoutManager);
            noData.setVisibility(View.GONE);
        }
        else {
            noData.setVisibility(View.VISIBLE);
        }
    }

    private void getFavoritesData() throws JSONException {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        List<XKCDComicBean> results = new ArrayList<>();
        TextView noData = (TextView) findViewById(R.id.noData);

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
