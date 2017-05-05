package com.marshmallowsocks.xkcd.util.whatif;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by vatsa on 5/5/17.
 */

public class WhatIfBean {
    private String type;
    private String body;
    private String imageUrl;
    private List<Citation> citations;

    public class Citation {
        private String citationNumber;
        private String citationBody;

        public Citation(String number, String body) {
            citationNumber = number;
            citationBody = body;
        }

        public String getCitationNumber() {
            return citationNumber;
        }

        public void setCitationNumber(String citationNumber) {
            this.citationNumber = citationNumber;
        }

        public String getCitationBody() {
            return citationBody;
        }

        public void setCitationBody(String citationBody) {
            this.citationBody = citationBody;
        }
    }
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<Citation> getCitations() {
        return citations;
    }

    public boolean hasCitations() {

        return citations != null && citations.size() != 0;
    }

    public void addCitation(String citationNumber, String citationBody) {
        if(citations == null) {
            citations = new ArrayList<>();
        }

        citations.add(new Citation(citationNumber, citationBody));
    }
}
