package com.exsoinn.epf;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Created by QuijadaJ on 5/3/2017.
 */
public class TargetElements implements Set<String> {
    private final Set<String> list;
    private final static Map<String, TargetElements> cachedLists = new ConcurrentHashMap();

    private TargetElements(String pTargetElems) {
        list = parseTargetElements(pTargetElems);
    }



    public static TargetElements valueOf(String pTargetElems)
            throws IllegalArgumentException {
        TargetElements cachedList = cachedLists.get(pTargetElems);
        if (null == cachedList) {
            TargetElements newList = new TargetElements(pTargetElems);
            cachedList = cachedLists.putIfAbsent(pTargetElems, newList);
            if (null == cachedList) {
                cachedList = newList;
            }
        }

        return cachedList;
    }

    public static TargetElements fromSet(Set<String> pTargetElems) {
        if (null == pTargetElems) {
            return null;
        }
        return TargetElements.valueOf(pTargetElems.parallelStream().collect(Collectors.joining(",")));
    }


    private Set<String> parseTargetElements(String pTargetElems) throws IllegalArgumentException{
        String[] tokens = pTargetElems.split(",");
        if (null == tokens) {
            return null;
        }
        Set<String> elems = new HashSet<>();

        for (String t : tokens) {
            elems.add(t);
        }

        return elems;
    }


    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return list.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list.contains(o);
    }

    @Override
    public Iterator<String> iterator() {
        Set<String> newList = new HashSet<>(list);
        return newList.iterator();
    }

    @Override
    public Object[] toArray() {
        Set<String> newList = new HashSet<>(list);
        return newList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Set<String> newList = new HashSet<>(list);
        return newList.toArray(a);
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
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends String> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }
}
