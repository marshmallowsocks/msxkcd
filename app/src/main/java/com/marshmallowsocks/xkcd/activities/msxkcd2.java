package com.marshmallowsocks.xkcd.activities;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.marshmallowsocks.xkcd.R;

import java.util.ArrayList;
import java.util.List;

import eminayar.com.cardhelper.HelperCardsLayout;
import eminayar.com.cardhelper.models.CardItem;

public class msxkcd2 extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_msxkcd2);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        HelperCardsLayout layout = (HelperCardsLayout) findViewById(R.id.cardHelper);

        List<CardItem> cardItems = new ArrayList<>();

        cardItems.add(new CardItem("Short Title With Image",
                "Description this can be some long text, " + "layout" + "will scale itself",
                R.drawable.approved));

        cardItems.add(new CardItem("Short Title With No Image",
                "Description this can be some long text, layout" + "will scale itself"));

        cardItems.add(new CardItem("Long title example to demonstrate users how can this textview can"
                + " be longer and longer with image",
                "Description this can be some long text, layout" + "will scale itself",
                R.drawable.fa_heart_on));

        cardItems.add(new CardItem("Long title example to demonstrate users how can this textview can"
                + " be longer and longer without image",
                "Description this can be some long text, layout" + "will scale itself"));

        cardItems.add(new CardItem("Very very long description example",
                "Description this can be some long text, layout"
                        + "will scale itself"
                        + "Description this can be some long text, layout"
                        + "will scale itself"
                        + "Description this can be some long text, layout"
                        + "will scale itself", R.drawable.approved));

        layout.setItems(cardItems);
        //layout.setOnCardClickListener(this);
        //layout.setOnCardLongClickListener(this);
    }

}
