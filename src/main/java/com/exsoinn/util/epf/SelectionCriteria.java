package com.exsoinn.util.epf;


import net.jcip.annotations.Immutable;

import java.util.regex.Pattern;

/**
 * Encapsulates a all te information necessary to find data in a {@link Context}. You can build {@link SelectionCriteria}
 * objects by passing a properly formatted String, and then invoking factory method
 * {@link SelectionCriteria#valueOf(String)}. The format is a pipe delimited string with exacty 3 tokens, one each corresponding
 * to, and in the same order, to a: {@link SearchPath}, {@link Filter} and {@link TargetElements}.
 * Refer to each individual class for details on the format for each.
 *
 * Created by QuijadaJ on 5/18/2017.
 */
@Immutable
public final class SelectionCriteria {
    private final SearchPath searchPath;
    private final Filter filter;
    private final TargetElements targetElements;
    private static final String sampleFormat = "node1.node2.node3[1]|key1=val1;key2=val2|elem1,elem2";
    public final static String SEARCH_CRITERIA_DELIM = "||";
    public final static String SEARCH_CRITERIA_NULL = "null";


    private SelectionCriteria(String pStr) {
        String[] tokens = pStr.split(Pattern.quote(SEARCH_CRITERIA_DELIM));
        if (null == tokens || tokens.length != 3) {
            throw new IllegalArgumentException(this.getClass().getName() + " string " + pStr
                    + " could not be parsed, check format and try again. Sample format is " + sampleFormat
                    + ". Selection criteria must contain 3 tokens. You can pass "
                    + SEARCH_CRITERIA_NULL + " for blank tokens.");
        }

        try {
            if (tokenIsNull(tokens[0])) {
                throw new IllegalArgumentException("At least search path must be provided: " + pStr);
            }
            searchPath = SearchPath.valueOf(tokens[0]);
            filter = tokenIsNull(tokens[1]) ? null : Filter.valueOf(tokens[1]);
            targetElements = tokenIsNull(tokens[2]) ? null : TargetElements.valueOf(tokens[2]);
        } catch (Exception e) {
            throw new IllegalArgumentException("SearchCriteria string " + pStr
                    + " could not be parsed, check format and try again. Sample format is " + sampleFormat, e);
        }
    }


    private SelectionCriteria(SearchPath pSearchPath, Filter pFilter, TargetElements pTargeElems) {
        searchPath = pSearchPath;
        filter = pFilter;
        targetElements = pTargeElems;
    }

    private boolean tokenIsNull(String pToken) {
        return SEARCH_CRITERIA_NULL.equalsIgnoreCase(pToken);
    }

    public static SelectionCriteria valueOf(String pStr) {
        return new SelectionCriteria(pStr);
    }

    public static SelectionCriteria fromObjects(SearchPath pSearchPath, Filter pFilter, TargetElements pTargeElems) {
        return new SelectionCriteria(pSearchPath, pFilter, pTargeElems);
    }


    /**
     * Replaces special characters with string parse-friendly substitutes. By parse-friendly, it means removing
     * characters that may have special meaning depending on who the consumer is, for example a regular expression
     * engine, a JSON/XML parser, etc... These special characters if not escape or removed will error out
     * when processed by some consumers. This convenience method attempts to take care of that conversion
     * for you.
     *
     * @return - {@code SelectionCriteria} string with special characters converted to some string substitute.
     */
    public String specialCharactersConverted() {
        String sc = toString();
        sc = sc.replaceAll(Pattern.quote("||"), "__DP__");
        sc = sc.replaceAll(Pattern.quote(";"), "__SC__");
        sc = sc.replaceAll(Pattern.quote("="), "__EQ__");
        sc = sc.replaceAll(Pattern.quote("["), "__SBO__");
        sc = sc.replaceAll(Pattern.quote("]"), "__SBC__");

        return sc;
    }


    public SearchPath getSearchPath() {
        return searchPath;
    }

    public TargetElements getTargetElements() {
        return targetElements;
    }

    public Filter getFilter() {
        return filter;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getSearchPath());
        sb.append(SEARCH_CRITERIA_DELIM);
        sb.append(null == getFilter() ? "NULL" : getFilter());
        sb.append(SEARCH_CRITERIA_DELIM);
        sb.append(null == getTargetElements() ? "NULL" : getTargetElements());

        return sb.toString();
    }

}
