package com.exsoinn.epf;

import java.util.*;

/**
 * Created by QuijadaJ on 5/3/2017.
 */
public class SearchPath implements List<String> {
    private final List<String> searchPath;
    private final String searchPathAsString;
    private final int currentNodeIndex;
    private final boolean atEndOfSearchPath;



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

    /*SearchPath(String pSearchPath) {
        searchPath = Collections.emptyList();
        currentNodeIndex = -1;
        atEndOfSearchPath = true;
        searchPathAsString = pSearchPath;
    }*/


    static SearchPath valueOf(final String pSearchPath) {
        return new SearchPath(pSearchPath, -1, false);
    }

    final SearchPath advanceToNextNode() {
        /*
         * If this go around is advancing to the last node in search path, set "atEndOfSearchPath"
         * to true. If we've already advanced to last node, and are trying to advance again, in addition set
         * current index to -1.
         */
        int nextNodeIdx = currentNodeIndex + 1;
        return new SearchPath(searchPathAsString,
                nextNodeIdx <= searchPath.size()-1 ? nextNodeIdx : -1, (nextNodeIdx >= searchPath.size()-1));
    }

    final String currentNode() {
        return get(currentNodeIndex);
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
        //return searchPath.toString();
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
        List<String> l = new ArrayList<>(nodes.length);
        Arrays.stream(nodes).forEach(e -> l.add(e));
        return l;
    }

    boolean isAtEndOfSearchPath() {
        return atEndOfSearchPath;
    }
}
