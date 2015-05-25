package alexiil.version;

public interface IFileTransformer {
    public byte[] transformFile(String name, byte[] data, ClassVersionWriter writer);

    public boolean matches(String name);
}
