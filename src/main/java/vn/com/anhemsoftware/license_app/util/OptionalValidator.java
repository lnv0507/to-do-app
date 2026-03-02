package vn.com.anhemsoftware.license_app.util;

import io.micrometer.common.util.StringUtils;

import java.util.function.Function;
import java.util.function.Supplier;

public class OptionalValidator<T>
{
    private final T value;
    private OptionalValidator(T value)
    {
        this.value = value;
    }

    public static <T> OptionalValidator<T> of(T value, String errorMessage)
    {
        if(value == null)
        {
            throw new IllegalArgumentException(errorMessage);
        }
        return new OptionalValidator<T>(value);
    }

    public static <T, E extends Exception> OptionalValidator<T> of(T value, Supplier<E> exception) throws Exception
    {
        if(value == null)
        {
            throw exception.get();
        }
        return new OptionalValidator<T>(value);
    }

    public  <R,E extends Exception> OptionalValidator<R> mapOrThrow(Function<T,R> mapper, Supplier<E> exception) throws Exception
    {
        R result= mapper.apply(value);
        if(result == null)
        {
            throw exception.get();
        }
        return new OptionalValidator<R>(result);
    }

    public <R> OptionalValidator<R> mapOrThrow(Function<T,R> mapper, String errorMessage)
    {
        R result= mapper.apply(value);
        if(result == null)
        {
            throw new IllegalArgumentException(errorMessage);
        }
        return new OptionalValidator<R>(result);
    }

    public <R> OptionalValidator<T> requireNonNull(Function<T,R> mapper, String errorMessage)
    {
        R result= mapper.apply(value);
        if(result == null)
        {
            throw new IllegalArgumentException(errorMessage);
        }
        return this;
    }

    public  OptionalValidator<T> requireNonBlank(Function<T,String> mapper, String errorMessage)
    {
        String result= mapper.apply(value);
        if(StringUtils.isBlank(result))
        {
            throw new IllegalArgumentException(errorMessage);
        }
        return this;
    }

    public T get()
    {
        return value;
    }
}
