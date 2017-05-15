package com.marshmallowsocks.xkcd.util.core;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.TextView;

import com.marshmallowsocks.xkcd.util.constants.Constants;

/**
 * Created by sriva on 5/6/2017.
 * A listener for new comics.
 */

public class MSNewComicReceiver extends BroadcastReceiver {
    private View msView;
    private View.OnClickListener msComicCallback;
    private View.OnClickListener msWhatIfCallback;
    public MSNewComicReceiver(View view, View.OnClickListener comicCallback, View.OnClickListener whatIfCallback) {
        msView = view;
        msComicCallback = comicCallback;
        msWhatIfCallback = whatIfCallback;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        if(Constants.NEW_COMIC_ADDED.equals(intent.getAction())) {
            Snackbar newComicSnackbar = Snackbar.make(msView, Constants.NEW_COMIC_ADDED.toUpperCase() + intent.getStringExtra(Constants.NEW_COMIC_ADDED).toUpperCase(), Snackbar.LENGTH_INDEFINITE);
            newComicSnackbar.setActionTextColor(Color.WHITE).setAction("Go", msComicCallback);
            TextView newComicBody = (TextView) newComicSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            newComicBody.setMaxLines(15);
            newComicSnackbar.show();
        }
        if(Constants.NEW_WHAT_IF_ADDED.equals(intent.getAction())) {
            Snackbar newComicSnackbar = Snackbar.make(msView, Constants.NEW_WHAT_IF_ADDED.toUpperCase() + intent.getStringExtra(Constants.NEW_WHAT_IF_ADDED).toUpperCase(), Snackbar.LENGTH_INDEFINITE);
            newComicSnackbar.setActionTextColor(Color.WHITE).setAction("Go", msWhatIfCallback);
            TextView newComicBody = (TextView) newComicSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            newComicBody.setMaxLines(15);
            newComicSnackbar.show();
        }
    }
}
