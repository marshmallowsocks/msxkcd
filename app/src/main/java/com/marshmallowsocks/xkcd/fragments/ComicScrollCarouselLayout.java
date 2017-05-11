package com.marshmallowsocks.xkcd.fragments;

import android.content.Context;
import android.graphics.Canvas;
import android.support.constraint.ConstraintLayout;
import android.util.AttributeSet;

/**
 * Created by sriva on 5/10/2017.
 * create a carousel effect
 */

public class ComicScrollCarouselLayout extends ConstraintLayout {
    private float mScale = 1.0f;
    public ComicScrollCarouselLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public ComicScrollCarouselLayout(Context context) {
        super(context);
    }

    public void setScaleBoth(float scale) {
        this.mScale = scale;
        this.invalidate();    // If you want to see the mScale every time you set
        // mScale you need to have this line here,
        // invalidate() function will call onDraw(Canvas)
        // to redraw the view for you
    }

    @Override
    protected void onDraw(Canvas canvas) {
        // The main mechanism to display mScale animation, you can customize it
        // as your needs
        int w = this.getWidth();
        int h = this.getHeight();
        canvas.scale(mScale, mScale, w / 2, h / 2);

        super.onDraw(canvas);
    }
}
