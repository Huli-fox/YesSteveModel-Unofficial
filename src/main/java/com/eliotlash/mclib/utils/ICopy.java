package com.eliotlash.mclib.utils;

/**
 * The Java Cloneable interface is only a marker interface to enforce
 * a convention to override the protected Object.clone() method with a public method.
 * This does not allow for dynamic validation whether an object actually has a public clone() method.
 * <p>
 * This interface should provide better knowledge at compile time whether a generic object
 * has a public copy method.
 *
 * @param <T> by convention this should be the type of the class that inherits this interface
 */
public interface ICopy<T> {

    T copy();

    default void copy(T origin) {}
}
