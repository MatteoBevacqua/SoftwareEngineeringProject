package server.support;

import java.io.Serializable;
import java.util.Objects;

public class MyOptional<T> implements Serializable {
    private final T value;

    public MyOptional(T val) {
        value = val;
    }

    public static <T> MyOptional<T> empty() {
        return new MyOptional<T>(null);
    }

    public static <T> MyOptional<T> of(T value) {
        return new MyOptional<>(Objects.requireNonNull(value));
    }

    public boolean isPresent() {
        return value != null;
    }

    public boolean isEmpty() {
        return value == null;
    }

    public T get() {
        if (isEmpty()) throw new RuntimeException("No value");
        return value;
    }

}

