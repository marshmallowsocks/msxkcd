package com.marshmallowsocks.xkcd.fragments;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.OnScaleChangedListener;
import com.github.chrisbanes.photoview.PhotoView;
import com.marshmallowsocks.xkcd.R;
import com.marshmallowsocks.xkcd.activities.msxkcd;
import com.marshmallowsocks.xkcd.util.constants.Constants;
import com.marshmallowsocks.xkcd.util.core.MSXkcdDatabase;
import com.marshmallowsocks.xkcd.util.http.MSRequestQueue;
import com.marshmallowsocks.xkcd.util.msxkcd.XKCDComicBean;

import org.json.JSONException;
import org.json.JSONObject;

public class ComicFragment extends Fragment {

    private MSRequestQueue msRequestQueue;
    private XKCDComicBean currentComic;
    private Integer which;
    private float originalScale = 1;
    private float errorMargin = 0.1f; //scale > 1.1 or < 0.9 is blocked.

    public ComicFragment() {
    }

    public void setComicContext(int which) {
        this.which = which;

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        msRequestQueue = MSRequestQueue.getInstance(context);
        if(which == null) {
            which = 1;
        }
        getComicData(which.toString());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_msxkcd, container, false);
        if(currentComic != null) {
            setupView(rootView);
        }
        float scale = this.getArguments().getFloat("scale");
        ((ComicScrollCarouselLayout)rootView).setScaleBoth(scale);
        return rootView;
    }
    private void setComicData() {
        if(getView() != null) {
            setupView(getView());
        }
    }
    private void setupView(final View rootView) {
        final TextView altText = (TextView) rootView.findViewById(R.id.altText);
        final TextView metadata = (TextView) rootView.findViewById(R.id.metadata);
        final TextView comicTitle = (TextView) rootView.findViewById(R.id.comicTitle);
        final Button closeButton = (Button) rootView.findViewById(R.id.closeOverlay);
        final Button explainButton = (Button) rootView.findViewById(R.id.explainButton);
        final PhotoView comicHolder = (PhotoView) rootView.findViewById(R.id.comicHolder);

        comicHolder.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                ((msxkcd)getActivity()).toggleAltText();
                return false;
            }
        });

        if(currentComic.getImageUrl().endsWith(".gif")) {
            Glide.with(getContext())
                 .load(currentComic.getImageUrl())
                 .asGif()
                 //.thumbnail(Glide.with(getContext()).load(Constants.LOADING_URL).asGif())
                 .into(comicHolder);
        }
        else {
            Glide.with(getContext())
                 .load(currentComic.getImageUrl())
                 //.thumbnail(Glide.with(this).load(Constants.LOADING_URL)).crossFade()
                 .into(comicHolder);
        }

        comicHolder.setOnScaleChangeListener(new OnScaleChangedListener() {
            @Override
            public void onScaleChange(float scaleFactor, float focusX, float focusY) {
                originalScale = originalScale * scaleFactor;
                ((msxkcd)getActivity()).toggleViewPager(Math.abs(1.0f - originalScale) < errorMargin);
            }
        });

        comicTitle.setText(currentComic.getTitle());
        altText.setText(currentComic.getAltText());
        metadata.setText("#" + currentComic.getNumber() + "\n" + currentComic.getDate());

        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAltText(rootView);
            }
        });

        explainButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(String.format(Constants.EXPLAIN_URL, which)));
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                i.setPackage("com.android.chrome");
                try {
                    startActivity(i);
                } catch (ActivityNotFoundException e) {
                    // Chrome is probably not installed
                    // Try with the default browser
                    i.setPackage(null);
                    startActivity(i);
                }
            }
        });
    }
    public void toggleAltText(View rootView) {
        final ConstraintLayout componentLayout = (ConstraintLayout) rootView.findViewById(R.id.componentHolder);
        final TextView altText = (TextView) rootView.findViewById(R.id.altText);
        final TextView metadata = (TextView) rootView.findViewById(R.id.metadata);
        final Button closeButton = (Button) rootView.findViewById(R.id.closeOverlay);
        final Button explainButton = (Button) rootView.findViewById(R.id.explainButton);
        final PhotoView comicHolder = (PhotoView) rootView.findViewById(R.id.comicHolder);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            componentLayout.setBackgroundResource(R.color.colorPrimaryLight);
            altText.setElevation(0.0f);
        }
        comicHolder.setAlpha(1.0f);
        ((msxkcd)getActivity()).toggleButtonBar(true);
        altText.setVisibility(View.GONE);
        closeButton.setVisibility(View.GONE);
        explainButton.setVisibility(View.GONE);
        metadata.setVisibility(View.GONE);
    }

    private void getComicData(String number) {
        String url;
        final MSXkcdDatabase db = new MSXkcdDatabase(getActivity());
        if(db.contains(Integer.parseInt(number))) {
            currentComic = db.getComic(Integer.parseInt(number));
            JSONObject representation = new JSONObject();
            try {
                representation.put(Constants.COMIC_TITLE, currentComic.getTitle());
                representation.put(Constants.COMIC_EXTRA, currentComic.getAltText());
                representation.put(Constants.COMIC_INDEX, currentComic.getNumber());
                representation.put(Constants.COMIC_URL, currentComic.getImageUrl());
                Integer day, month, year;
                String date = currentComic.getDate();
                day = Integer.parseInt(date.split("-")[1]);
                month = Integer.parseInt(date.split("-")[0]);
                year = Integer.parseInt(date.split("-")[2]);

                representation.put(Constants.COMIC_DAY, day);
                representation.put(Constants.COMIC_MONTH, month);
                representation.put(Constants.COMIC_YEAR, year);
            }
            catch (JSONException e) {
                e.printStackTrace();
            }
            currentComic.setJsonRepresentation(representation);
            setComicData();
            return;
        }

        url = String.format(Constants.URL_PATTERN, number);
        StringRequest strRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response)
                    {
                        try {
                            JSONObject result = new JSONObject(response);
                            String date;
                            currentComic = new XKCDComicBean();
                            currentComic.setTitle(result.getString(Constants.COMIC_TITLE));
                            currentComic.setAltText(result.getString(Constants.COMIC_EXTRA).toUpperCase());
                            currentComic.setImageUrl(result.getString(Constants.COMIC_URL));
                            currentComic.setNumber(result.getInt(Constants.COMIC_INDEX));
                            currentComic.setJsonRepresentation(result);

                            date = result.getString(Constants.COMIC_MONTH);
                            date += "-" + result.getString(Constants.COMIC_DAY);
                            date += "-" + result.getString(Constants.COMIC_YEAR);

                            currentComic.setDate(date);

                            if(!db.contains(currentComic.getNumber())) {
                                if (!(db.addNewMetadata(currentComic))) {
                                    Toast.makeText(getActivity(), "An error occurred with the database", Toast.LENGTH_SHORT);
                                }
                            }
                        }
                        catch(JSONException e) {
                            Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        setComicData();
                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error)
                    {
                        Toast.makeText(getActivity(), error.toString(), Toast.LENGTH_SHORT).show();
                    }
                });
        msRequestQueue.addToRequestQueue(strRequest, getActivity());
    }
}