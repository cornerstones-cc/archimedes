package cc.cornerstones.biz.share.types;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD})
public @interface ResourceReferenceHandler {
    String name();

    /**
     * init, invoked when JobThread init
     */
    String init() default "";

    /**
     * destroy, invoked when JobThread destroy
     */
    String destroy() default "";
}
