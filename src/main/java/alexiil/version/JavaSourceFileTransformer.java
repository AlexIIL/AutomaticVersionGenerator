package alexiil.version;

public class JavaSourceFileTransformer implements IFileTransformer {
    @Override
    public byte[] transformFile(String name, byte[] data, ClassVersionWriter writer) {

        return data;
    }

    @Override
    public boolean matches(String name) {
        return name.endsWith(".java");
    }
}
