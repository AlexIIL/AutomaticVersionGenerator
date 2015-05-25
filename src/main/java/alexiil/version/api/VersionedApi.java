package alexiil.version.api;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** The sub-annotations are used to determine what stage the api is at, and is documented in each one. Any class or
 * method annotated with @VersionedApi, @Beta or @Final will have the @Since annotation applied to them at build time. */
@Retention(RetentionPolicy.RUNTIME)
public @interface VersionedApi {
    /** An API that is marked as @Beta doesn't change the major version if it is removed, marked as deprecated or has its
     * method's arguments changed, only the minor part. If a class is marked as @Beta then all methods are considered to
     * be @Beta as well, even if they are marked as @Final. */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Beta {}

    /** A method that is marked as @Final changes the MAJOR part if it is removed or changed in a non-backwards
     * compatible manor (the method's arguments change), the MINOR part changes if it is changed in a backwards
     * compatible manor (Marked as @Deprecated or is added completely). */
    @Retention(RetentionPolicy.RUNTIME)
    public @interface Final {}
}
