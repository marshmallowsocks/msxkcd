package com.marshmallowsocks.xkcd.activities;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfSearchBean;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfSearchResultAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WhatIfSearchResults extends ComicSearchResults {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(Constants.ALL_WHAT_IF.equals(intent.getAction())) {
            getAllWhatIfData();
        }
        else if(Constants.WHAT_IF_FAVORITE_KEY.equals(intent.getAction())) {
            try {
                toolbar.setTitle("FAVORITES");
                setSupportActionBar(toolbar);
                getWhatIfFavoritesData();
            }
            catch(JSONException e) {
                //TODO
            }
        }
    }

    @Override
    protected void getSearchData(String query) {
        List<WhatIfSearchBean> results = db.searchWhatIf(query);
        setAdapter(results);
    }

    private void getAllWhatIfData() {
        List<WhatIfSearchBean> results = db.getAllWhatIf();
        setAdapter(results);
    }

    private void getWhatIfFavoritesData() throws JSONException {
        SharedPreferences preferences = getSharedPreferences(Constants.SHARED_PREFERENCES_FILE, Context.MODE_PRIVATE);
        List<WhatIfSearchBean> results = new ArrayList<>();

        for(Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
            if(entry.getKey().startsWith("what_if_favorite_")) {
                JSONObject result = new JSONObject((String)entry.getValue());
                WhatIfSearchBean comic = new WhatIfSearchBean();
                comic.setNumber(result.getInt("number"));
                comic.setTitle(result.getString(Constants.COMIC_TITLE));
                results.add(comic);
            }
        }
        setAdapter(results);
    }

    private void setAdapter(List<WhatIfSearchBean> results) {
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
}
