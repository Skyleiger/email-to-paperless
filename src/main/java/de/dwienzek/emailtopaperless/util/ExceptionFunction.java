package de.dwienzek.emailtopaperless.util;

public interface ExceptionFunction<T, R> {

    R apply(T t) throws Exception;


}
