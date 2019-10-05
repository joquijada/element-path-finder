package com.exsoinn.util.epf;

import java.util.*;
import java.util.stream.Collectors;

/**
 * This API is meant to make it easier to search disparate data formats (e.g. XML, JSON), by acting as a wrapper
 * around those data structures to provide a consistent API to query the data, whih allows decoupling an application
 * from the data format that it acts on.. The data structures that it wraps may be hierarchical in nature, in which case recursion
 * can be applied. However, such implementation details are left up to implementing classes to handle according to the
 * characteristics of the data structure they intend to support.

 * The main main motivation of this API is to keep client code decoupled from the details of the underlying format of the data,
 * be it JSON, XML and the like. The only coupling/contract is done via input arguments to the various methods provided by this
 * API.
 * The only pain point is that the input parameters must be properly configured before this method gets invoked,
 * but that's a small price to pay in comparison to the maintainability and re-usability that is achieved via
 * the use of this API. The input parameters to search a {@code Context} object can be compared to the
 * <a href="https://www.w3schools.com/xml/xml_xpath.asp">XPath</a> syntax, which is used to navigate
 * the elements and attribute of an XML document. Only difference here is that this API is not married to any specific
 * format.
 *
 * All the operations provided by this interface are read-only, which essentially renders the {@link Context} object
 * immutable after its creation.
 *
 * Created by QuijadaJ on 5/3/2017.
 */
public interface Context {
    String FOUND_ELEM_VAL_IS_REGEX = "foundElemValIsRegex";
    String PARTIAL_REGEX_MATCH = "partialRegexMatch";
    String IGNORE_INCOMPATIBLE_SEARCH_PATH_PROVIDED_ERROR = "ignoreIncompatibleSearchPathProvidedError";
    String IGNORE_INCOMPATIBLE_TARGET_ELEMENT_PROVIDED_ERROR = "ignoreIncompatibleTargetElementProvidedError";

