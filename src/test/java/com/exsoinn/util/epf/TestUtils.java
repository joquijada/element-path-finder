package com.exsoinn.util.epf;

import org.json.JSONObject;
import org.json.XML;

/**
 * Created by QuijadaJ on 5/5/2017.
 */
public class TestUtils {
    /**
     *
     * @param pXml
     * @return
     */
    public static String convertXmlToJson(String pXml) {
        JSONObject json = convertXmlToJsonObject(pXml);
        return json.toString();
    }

    public static JSONObject convertXmlToJsonObject(String pXml) {
        return XML.toJSONObject(pXml);
    }
}
