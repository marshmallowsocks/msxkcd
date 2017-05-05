package com.marshmallowsocks.xkcd.activities;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.xkcd.XKCDComicBean;
import com.marshmallowsocks.xkcd.util.xkcd.search.ComicSearchResultAdapter;

import java.util.List;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class ComicSearchResults extends AppCompatActivity {

    private RecyclerView searchResults;
    private LinearLayoutManager layoutManager;

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

        searchResults = (RecyclerView) findViewById(R.id.searchResults);
        searchResults.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        // Get the intent, verify the action and get the query
        Intent intent = getIntent();
        if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
            String query = intent.getStringExtra(SearchManager.QUERY);
            getSearchData(query);
        }
    }

    private void getSearchData(String query) {
        MSXkcdDatabase db = new MSXkcdDatabase(this);
        List<XKCDComicBean> results = db.searchComic(query);
        TextView noData = (TextView) findViewById(R.id.noData);

        if(results.size() > 0 && searchResults != null) {
            searchResults.setAdapter(new ComicSearchResultAdapter(this, results));
            searchResults.setLayoutManager(layoutManager);
            noData.setVisibility(View.GONE);
        }
        else {
            noData.setVisibility(View.VISIBLE);
        }
    }
}
