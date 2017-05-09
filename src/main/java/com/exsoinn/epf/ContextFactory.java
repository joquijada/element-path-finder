package com.exsoinn.epf;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import java.util.List;

/**
 * Created by QuijadaJ on 5/3/2017.
 */
public enum ContextFactory {
    INSTANCE;


    /**
     * Factory method which will attempt to return a {@link Context} by inspecting the passed in {@param pData}. The
     * {@param pData} passed in must be in a format that will be recognized, otherwise {@link IllegalArgumentException}
     * exception gets thrown. At this time only JSON format is supported, and the JSON is built by relying
     * on <a href="https://google.github.io">Google JSON API</a>. Plans are to add support for XML format using a similar,
     * already existing 3rd party XML API.
     *
     * @param pData - The data from which a {@code Context} will be constructed.
     * @return - Data of some format wrapped inside a {@link Context} object.
     * @throws IllegalArgumentException
     */
    public Context obtainContext(Object pData)
            throws IllegalArgumentException {
        JsonElement je = convertToJson(pData);
        if (null != je) {
            return new JsonContext(je);
        } else if (pData instanceof List && null != (je = convertToJson(pData.toString()))) {
            return new JsonContext(je);
        } else {
            throw new IllegalArgumentException("Passed in argument not recognized as one of the supported formats: " + pData);
        }
    }

    /*
     * This could have been made to return a boolean, but then the calling code would have to again
     * do a JsonParser.parse() operation, which is unnecessary.
     */
    private JsonElement convertToJson(Object pData) {
        try {
            JsonParser jp = new JsonParser();
            if (pData instanceof JsonElement) {
                return (JsonElement) pData;
            } else if (pData instanceof String) {
                return jp.parse((String) pData);
            }
        } catch (JsonParseException e) {
            System.err.println("Problem parsing string " + pData + ", it could be because it's not valid JSON: " + e);
        }

        return null;
    }

}
