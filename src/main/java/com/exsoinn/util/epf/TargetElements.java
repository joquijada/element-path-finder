package com.exsoinn.util.epf;

import com.exsoinn.util.ForwardingImmutableSet;
import net.jcip.annotations.Immutable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Encapsulates a set of elements to return from a {@link Context} search. As a performance optimization,
 * instances of this class are cached based on the <strong>order</strong> of the elements in the set. This means
 * that sets "elem1,elem2" and "elem2,elem1" will be treated as two distinct objects, even though it's the same set.
 * We figured that if we sorted the set elements every time an instance of this class is requested, it might
 * result in a performance penalty, which defeats the purpose of caching in the first place. Having a few duplicate
 * elements here and there is better as opposed to having to sort on every invocation.
 * Objects of this class are made unmodifiable by having this class extend {@link ForwardingImmutableSet}.
 * Created by QuijadaJ on 5/3/2017.
 */
@Immutable
public final class TargetElements extends ForwardingImmutableSet<String> {
    private final static Map<String, TargetElements> cachedLists = new ConcurrentHashMap<>();
    private final Set<String> targetElementsSet;

    private TargetElements(Set<String> pSet) {
        super(pSet);
        targetElementsSet = pSet;
    }


    public static TargetElements valueOf(String pTargetElems)
            throws IllegalArgumentException {
        if (null == pTargetElems) {
            return null;
        }
        TargetElements cachedList = cachedLists.get(pTargetElems);
        if (null == cachedList) {
            final Set<String> elems = parseTargetElements(pTargetElems);
            TargetElements newList = new TargetElements(elems);
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


    private static Set<String> parseTargetElements(String pTargetElems) throws IllegalArgumentException{
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
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(targetElementsSet.parallelStream().collect(Collectors.joining(",")));
        return sb.toString();
    }
}
