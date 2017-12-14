package com.exsoinn.util.epf;

/**
 * Created by QuijadaJ on 5/24/2017.
 */
public interface MutableContext extends Context {
    void addMember(String pName, Context pContext);

    void addEntryToArray(Context pEntry);
}
