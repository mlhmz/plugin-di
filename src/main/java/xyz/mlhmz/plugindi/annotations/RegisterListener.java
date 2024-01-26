package xyz.mlhmz.plugindi.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterListener {
    String module() default "default";
}
