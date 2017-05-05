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

    private SearchPath(String pSearchPath, int pNodeIdx) {
        searchPath = parseElementSearchPath(pSearchPath);
        searchPathAsString = pSearchPath;
        currentNodeIndex = pNodeIdx;
        atEndOfSearchPath = false;
    }

    private SearchPath(String pSearchPath) {
        searchPath = Collections.EMPTY_LIST;
        currentNodeIndex = -1;
        atEndOfSearchPath = true;
        searchPathAsString = pSearchPath;
    }


    public static SearchPath valueOf(final String pSearchPath) {
        return new SearchPath(pSearchPath, -1);
    }

    public final SearchPath advanceToNextNode() {
        /*
         * We're already at last node (I.e. the search path has already been traversed), advancing will just construct
         * an empty list SearchPath
         */
        if (searchPath.size() == currentNodeIndex) {
            return new SearchPath(searchPathAsString);
        }
        return new SearchPath(searchPathAsString, currentNodeIndex+1);
    }

    public final String currentNode() {
        return get(currentNodeIndex);
    }

    public final int currentNodeIndex() {
        return currentNodeIndex;
    }

    public final boolean atEndOfSearchPath() {
        return false;
    }

    @Override
    public int size() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
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
    public String[] toArray() {
        return (String[]) searchPath.toArray();
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
     * @return
     */
    @Override
    public String toString() {
        return searchPathAsString;
    }

    private static List<String> parseElementSearchPath(String pElemSearchPath) {
        String[] nodes = pElemSearchPath.split("\\.");
        List<String> l = new ArrayList<>();
        Arrays.stream(nodes).forEach(e -> l.add(e));
        return l;
    }

    public boolean isAtEndOfSearchPath() {
        return atEndOfSearchPath;
    }
}
