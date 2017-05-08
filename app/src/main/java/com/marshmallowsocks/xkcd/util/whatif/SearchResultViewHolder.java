package com.marshmallowsocks.xkcd.util.whatif;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.marshmallowsocks.xkcd.R;

/**
 * Created by vatsa on 5/5/17.
 * custom viewholder
 */

class SearchResultViewHolder extends RecyclerView.ViewHolder {

    TextView comicTitle;
    TextView comicNumber;

    SearchResultViewHolder(View itemView) {
        super(itemView);
        comicTitle = (TextView) itemView.findViewById(R.id.comicTitle);
        comicNumber = (TextView) itemView.findViewById(R.id.comicNumber);
    }
}
