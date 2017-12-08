package com.exsoinn.util.epf;

import net.jcip.annotations.Immutable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


/**
 * Abstract implementation of {@link Context}. This class should be suitable for all cases, however users are free to implement
 * {@link Context} from scratch, provided the contract is upheld.
 *
 * The caller can specify the matching style/behavior on either the filter values provided in the {@link Filter} object, or
 * the values configured on the target {@link Context} being search. Both cannot be specified though, it's either one or
 * the other. In case caller has specified both, the code gives preference to {@link Context} matching style/behavior. This
 * behavior is controlled by either:
 *   - Passing flag {@link AbstractContext#FOUND_ELEM_VAL_IS_REGEX}, and optionally flag {@link AbstractContext#PARTIAL_REGEX_MATCH}
 *     in the extra parameters {@code Map} argument that {@link Context#findElement(SearchPath, Filter, TargetElements, Map)}
 *     accepts. This affects behavior on the {@code Context} only. The former flag says that for filtering purposes, the
 *     relevant {@code Context} values should behave as if they were regular expressions, in which case the code will
 *     use a {@link Pattern} to make the comparison on the <strong>entire</strong> filter key value, meaning they both have to match
 *     exactly. Internally code uses {@link Matcher#matches()} method, read that documentation for that for details. But if you want to do
 *     partial matching only, <strong>in addition </strong> pass latter flag as well. Internally the code will use method
 *     {@link Matcher#find()}  to do these kind of partial matches. Refer to that method's documentation for details.
 *   - To control behavior on the filter values instead, simply use asterisk (*) on the filter values that should match
 *     partially against the {@code Context} values in question. Currently only asterisk at beginning or end,
 *     or both of string are supported. Placement of asterisk that deviates from these will throw exception.
 *   Note that if you mix both methods above, the first one, regular expression matching will take precedence, and passing wildcards (*)
 *   in the filter values will have no effect (get completely ignored by code).
 *
 *
 * Created by QuijadaJ on 5/4/2017.
 */
@Immutable
abstract class AbstractContext implements Context {
    private static final char WILD_CARD = '*';
    private static final Map<String, Pattern> patternCache = new ConcurrentHashMap<>();
    private static final String ANON_ARY_HANDLE = "anonymousArray";


    @Override
    public SearchResult findElement(SearchPath pSearchPath,
                                    Filter pFilter,
                                    TargetElements pTargetElements,
                                    Map<String, String> pExtraParams)
            throws IllegalArgumentException {
        Map<String, Context> found = findElement(this, pSearchPath, pFilter, pTargetElements, null, pExtraParams);
        return SearchResult.createSearchResult(found);
    }

    @Override
    public SearchResult findElement(SelectionCriteria pSelectCriteria,
                                    Map<String, String> pExtraParams) throws IllegalArgumentException {
        return findElement(
                pSelectCriteria.getSearchPath(), pSelectCriteria.getFilter(), pSelectCriteria.getTargetElements(), pExtraParams);
    }


