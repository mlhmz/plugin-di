package xyz.mlhmz.plugindi.exceptions;

public class UnknownAnnotationType extends RuntimeException {
    public UnknownAnnotationType(Class<?> clazz) {
        super(clazz.getSimpleName() + " has no corresponding annotation.");
    }

    public UnknownAnnotationType(Object object) {
        this(object.getClass());
    }
}
