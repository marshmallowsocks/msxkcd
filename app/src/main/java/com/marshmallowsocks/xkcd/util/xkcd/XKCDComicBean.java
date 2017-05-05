package com.marshmallowsocks.xkcd.util.xkcd;

/**
 * Created by marshmallowsocks on 5/2/17.
 * A simple bean that describes the xkcd json object.
 */

public class XKCDComicBean {
    private String title;
    private String altText;
    private String imageUrl;
    private Integer number;
    private String date;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getNumber() {
        return number;
    }

    public void setNumber(Integer number) {
        this.number = number;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
}
