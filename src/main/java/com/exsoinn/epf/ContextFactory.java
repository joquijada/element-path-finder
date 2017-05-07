package com.exsoinn.epf;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/**
 * Created by QuijadaJ on 5/3/2017.
 */
public enum ContextFactory {
    INSTANCE;


    public Context obtainContext(Object pData)
            throws IllegalArgumentException {
        JsonElement je = convertToJson(pData);
        if (null != je) {
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
