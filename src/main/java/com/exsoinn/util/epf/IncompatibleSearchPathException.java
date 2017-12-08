package com.exsoinn.util.epf;

/**
 * Thrown when the {@link SearchPath} provided does not exactly match the {@link Context} being searched. More specifically,
 * if a node in the {@link SearchPath} is not found in the area of the {@link Context} object currently being search, then this
 * {@code Exception} is used to describe that scenario.
 *
 * Created by QuijadaJ on 7/29/2017.
 */
public class IncompatibleSearchPathException extends Exception {
    private final SearchPath searchPath;
    private final String node;
    private final Context context;

    public IncompatibleSearchPathException(SearchPath pSearchPath, String pNode, Context pCtx) {
        super("Did not find expected path node '" + pNode
                + "' in the current part of the element currently being processed/searched. Check that the "
                + "search path is correct: " + pSearchPath.toString()
                + ". Element is " + pCtx.stringRepresentation());
        searchPath = pSearchPath;
        node = pNode;
        context = pCtx;
    }

    /*
     * Getters
     */
    public SearchPath getSearchPath() {
        return searchPath;
    }

    public String getNode() {
        return node;
    }

    public Context getContext() {
        return context;
    }
}
