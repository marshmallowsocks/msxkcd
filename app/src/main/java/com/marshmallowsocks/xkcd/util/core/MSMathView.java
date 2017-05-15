package com.marshmallowsocks.xkcd.util.core;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.webkit.WebView;

/**
 * Created by vatsa on 5/13/17.
 */

public class MSMathView extends WebView {
    @SuppressLint("SetJavaScriptEnabled")
    public MSMathView(Context context) {
        super(context);
        getSettings().setJavaScriptEnabled(true);
    }
    public void createFrom(String mathView, String mathText) {

        StringBuilder resolvedView = new StringBuilder(mathView);
        resolvedView.insert(mathView.indexOf("MS_math_text_stub") + 19, mathText);
        loadData(resolvedView.toString(), "text/html", null);
        setBackgroundColor(Color.TRANSPARENT);
    }
}
