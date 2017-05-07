package com.exsoinn.epf;

import java.util.*;

/**
 * Created by QuijadaJ on 5/3/2017.
 */
public class SearchResult implements Map<String, String> {

    private final Map<String, String> m;

    private SearchResult(Map<String, String> pResult) {
        /*
         * Defensively copying passed in Map to enforce immutability.
         */
        m = new HashMap<>(pResult);
    }

    public static SearchResult createSearchResult(Map<String, String> pResult) {
        return new SearchResult(pResult);
    }

    @Override
    public int size() {
        return m.size();
    }

    @Override
    public boolean isEmpty() {
        return m.isEmpty();
    }

    @Override
    public boolean containsKey(Object pKey) {
        return m.containsKey(pKey);
    }

    @Override
    public boolean containsValue(Object pValue) {
        return m.containsValue(pValue);
    }

    @Override
    public String get(Object pKey) {
        return m.get(pKey);
    }

    @Override
    public String put(String pKey, String pValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String remove(Object pKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends String> pMap) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<String> keySet() {
        return new HashSet<>(m.keySet());
    }

    @Override
    public Collection<String> values() {
        return new ArrayList<>(m.values());
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        return new HashSet<>(m.entrySet());
    }


}
