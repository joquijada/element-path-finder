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
     * When the search results is a JSON object that contains just one name/value pair, and in case of arrays
     * only one JSON object with only one name/value pair, *and* the sole element name is in the pTargetElems Set, *and*
     * the value is a JSON primitive then the search results Map gets cleared, and this single name/value pair stored in results.
     * This was done to remove burden from client, to make it convenient for them where they can just blindly get
     * the key and value as-is from the search results map.
     */
    @Override
    void handleSingleComplexObjectFound(Map<String, Context> pSearchRes,
                                Set<String> pTargetElems) {
        if (null == pTargetElems || pTargetElems.isEmpty()) {
            return;
        }
        try {
            Set<Map.Entry<String, Context>> entries = pSearchRes.entrySet();
            if (entries.size() != 1) {
                return;
            }

            final Context elem = entries.iterator().next().getValue();
            Context ctx = null;
            if (elem.isArray() && elem.asArray().size() == 1) {
                Context aryElem = elem.asArray().iterator().next();
                if (aryElem.isRecursible()) {
                    ctx = aryElem;
                }
            } else if (elem.isRecursible() && elem.entrySet().size() == 1) {
                ctx = elem;
            } else {
                return;
            }

            if (null != ctx) {
                pSearchRes.clear();
                Set<Map.Entry<String, Context>> ctxEntSet = ctx.entrySet();

                for (Map.Entry<String, Context> entry : ctxEntSet) {
                    if (null != pTargetElems && pTargetElems.contains(entry.getKey())
                            && entry.getValue().isPrimitive()) {
                        pSearchRes.put(entry.getKey(), entry.getValue());
                    }
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
