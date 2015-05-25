package alexiil.version;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;

import alexiil.version.VersionGenerator.API;
import alexiil.version.api.Since;
import alexiil.version.api.Version;
import alexiil.version.api.VersionedApi;

public class ClassVersionReader {
    private static final String SINCE_TYPE = Type.getDescriptor(Since.class);
    private static final String VERSION_TYPE = Type.getDescriptor(Version.class);
    private static final String VERSIONED_TYPE = Type.getDescriptor(VersionedApi.class);
    private static final String VERSIONED_BETA_TYPE = Type.getDescriptor(VersionedApi.Beta.class);
    private static final String VERSIONED_FINAL_TYPE = Type.getDescriptor(VersionedApi.Final.class);

    public boolean incMajor, incMinor, incPatch;
    public List<API> apis = new ArrayList<API>();

    public ClassVersionReader() {
        incMajor = false;
        incMinor = false;
        incPatch = true;
    }

    public boolean scanClasses(String name, byte[] original, byte[] newer) {
        ClassReader reader = original == null ? null : new ClassReader(original);
        List<APIMethod> methods = new ArrayList<APIMethod>();

        // reader.accept(new TraceClassVisitor(new PrintWriter(System.out)), 0);

        if (reader != null) {
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            boolean classApi = false;
            boolean allBeta = false;
            String since = null;

            if (node.visibleAnnotations != null) {
                for (AnnotationNode an : node.visibleAnnotations) {
                    if (VERSIONED_TYPE.equals(an.desc) || VERSIONED_FINAL_TYPE.equals(an.desc))
                        classApi = true;
                    else if (VERSIONED_BETA_TYPE.equals(an.desc)) {
                        allBeta = true;
                        classApi = true;
                    }
                    else if (SINCE_TYPE.equals(an.desc)) {
                        System.out.println("Found " + an.desc);
                        since = (String) an.values.get(1);
                    }
                }
            }

            if (classApi) {
                methods.add(new APIMethod(null, null, since, allBeta));
            }

            for (MethodNode mn : node.methods) {
                boolean isApi = false;
                boolean isBeta = allBeta;
                since = null;

                if (mn.visibleAnnotations != null)
                    for (AnnotationNode an : mn.visibleAnnotations) {
                        // TODO: convert ALL invisible annotations to visible
                        if (VERSIONED_BETA_TYPE.equals(an.desc)) {
                            isApi = true;
                            isBeta = true;
                        }
                        else if (VERSIONED_FINAL_TYPE.equals(an.desc)) {
                            isApi = true;
                            isBeta = allBeta;
                        }
                        else if (VERSIONED_TYPE.equals(an.desc)) {
                            isApi = true;
                            isBeta = allBeta;
                        }
                        else if (SINCE_TYPE.equals(an.desc)) {
                            since = (String) an.values.get(1);
                        }
                    }

                if (isApi) {
                    methods.add(new APIMethod(mn.name, mn.desc, since, isBeta));
                }
            }
        }

        reader = newer == null ? null : new ClassReader(newer);

        boolean hasVersionAnnotation = false;

        if (reader == null) {
            if (methods.size() > 0) {
                boolean anyFinal = false;
                boolean anyMethods = false;
                for (APIMethod meth : methods) {
                    if (meth.name != null) {
                        anyFinal |= !meth.isBeta;
                        anyMethods = true;
                    }
                }

                if (anyMethods) {
                    incMajor |= anyFinal;
                    incMinor = true;
                }
            }
        }
        else {
            ClassNode node = new ClassNode();
            reader.accept(node, 0);

            boolean allBeta = false;

            for (AnnotationNode an : node.visibleAnnotations) {
                if (VERSIONED_BETA_TYPE.equals(an.desc)) {
                    allBeta = true;
                    break;
                }
            }

            for (Object obj : node.fields) {
                FieldNode fn = (FieldNode) obj;

                if (fn.visibleAnnotations != null)
                    for (Object objAn : fn.visibleAnnotations) {
                        AnnotationNode an = (AnnotationNode) objAn;
                        if (VERSION_TYPE.equals(an.desc)) {
                            hasVersionAnnotation = true;
                            break;
                        }
                    }
            }

            List<APIMethod> newerMethods = new ArrayList<APIMethod>();

            for (MethodNode mn : node.methods) {
                boolean isApi = false;
                boolean isBeta = allBeta;

                if (mn.visibleAnnotations != null)
                    for (AnnotationNode an : mn.visibleAnnotations) {
                        if (VERSIONED_BETA_TYPE.equals(an.desc)) {
                            isApi = true;
                            isBeta = true;
                            break;
                        }
                        if (VERSIONED_FINAL_TYPE.equals(an.desc)) {
                            isApi = true;
                            isBeta = allBeta;
                            break;
                        }
                    }

                if (isApi) {
                    newerMethods.add(new APIMethod(mn.name, mn.desc, null, isBeta));
                }
            }

            for (int i = 0; i < methods.size(); i++) {
                for (int j = 0; j < newerMethods.size(); j++) {
                    APIMethod older = methods.get(i);
                    APIMethod nw = newerMethods.get(j);
                    if (nw.pairsPerfectly(older)) {
                        apis.add(older.convertToAPI(name));
                        methods.remove(i);
                        newerMethods.remove(j);
                        i--;
                        break;
                    }
                }
            }

            boolean anyFinal = false;

            for (APIMethod meth : methods) {
                if (meth.name != null)
                    anyFinal |= !meth.isBeta;
            }

            boolean anyNewMethods = false;

            for (APIMethod meth : newerMethods) {
                if (meth.name != null && !meth.isBeta) {
                    anyNewMethods = true;
                }
                apis.add(meth.convertToAPI(name));
            }

            for (APIMethod meth : methods) {
                if (meth.name == null)
                    apis.add(meth.convertToAPI(name));
            }

            if (anyFinal) {
                incMajor = true;
                return hasVersionAnnotation;
            }
            if (anyNewMethods)
                incMinor = true;
        }
        return hasVersionAnnotation;
    }

    public class APIMethod {
        public final String name, descriptor, since;
        public final boolean isBeta;

        public APIMethod(String name, String descriptor, String since, boolean isBeta) {
            this.name = name;
            this.descriptor = descriptor;
            this.since = since;
            this.isBeta = isBeta;
        }

        public boolean pairsPerfectly(APIMethod older) {
            if (older.name == null || name == null) {
                return older.name == name;
            }
            return older.name.equals(name) && older.descriptor.equals(descriptor);
        }

        public API convertToAPI(String fileName) {
            return new API(since, fileName, name, descriptor);
        }
    }
}
