package com.marshmallowsocks.xkcd.util.whatif;

import android.annotation.SuppressLint;
import android.graphics.Color;
import android.support.design.widget.Snackbar;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

/**
 * Created by vatsa on 5/5/17.
 */

@SuppressLint("ParcelCreator")
public class CitationSpan extends URLSpan {

    private String body;

    public CitationSpan(String url, String body) {
        super(url);
        this.body = body;
    }

    @Override
    public void onClick(View widget) {
        Snackbar citation = Snackbar.make(widget, body, Snackbar.LENGTH_INDEFINITE);
        TextView citationBody = (TextView) citation.getView().findViewById(android.support.design.R.id.snackbar_text);
        citationBody.setMaxLines(15);
        citation.setActionTextColor(Color.WHITE)
                .setAction("DISMISS", new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //do nothing
                    }
                }).show();
    }
}
