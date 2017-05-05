package com.marshmallowsocks.xkcd.util.xkcd.search;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.marshmallowsocks.xkcd.R;

/**
 * Created by vatsa on 5/5/17.
 * custom viewholder
 */

class SearchResultViewHolder extends RecyclerView.ViewHolder {

    TextView comicTitle;
    ImageView comicImage;

    SearchResultViewHolder(View itemView) {
        super(itemView);
        comicTitle = (TextView) itemView.findViewById(R.id.comicTitle);
        comicImage = (ImageView) itemView.findViewById(R.id.comicImage);
    }
}
