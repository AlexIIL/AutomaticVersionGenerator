package alexiil.version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;

import alexiil.version.api.VersionedApi;

@VersionedApi
public class VersionGenerator {
    private final String olderVersion, packageName;
    /** The older version of the compiled jar */
    private final File compiledOlder;
    /** The newer version of the compiled jar, which will be edited adding the annotations etc */
    private final File compiledNewer;
    /** A list of all the newer files that need to have annotations added. This can be java source and/or java compiled
     * all in one */
    private final List<File> editableFiles;
    private String newVersion;
    private List<API> apis = new ArrayList<API>();

    @VersionedApi.Final
    public VersionGenerator(String olderVersion, String packageName, File compiledOlder, File compiledNewer, List<File> otherFiles)
            throws IOException {
        this.olderVersion = olderVersion;
        this.packageName = packageName;
        this.compiledOlder = compiledOlder;
        this.compiledNewer = compiledNewer;
        editableFiles = new ArrayList<File>();
        editableFiles.addAll(otherFiles);
        if (!otherFiles.contains(compiledNewer)) {
            editableFiles.add(compiledNewer);
        }
    }

    @VersionedApi.Beta
    public String getVersion() {
        return newVersion;
    }

    @VersionedApi.Beta
    public int[] getVersionInts() {
        if (newVersion == null)
            return new int[] { -1, -1, -1 };

        String[] versions = newVersion.split("\\.");

        int major = Integer.valueOf(versions[0]);
        int minor = Integer.valueOf(versions[1]);
        int patch = Integer.valueOf(versions[2]);

        return new int[] { major, minor, patch };
    }

    /** Sets the new version to the given string. It is recommended that you call this after {@link #makeVersioning()}
     * but before {@link #editFiles()}. */
    @VersionedApi.Beta
    public void setVersion(String version) {
        newVersion = version;
    }

    @VersionedApi.Beta
    public void setVersion(int[] version) {
        if (version == null)
            throw new NullPointerException("Cannot set a null version!");
        if (version.length != 3)
            throw new IllegalArgumentException("Must have a length of 3!");
        newVersion = version[0] + "." + version[1] + "." + version[2];
    }

    /** Makes the newer version and the list of files names that need to be changed from the differences between the
     * {@link #compiledOlder} and the {@link #compiledNewer} files; */
    @VersionedApi.Final
    public void makeVersioning() {
        Map<String, byte[]> originalEntrys = populateEntrys(InternalUtils.convertToZipInputStream(compiledOlder));
        Map<String, byte[]> newerEntrys = populateEntrys(InternalUtils.convertToZipInputStream(compiledNewer));

        List<String> keys = new ArrayList<String>();
        keys.addAll(originalEntrys.keySet());
        for (String key : newerEntrys.keySet()) {
            if (!keys.contains(key))
                keys.add(key);
        }

        List<String> javaClassFiles = new ArrayList<String>();

        for (String name : keys) {
            if (name.endsWith(".class"))
                javaClassFiles.add(name);
        }

        // Scan all classes to see what API methods they contain

        ClassVersionReader rd = new ClassVersionReader();

        for (String name : javaClassFiles) {
            byte[] older = originalEntrys.containsKey(name) ? originalEntrys.get(name) : null;
            byte[] nw = newerEntrys.containsKey(name) ? newerEntrys.get(name) : null;
            String withoutExtension = name.substring(0, name.length() - ".class".length());
            rd.scanClasses(withoutExtension, older, nw);
        }

        apis.addAll(rd.apis);

        String[] versions = olderVersion.split("\\.");

        int major = Integer.valueOf(versions[0]);
        int minor = Integer.valueOf(versions[1]);
        int patch = Integer.valueOf(versions[2]);

        if (rd.incMajor) {
            major++;
            minor = 0;
            patch = 0;
        }
        else if (rd.incMinor) {
            minor++;
            patch = 0;
        }
        else if (rd.incPatch) {
            patch++;
        }

        newVersion = major + "." + minor + "." + patch;

        System.out.println("Before API conversion");
        for (API api : apis) {
            System.out.println("\t" + api);
        }

        for (int i = 0; i < apis.size(); i++) {
            API api = apis.get(i);
            if (api.firstVersion == null) {
                apis.set(i, api.convertVersionIfRequired(newVersion));
            }
        }

        System.out.println("After API conversion");
        for (API api : apis) {
            System.out.println("\t" + api);
        }
    }

    /** Actually edit the files to change them to have */
    @VersionedApi.Final
    public void editFiles() {
        ClassVersionWriter writer = new ClassVersionWriter(newVersion, packageName, apis);
        writer.editFile(compiledNewer);
        for (File file : editableFiles)
            writer.editFile(file);
    }

    private Map<String, byte[]> populateEntrys(ZipInputStream stream) {
        Map<String, byte[]> map = new HashMap<String, byte[]>();
        ZipEntry entry = null;
        try {
            while ((entry = stream.getNextEntry()) != null) {
                String name = entry.getName();
                byte[] bytes = IOUtils.toByteArray(stream);
                map.put(name, bytes);
                System.out.println(name + " mapped to " + bytes.length + " bytes");
                stream.closeEntry();
            }
        }
        catch (IOException e) {
            throw new Error(e);
        }
        return map;
    }

    /** A class that contains information about a particular API. This might not have changed between the versions of the
     * file. */
    static class API {
        /** The first version that added this APIChange */
        public final String firstVersion;
        /** The string name of the position of the file, without the file type (so "alexiil/mods/civ/CivCraft.java" would
         * be "alexiil/mods/civ/CivCraft)" */
        public final String file;
        /** The method name of the API. Will be null if this API describes a class */
        public final String methodName;
        /** The method descriptor of the API. WIll be null if this API describes is a class */
        public final String descriptor;

        public API(API api, String newVersion) {
            this(newVersion, api.file, api.methodName, api.descriptor);
        }

        public API(String firstVersion, String file, String methodName, String descriptor) {
            this.firstVersion = firstVersion;
            this.file = file;
            this.methodName = methodName;
            this.descriptor = descriptor;
        }

        /** This checks to see if the string following an @VersionedAPI annotation. */
        public boolean matchesSource(String methodHeader) {
            return false;// TODO: this
        }

        public API convertVersionIfRequired(String newVersion) {
            System.out.println("convertVersion(" + newVersion + ") " + this);
            if (firstVersion != null && firstVersion.length() != 0)
                return this;
            System.out.println("Converted to new version " + newVersion);
            return new API(this, newVersion);
        }

        @Override
        public String toString() {
            return "API [firstVersion=" + firstVersion + ", file=" + file + ", methodName=" + methodName + ", descriptor=" + descriptor + "]";
        }
    }
}
