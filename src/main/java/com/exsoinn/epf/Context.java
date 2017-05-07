package com.exsoinn.epf;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * This interface is meant to make it easier to search disparate data formats (e.g. XML, JSON), by acting as a wrapper
 * around those data structures. The data structures that it wraps may be hierarchical in nature, in which case recursion
 * can be applied. However, such implementation details are left up to implemented classes to handle according to the
 * characteristics of the data structure the intend to support.
 * Created by QuijadaJ on 5/3/2017.
 */
public interface Context {

    /**
     * Represents the entry point to begin searching the underlying data structure.
     * @param pSearchPath - The path that in the underlying data structure that this method has been instructed to find.
     * @param pFilter - This applies when the results found is an array of complex structures. It specifies criteria used
     *                to select one or more of the array's complex structures
     * @param pTargetElements - Used to refine the search results, in the case where the last node specified in the
     *                search path is data structure made of of names/values.
     * @param pExtraParams - Implementing classes can use this {@link Map} to provide arbitrary list of name/value
     *                     pairs to provide features/functionality that this interface does not already plan for,
     * @return
     * @throws IllegalArgumentException - Thrown if parameter <code>pSearchPath</code> is determined to be invalid
     *   for whatever reason.
     */
    SearchResult findElement(SearchPath pSearchPath,
                             Filter pFilter,
                             TargetElements pTargetElements,
                             Map<String, String> pExtraParams) throws IllegalArgumentException;

    /**
     * Implementing classes use this method to tell if underlying data is a primitive (I.e. long, int, double,
     * {@link String}, etc...
     * @return
     */
    boolean isPrimitive();


    /**
     * Implementing classes use this method to tell if underlying data is complex (I.e. not a primitive).
     * @return
     */
    boolean isRecursible();

    /**
     * Implementing classes use this method to tell if underlying data is a type of array or list-like.
     * @return
     */
    boolean isArray();

    /**
     * Meant for use only {@link #isArray()} is true, will return a {@link List} of the underlying array-like
     * structure.
     * @return
     * @throws IllegalStateException
     */
    List<Context> asArray() throws IllegalStateException;


    /**
     * If the underlying data is array-like, will retrieve entry at index <code>pIdx</code>
     * @param pIdx
     * @return
     * @throws IllegalStateException
     */
    Context entryFromArray(int pIdx) throws IllegalStateException;

    /**
     * If the underlying data provides implementation aside from the {@link #toString} to return the data as a string,
     * then this method is expected to wrap such an implmentation.
     * @return
     */
    String stringRepresentation();


    /**
     * To be used only when the underlying data is complex, returns true if underlying data contains the
     * <code>pElemName</code> given
     * @param pElemName
     * @return
     * @throws IllegalStateException
     */
    boolean containsElement(String pElemName) throws IllegalStateException;



    /**
     * To be used only when the underlying data is complex, returns a {@link Set} of {@link Map.Entry}s
     * to represent the name/value pairs contained int he complex data structure.
     * @return
     * @throws IllegalStateException
     */
    Set<Map.Entry<String, Context>> entrySet() throws IllegalStateException;


    /**
     * To be used only when the underlying data is complex, returns the value of the specified <code>pMemberName</code>
     * @param pMemberName
     * @return
     * @throws IllegalStateException
     */
    Context memberValue(String pMemberName) throws IllegalStateException;
}