    /**
     * Represents the entry point to begin searching the underlying data structure. The search works by specifying a path
     * to the node of interest, represented by a {@link SearchPath}.
     * The {@link Context} object off of which this method can be invoked was previously obtained via a call to factory
     * method {@link ContextFactory#obtainContext(Object)}.
     * To further refine the results use the pFilter and pTargetElements arguments.
     * TODO: Add more examples of usage
     * @param pSearchPath - The path that in the underlying data structure that this method has been instructed to find. For more
     *                    information on how to build a {@code SearchPath} object to then pass it here, read the documentation
     *                    of {@link SearchPath#valueOf(String)}.
     *                    Basically the last node of the <code>SearchPath</code> is what gets returned to the caller. This can be
     *                    a primitive, and array (of primitives, or other complex structures, or a combination thereof), or a complex
     *                    data structure. The type of the found element determines how the pFilter and pTargetElements
     *                    arguments behave. Read respective description of these arguments for more details.
     *                    If an array element will be
     *                    encountered somewhere in the search path, then the corresponding node should contain square
     *                    brackets like this: someElemName[0]. If an array element is encountered yet the path
     *                    did not tell this method to expect an array at this point (by appending "[N]" to the
     *                    path node), IllegalArgumentException is thrown, *unless* the array element happens to be the
     *                    last node of the search path.
     * @param pFilter - Use this argument to further refine the search rules. Build a filter by specifying a semi-colon
     *                separated list of name/value pairs separated by equals sign, and pass that string to
     *                factory method {@link Filter#valueOf(String)}, like this:
     *
     *                <code>Filter.valueOf("name1=val1;name2=val2")</code>
     *
     *                The keys specified should correspond to keys found in the last node of the <code>pSearchPath</code>.
     *                This assumes that developer is intimately familiar with the data structure he's working with, and that
     *                he'll know exactly what result <code>pSearchPath</code> will yield.
     *
     *                How <code>pFilter</code> gets applied depends on the type of data found in the last node of the
     *                <code>pSearchPath</code>. In all cases, the members of the search results are checked to see if
     *                a corresponding key is found in the <code>Filter</code> specified, and if so that member value
     *                is compared against the filter value.
     *                The filter values can have wildcards too. There can be a wildcard at either the beginning, the end
     *                or both.
     *                Below lists the possible types of data that can be found at end of search path, and how the filter
     *                gets applied in each case:
     *
     *                primitive: simply compare the corresponding filter value against the primitive, and if there's a match
     *                  that primitive will be included in the search result
     *                array: Primitive entries are handled the same way single primitive results are found. Complex
     *                  structures in the array have their members checked against the filter the same way
     *                  single complex structures are handled as described below.
     *                complex structure: Each member of the complex structure is checked to see if there's a filter
     *                  value provided. If there's a match, then this complex structure will be included in the results.
     *
     *                A <code>Filter</code> key can be a search path also, in which case the value for the filter
     *                search path is found relative to the last node of the search path passed to this method. An error
     *                is thrown if the filter search path is not found, otherwise the node will be included in the
     *                search results if the filter value matches the value found in the underlying <code>Context</code>.
     *                An example of a filter that has a search path as key is:
     *
     *                SearchPath: top_node.inner_node.member2
     *                Filter: key=sub_member5.key=1234
     *                Context (assuming underlying data is in JSON format):
    {
    "top_node":{
    "inner_node":{
    "member1":1110,
    "member2":[
    {
    "sub_member_1":1110,
    "sub_member_2":15199,
    "sub_member_3":13135,
    "sub_member_4":7184441216,
    "sub_member5": {
    "key": "abcd"
    }
    },
    {
    "sub_member_1":1110,
    "sub_member_2":15199,
    "sub_member_3":24099,
    "sub_member_4":7184441216,
    "sub_member5": {
    "key": "1234"
    }
    }
    ],
    "member3":1073,
    "member4":7184441216,
    "member5":19555
    }
    }
    }

     *                  Based on the filter, the second entry of the "member2" array could be selected.
     *
     *                  TODO: Filter key values also support passing in a comma separated list of values
     *
     *
     *
     * @param pTargetElements - Use it to further refine the search results by specifying which elements to return in the search
     *                        results when the last node of the pSearchPath yields a complex data structure, and you're only
     *                        interested in a sub-set of the members of the found complex data structure.
     * @param pExtraParams - Implementing classes can use this {@link Map} to provide arbitrary list of name/value
     *                     pairs to provide features/behavior that this interface does not already plan for.
     *                     TODO: Clearly document *all* keys supported; they're defined as constants at top of this class
     * @return - A {@link SearchResult} which is nothing more than a {@link Map} that maps keys to {@link Context}
     *   objects. Each {@link Context} object in the {@link SearchResult} can be a primitive, or an array, or
     *   another complex object. Refer to the other methods of this class for the available operations.
     * @throws IllegalArgumentException - Thrown if parameter <code>pSearchPath</code> is determined to be invalid
     *   for whatever reason.
     */
    SearchResult findElement(SearchPath pSearchPath,
                             Filter pFilter,
                             TargetElements pTargetElements,
                             Map<String, String> pExtraParams) throws IllegalArgumentException;


    /**
     * Works similar to {@link Context#findElement(SearchPath, Filter, TargetElements, Map)}, except that the first three
     * arguments are replaced with a {@link SelectionCriteria} element.
     * @param pSelectCriteria - pSelectCriteria
     * @param pExtraParams - pExtraParams
     * @return - TODO
     * @throws IllegalArgumentException - TODO
     */
    SearchResult findElement(SelectionCriteria pSelectCriteria,
                             Map<String, String> pExtraParams) throws IllegalArgumentException;
    /**
     * Implementing classes use this method to tell if underlying data is a primitive (I.e. long, int, double,
     * {@link String}, etc...
     * @return - TODO
     */
    boolean isPrimitive();


    /**
     * Implementing classes use this method to tell if underlying data is complex (I.e. not a primitive).
     * @return - TODO
     */
    boolean isRecursible();

    /**
     * Implementing classes use this method to tell if underlying data is a type of array or list-like.
     * @return - TODO
     */
    boolean isArray();

    /**
     * Meant for use only {@link #isArray()} is true, will return a {@link List} of the underlying array-like
     * structure.
     * @return - TODO
     * @throws IllegalStateException - TODO
     */
    List<Context> asArray() throws IllegalStateException;


