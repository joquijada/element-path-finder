package com.exsoinn.util.epf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by QuijadaJ on 9/7/2017.
 */
public class Utilities {
    public static final String MULTI_VAL_DELIM = "||";

    /*
     * Suppresses default constructor, ensuring non-instantiability.
     */
    private Utilities() {

    }


    /**
     * Converts the passed in {@param pCtxList} to a {@link Utilities#MULTI_VAL_DELIM} delimited <code>String</code>. Certain
     * things are enforced in the passed in {@link Context} list. If any are violated, then a runtime {@link IllegalArgumentException}
     * gets thrown:
     *   1) Every element in the list must be either a primitive or recursible (I.e. complex) <code>Context</code>
     *     object.
     *   2) In case of a recursible (I.e. complex) <code>Context</code>, then it must contain at most one name/value pair.
     * Later on, as needed, can relax these enforcements as it makes sense for rules engine evolution/needs.
     *
     * @param pCtxList
     * @return - A delimited string, using {@link Utilities#MULTI_VAL_DELIM} as the delimiter.
     * @throws IllegalArgumentException
     */
    public static String toDelimitedString(List<Context> pCtxList) throws IllegalArgumentException {
        List<String> leftOpVals = new ArrayList<>();
        Iterator<Context> srIt = pCtxList.iterator();
        while (srIt.hasNext()) {
            Context c = srIt.next();

            if (c.isRecursible()) {
                if (c.entrySet().size() > 1) {
                    throw new IllegalArgumentException("A complex object in the context list "
                            + "contained more than one name/value pair: " + c.stringRepresentation());
                }
                leftOpVals.add(c.entrySet().iterator().next().getValue().stringRepresentation());
            } else if (c.isPrimitive()) {
                leftOpVals.add(c.stringRepresentation());
            } else {
                throw new IllegalArgumentException("An object in the array "
                        + "is neither complex nor a primitive, unsure what to do: " + c.stringRepresentation());
            }
        }
        return leftOpVals.stream().collect(Collectors.joining(MULTI_VAL_DELIM));
    }
}
