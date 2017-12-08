package com.exsoinn.util.epf;

import com.exsoinn.util.DnbBusinessObject;

import java.util.*;

/**
 * Created by QuijadaJ on 5/3/2017.
 */
public final class SearchResult implements Map<String, Context>, DnbBusinessObject {

    private final Map<String, Context> m;

    private SearchResult(Map<String, Context> pResult) {
        /*
         * Defensively copying passed in Map to enforce immutability.
         */
        m = new HashMap<>(pResult);
    }

    public Context firstResult() {
        if (null == m || m.isEmpty()) {
            return null;
        }
        return m.entrySet().iterator().next().getValue();
    }

    public static SearchResult emptySearchResult() {
        return new SearchResult(Collections.emptyMap());
    }

    public static SearchResult createSearchResult(Map<String, Context> pResult) {
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
    public Context get(Object pKey) {
        return m.get(pKey);
    }

    @Override
    public Context put(String pKey, Context pValue) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Context remove(Object pKey) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(Map<? extends String, ? extends Context> pMap) {
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
    public Collection<Context> values() {
        return new ArrayList<>(m.values());
    }

    @Override
    public Set<Entry<String, Context>> entrySet() {
        return new HashSet<>(m.entrySet());
    }

    @Override
    public String toString() {
        return m.toString();
    }


}
