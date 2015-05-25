package alexiil.version;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.zip.ZipInputStream;

public class InternalUtils {
    public static ZipInputStream convertToZipInputStream(File file) {
        try {
            return new ZipInputStream(new FileInputStream(file));
        }
        catch (FileNotFoundException e) {
            throw new Error("Could not find " + file + ", aborting", e);
        }
    }
}
