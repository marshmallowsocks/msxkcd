package com.marshmallowsocks.xkcd.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfSearchBean;
import com.marshmallowsocks.xkcd.util.whatif.WhatIfSearchResultAdapter;

import java.util.List;

public class WhatIfSearchResults extends ComicSearchResults {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        if(Constants.ALL_WHAT_IF.equals(intent.getAction())) {
            getAllWhatIfData();
        }
    }

    @Override
    protected void getSearchData(String query) {
        List<WhatIfSearchBean> results = db.searchWhatIf(query);
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
}
