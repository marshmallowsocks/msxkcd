package com.marshmallowsocks.xkcd.util.msxkcd;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class InteractiveComicView extends WebView {
    public final String URL = "https://m.xkcd.com/";

    @SuppressLint("setJavaScriptEnabled")
    public InteractiveComicView(Context context) {
        super(context);
        getSettings().setJavaScriptEnabled(true);
    }
    public InteractiveComicView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }
    public void load(Integer which) {
        setWebViewClient(new WebViewClient());
        loadUrl(URL + which);
    }
}
