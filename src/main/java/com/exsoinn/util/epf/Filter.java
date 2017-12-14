package com.exsoinn.util.epf;


import java.util.*;
import java.util.stream.Collectors;

/**
 * Unmodifiable class to encapsulate a {@code Filter} used when searching an element off of a {@link Context} object. The
 * sole factory method used to build a {@code Filter} instance accepts a filter {@link String} that follows the following
 * format only:
 *
 * key1=val1;key2=val2;key3=val3
 *
 * If the filter {@link String} can't be successfully parsed for whatever reason, {@link IllegalArgumentException}
 * gets thrown.
 *
 * This class is a specialized implementation of {@link Map} interface, backed by a {@link Map} instance to which
 * all {@link Map} operations are forwarded (composition design pattern). It will throw {@link UnsupportedOperationException}
 * on any method invocation of {@link Map} that would otherwise modify the underlying {@link Map}, because instances of
 * this class aremeant to be read-ony.
 *
 * Created by QuijadaJ on 5/3/2017.
 */
public final class Filter implements Map<String, String> {
    private static final String sampleFormat = "key1=val1;key2=val2;key3=val3";
    private final Map<String, String> m = new HashMap<>();
    private static final String BLANK = "__BLANK__";

    private Filter(String pFilterStr) {
        parseFilter(pFilterStr);
    }


    public static Filter valueOf(String pFilter)
            throws IllegalArgumentException {
        if (null == pFilter
                || pFilter.equalsIgnoreCase("")
                || pFilter.equalsIgnoreCase(SelectionCriteria.SEARCH_CRITERIA_NULL)) {
            return null;
        }

        return new Filter(pFilter);
    }


    public static Filter fromMap(Map<String, String> pFilterMap) {
        if (null == pFilterMap) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(pFilterMap.entrySet().parallelStream().map(Map.Entry::toString)
                .collect(Collectors.joining(";", "", "")));

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

    @Override
    public String toString() {
        return m.entrySet().stream().map(Map.Entry::toString).collect(Collectors.joining(";"));
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
                m.put(vals[0], vals.length == 1 ? BLANK : vals[1]);
            });
        } catch (Exception e) {
            throw new IllegalArgumentException("Filter string " + pFilter
                    + " could not be parsed, check format and try again. Sample format is " + sampleFormat, e);
        }
    }
}