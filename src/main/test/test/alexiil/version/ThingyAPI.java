package test.alexiil.version;

import alexiil.version.api.Version;
import alexiil.version.api.VersionedApi;

@VersionedApi
public class ThingyAPI {
    @Version("test.alexiil.version")
    public static String version;

    @Version("test.alexiil.version")
    public String fieldVersion;

    @Version("test.alexiil.version")
    public static int intVersion;

    public static int newField;

    @VersionedApi.Final
    public static void aDifferentThing(int arg1, double arg2) {
        for (int i = 0; i < arg1; i++)
            anInternalThing(arg2++);
    }

    private static void anInternalThing(double value) {}

    @VersionedApi.Beta
    public static void aLol(int abcdef) {}
}