    /**
     * If the underlying data is array-like, will retrieve entry at index <code>pIdx</code>
     * @param pIdx - pIdx
     * @return - TODO
     * @throws IllegalStateException - TODO
     */
    Context entryFromArray(int pIdx) throws IllegalStateException;

    /**
     * If the underlying data provides implementation aside from the toString() to return the data as a string,
     * then this method is expected to wrap such an implmentation.
     * @return - TODO
     */
    String stringRepresentation();


    /**
     * To be used only when the underlying data is complex, returns true if underlying data contains the
     * <code>pElemName</code> given
     * @param pElemName - pElemName
     * @return - TODO
     * @throws IllegalStateException - TODO
     */
    boolean containsElement(String pElemName) throws IllegalStateException;



    /**
     * To be used only when the underlying data is complex, returns a {@link Set} of {@link Map.Entry}s
     * to represent the name/value pairs contained int he complex data structure.
     * @return - TODO
     * @throws IllegalStateException - TODO
     */
    Set<Map.Entry<String, Context>> entrySet() throws IllegalStateException;


    /**
     * To be used only when the underlying data is complex, returns the value of the specified <code>pMemberName</code>
     * @param pMemberName - pMemberName
     * @return - TODO
     * @throws IllegalStateException - TODO
     */
    Context memberValue(String pMemberName) throws IllegalStateException;


    /**
     * When the {@code Context} is an array, checks if the value is contained in it.
     *
     * @param pVal - pVal
     * @return - TODO
     * @throws IllegalStateException - TODO
     */
    boolean arrayContains(String pVal) throws IllegalStateException;

    /**
     * Gives the starting search path of this {@link Context}. This works only when the data is recursible
     * ({@link Context#isRecursible()} yields <code>true</code>), and there's only one outer most element, else
     * this method returns null. This was added mainly as a convenience so that calling code does not need to be
     * calculating this value over and over again. There's no reason the {@link Context} object itself can't provide
     * this information.
     * @return - The starting {@link SearchPath} if there's just a single outermost element, <code>null</code>
     *   otherwise.
     */
    SearchPath startSearchPath();


    /**
     * This method is applicable for {@link Context}'s that return <code>true</code> when {@link Context#isRecursible()}
     * gets invoked.
     *
     * @return - A list of what the top level field names are for this recursible {@link Context}. If this
     * {@link Context} is not recursible, then <code>null</code> is returned.
     */
    List<String> topLevelElementNames();


    /**
     * Utility method which attempts to transform passed in argument to a {@link List}. Argument must be a {@code String}
     * object or a sub-type, and must be a comma-separated list of values, or a value that
     * {@link ContextFactory#obtainContext(Object)} can transform to an array of primitives (for example a string
     * like '[a,b,c,d]' is recognized as JSON, hence can be transformed to Context by {@link ContextFactory#obtainContext(Object)}
     * because JSON is one the formats recognized by that factory).
     *
     * @param pArg - What will get transformed to a <code>List</code> of <code>String</code>'s, if possible.
     * @param <T> - T
     * @return - The {@code List} of {@code String}'s produced, if possible, <code>null</code> otherwise
     */
    static <T extends String> List<T> transformArgumentToListObject(T pArg) {
        boolean errorParsingCtx = true;
        try {
            Context ctx;
            if (null != (ctx = ContextFactory.obtainContext(pArg)) && ctx.isArray()) {
                List<Context> ctxAry = ctx.asArray();
                // All the Context objects in the List must be of type primitive.
                if (ctxAry.parallelStream().filter(e -> !e.isPrimitive()).findAny().isPresent()) {
                    throw new IllegalArgumentException("The produced list did not contain all primitive values, please check: "
                            + pArg);
                }
                errorParsingCtx = false;
                return (List<T>) ctxAry.stream().map(e -> e.stringRepresentation()).
                        collect(Collectors.toCollection(() -> new ArrayList<>()));
            }
        } catch (Exception ignore) {
            // Ignore
        } finally {
            if (errorParsingCtx) {
                /**
                 * One last ditch attempt at converting argument to a list
                 */
                if (pArg.indexOf(",") > 0) {
                    return (List<T>) Arrays.stream(pArg.split(",")).collect(Collectors.toCollection(() -> new ArrayList<>()));
                }
            }
        }
        return null;
    }
}
