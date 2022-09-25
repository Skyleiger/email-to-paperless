package de.dwienzek.emailtopaperless.util;

public interface ExceptionBiConsumer<T, U> {

    void accept(T t, U u) throws Exception;

}
