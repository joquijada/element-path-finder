package com.exsoinn.util;

/**
 * Any DNB object can feel free to implement this interface. This interface can be used to tie together a category
 * of objects, where you define what that categorization is according to the application in question.
 * Created by QuijadaJ on 7/20/2017.
 */
public interface DnbBusinessObject {
    /**
     * For different objects the this method can have a different meaning. For example, if an implementing class
     * happens to also be a child of {@link java.util.Map}, then it will automatically implement this method.
     * @return
     */
    boolean isEmpty();
}
