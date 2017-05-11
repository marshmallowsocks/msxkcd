package com.marshmallowsocks.xkcd.util.msxkcd;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.bumptech.glide.Glide;
import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.activities.msxkcd;
import com.marshmallowsocks.xkcd.util.constants.Constants;

import java.util.List;

/**
 * Created by vatsa on 5/5/17.
 * custom search adapter
 */

public class ComicSearchResultAdapter extends RecyclerView.Adapter<SearchResultViewHolder> {

    private List<XKCDComicBean> searchResultData;
    private Context msContext;

    public ComicSearchResultAdapter(Context context, List<XKCDComicBean> searchResultData) {
        msContext = context;
        this.searchResultData = searchResultData;
    }

    @Override
    public SearchResultViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.comic_search_result_row, parent, false);
        return new SearchResultViewHolder(view);
    }

    @Override
    public void onBindViewHolder(final SearchResultViewHolder holder, int position) {
        holder.comicTitle.setText(searchResultData.get(position).getTitle().toUpperCase());
        Glide.with(msContext).load(searchResultData.get(position).getImageUrl()).into(holder.comicImage);
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
