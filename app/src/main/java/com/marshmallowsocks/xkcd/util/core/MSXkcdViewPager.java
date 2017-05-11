package com.marshmallowsocks.xkcd.util.core;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;

/**
 * Created by sriva on 5/9/2017.
 * asd
 */

public class MSXkcdViewPager extends ViewPager {

    private boolean enabled;

    public MSXkcdViewPager(Context context) {
        super(context);
        this.enabled = true;
    }
    public MSXkcdViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.enabled = true;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (this.enabled) {
            return super.onTouchEvent(event);
        }

        return false;
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
        return this.enabled && super.onInterceptTouchEvent(event);

    }

    public void setPagingEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
