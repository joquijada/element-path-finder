package com.exsoinn.util;


import java.util.*;

/**
 * A forwarding {@code Set} which cannot be modified once built.
 *
 * <strong>WARNING:</strong> A malicious/careless client can still extend this class and override the methods that mutate
 *   the object, which in this implementation throw {@link UnsupportedOperationException}. Therefore this class
 *   can at best be considered conditionally thread safe. To provide more robust protection, child classes should be
 *   declared <code>final</code>.
 *
 * Created by QuijadaJ on 5/25/2017.
 */
public class ForwardingImmutableSet<E> implements Set<E> {
    private final Set<E> list;


    public ForwardingImmutableSet(Set<? extends E> pSet) {
        list = new HashSet<>(pSet);
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
    public Iterator<E> iterator() {
        Set<E> newList = new HashSet<>(list);
        return newList.iterator();
    }

    @Override
    public Object[] toArray() {
        Set<E> newList = new HashSet<>(list);
        return newList.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        Set<E> newList = new HashSet<>(list);
        return newList.toArray(a);
    }

    @Override
    public boolean add(E s) {
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
    public boolean addAll(Collection<? extends E> c) {
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

    @Override
    public String toString() {
        return list.toString();
    }
}
