package com.marshmallowsocks.xkcd.util.msxkcd;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.activities.ComicSearchResults;
import com.marshmallowsocks.xkcd.activities.msxkcd;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.util.List;

import static com.bumptech.glide.load.engine.DiskCacheStrategy.SOURCE;

/**
 * Created by vatsa on 5/5/17.
 * custom search adapter
 */

public class ComicSearchResultAdapter extends RecyclerView.Adapter<SearchResultViewHolder> {

    private List<XKCDComicBean> searchResultData;
    private Context msContext;
    private ComicSearchResults.LayoutMode msLayoutMode;
    public ComicSearchResultAdapter(Context context, List<XKCDComicBean> searchResultData) {
        msContext = context;
        this.searchResultData = searchResultData;
        msLayoutMode = ComicSearchResults.LayoutMode.LINEAR;
    }

    public void setLayoutMode(ComicSearchResults.LayoutMode layoutMode) {
        msLayoutMode = layoutMode;
    }

    @Override
    public SearchResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view;
        if(msLayoutMode == ComicSearchResults.LayoutMode.LINEAR) {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comic_search_result_row, parent, false);
        }
        else {
            view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comic_search_result_grid, parent, false);
        }
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SearchResultViewHolder holder, int position) {
        holder.comicTitle.setText(searchResultData.get(position).getTitle().toUpperCase());
        XKCDComicBean currentComic = searchResultData.get(position);
        File image = new File(msContext.getFilesDir().getAbsolutePath() + "/xkcd_" + currentComic.getNumber() + currentComic.getImageUrl().substring(currentComic.getImageUrl().length() - 4));
        String imageUrl;
        if(image.exists()) {
            imageUrl = "file:/" + image.getAbsolutePath();
            Log.d("comicHolder url", imageUrl);
            if (currentComic.getImageUrl().endsWith(".gif")) {
                Glide.with(msContext)
                        .load(image)
                        .asGif()
                        .diskCacheStrategy(SOURCE)
                        .into(holder.comicImage);
            } else if (currentComic.getImageUrl().endsWith(".png") || currentComic.getImageUrl().endsWith(".jpg")) {
                Picasso.with(msContext)
                        .load(image)
                        .into(holder.comicImage);
            }
        }
        else {
            imageUrl = currentComic.getImageUrl();
            if (currentComic.getImageUrl().endsWith(".gif")) {
                Glide.with(msContext)
                        .load(imageUrl)
                        .asGif()
                        .diskCacheStrategy(SOURCE)
                        .into(holder.comicImage);
            } else if (currentComic.getImageUrl().endsWith(".png") || currentComic.getImageUrl().endsWith(".jpg")) {
                Picasso.with(msContext)
                        .load(imageUrl)
                        .into(holder.comicImage);
            }
        }
        if(searchResultData.get(position).getImageUrl().endsWith(".gif")) {
            Glide.with(msContext).load(searchResultData.get(position).getImageUrl()).asGif().diskCacheStrategy(SOURCE)
                    .into(holder.comicImage);
        }
        else {
            Picasso.with(msContext).load(searchResultData.get(position).getImageUrl()).into(holder.comicImage);
        }

        final int intentPosition = position;

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent goToHomePage = new Intent(msContext, msxkcd.class);
                goToHomePage.setAction(Constants.SEARCH_TO_PAGE_ACTION);
                goToHomePage.putExtra("newPage", searchResultData.get(intentPosition).getNumber());
                msContext.startActivity(goToHomePage);
            }
        });
    }

    @Override
    public int getItemCount() {
        return searchResultData.size();
    }
}
