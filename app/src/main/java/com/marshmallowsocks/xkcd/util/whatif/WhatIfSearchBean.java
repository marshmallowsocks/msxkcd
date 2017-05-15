package com.marshmallowsocks.xkcd.util.whatif;

import com.marshmallowsocks.xkcd.util.constants.Constants;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by vatsa on 5/7/17.
 */

public class WhatIfSearchBean {
    private int number;
    private String title;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getNumber() {
        return number;
    }

    public void setNumber(int number) {
        this.number = number;
    }

    public String jsonify() throws JSONException {
        JSONObject result = new JSONObject();
        result.put("number", number);
        result.put(Constants.COMIC_TITLE, title);
        return result.toString();
    }
}
