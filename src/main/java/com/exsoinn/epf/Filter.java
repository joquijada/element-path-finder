package com.exsoinn.epf;


import java.util.*;

/**
 * Immutable class to encapsulate a {@code Filter} used when searching an element. The sole factory method used to build
 * the immutable {@code Filter} instance accepts a filter {@link String} that follows the following format only:
 *
 * key1=val1;key2=val2;key3=val3
 *
 * If the filter {@link String} can't be successfully parsed for whatever reason, {@link IllegalArgumentException}
 * gets thrown.
 *
 * This class is a specialized implementation of {@link Map} interface, backed by a {@link Map} instance to which
 * all {@link Map} operations are forwarded. It will throw {@link UnsupportedOperationException}
 * on any method invocation of {@link Map} that would otherwise modify the underlying {@link Map}, because this class is
 * immutable.
 *
 * Created by QuijadaJ on 5/3/2017.
 */
public class Filter implements Map<String, String> {
    private final Map<String, String> m = new HashMap<>();
    private static final String sampleFormat = "key1=val1;key2=val2;key3=val3";

    private Filter(String pFilter) {
        parseFilter(pFilter);
    }


    public static Filter valueOf(String pFilter)
            throws IllegalArgumentException {
        return new Filter(pFilter);
    }


    public static Filter fromMap(Map<String, String> pFilterMap) {
        if (null == pFilterMap) {
            return null;
        }
        Set<Map.Entry<String, String>> entries = pFilterMap.entrySet();
        StringBuilder sb = new StringBuilder();
        entries.forEach(e -> {
            sb.append(e.getKey());
            sb.append("=");
            sb.append(e.getValue());
            sb.append(";");
        });

        sb.deleteCharAt(sb.length()-1);
        return Filter.valueOf(sb.toString());
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


    private void parseFilter(String pFilter)
            throws IllegalArgumentException {
        String[] tokens = pFilter.split(";");

        if (tokens.length == 0) {
            throw new IllegalArgumentException("Filter string " + pFilter
                    + " could not be parsed, check format and try again. Sample format is " + sampleFormat);
        }

        try {
            Arrays.stream(tokens).forEach(t -> {
                String[] vals = t.split("=");
                m.put(vals[0], vals[1]);
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Filter string " + pFilter
                    + " could not be parsed, check format and try again. Sample format is " + sampleFormat, e);
        }
    }
}