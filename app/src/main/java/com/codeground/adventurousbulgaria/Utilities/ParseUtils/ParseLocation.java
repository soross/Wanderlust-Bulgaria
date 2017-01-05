package com.codeground.adventurousbulgaria.Utilities.ParseUtils;

import com.parse.ParseClassName;
import com.parse.ParseFile;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseRelation;

@ParseClassName("Location")
public class ParseLocation extends ParseObject {
    public String getName() {
        return getString("name");
    }

    public String getDescription() {
        return getString("description");
    }

    public String getCity() {
        return getString("city");
    }

    public ParseGeoPoint getLocation(){
        return getParseGeoPoint("location");
    }

    public ParseFile getIcon(){
        return getParseFile("icon");
    }

    public ParseFile getPhoto(){
        return getParseFile("photo");
    }

    public String getCategory() { return getString("category"); }

    public ParseRelation getComments() { return getRelation("comments"); }

}
