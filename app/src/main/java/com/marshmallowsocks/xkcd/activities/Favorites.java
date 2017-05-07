package com.marshmallowsocks.xkcd.activities;

import android.content.Context;
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
import com.marshmallowsocks.xkcd.util.xkcd.XKCDComicBean;
import com.marshmallowsocks.xkcd.util.xkcd.search.ComicSearchResultAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;
import uk.co.chrisjenx.calligraphy.CalligraphyContextWrapper;

public class Favorites extends AppCompatActivity {

    private RecyclerView favorites;
    private LinearLayoutManager layoutManager;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(CalligraphyContextWrapper.wrap(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/xkcd.otf")
                .setFontAttrId(R.attr.fontPath)
                .build()
        );

        favorites = (RecyclerView) findViewById(R.id.favorites);
        favorites.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        try {
            getFavoritesData();
        }
        catch(JSONException e) {
            //TODO
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
        if(results.size() > 0 && favorites != null) {
            favorites.setAdapter(new ComicSearchResultAdapter(this, results));
            favorites.setLayoutManager(layoutManager);
            noData.setVisibility(View.GONE);
        }
        else {
            noData.setVisibility(View.VISIBLE);
        }
    }

}
