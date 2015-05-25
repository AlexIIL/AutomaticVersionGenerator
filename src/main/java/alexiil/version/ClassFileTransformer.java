package alexiil.version;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import alexiil.version.VersionGenerator.API;
import alexiil.version.api.Since;
import alexiil.version.api.Version;
import alexiil.version.api.VersionedApi;

public class ClassFileTransformer implements IFileTransformer {
    private static final String SINCE_TYPE = Type.getDescriptor(Since.class);
    private static final String STRING_TYPE = Type.getDescriptor(String.class);
    private static final String VERSION_TYPE = Type.getDescriptor(Version.class);
    private static final String VERSIONED_TYPE = Type.getDescriptor(VersionedApi.class);
    private static final String VERSIONED_BETA_TYPE = Type.getDescriptor(VersionedApi.Beta.class);
    private static final String VERSIONED_FINAL_TYPE = Type.getDescriptor(VersionedApi.Final.class);

    @Override
    public byte[] transformFile(String name, byte[] data, ClassVersionWriter writer) {
        List<API> apis = new ArrayList<API>();
        System.out.println("File API search (" + name + ")");
        for (API api : writer.apis) {
            System.out.println("\t" + api);
            if (api.file.equals(name))
                apis.add(api);
        }

        ClassReader cr = new ClassReader(data);
        ClassNode classNode = new ClassNode();
        cr.accept(classNode, 0);

        boolean hasAnnotation = false;

        if (classNode.visibleAnnotations != null)
            for (AnnotationNode an : classNode.visibleAnnotations) {
                if (VERSIONED_TYPE.equals(an.desc) || VERSIONED_BETA_TYPE.equals(an.desc) || VERSIONED_FINAL_TYPE.equals(an.desc)) {
                    hasAnnotation = true;
                    break;
                }
            }

        if (hasAnnotation) {
            API api = null;
            System.out.println("Class API Search");
            for (API apiInList : apis) {
                System.out.println("\t" + apiInList);
                if (apiInList.methodName == null) {
                    api = apiInList;
                }
            }
            System.out.println("api = " + api);

            if (api != null) {
                if (classNode.visibleAnnotations == null)
                    classNode.visibleAnnotations = new ArrayList<AnnotationNode>();
                else {
                    for (int i = 0; i < classNode.visibleAnnotations.size(); i++) {
                        AnnotationNode an = classNode.visibleAnnotations.get(i);
                        if (an.desc.equals(SINCE_TYPE)) {
                            classNode.visibleAnnotations.remove(i);
                            i--;
                        }
                    }
                }
                AnnotationNode an = new AnnotationNode(SINCE_TYPE);
                an.values = new ArrayList<Object>();
                an.values.add("value");
                an.values.add(api.firstVersion);
                classNode.visibleAnnotations.add(an);
            }
        }

        for (FieldNode fieldNode : classNode.fields) {
            hasAnnotation = false;
            if (fieldNode.visibleAnnotations != null)
                for (AnnotationNode an : fieldNode.visibleAnnotations) {
                    if (VERSION_TYPE.equals(an.desc)) {
                        // if (writer.packageName.equals(an.values.get(1)))
                        // TODO: Uncomment this!
                        hasAnnotation = true;
                    }
                }
            if (hasAnnotation) {
                if (STRING_TYPE.equals(fieldNode.desc)) {
                    fieldNode.value = writer.versionNumber;
                }
            }
        }

        for (MethodNode methodNode : classNode.methods) {
            hasAnnotation = false;
            if (methodNode.visibleAnnotations != null)
                for (AnnotationNode an : methodNode.visibleAnnotations) {
                    if (VERSIONED_TYPE.equals(an.desc) || VERSIONED_BETA_TYPE.equals(an.desc) || VERSIONED_FINAL_TYPE.equals(an.desc)) {
                        hasAnnotation = true;
                    }
                }
            if (hasAnnotation) {
                API api = null;
                for (API apiInList : apis) {
                    if (apiInList.methodName == null || apiInList.descriptor == null) {
                        continue;
                    }
                    if (apiInList.methodName.equals(methodNode.name) && apiInList.descriptor.equals(methodNode.desc)) {
                        api = apiInList;
                        break;
                    }
                }
                if (api != null) {
                    AnnotationNode newAnnotation = new AnnotationNode(SINCE_TYPE);
                    newAnnotation.values = new ArrayList<Object>();
                    newAnnotation.values.add("value");
                    newAnnotation.values.add(api.firstVersion);
                    if (methodNode.visibleAnnotations == null)
                        methodNode.visibleAnnotations = new ArrayList<AnnotationNode>();
                    else {
                        for (int i = 0; i < methodNode.visibleAnnotations.size(); i++) {
                            AnnotationNode an = methodNode.visibleAnnotations.get(i);
                            if (an.desc.equals(SINCE_TYPE)) {
                                methodNode.visibleAnnotations.remove(i);
                                i--;
                            }
                        }
                    }
                    methodNode.visibleAnnotations.add(newAnnotation);
                }
            }
        }

        ClassWriter cw = new ClassWriter(0);
        classNode.accept(cw);

        return cw.toByteArray();
    }

    @Override
    public boolean matches(String name) {
        return name.endsWith(".class");
    }
}
