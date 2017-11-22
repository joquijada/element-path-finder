package com.exsoinn.util.epf;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Created by QuijadaJ on 5/24/2017.
 */
public class MutableJsonContext extends JsonContext implements MutableContext {
    MutableJsonContext(JsonElement pJsonElement) {
        super(pJsonElement);
    }

    @Override
    public void addMember(String pName, Context pContext) {
        if (!isRecursible()) {
            throw new IllegalStateException("This operation not supported for elements that are not complex.");
        }

        // Need the underlying JSON elements of both sender and receiving objects in order
        // to be able to invoke GSON's API to add to it
        JsonElement srcJson = ((JsonContext) pContext).unwrap();
        JsonObject targetJson = (JsonObject) unwrap();
        targetJson.add(pName, srcJson);
    }

    @Override
    public void addEntryToArray(Context pEntry) {
        if (!isArray()) {
            throw new IllegalStateException("This operation not supported for elements that are not array.");
        }

        JsonElement srcJson = ((JsonContext) pEntry).unwrap();
        JsonArray targetJson = (JsonArray) unwrap();
        targetJson.add(srcJson);
    }
}
