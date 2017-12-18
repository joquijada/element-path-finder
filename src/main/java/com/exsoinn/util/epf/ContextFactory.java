package com.exsoinn.util.epf;

import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import org.json.JSONObject;
import org.json.XML;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Factory for building {@link Context}'s from passed in data. Currently any valid JSON or XML string can be
 * converted to a Context.
 * TODO: Enhance to suppert Nashorn ECMA JSON objects, do what client code does not have to do JSON.stringify() on those???
 * Created by QuijadaJ on 5/3/2017.
 */
public enum ContextFactory {
    INSTANCE;
    private static final String CLASS_NAME_JSON_CTX = "com.exsoinn.util.epf.JsonContext";
    private static final String CLASS_NAME_MUT_JSON_CTX = "com.exsoinn.util.epf.MutableJsonContext";


    /**
     * Factory method which will attempt to return a {@link Context} by inspecting the passed in pData. The
     * pData passed in must be in a format that will be recognized, otherwise {@link IllegalArgumentException}
     * exception gets thrown. At this time only JSON format is supported, and the JSON is built by relying
     * on <a href="https://google.github.io">Google JSON API</a>. Plans are to add support for XML format using a similar,
     * already existing 3rd party XML API.
     *
     * @param pData - The data from which a {@code Context} will be constructed.
     * @return - Data of some format wrapped inside a {@link Context} object.
     * @throws IllegalArgumentException - TODO
     */
    public static Context obtainContext(Object pData) throws IllegalArgumentException {
        return obtainContext(pData, CLASS_NAME_JSON_CTX);
    }


    /**
     * Sames as {@link ContextFactory#obtainContext(Object)}, but returns ab object of type {@link MutableContext}.
     * @param pData - pData
     * @return - TODO
     * @throws IllegalArgumentException - TODO
     */
    public static MutableContext obtainMutableContext(Object pData) throws IllegalArgumentException {
        return (MutableContext) obtainContext(pData, CLASS_NAME_MUT_JSON_CTX);
    }


    private static Context obtainContext(Object pData, String pClassName)
            throws IllegalArgumentException {
        JsonElement je = convertToJson(pData);
        String xmlToJsonStr;

        boolean formatIsSupported = false;
        try {
            Class<? extends JsonContext> jsonCtxClass = (Class<? extends JsonContext>) Class.forName(pClassName);
            Constructor<? extends JsonContext> jsonCtxClassConstructor =
                    jsonCtxClass.getDeclaredConstructor(JsonElement.class);
            jsonCtxClassConstructor.setAccessible(true);
            if (null != je) {
                formatIsSupported = true;
            } else if (pData instanceof List && null != (je = convertToJson(pData.toString()))) {
                formatIsSupported = true;
            } else if (pData instanceof String && null != (xmlToJsonStr = tryXml((String) pData))
                    && null != (je = convertToJson(xmlToJsonStr))) {
                /*
                 * Because we got XML, (I.e. the tryXml() method call did not return NULL), check if we still
                 * ended up with empty JSON, in which case we will throw error
                 */
                if (je.isJsonPrimitive() || (je.isJsonArray() && je.getAsJsonArray().size() != 0)
                        || (je.isJsonObject() && je.getAsJsonObject().size() != 0)) {
                    formatIsSupported = true;
                }

            }
            if (formatIsSupported) {
                return jsonCtxClassConstructor.newInstance(je);
            } else {
                throw new IllegalArgumentException("Passed in argument not recognized as one of the supported formats: "
                        + pData + "\nIf format is supported, check that it is valid.");
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException(e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException(e);
        } catch (InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }


    /*
     * See if we get lucky and this is XML
     */
    private static String tryXml(String pData) {
        try {
            JSONObject jo = XML.toJSONObject(pData);
            if (null != jo) {
                return jo.toString();
            }
        } catch (Exception e) {
            return null;
        }

        return null;
    }

    /*
     * This could have been made to return a boolean, but then the calling code would have to again
     * do a JsonParser.parse() operation, which is unnecessary.
     */
    private static JsonElement convertToJson(Object pData) {
        try {
            JsonParser jp = new JsonParser();
            if (pData instanceof JsonElement) {
                return JsonContext.generateBrandNewJsonElementObject((JsonElement) pData);
            } else if (pData instanceof String) {
                return jp.parse((String) pData);
            }
        } catch (JsonParseException e) {
            return null;
        }

        return null;
    }

}
