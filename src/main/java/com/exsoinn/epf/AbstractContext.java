package com.exsoinn.epf;

import java.util.*;


/**
 * Created by QuijadaJ on 5/4/2017.
 */
abstract class AbstractContext implements Context {
    /**
     * Via externally configurable arguments, this method will search a {@link Context} structure for the
     * elements specified. If found, the results are returned as name/value pairs for each
     * of the elements that the caller was searching for.
     * The {@link Context} object off of which this method can be invoked, was previously obtained via a call to factory
     * method {@link ContextFactory#obtainContext(Object)}.
     * The main idea here is to keep client code decoupled from the underlying format of the data, be it JSON, XML and
     * the link. The only coupling/contract is done via input arguments, to modify the behavior of this method.
     * The only pain point is that the input parameters must be properly configured before this method gets invoked,
     * but that's a small price to pay in comparison to the maintainability and re-usability that is gained by externally
     * configuring things in this way.
     *
     * @param pSearchPath - A SearchPath object that was constructed vya {@link SearchPath#valueOf(String)}. If an array element will be
     *                    encountered somewhere in the search path, then the corresponding node should contain square
     *                    brackets like this: someElemName[0]. If an array element is encountered yet the path
     *                    did not tell this method to expect an array at this point (by appending "[N]" to the
     *                    path node), IllegalArgumentException is thrown, *unless* the array element happens to be the
     *                    last node of the search path.
     * @param pFilter - A {@link Filter} which will be applied in all-or-nothing fashion to select a data node that matches
     *                *all* of the name/value pairs contained in {@param pFilter}. The key in the {@param pFilter} corresponds
     *                to a member in te underlying {@link Context}, and the value corresponds to what the value
     *                should be in order for that node to be included in the search results. {@param pFilter} applies only
     *                to complex (I.e. non-primitive} data types only of the underlying data object that the {@link Context}
     *                being searched encapsulates
     * @param pTargetElements - This argument is nothing but a {@link java.util.Set} that gets applied to the last node in
     *                        the search of element names that should be included in the results.
     *                        It gets applied only to found complex elements whether those complex elements be inside or outside of an array.
     * @return - Map of {@link SearchResult} object that contains name/value pairs found as per the {@param pElemPath},
     *           where name is the element name found, and value is the value of such element. The
     *           values returned will all be in string format.
     * @throws IllegalArgumentException - Thrown if any of the input parameters are deemed incorrect.
     *
     */
    @Override
    public SearchResult findElement(SearchPath pSearchPath,
                                    Filter pFilter,
                                    TargetElements pTargetElements,
                                    Map<String, String> pExtraParams)
            throws IllegalArgumentException{
        Map<String, String> found = findElement(this, pSearchPath, pFilter, pTargetElements, null, pExtraParams);
        return SearchResult.createSearchResult(found);
    }


    Map<String, String> findElement(Context pElem,
                                    SearchPath pSearchPath,
                                    Filter pFilter,
                                    TargetElements pTargetElements,
                                    Map<String, String> pFoundElemVals,
                                    Map<String, String> pExtraParams)
            throws IllegalArgumentException{

        if (null == pFoundElemVals) {
            pFoundElemVals = new HashMap<>(pTargetElements != null ? pTargetElements.size() : 0);
        }

        String curNodeInPath;

        /*
         * Advance the to the next element/node in search path. Because the object is immutable, it's a
         * 2-step process to do so. Read the API javadoc of SearchPath for more details.
         */
        pSearchPath = pSearchPath.advanceToNextNode();
        curNodeInPath = pSearchPath.currentNode();

        String curNodeInPathNoBrackets = curNodeInPath;
        if (curNodeInPath.indexOf('[') >= 0) {
            curNodeInPathNoBrackets = curNodeInPath.substring(0, curNodeInPath.indexOf('['));
        }

        boolean atEndOfSearchPath = pSearchPath.isAtEndOfSearchPath();


        /*
         * If below if() is true, then we're dealing with a complex structure. At this
         * point check if the current node in the search path we've been given exists in the current
         * element. If not, then it means the element will not be found, hence throw
         * IllegalArgumentException. The full search path given has to exist in order to return any results.
         */
        Set<Map.Entry<String, Context>> elemEntries = null;
        if (pElem.isRecursible()) {
            if (pElem.containsElement(curNodeInPathNoBrackets)) {
                elemEntries = pElem.entrySet();
            } else {
                throw new IllegalArgumentException("Did not find expected path node " + curNodeInPathNoBrackets
                        + " in the current part of the element currently being processed search. Check that the "
                        + "search path is correct: " + pSearchPath.toString()
                        + ". element is " + pElem.toString());
            }
        }

        /*
         * If "elemEntries" is not NULL, it means we're dealing with a complex structure (I.e. not a primitive)
         * and the current element in the search path has been found at this location of the passed in element to search. Why
         * am I structuring if() statements like this instead of nesting them? Makes code easier to read and
         * hence maintain, less nestedness.
         */
        if (null != elemEntries) {
            for (Map.Entry<String, Context> elemEntry : elemEntries) {
                /*
                 * If this pFoundElemVals is not empty, exit, no need to process further. It means we reached
                 * the last node in search path and found the goods. This was added here so that JVM does not
                 * continue iterating if there's more than one element in the element node that contains the element we're
                 * searching for.
                 */
                if (!pFoundElemVals.isEmpty()) {
                    return pFoundElemVals;
                }
                boolean shouldRecurse = false;
                Context elemToRecurseInto = null;
                String curElemName = elemEntry.getKey();

                if (!curNodeInPathNoBrackets.equals(curElemName)) {
                    continue;
                }
                Context curElemVal = elemEntry.getValue();
                /*
                 * If below evaluates to true, we're at the last node of our search path. Invoke helper
                 * method to add the elements to results for us.
                 */
                if (atEndOfSearchPath) {
                    processElement(curElemName, curElemVal, pFilter, pTargetElements, pFoundElemVals, pExtraParams);
                } else if (curElemVal.isArray()) {
                    if (curNodeInPath.indexOf('[') < 0) {
                        throw new IllegalArgumentException("Found an array element, yet the search path did not tell me"
                                + " to expect an array element here. The node of search path being processed when error " +
                                " occurred was " + curNodeInPath + ", and the element found was " + curElemVal.toString()
                                + ". The search path was " + pSearchPath.toString());
                    }
                    int aryIdx =
                            Integer.parseInt(
                                    curNodeInPath.substring(curNodeInPath.indexOf('[') + 1, curNodeInPath.indexOf(']')));
                    Context curAryElemVal = curElemVal.entryFromArray(aryIdx);
                    if (curAryElemVal.isRecursible()) {
                        shouldRecurse = true;
                        elemToRecurseInto = curAryElemVal;
                    }
                } else if (curElemVal.isRecursible()) {
                    shouldRecurse = true;
                    elemToRecurseInto = curElemVal;
                }
                if (shouldRecurse) {
                    findElement(elemToRecurseInto, pSearchPath, pFilter, pTargetElements, pFoundElemVals, pExtraParams);
                }
            }
        }

        return pFoundElemVals;
    }


