package com.marshmallowsocks.xkcd.util.http;

import android.content.Context;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by marshmallowsocks on 4/14/2017.
 * MSRequestQueue. Singleton class that implements a volley queue.
 */

public class MSRequestQueue {
    /*
    Singleton Marshmallowsocks request queue. Uses volley.
     */

    private static MSRequestQueue msInstance;
    private RequestQueue msRequestQueue;

    private MSRequestQueue(Context context) {
        msRequestQueue = getRequestQueue(context);
    }

    public static synchronized MSRequestQueue getInstance(Context context) {
        if (msInstance == null) {
            msInstance = new MSRequestQueue(context);
        }
        return msInstance;
    }

    private RequestQueue getRequestQueue(Context msContext) {
        if (msRequestQueue == null) {
            msRequestQueue = Volley.newRequestQueue(msContext.getApplicationContext());
        }
        return msRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, Context context) {
        getRequestQueue(context).add(req);
    }
}
