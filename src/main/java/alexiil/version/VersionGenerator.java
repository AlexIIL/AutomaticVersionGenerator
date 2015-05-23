package alexiil.version;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

public class VersionGenerator {
    /** arg[0] MUST be the file path to the original jar file, that it is constructed from.
     * <p>
     * arg[1] MUST be the version number the original jar file is.
     * <p>
     * arg[2] MUST be the file path to the newer jar file, that is to be read for API changes to the older version */
    public static void main(String[] args) {
        if (args.length != 3) {
            throw new Error("Incorrect number of arguments!");
        }
        String origonal = args[0];
        String version = args[1];
        String newer = args[2];
        
        String[] versionSplit = version.split("\\.");
        
        if (versionSplit.length < 3)
            throw new Error("Version argument MUST be at least 3 versions long (got " + versionSplit + ")");
        
        int major = Integer.parseInt(versionSplit[0]);
        int minor = Integer.parseInt(versionSplit[1]);
        int patch = Integer.parseInt(versionSplit[2]);
        
        File origonalFile = new File(origonal);
        File newerFile = new File(newer);
        
        ZipInputStream zisO;
        try {
            zisO = new ZipInputStream(new FileInputStream(origonalFile));
        }
        catch (FileNotFoundException e) {
            throw new Error("Could not find the origonal input file!", e);
        }
        ZipInputStream zisN;
        try {
            zisN = new ZipInputStream(new FileInputStream(newerFile));
        }
        catch (FileNotFoundException e) {
            try {
                zisO.close();
            }
            catch (IOException ignored) {}
            throw new Error("Couold not find the newer input file!", e);
        }
        
        byte[] newFile = generateVersion(zisO, zisN, major, minor, patch);
        try {
            zisO.close();
            zisN.close();
        }
        catch (Throwable ignored) {}
        
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(newerFile);
        }
        catch (FileNotFoundException e) {
            throw new Error("Could not find the input file!", e);
        }
        
        try {
            fos.write(newFile);
        }
        catch (IOException e) {
            throw new Error("Could not write to the output file!", e);
        }
        finally {
            try {
                fos.close();
            }
            catch (IOException ignored) {}
        }
    }
    
    public static byte[] generateVersion(ZipInputStream origonal, ZipInputStream newer, int major, int minor, int patch) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        
        Map<String, byte[]> originalEntrys = populateEntrys(origonal);
        Map<String, byte[]> newerEntrys = populateEntrys(newer);
        
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
        
        boolean incMajor = false;
        boolean incMinor = false;
        boolean incPatch = true;
        
        // Scan all classes to see what API methods they contain
        
        ClassVersionReader rd = new ClassVersionReader();
        
        for (String name : javaClassFiles) {
            byte[] older = originalEntrys.containsKey(name) ? originalEntrys.get(name) : null;
            byte[] nw = newerEntrys.containsKey(name) ? newerEntrys.get(name) : null;
            rd.scanClasses(older, nw);
        }
        
        if (incMajor) {
            major++;
            minor = 0;
            patch = 0;
        }
        else if (incMinor) {
            minor++;
            patch = 0;
        }
        else if (incPatch) {
            patch++;
        }
        
        String version = major + "." + minor + "." + patch;
        
        // Output the newer version of the class to see what it should be
        // TODO: make the output, and WRITE SOME TESTS
        
        return baos.toByteArray();
    }
    
    private static Map<String, byte[]> populateEntrys(ZipInputStream stream) {
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
    
    public static byte[] getVersionedEntry(byte[] origonal, byte[] newer, String version) {
        return null;
    }
}
