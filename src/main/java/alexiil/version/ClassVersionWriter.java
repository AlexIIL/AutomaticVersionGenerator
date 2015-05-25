package alexiil.version;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.IOUtils;

import alexiil.version.VersionGenerator.API;

public class ClassVersionWriter {
    private static List<IFileTransformer> fileTransformers;

    static {
        fileTransformers = new ArrayList<IFileTransformer>();
        fileTransformers.add(new ClassFileTransformer());
        fileTransformers.add(new JavaSourceFileTransformer());
        // Scala source?
    }

    public final String versionNumber, packageName;
    public final List<API> apis;

    public ClassVersionWriter(String versionNumber, String packageName, List<API> apis) {
        this.versionNumber = versionNumber;
        this.packageName = packageName;
        this.apis = Collections.unmodifiableList(apis);

    }

    public void editFile(File file) {
        ZipInputStream zis = InternalUtils.convertToZipInputStream(file);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);
        try {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                byte[] data = IOUtils.toByteArray(zis);
                zis.closeEntry();
                byte[] newData = data;
                String withoutExtension = name.substring(0, name.lastIndexOf('.'));
                for (IFileTransformer trans : fileTransformers) {
                    if (trans.matches(name)) {
                        newData = trans.transformFile(withoutExtension, data, this);
                    }
                }
                ZipEntry nze = new ZipEntry(name);
                zos.putNextEntry(nze);
                zos.write(newData);
                zos.closeEntry();
            }
            zos.close();
            zis.close();
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(baos.toByteArray());
            fos.close();
        }
        catch (IOException io) {
            throw new Error("Could not edit the file " + file, io);
        }
    }
}
