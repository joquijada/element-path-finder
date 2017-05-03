package com.exsoinn.epf;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by QuijadaJ on 5/3/2017.
 */
public class TargetElements {
    private final Set<String> list;
    private final static Map<String, TargetElements> cachedLists = new ConcurrentHashMap();

    private TargetElements(String pTargetElems) {
        list = parseTargetElements(pTargetElems);
    }



    public static TargetElements valueOf(String pTargetElems)
            throws IllegalArgumentException {
        TargetElements cachedList = cachedLists.get(pTargetElems);
        if (null == cachedList) {
            TargetElements newList = new TargetElements(pTargetElems);
            cachedList = cachedLists.putIfAbsent(pTargetElems, null);
            if (null == cachedList) {
                cachedList = newList;
            }
        }

        return cachedList;
    }


    private Set<String> parseTargetElements(String pTargetElems) throws IllegalArgumentException{
        Set<String> elems = new HashSet();
        String[] tokens = pTargetElems.split(",");
        if (null == tokens) {
            return null;
        }

        for (String t : tokens) {
            elems.add(t);
        }

        return elems;
    }

    public Set<String> getList() {
        return new HashSet(list);
    }
}
