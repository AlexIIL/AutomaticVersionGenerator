package alexiil.version.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** This is an annotation that is added by the version generator that will contain the string version number when it was
 * first added to the project. */
@Retention(RetentionPolicy.RUNTIME)
public @interface Since {
    public String value();
}
