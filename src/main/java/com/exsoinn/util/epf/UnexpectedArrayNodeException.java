package com.exsoinn.util.epf;

/**
 * Created by QuijadaJ on 9/5/2017.
 */
public class UnexpectedArrayNodeException extends Exception {
    private final SearchPath searchPath;
    private final String node;
    private final Context context;

    public UnexpectedArrayNodeException(SearchPath pSearchPath, String pNode, Context pCtx) {
        super("Found an array element, yet the search path did not tell me"
                + " to expect an array element here. The node of search path being processed when error " +
                "occurred was " + pNode + ", and the element found was " + pCtx.stringRepresentation()
                + ". The search path was " + pSearchPath.toString());
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
