package com.exsoinn.epf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.*;

/**
 * Created by QuijadaJ on 5/4/2017.
 */
class JsonContext extends AbstractContext {
    private final JsonElement je;

    JsonContext(JsonElement pJsonElement) {
        je = pJsonElement;
    }

    @Override
    public boolean isPrimitive() {
        return je.isJsonPrimitive();
    }

    @Override
    public boolean isRecursible() {
        return je.isJsonObject();
    }

    @Override
    public boolean isArray() {
        return je.isJsonArray();
    }

    @Override
    public Context entryFromArray(int pIdx)
            throws IllegalStateException {
        if (!isArray()) {
            throw new IllegalStateException("This is not an array element, " + je);
        }

        return new JsonContext(je.getAsJsonArray().get(pIdx));
    }

    @Override
    public String stringRepresentation() {
        /*
         * The Google JSOn API says that this operation will not work for all element types,
         * therefore to make our lives easier, silently catch problems if any, and resort to using toString().
         */
        try {
            return je.getAsString();
        } catch (Exception e) {
            return je.toString();
        }
    }


    @Override
    public String toString() {
        return je.toString();
    }

    @Override
    public List<Context> asArray() throws IllegalStateException {
        if (!je.isJsonArray()) {
            throw new IllegalStateException("Object is not an JSON array, therefore asArray() call is invalid: " + je);
        }
        List<Context> list = new ArrayList<>();
        JsonArray ja = je.getAsJsonArray();
        ja.iterator().forEachRemaining(e -> list.add(new JsonContext(e)));

        return list;
    }

    @Override
    public boolean containsElement(String pElemName) {
        if (!je.isJsonObject()) {
            throw new IllegalStateException("Object is not an JSON object, therefore containsElement() call is invalid: " + je);
        }

        return ((JsonObject)je).has(pElemName);
    }

    @Override
    public Set<Map.Entry<String, Context>> entrySet() throws IllegalStateException {
        if (!je.isJsonObject()) {
            throw new IllegalStateException("Object is not an JSON object, therefore entrySet() call is invalid: " + je);
        }

        JsonObject jo = je.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> ents = jo.entrySet();
        Map<String, Context> newMap = new HashMap<>();
        ents.forEach(e -> newMap.put(e.getKey(), new JsonContext(e.getValue())));
        return newMap.entrySet();
    }

    @Override
    public Context memberValue(String pMemberName) throws IllegalStateException {
        if (!je.isJsonObject()) {
            throw new IllegalStateException("Object is not an JSON object, therefore memberValue() call is invalid: " + je);
        }

        return new JsonContext(je.getAsJsonObject().get(pMemberName));
    }



    @Override
    boolean shouldExcludeFromResults(Context pElem, Filter pFilter)
            throws IllegalArgumentException {
        if (null == pFilter) {
            return false;
        }

        Set<Map.Entry<String, String>> filterEntries = pFilter.entrySet();
        for (Map.Entry<String, String> filterEntry : filterEntries) {
            boolean elemToFilterOnIsNested = filterEntry.getKey().indexOf('.') >= 0;

            if (elemToFilterOnIsNested) {
                /*
                 * Handles case when the value we want to filter on is buried one or more levels deeper than found the
                 * found element.
                 * We leverage findElement(), which accepts a dot (.) separated element search path. Also, we support only filtering
                 * on primitive values, therefore assume that the found element will be a single name value pair.
                 * If the path of the filter element is not found, IllegalArgumentException is thrown.
                 */
                SearchPath elemSearchPath = SearchPath.valueOf(filterEntry.getKey());
                if (!pElem.containsElement(elemSearchPath.get(0))) {
                    return true;
                }
                Map<String, String> filterElemFound = findElement(pElem, elemSearchPath, null, null, null, null);
                Set<Map.Entry<String, String>> entries = filterElemFound.entrySet();
                String filterVal = entries.iterator().next().getValue();
                if (null == filterVal) {
                    throw new IllegalArgumentException("The filter element value specified was not found off of this node: " +
                            filterEntry.getKey());
                }

                if (!filterVal.equals(filterEntry.getValue())) {
                    return true;
                }
            } else {
                Context elem = pElem.memberValue(filterEntry.getKey());
                if (null != elem && !elem.toString().equals(filterEntry.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    /*
     * Gets rid of elements in the result set that the caller did not request via pTargetElements
     */
    @Override
    Context filterUnwantedElements(Context pElem, TargetElements pTargetElems) {
        if (null == pTargetElems) {
            return pElem;
        }

        Set<Map.Entry<String, Context>> ents = pElem.entrySet();
        JsonObject jo = new JsonObject();
        ents.stream().filter(entry -> pTargetElems.contains(entry.getKey()))
                .forEach(entry -> jo.add(entry.getKey(), ((JsonContext) entry.getValue()).unwrap()));
        return new JsonContext(jo);
    }

    /*
     *
     * When the search results is a JSON object that contain just one name/value pair, and in case of arrays
     * only one JSON object with only one name/value pair, *and* the sole element name is in the pTargetElems Set, *and*
     * the value is a JSON primitive then the search results Map gets cleared, and this single name/value pair stored in results.
     * This was done to remove burden from client, to make it convenient for them where they can just blindly get
     * the key and value as-is from the search results map.
     */
    @Override
    void handleSingleValueFound(Map<String, String> pSearchRes,
                                                      Set<String> pTargetElems) {
        Set<Map.Entry<String, String>> entries = pSearchRes.entrySet();
        if (entries.size() != 1) {
            return;
        }

        String val = entries.iterator().next().getValue();
        JsonParser jsonParser = new JsonParser();

        try {
            final JsonElement elem = jsonParser.parse(val);
            JsonObject jo = null;
            if (elem.isJsonArray() && elem.getAsJsonArray().size() == 1) {
                JsonElement aryElem = elem.getAsJsonArray().iterator().next();
                if (aryElem.isJsonObject()) {
                    jo = (JsonObject) aryElem;
                }
            } else if (elem.isJsonObject() && elem.getAsJsonObject().entrySet().size() == 1) {
                jo = (JsonObject) elem;
            } else {
                return;
            }

            if (null != jo) {
                Map.Entry<String, JsonElement> entry = jo.entrySet().iterator().next();
                if (null != pTargetElems && pTargetElems.contains(entry.getKey()) && entry.getValue().isJsonPrimitive()) {
                    pSearchRes.clear();
                    pSearchRes.put(entry.getKey(), entry.getValue().getAsString());
                }
            }
        } catch (Exception e) {
            System.err.println("There was a problem: " + e);
        }
    }


    private JsonElement unwrap() {
        return je;
    }
}
