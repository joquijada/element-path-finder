package com.exsoinn.util.epf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.jcip.annotations.Immutable;

import java.util.*;

/**
 * Implementation of {@link AbstractContext} to operate on JSON structures, with the aid of third party
 * <a href="https://google.github.io/gson/apidocs/">google JSON API</a>.
 *
 * Created by QuijadaJ on 5/4/2017.
 */
@Immutable
class JsonContext extends AbstractContext {
    private final JsonElement je;


    /**
     * In order to keep our invariants true, this constructor defensively re-generates the JsonElement object. See
     * {@link JsonContext#generateBrandNewJsonElementObject(JsonElement)} for details.
     * @param pJsonElement - pJsonELement
     */
    JsonContext(JsonElement pJsonElement) {
        this(pJsonElement, false);
    }


    JsonContext(JsonElement pJsonElement, boolean pCreateCopy) {
        if (pCreateCopy) {
            je = generateBrandNewJsonElementObject(pJsonElement);
        } else {
            je = pJsonElement;
        }
    }


    /**
     * Takes the passed in {@code JsonElement} and generates a brand new object. This way the client code can't
     * break the invariants of this class.
     * This is done by converting the passed in {@link JsonElement} to string if it is a complex, and then invoking
     * {@link JsonParser#parse(String)} to generate a brand new {@code JsonElement}.
     * If the passed is {@code JsonElement} is a primitive then just return it as is,
     * because a primitive is already a {@code String}, and {@code String}'s are immutable in Java. Besides JSON
     * parser will not be able to parse primitives because they do not conform to JSON format.
     * @param pJsonElem - pJsonElem
     * @return - TODO
     */
    static JsonElement generateBrandNewJsonElementObject(JsonElement pJsonElem) {
        if (pJsonElem.isJsonPrimitive()) {
            return pJsonElem;
        } else {
            String jsonAsString;
            try {
                /*
                 * When calling JsonArray.getAsString(), the Google JSON API will "unwrap" from array if it's a
                 * single element array and it is a primitive element. We don't want that for our purposes; we
                 * still want it represented as an array even if just one element, hence the check below call
                 * toString() in "else" when JsonArray contains just one element, and not getAsString().
                 * See "https://google.github.io/gson/apidocs/com/google/gson/JsonArray.html#getAsString--"
                 * for reference.
                 */
                if (!pJsonElem.isJsonArray() || pJsonElem.getAsJsonArray().size() > 1) {
                    jsonAsString = pJsonElem.getAsString();
                } else {
                    jsonAsString = pJsonElem.toString();
                }
            } catch (Exception e) {
                jsonAsString = pJsonElem.toString();
            }
            JsonParser jp = new JsonParser();
            return jp.parse(jsonAsString);
        }
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
         * The Google JSON API says that this operation will not work for all element types,
         * therefore to make our lives easier, silently catch problems if any, and fallback to toString()
         * if things go awry.
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


    /**
     * TODO: Can perhaps return unmodifiable list of the same JsonArray entries. Would need to overload
     * TODO: constructor to pass flag that tells it not to generate a new JsonElement
     * @return - TODO
     * @throws IllegalStateException - TODO
     */
    @Override
    public List<Context> asArray() throws IllegalStateException {
        if (!je.isJsonArray()) {
            throw new IllegalStateException("Object is not a JSON array, therefore asArray() call is invalid: " + je);
        }
        List<Context> list = new ArrayList<>();
        JsonArray ja = je.getAsJsonArray();


        ja.iterator().forEachRemaining(e -> list.add(new JsonContext(e)));

        return Collections.unmodifiableList(list);
    }


    /**
     *
     * @param pElemName - pElemName
     * @return - TODO
     */
    @Override
    public boolean containsElement(String pElemName) {
        if (!je.isJsonObject()) {
            throw new IllegalStateException("Expected a JSON object, but found '" + je.getClass().getName()
                    + "', therefore containsElement() call is invalid. Element was: " + je);
        }

        return ((JsonObject)je).has(pElemName);
    }


    /**
     * If this is a {@link JsonObject}, return an {@link Set} of {@link Map.Entry} of the members
     * of this object.
     * @return - TODO
     * @throws IllegalStateException - TODO
     */
    @Override
    public Set<Map.Entry<String, Context>> entrySet() throws IllegalStateException {
        if (!je.isJsonObject()) {
            throw new IllegalStateException("Object is not an JSON object, therefore entrySet() call is invalid: " + je);
        }

        JsonObject jo = je.getAsJsonObject();
        Set<Map.Entry<String, JsonElement>> ents = jo.entrySet();
        Map<String, Context> newMap = new LinkedHashMap<>();
        /**
         * Defensively generate brand new JsonContext objects so that client cannot break
         * the invariants of this class
         */
        ents.forEach(e -> newMap.put(e.getKey(), new JsonContext(e.getValue())));
        return Collections.unmodifiableSet(newMap.entrySet());
    }

    @Override
    public Context memberValue(String pMemberName) throws IllegalStateException {
        if (!je.isJsonObject()) {
            throw new IllegalStateException("Object is not an JSON object, therefore memberValue() call is invalid: " + je);
        }

        return new JsonContext(je.getAsJsonObject().get(pMemberName));
    }

    @Override
    public boolean arrayContains(String pVal) throws IllegalStateException {
        if (!je.isJsonArray()) {
            throw new IllegalStateException("Object is not an JSON array, therefore arrayContains call is invalid: " + je);
        }

        for (Context c : asArray()) {
            if (pVal.equals(c.stringRepresentation())) {
                return true;
            }
        }
        return false;
    }



    JsonElement unwrap() {
        return je;
    }
}
