package alexiil.version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) {
        String oldVersion = "2.0.0";
        String packageName = "test.alexiil.version";
        File older = new File("./ThingyAPIver7.jar");
        File newer = new File("./ThingyAPIver8.jar");
        List<File> others = new ArrayList<File>();

        try {
            VersionGenerator vg = new VersionGenerator(oldVersion, packageName, older, newer, others);
            System.out.println("Created the version Generator");
            vg.makeVersioning();
            System.out.println("Made the versioning");
            vg.editFiles();
            System.out.println("Edited the files");
        }
        catch (IOException io) {
            throw new Error(io);
        }
    }
}