    private void processElement(String pElemName,
                                Context pElem,
                                Filter pFilter,
                                TargetElements pTargetElements,
                                Map<String, String> pFoundElemVals,
                                Map<String, String> pExtraParams) throws IllegalArgumentException {
        Object elemVal = null;
        /*
         * Handle case when element in last node of search path is primitive or another complex structure
         */
        if (pElem.isPrimitive() || pElem.isRecursible()) {
            /*
             * Hm, here the shouldExcludeFromResults() check might not be necessary. Why would the caller give
             * an element as last node in search path, and also give that element name in the pElemFilter Map?? In
             * other words, this might be a scenario that never happens, but leaving code here for now in case
             * there's something I'm missing.
             */
            if (pElem.isRecursible() && shouldExcludeFromResults(pElem, pFilter)) {
                return;
            }

            if (pElem.isPrimitive()) {
                elemVal = pElem.stringRepresentation();
            } else {
                elemVal = pElem.toString();
            }


            /*
             * The pTargetElems parameter applies only when results contain another complex structure, apply here.
             */
            if (pElem.isRecursible()) {
                elemVal = filterUnwantedElements(pElem, pTargetElements);
            }
        } else if (pElem.isArray()) {
            Iterator<Context> itElem = pElem.asArray().iterator();
            List<Object> elemValList = new ArrayList<>();
            itElem.forEachRemaining(elem -> {

                /*
                 * In below if() expressions, if element is *not* a complex structure, then the first part of OR
                 * will be true, and the rest will not get evaluated, as per JVM optimizations of if() statements. But
                 * if element *is* a complex structure, then first part of OR evaluates to false, which allows us to safely
                 * assume that it is a complex structure when calling shouldExcludeFromResults(), which
                 * expects its first argument type to be a complex structure.
                 */
                if (!elem.isRecursible() || !shouldExcludeFromResults(elem, pFilter)) {
                    if (elem.isRecursible()) {
                        /*
                         * See comment further above regarding pTargetElems, same applies here.
                         */
                        elem = filterUnwantedElements(elem, pTargetElements);
                    }
                    elemValList.add(elem.toString());
                }
            });
            if (!elemValList.isEmpty()) {
                elemVal = elemValList;
            }
        } else {
            throw new IllegalArgumentException("One of the elements to search is of type not currently supported."
                    + "Element name/type is " + pElemName + "/" + pElem.getClass().getName());
        }

        if (null != elemVal) {
            /*
             * TODO: The below is effectively changing a List to a String, and storing it in Map, if the above found an
             * array structure. Re-visit.
             */
            pFoundElemVals.put(pElemName, elemVal.toString());
            handleSingleComplexObjectFound(pFoundElemVals, pTargetElements);
        }
    }

    /**
     * This method should be implemented by child classes to handle {@link TargetElements}, to exclude
     * elements not contain therein
     * @param pElem
     * @param pTargetElems
     * @return
     */
    abstract Context filterUnwantedElements(Context pElem, TargetElements pTargetElems);


    /**
     * This method is applicable only when a {@link TargetElements} has been passed by the calling code, and when the
     * data found in the last node of search path is a single complex object, or if it's list like object with a single complex
     * object as member. It should take the name/value pairs of the complex object and store them in passed in
     * {@param pSearchResult} {@link Map}. The idea here is to make it convenient for client code to access these members
     * of the complex object without any additional processing, effectively shifting that burden onto this API.
     *
     * @param pSearchRes
     * @param pTargetElems
     */
    abstract void handleSingleComplexObjectFound(Map<String, String> pSearchRes, Set<String> pTargetElems);


    /**
     * The implementation of this method provided by child classes should provide data format specific logic to
     * handle {@link Filter} <code>pFilter</code> param. The {@link Filter} is to be applied once the last node of
     * the search path is located, and the node is an array of complex objects, to filter out nodes not wanted in the
     * final search results.
     * @param pElem
     * @param pFilter
     * @return
     */
    abstract boolean shouldExcludeFromResults(Context pElem, Filter pFilter);

}
