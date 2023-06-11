package cc.cornerstones.arbutus.tinyid.service;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD})
public @interface TinyId {

    String bizType();

    long beginId() default 1;

    long maxId() default 1;

    int step() default 100;

    int remainder() default 1;

    int delta() default 1;
}
