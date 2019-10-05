package com.exsoinn.util.epf;

import net.jcip.annotations.Immutable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Provides unmodifiable instance of {@code SearchPath} objects, via the {@link SearchPath#valueOf(String)} constructor. Once
 * constructed, you can traverse the nodes by invoking {@link SearchPath#advanceToNextNode()}. If already at last node
 * and you invoke {@link SearchPath#advanceToNextNode()} again, then it will circle back to the first node. In other words
 * this class is modular in that way, so be mindful of this when you write code that invokes it.
 * Methods such as {@link SearchPath#currentNode()} and {@link SearchPath#isAtEndOfSearchPath()}, allow you to query
 * the current state of the {@code SearchPath} object. Also because this class implements {@link List}, all the read-only
 * methods are available for you to invoke. However invoking methods that modify state will throw
 * {@link UnsupportedOperationException}.
 *
 * Last but not least, for space efficiency, instances of this class are internally cached.
 *
 * Created by QuijadaJ on 5/3/2017.
 */
@Immutable
public final class SearchPath implements List<String> {
    private final List<String> searchPath;
    private final String searchPathAsString;
    private final int currentNodeIndex;
    private final boolean atEndOfSearchPath;
    private static final String KEY_SEP = "__";
    private static final int INIT_SP = -1;
    private final static Map<String, SearchPath> cachedSearchPaths = new ConcurrentHashMap<>();



    static String generateCacheKey(String pSearchPath, int pIdx, boolean pAtEnd) {
        StringBuilder key = new StringBuilder();
        key.append(pSearchPath);
        key.append(KEY_SEP);
        key.append(pIdx);
        key.append(KEY_SEP);
        key.append(pAtEnd);
        return key.toString();
    }

    SearchPath(String pSearchPath, int pNodeIdx, boolean pAtEnd) {
        if (pNodeIdx < 0 && pAtEnd) {
            searchPath = Collections.emptyList();
        } else {
            searchPath = parseElementSearchPath(pSearchPath);
        }
        searchPathAsString = pSearchPath;
        currentNodeIndex = pNodeIdx;
        atEndOfSearchPath = pAtEnd;
    }


    /**
     * This public static method builds a {@code SearchPath}. The format is a dot (.) separated string of tokens, for example:
     *
     * someNode.innerNode[0].finalNode
     *
     * The search path specifies what information in the underlying hierarchical data the caller is interested in finding. Notice in the
     * example above one of the tokens contains square brackets. This is used to indicate if an array-like structure will
     * be encountered somewhere along the search path.
     * @param pSearchPath - pSearchPath
     * @return - TODO
     */
    public static SearchPath valueOf(final String pSearchPath) {
        final boolean atEnd = false;
        String key = generateCacheKey(pSearchPath, INIT_SP, atEnd);
        SearchPath sp = cachedSearchPaths.get(key);
        if (null == sp) {
            sp = new SearchPath(pSearchPath, INIT_SP, atEnd);
            SearchPath spFromCache = cachedSearchPaths.putIfAbsent(key, sp);
            sp = (null == spFromCache) ? sp : spFromCache;
        }

        return sp;
    }


    /**
     * Traverses the {@code SearchPath} object in circular/modular manner, meaning that when you have reached
     * the last node by advancing, if you invoke {@link SearchPath#advanceToNextNode()} again it will go back
     * to first note. This means you can traverse the {@code SearchPath} object indefinitely.
     * @return - TODO
     */
    final SearchPath advanceToNextNode() {
        /*
         * If this go around is advancing to the last node in search path, set "atEndOfSearchPath"
         * to true. If we've already advanced to last node, and are trying to advance again, set
         * current index to -1. That's the reason why we're "pre-checking" the effect of advancing to
         * next node index via "(currentNodeIndex + 1) <= searchPath.size()-1"
         */
        int nextNodeIdx = (currentNodeIndex + 1) <= searchPath.size()-1 ? (currentNodeIndex + 1) : -1 ;

        /**
         * Set flag which indicates that this time we advanced to last node of search path. If caller again
         * invokes advanceToNextNode() after having already advanced to last node, then "atEnd" gets set to false and
         * again you can traverse the search path object.
         */
        boolean atEnd = (nextNodeIdx >= searchPath.size()-1);

        String key = generateCacheKey(searchPathAsString, nextNodeIdx, atEnd);
        SearchPath sp = cachedSearchPaths.get(key);
        if (null == sp) {
            sp = new SearchPath(searchPathAsString, nextNodeIdx, atEnd);
            SearchPath spFromCache = cachedSearchPaths.putIfAbsent(key, sp);
            sp = (null == spFromCache) ? sp : spFromCache;
        }
        return sp;
    }


    public String lastNode() {
        return searchPath.get(searchPath.size() - 1);
    }

    public static void main(String[] args) {
        SearchPath sp = valueOf("node1.node2.node3.node4");
        SearchPath newSp = sp.advanceToNextNode();
        newSp = newSp.advanceToNextNode();
        newSp = newSp.advanceToNextNode();
        newSp = newSp.advanceToNextNode();
        newSp = newSp.advanceToNextNode();
        newSp = newSp.advanceToNextNode();
        newSp = newSp.advanceToNextNode();
        newSp = newSp.advanceToNextNode();
        newSp = newSp.advanceToNextNode();
    }

    final String currentNode() {
        return get(currentNodeIndex);
    }

    final int currentNodeIndex() {
        return currentNodeIndex;
    }

    @Override
    public int size() {
        return searchPath.size();
    }

    @Override
    public boolean isEmpty() {
        return searchPath.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return searchPath.contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        return new ArrayList<>(searchPath).iterator();
    }

    @Override
    public Object[] toArray() {
        return searchPath.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return searchPath.toArray(a);
    }

    @Override
    public boolean add(String s) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(Object o) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return searchPath.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean addAll(int index, Collection<? extends String> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String get(int index) {
        return searchPath.get(index);
    }

    @Override
    public String set(int index, String element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(int index, String element) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String remove(int index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int indexOf(Object o) {
        return searchPath.indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return searchPath.lastIndexOf(o);
    }

    @Override
    public ListIterator<String> listIterator() {
        return new ArrayList<>(searchPath).listIterator();
    }

    @Override
    public ListIterator<String> listIterator(int index) {
        return new ArrayList<>(searchPath).listIterator(index);
    }

    @Override
    public List<String> subList(int fromIndex, int toIndex) {
        return new ArrayList<>(searchPath).subList(fromIndex, toIndex);
    }


    /**
     * Express the search path as a dot separated {@link String} of tokens, which was the original
     * format provided when this {@code SearchPath} instance was constructed.
     * @return - Search path string
     */
    @Override
    public String toString() {
        return searchPathAsString;
    }

    @Override
    public boolean equals(Object o) {
        return searchPath.equals(o);
    }

    @Override
    public int hashCode() {
        return searchPath.hashCode();
    }

    private static List<String> parseElementSearchPath(String pElemSearchPath) {
        String[] nodes = pElemSearchPath.split("\\.");
        return Arrays.stream(nodes).collect(Collectors.toCollection(() -> new ArrayList<>(nodes.length)));
    }

    boolean isAtEndOfSearchPath() {
        return atEndOfSearchPath;
    }
}
