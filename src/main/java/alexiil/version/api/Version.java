package alexiil.version.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/** Annotate a String final field with this and the version of the package corresponding to the {@link #value()} will be
 * set to it. */
@Target(ElementType.FIELD)
public @interface Version {
    /** The package name of the versioned project. (For example, AlexIILLib is "alexiil.mods.lib", but this is specified at
     * build time so it doesn't have to conform to anything in particular) */
    public String value();
}