    Map<String, Context> findElement(Context pElem,
                                     SearchPath pSearchPath,
                                     Filter pFilter,
                                     TargetElements pTargetElements,
                                     Map<String, Context> pFoundElemVals,
                                     Map<String, String> pExtraParams)
            throws IllegalArgumentException {

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

        /**
         * Deal with case where the original Context given is an anonymous array. In this scenario we expect search path
         * to be "[N]||nodeX||nodeY||nodeZ||...". The way we handle is that we make the array non-anonymous and identify it by
         * {@link this#ANON_ARY_HANDLE}, then modify the current node in search path by adding {@link this#ANON_ARY_HANDLE}
         * in front of the "[]", and finally we let the logic further below deal with an array inside recursible we've just
         * created. That code already does all checks, throws exception where appropriate, etc.
         */
        if (pSearchPath.currentNodeIndex() == 0 && curNodeInPath.indexOf("[") == 0 && pElem.isArray()) {
            MutableContext mc = ContextFactory.obtainMutableContext("{}");
            mc.addMember(ANON_ARY_HANDLE, ContextFactory.obtainContext(pElem.stringRepresentation()));
            curNodeInPath = ANON_ARY_HANDLE + curNodeInPath;
            pElem = mc;
        }

        String curNodeInPathNoBrackets = curNodeInPath;
        if (arrayIndex(curNodeInPath) >= 0) {
            curNodeInPathNoBrackets = removeBrackets(curNodeInPath);
        }

        boolean atEndOfSearchPath = pSearchPath.isAtEndOfSearchPath();


        /**
         * If below if() is true, then we're dealing with a complex structure. At this
         * point check if the current node in the search path we've been given exists in the current
         * element. If not, or if it designates an array node with index > 0 yet encountered node
         * is not in fact of type array, then it means the element will not be found, hence throw
         * IllegalArgumentException, unless the {@link Context#IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR} was
         * passed in the extra parameters map. The full search path given has to exist in order to return any results.
         */
        Set<Map.Entry<String, Context>> elemEntries = null;
        if (pElem.isRecursible()) {
            /**
             * The 'arrayIndex()...' condition is there to see if caller expects array node to be found yet actual
             * is not an array, and they specified an index greater than 1, in which case throw exception unless
             * we were specifically instructed to ignore such scenarios (via presence
             * of {@link Context#IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR)}).
             * We're interested in aforementioned check for non-array nodes only.
             */
            if (pElem.containsElement(curNodeInPathNoBrackets) && (arrayIndex(curNodeInPath) <= 0
                    || pElem.memberValue(curNodeInPathNoBrackets).isArray())) {
                /**
                 * Check inverse of "UnexpectedArrayNodeException" further below; a none-array node encountered,
                 * yet search path told to expect array here. Unless the array index is 0, throw exception. The
                 * motivation to make an exception if array index is 0 is to offer some flexibility to calling code. The same
                 * data node can sometimes be an array, and at others a non-array. This can happen when there's no
                 * schema backing things up, and in data conversion situations, the target data uses presence
                 * of multi node or single to display respectively as array or not. A concrete example:
                 * <xml><node>...</node><node></node></xml> -> {xml: {node: [{}, {}]}}
                 *
                 * or
                 *
                 * <xml><node>...</node></xml> -> {xml: {node: {}}}
                 *
                 * Notice in first, the node is array, in second it's not. It all depends on how original
                 * data looked. The rationale for this logic is as follows:
                 * The client just wants the first node in array if index specified is [0], therefore
                 * give it to them if it is a none-array, which obviously is a single element. However if client
                 * gave [idx > 0], then I'm confused and don't know what to do, so throw it back to client
                 * to decide what they want to do.
                 */

                elemEntries = pElem.entrySet();
            } else {

                /**
                 * Have to wrap into an IllegalArgumentException because the method signature says so. When it was
                 * decided to throw a checked exception, namely IncompatibleSearchPathException, there would have had
                 * to be a lot of changes made in dependent code to reflect an updated method signature. Hence the reason
                 * the below exception wrapping is made.
                 */
                if (null != pExtraParams && pExtraParams.containsKey(IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR)) {
                    /*
                     * Handles case where caller instructed this API to ignore it if search path is not
                     * applicable for node in question. In such cases the node simply gets ignored and is excluded from search
                     * results.
                     */
                    return pFoundElemVals;
                } else {
                    IncompatibleSearchPathException ispe = new IncompatibleSearchPathException(
                            pSearchPath, curNodeInPathNoBrackets, pElem);
                    throw new IllegalArgumentException(ispe);
                }

            }
        }

        /**
         * If "elemEntries" is not NULL, it means we're dealing with a complex structure (I.e. not a primitive)
         * and the current element in the search path has been found at this location of the passed in element to search.
         * Why am I constructing if() statements like this instead of nesting them? Makes code easier to read and
         * hence maintain, less nesting which means less indentation.
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

                String curElemName = elemEntry.getKey();

                if (!curNodeInPathNoBrackets.equals(curElemName)) {
                    continue;
                }

                Context elemToProcessNext = elemEntry.getValue();
                /*
                 * If the current element is of type array, deal with it below. If we're *not* at the last node
                 * of the search path, enforce requirement that user must specify which array entry to select
                 * to continue on that path of the search.
                 * Otherwise, if we're already at last node of search path, the requirement is relaxed, and caller has
                 * option of either specifying and array entry to select, or just select the entire array.
                 */
                if (elemToProcessNext.isArray()) {
                    /*
                     * If we're not at end of search path and we encountered an array node, yet the search path
                     * did not tell us to expect an array at this spot of the search path, throw exception. If the
                     * caller does not explicitly say what array entry to select, how do we know which path to continue on?
                     * Also if we didn't enforce this, then it might result in hard to trace bugs in the callers code.
                     * This is the inverse of check further above, where error is thrown if search path said to expect
                     * an array but the actual node is not an array.
                     * Note that this rule is relaxed if the array contains only one entry; in such a case, the client code
                     * is not required to specify in the search path that the node is an array, the code will
                     * auto select the only choice, namely the only array entry.
                     */
                    if (arrayIndex(curNodeInPath) < 0 && !atEndOfSearchPath && elemToProcessNext.asArray().size() > 1) {
                        UnexpectedArrayNodeException uane =
                                new UnexpectedArrayNodeException(pSearchPath, curNodeInPath, elemToProcessNext);
                        throw new IllegalArgumentException(uane);
                    }


                    /**
                     * The search path did specify what array entry to grab, deal with that logic in the if() block
                     * below. Then further below this "if()" we check if this is the last node of search path
                     * or not. These two pieces of logic combined is what allows the client to specify what array entry to grab
                     * from last node, or grab the entire last array node.
                     */
                    int aryIdx;
                    if ((aryIdx = arrayIndex(curNodeInPath)) >= 0) {
                        /**
                         * Handles scenario where a node in the search path specifies an array entry that does not exist,
                         * and caller wants to ignore node-not-found error.
                         */
                        if (aryIdx >= elemToProcessNext.asArray().size()) {
                            if (null != pExtraParams && pExtraParams.containsKey(IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR)) {
                                return pFoundElemVals;
                            } else {
                                IncompatibleSearchPathException ispe = new IncompatibleSearchPathException(
                                        pSearchPath, curNodeInPath, elemToProcessNext);
                                throw new IllegalArgumentException(ispe);
                            }
                        }

                        elemToProcessNext = elemToProcessNext.entryFromArray(aryIdx);
                    }
                }


                /*
                 * If below evaluates to true, we're at the last node of our search path. Invoke helper
                 * method to add the elements to results for us.
                 * WARNING: Watch out, do not alter code below; do "atEndOfSearchPath" first. Once we have reached end of search path,
                 *   recursion does not make sense. If we didn't do this check first, because the element to process next
                 *   might be recursible, we might recurse even though we're at end of search path!!!
                 */
                if (atEndOfSearchPath) {
                    processElement(curElemName, elemToProcessNext, pFilter, pTargetElements, pFoundElemVals, pExtraParams);
                } else if (elemToProcessNext.isRecursible()) {
                    findElement(elemToProcessNext, pSearchPath, pFilter, pTargetElements, pFoundElemVals, pExtraParams);
                }
            }
        }

        return pFoundElemVals;
    }


    /**
     * Extracts the index specified between square brackets. If passed in string contains no
     * square brackets, -1 is returned.
     *
     * @param pNode
     * @return - The intenger contained within square brackets, -1 if no brackets found.
     */
    private int arrayIndex(String pNode) {
        if (pNode.indexOf('[') < 0) {
            return -1;
        }
        return Integer.parseInt(pNode.substring(pNode.indexOf('[') + 1, pNode.indexOf(']')));
    }


    private void processElement(String pElemName,
                                Context pElem,
                                Filter pFilter,
                                TargetElements pTargetElements,
                                Map<String, Context> pFoundElemVals,
                                Map<String, String> pExtraParams) throws IllegalArgumentException {
        Context elemValToStore = null;
        /*
         * Handle case when element in last node of search path is primitive or another complex structure
         */
        if (pElem.isPrimitive() || pElem.isRecursible()) {
            /*
             * Hm, here the shouldExcludeFromResults() check might not be necessary. Why would the caller give
             * an element as last node in search path, and also give that element name in the pFilter Map?? In
             * other words, this might be a scenario that never happens, but leaving code here for now in case
             * there's something I'm missing.
             */
            if (shouldExcludeFromResults(pElemName, pElem, pFilter, pExtraParams)) {
                return;
            }

            elemValToStore = pElem;

            /*
             * The pTargetElems parameter applies only when results contain another complex structure.
             */
            if (pElem.isRecursible()) {
                elemValToStore = filterUnwantedElements(pElem, pTargetElements, pExtraParams);
            }
        } else if (pElem.isArray()) {
            Iterator<Context> itElem = pElem.asArray().iterator();
            List<Object> elemValList = new ArrayList<>();
            itElem.forEachRemaining(elem -> {

                /*
                 * Apply filtering if caller provided one. The shouldExcludeFromResults() method assumes
                 * that the passed in element (the 2nd argument) is either a primitive or a complex object. This
                 * logic assumes that an array will never contain an array (for example valid JSON does not allow
                 * arrays inside arrays, otherwise how in the world can you reference an anonymous array in JSON???), so safely
                 * invoke shouldExcludeFromResults() with this in mind.
                 */
                if (!shouldExcludeFromResults(pElemName, elem, pFilter, pExtraParams)) {
                    if (elem.isRecursible()) {
                        /*
                         * See comment further above regarding pFilter, same applies here
                         * to pTargetElements
                         */
                        elem = filterUnwantedElements(elem, pTargetElements, pExtraParams);
                    }
                    elemValList.add(elem.toString());
                }
            });

            /*
             * In the SearchResult we can only store a Context. The below is a lame attempt
             * to try to convert the array-like structure to a Context object, just so that we're able to obey
             * the contract of SearchResult. As long as the factory can find a suitable API to handle this,
             * then I guess it should be OK - all the client code cares about is having a Context object with methods
             * that behave correctly.
             */
            if (!elemValList.isEmpty()) {
                elemValToStore = ContextFactory.INSTANCE.obtainContext(elemValList);
            }
        } else {
            throw new IllegalArgumentException("One of the elements to search is of type not currently supported."
                    + "Element name/type is " + pElemName + "/" + pElem.getClass().getName());
        }

        if (null != elemValToStore) {
            /*
             * TODO: The below is effectively changing a List to a String, and storing it in Map, if the above found an
             * TODO: array structure. Re-visit.
             */
            pFoundElemVals.put(pElemName, elemValToStore);
            handleSingleComplexObjectFound(pFoundElemVals, pTargetElements);
        }
    }

    /**
     * This method should be implemented by child classes to handle {@link TargetElements}, to exclude
     * elements not contained therein.
     *
     * @param pElem
     * @param pTargetElems
     * @return
     */
    Context filterUnwantedElements(Context pElem, TargetElements pTargetElems, Map<String, String> pExtraParams) {
        if (null == pTargetElems) {
            return pElem;
        }

        MutableContext mc = ContextFactory.INSTANCE.obtainMutableContext("{}");
        /*
         * Handle any target element that is one or more levels
         * deeper than found node.
         */
        for (String e : pTargetElems) {
            if (!stringIsASearchPath(e)) {
                continue;
            }
            SearchPath sp = SearchPath.valueOf(e);
            SearchResult sr = pElem.findElement(sp, null, null, pExtraParams);

            if (null == sr || sr.size() != 1) {
                /*
                 * If caller said to ignore it if the target element search path is not valid for a node, then do so and continue
                 * processing other target elements provided.
                 */
                if (null != pExtraParams && pExtraParams.containsKey(IGNORE_INCOMPATIBLE_TARGET_ELEMENT_PROVIDED_ERROR) &&
                        (sr == null || sr.isEmpty())) {
                    continue;
                } else {
                    throw new IllegalArgumentException("Either found more than one element for target element search path "
                            + sp.toString() + ", or did not find any results. Check the search path and try again. Results "
                            + "were " + (null == sr ? "NULL" : "EMPTY") + ". Target elements is " + pTargetElems.toString()
                            + " and node is " + pElem.stringRepresentation());
                }
            }

            Map.Entry<String, Context> found = sr.entrySet().iterator().next();
            mc.addMember(sp.toString(), found.getValue());
        }

        /*
         * Now exclude any other element that was not requested by the caller.
         */
        Set<Map.Entry<String, Context>> ents = pElem.entrySet();
        ents.stream().filter(entry -> pTargetElems.contains(entry.getKey()))
                .forEach(entry -> mc.addMember(entry.getKey(), entry.getValue()));

        return ContextFactory.INSTANCE.obtainContext(mc.stringRepresentation());
    }


    /**
     * This method is applicable only when a {@link TargetElements} has been passed by the calling code, and the
     * data found in the last node of search path is a single complex object, or a list-like object with a single complex
     * object as member. In either case it should contain just one name/value pair. It will take the sole name/value pair
     * of the complex object and store it in passed in {@param pSearchResult} {@link Map}, first clearing any results
     * contained in {@param pSearchResult}. The idea here is to make it convenient for client code to access the single
     * name/value pair found without having to do any additional checks, effectively shifting that burden onto this API. The
     * calling code can just just blindly get the key and value as-is from the search results map.
     *
     * @param pSearchRes
     * @param pTargetElems
     */
    void handleSingleComplexObjectFound(Map<String, Context> pSearchRes,
                                        Set<String> pTargetElems) {
        if (null == pTargetElems || pTargetElems.isEmpty()) {
            return;
        }
        try {
            Set<Map.Entry<String, Context>> entries = pSearchRes.entrySet();
            if (entries.size() != 1) {
                return;
            }

            final Context elem = entries.iterator().next().getValue();
            Context ctx;
            // If element is inside an array, unwrap it first, else grab as is
            if (elem.isArray() && elem.asArray().size() == 1) {
                Context aryElem = elem.asArray().iterator().next();
                ctx = aryElem;
            } else {
                ctx = elem;
            }

            /*
             * Now check that element (whether found inside an array or not, see above) is a
             * complex object and with a single name/value pair, otherwise return w/o doing anything.
             * Hint: If element is recursible in OR, then entrySet check below will not fail, because of if()
             *   logic optimization in OR statements done by JVM, where it stops evaluating when truthfulness has been established
             */
            if (!ctx.isRecursible() || ctx.entrySet().size() > 1) {
                return;
            }

            if (null != ctx) {
                pSearchRes.clear();
                Set<Map.Entry<String, Context>> ctxEntSet = ctx.entrySet();

                for (Map.Entry<String, Context> entry : ctxEntSet) {
                    if (null != pTargetElems && pTargetElems.contains(entry.getKey())) {
                        /*
                         * If the key is a search path, convert to single string by removing the leading nodes
                         * and leaving only the last
                         */
                        String k = entry.getKey();
                        if (stringIsASearchPath(k)) {
                            k = SearchPath.valueOf(k).lastNode();
                        }
                        pSearchRes.put(k, entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("There was a problem: " + e);
        }
    }

    private boolean stringIsASearchPath(String pStr) {
        return pStr.indexOf(".") > 0;
    }


    /**
     * Contains logic to handle {@link Filter} <code>pFilter</code> param. The {@link Filter} is nothing more than
     * a list of name/value pairs used to further refine search result. Note that the field names in the
     * {@link Filter} can also themselves be search paths, specified in string format using dot separated token
     * notation (see {@link SearchPath#valueOf(String)} for more details), for example:
     * <p>
     * field1.field2[0].field3
     * <p>
     * This is meant to filter nodes on fields which are one or more levels deeper from the found node itself.
     * <p>
     * See {@link Context#findElement(SearchPath, Filter, TargetElements, Map)} for more details on how the
     * {@link Filter} argument is handled.
     * <p>
     * This method assumes that the passed in {@param pElem} is either a primitive or a complex object, and will
     * <strong>never</strong> be an array, else {@link IllegalArgumentException} will get thrown!!! The calling code
     * is expected to do the appropriate checks.
     *
     * @param pElemName - Matters only in the context of a search result which is a primitive
     * @param pElem     - The {@code Context} object to which the {@param pFilter} gets applied.
     * @param pFilter   - The {@code Filter} object to use to refine search results.
     * @return - <code>true</code> if the data should be excluded from the search results, <code>false</code>
     * otherwise
     */
    boolean shouldExcludeFromResults(String pElemName, Context pElem, Filter pFilter, Map<String, String> pExtraParams)
            throws IllegalArgumentException {
        if (null == pFilter) {
            return false;
        }

        if (pElem.isArray()) {
            throw new IllegalArgumentException("Got an array element when applying search filter "
                    + pFilter.entrySet().stream().map(Map.Entry::toString).collect(Collectors.joining())
                    + ". The element in question is " + pElemName + " ===>>> " + pElem.stringRepresentation());
        }

        /*
         * Check up front if filter is not applicable to found results,
         * throw runtime exception if that's the case.
         */
        StringBuilder filterNotApplicableReason = new StringBuilder();
        if (!filterIsApplicableToFoundElement(pElem, pElemName, pFilter, filterNotApplicableReason)) {
            throw new IllegalArgumentException("Filter not applicable to found element: " + filterNotApplicableReason.toString());
        }

        Set<Map.Entry<String, String>> filterEntries = pFilter.entrySet();
        for (Map.Entry<String, String> filterEntry : filterEntries) {
            String filterKey = filterEntry.getKey();
            String filterVal = filterEntry.getValue();

            if (stringIsASearchPath(filterKey)) {
                // TODO: Throw exception when the found element is a primitive? Reasoning is that nested filter element applies only
                // TODO: when search result is a non-primitive
                /**
                 * Handles case when the value we want to filter on is buried one or more levels deeper than the
                 * found element.
                 * We leverage findElement(), which accepts a dot (.) separated element search path. Also, we support only filtering
                 * on primitive values, therefore assume that the found element will be a single name value pair.
                 * If the path of the filter element is not found, IllegalArgumentException is thrown.
                 */
                SearchPath elemSearchPath = SearchPath.valueOf(filterKey);
                // Comparison has to be done with brackets removed from filtering key, else comparison is not valid
                // and this will return true, because the Context member name does not have brackets
                if (!pElem.containsElement(removeBrackets(elemSearchPath.get(0)))) {
                    /*
                     * Return true because the top node of the specified search path
                     * was not even found in this context
                     */
                    return true;
                }


                Map<String, Context> nestedElemSearchRes;
                nestedElemSearchRes = findElement(pElem, elemSearchPath, null, null, null, pExtraParams);
                Context nestedElemCtx;
                if (null != nestedElemSearchRes && nestedElemSearchRes.size() > 0) {
                    Set<Map.Entry<String, Context>> entries = nestedElemSearchRes.entrySet();
                    nestedElemCtx = entries.iterator().next().getValue();
                } else {
                    /*
                     * When the nested filter key (I.e. search path) failed to find results, simply ignore it if caller
                     * so has instructed, else throw exception.
                     */
                    if (null == pExtraParams || !pExtraParams.containsKey(IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR)) {
                        throw new IllegalArgumentException("The filter element value specified was not found off of this node: " +
                                filterKey);
                    } else {
                        return true;
                    }
                }

                if (!filterValueMatches(nestedElemCtx, filterVal, pExtraParams)) {
                    return true;
                }
            } else {
                Context elem;
                if (pElem.isPrimitive()) {
                    elem = pElem;
                } else {
                    elem = pElem.memberValue(filterKey);
                }


                if (!filterValueMatches(elem, filterVal, pExtraParams)) {
                    return true;
                }
            }
        }
        return false;
    }


    /**
     * Processes a value from the {@link Filter} provided by caller against the found region that it applies
     * to of the {@link Context} passed to {@link Context#findElement(SelectionCriteria, Map)} (or
     * {@link Context#findElement(SearchPath, Filter, TargetElements, Map)} method.
     * context that was provided in
     *
     * @param pFoundElem
     * @param pFilterVal
     * @param pExtraParams
     * @return
     * @throws IllegalArgumentException
     */
    boolean filterValueMatches(Context pFoundElem, String pFilterVal, Map<String, String> pExtraParams)
            throws IllegalArgumentException {

        if (pFoundElem.isArray()) {
            /**
             * Handle scenario where the target data to apply filter to is an array of values. If
             * that's the case, then each array entry is compared to the supplied filter value. If match
             * is found in any of the array elements, then the element should *not* be excluded from
             * the search results.
             */
            return pFoundElem.asArray()
                    .parallelStream().anyMatch(v -> filterValueAndFoundValueMatch(v.stringRepresentation(),
                            pFilterVal, pExtraParams));

        } else {
            return filterValueAndFoundValueMatch(pFoundElem.stringRepresentation(), pFilterVal, pExtraParams);

        }
    }


    /**
     * Implements logic to check if a value found in the search {@link Context} object matches a given value
     * from the {@link Filter} object provided by the caller.
     *
     * @param pFoundVal
     * @param pFilterVal
     * @param pExtraParams
     * @return
     * @throws IllegalArgumentException
     */
    boolean filterValueAndFoundValueMatch(String pFoundVal, String pFilterVal, Map<String, String> pExtraParams)
            throws IllegalArgumentException {
        /**
         * See if caller has requested that the values in the {@code Context} themselves behave
         * as regular expressions for purposes of filtering. In this case we ignore the matching style requested
         * for the filter key values, and instead just do the RegEx logic below
         */
        if (null != pExtraParams && pExtraParams.containsKey(FOUND_ELEM_VAL_IS_REGEX)) {
            /*
             * For performance gains, cache already seen regex patterns, and retrieve from
             * cache if same regex comes again.
             */
            Pattern p = patternCache.get(pFoundVal);
            if (null == p) {
                p = Pattern.compile(pFoundVal);
                Pattern prevPatt = patternCache.putIfAbsent(pFoundVal, p);
                if (null != prevPatt) {
                    p = prevPatt;
                }
            }
            List<String> filterVals = buildListFilter(pFilterVal);

            for (String f : filterVals) {
                Matcher m = p.matcher(f);
                if (pExtraParams.containsKey(PARTIAL_REGEX_MATCH)) {
                    if (m.find()) {
                        return true;
                    }
                } else {
                    if (m.matches()) {
                        return true;
                    }
                }
            }

            return false;
        }

        /**
         * See if the filter value is an array of values. Below method call will create list of those values. If it's
         * a single value, the resulting list will contain just that one entry.
         * Then loop over each filter value, and return at the first match of filter value against found value. Else
         * return false because none of the filter values matched.
         */
        List<String> filterVals = buildListFilter(pFilterVal);
        for (String filterVal : filterVals) {
            boolean unsupportedWildCardPlacement = true;
            // If no wild card found, do exact match
            if (filterVal.indexOf(WILD_CARD) < 0) {
                unsupportedWildCardPlacement = false;
                if (pFoundVal.equals(filterVal)) {
                    return true;
                }
            }

            String filterValWithoutWildCard = filterVal.replaceAll("\\*", "");
            // Wild card found at both beginning and end, to a partial match comparison
            if (filterVal.charAt(0) == WILD_CARD && filterVal.charAt(filterVal.length() - 1) == WILD_CARD) {
                unsupportedWildCardPlacement = false;
                if (pFoundVal.indexOf(filterValWithoutWildCard) >= 0) {
                    return true;
                }
            } else if (filterVal.charAt(0) == WILD_CARD) {
                unsupportedWildCardPlacement = false;
                // Must match at end
                if (pFoundVal.lastIndexOf(filterValWithoutWildCard) == pFoundVal.length() - filterValWithoutWildCard.length()) {
                    return true;
                }

            } else if (filterVal.charAt(filterVal.length() - 1) == WILD_CARD) {
                unsupportedWildCardPlacement = false;
                // Must match at beginning
                if (pFoundVal.indexOf(filterValWithoutWildCard) == 0) {
                    return true;
                }
            }

            if (unsupportedWildCardPlacement) {
                throw new IllegalArgumentException("Illegal placement of wildcard character '" + WILD_CARD
                        + "' found in filter value '" + filterVal + "'. Only begin/end, or either begin or end wildcard"
                        + " placement is supported.");
            }
        }

        return false;
    }


    private List<String> buildListFilter(String pFilterVal) {
        List<String> filterVals;
        if (null == (filterVals = Context.transformArgumentToListObject(pFilterVal))) {
            filterVals = new ArrayList<>();
            filterVals.add(pFilterVal);
        }

        return filterVals;
    }


    /**
     * Checks if the caller passed in a {@code Filter} that applies to expected search results as per
     * {@code SearchPath} provided. The exception to the rule is for when a filter key is a for an element
     * one or more levels deeper than found search results (in the case of found search results being a
     * complex object). In such cases the requirement is relaxed, because of the case when not all nodes found
     * contain the nested element filter key passed.
     * <p>
     * TODO: Might need to relax as well for case when search results is two or more complex objects, and not all of them
     * TODO: contain the same set of keys, and one of those keys is being used as filter. Under current logic, such
     * TODO: scenario would throw error.
     *
     * @param pFoundCtx
     * @param pFoundElemName
     * @param pFilter
     * @param pReason
     * @return
     */
    boolean filterIsApplicableToFoundElement(Context pFoundCtx, String pFoundElemName, Filter pFilter, StringBuilder pReason) {
        if (pFoundCtx.isPrimitive() && pFilter.size() > 1) {
            if (null != pReason) {
                pReason.append("Found element " + pFoundElemName + " ===>>> " + pFoundCtx.stringRepresentation()
                        + " is a primitive yet filter contained more than one entry: "
                        + pFilter.entrySet().stream().map(Map.Entry::toString).collect(Collectors.joining()));
            }
            return false;
        }
        for (Map.Entry<String, String> e : pFilter.entrySet()) {
            String k = e.getKey();
            boolean applies = true;

            if (pFoundCtx.isPrimitive()) {
                if (!pFoundElemName.equals(k)) {
                    if (null != pReason) {
                        pReason.append("The supplied filter key does not match for found primitive element name: " + pFoundElemName
                                + " ===>>> " + pFoundCtx.stringRepresentation() + ". Filter is "
                                + pFilter.entrySet().stream().map(Map.Entry::toString).collect(Collectors.joining()));
                    }
                    applies = false;
                }
            } else {
                if (stringIsASearchPath(k)) {
                    continue;
                }
                if (!pFoundCtx.containsElement(k)) {
                    if (null != pReason) {
                        pReason.append("The supplied filter key is not found in complex object: " + pFoundElemName
                                + " ===>>> " + pFoundCtx.stringRepresentation() + ". Filter is key " + k
                                + ". Full filter provided is "
                                + pFilter.entrySet().stream().map(Map.Entry::toString).collect(Collectors.joining(";")));
                    }
                    applies = false;
                }
            }

            if (!applies) {
                return false;
            }
        }

        return true;
    }


    @Override
    public SearchPath startSearchPath() {
        if (isRecursible() && entrySet().size() == 1) {
            return SearchPath.valueOf(entrySet().iterator().next().getKey());
        }

        return null;
    }

    public List<String> topLevelElementNames() {
        if (isRecursible()) {
            return entrySet().parallelStream().map(Map.Entry::getKey).collect(Collectors.toList());
        }

        return null;
    }


    private String removeBrackets(String pIn) {
        if (arrayIndex(pIn) < 0) {
            return pIn;
        }
        return pIn.substring(0, pIn.indexOf('['));
    }
}
