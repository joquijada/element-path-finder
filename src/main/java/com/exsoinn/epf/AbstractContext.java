package com.exsoinn.epf;

import java.util.*;


/**
 * Created by QuijadaJ on 5/4/2017.
 */
abstract class AbstractContext implements Context {
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
                    Context curAryElemVal = entryFromArray(aryIdx);
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
            handleSingleValueFound(pFoundElemVals, pTargetElements);
        }
    }

    abstract Context filterUnwantedElements(Context pElem, TargetElements pTargetElems);

    abstract void handleSingleValueFound(Map<String, String> pSearchRes, Set<String> pTargetElems);

    abstract boolean shouldExcludeFromResults(Context pElem, Filter pFilter);

}
