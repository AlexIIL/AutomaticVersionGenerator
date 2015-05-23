package alexiil.version;

import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import alexiil.version.api.VersionedApi;

public class ClassVersionReader {
    private static final String VERSIONED_BETA_TYPE = Type.getDescriptor(VersionedApi.Beta.class);
    private static final String VERSIONED_FINAL_TYPE = Type.getDescriptor(VersionedApi.Final.class);
    
    public boolean incMajor, incMinor, incPatch;
    
    public ClassVersionReader() {
        incMajor = false;
        incMinor = false;
        incPatch = false;
    }
    
    public void scanClasses(byte[] original, byte[] newer) {
        ClassReader reader = original == null ? null : new ClassReader(original);
        List<APIMethod> methods = new ArrayList<APIMethod>();
        
        if (reader != null) {
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            
            boolean allBeta = false;
            
            for (Object obj : node.visibleAnnotations) {
                AnnotationNode an = (AnnotationNode) obj;
                if (VERSIONED_BETA_TYPE.equals(an.desc)) {
                    allBeta = true;
                    break;
                }
            }
            
            for (Object obj : node.methods) {
                MethodNode mn = (MethodNode) obj;
                boolean isApi = false;
                boolean isBeta = allBeta;
                
                for (Object objAn : mn.visibleAnnotations) {
                    AnnotationNode an = (AnnotationNode) objAn;
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
                    methods.add(new APIMethod(mn.name, mn.desc, isBeta));
                }
            }
        }
        
        reader = newer == null ? null : new ClassReader(newer);
        
        if (reader == null) {
            if (methods.size() > 0) {
                boolean anyFinal = false;
                for (APIMethod meth : methods) {
                    anyFinal |= !meth.isBeta;
                }
                
                incMajor |= anyFinal;
                incMinor = true;
            }
        }
        else {
            ClassNode node = new ClassNode();
            reader.accept(node, 0);
            
            boolean allBeta = false;
            
            for (Object obj : node.visibleAnnotations) {
                AnnotationNode an = (AnnotationNode) obj;
                if (VERSIONED_BETA_TYPE.equals(an.desc)) {
                    allBeta = true;
                    break;
                }
            }
            
            List<APIMethod> newerMethods = new ArrayList<APIMethod>();
            
            for (Object obj : node.methods) {
                MethodNode mn = (MethodNode) obj;
                boolean isApi = false;
                boolean isBeta = allBeta;
                
                for (Object objAn : mn.visibleAnnotations) {
                    AnnotationNode an = (AnnotationNode) objAn;
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
                    newerMethods.add(new APIMethod(mn.name, mn.desc, isBeta));
                }
            }
            
            for (int i = 0; i < methods.size(); i++) {
                for (int j = 0; j < newerMethods.size(); j++) {
                    APIMethod older = methods.get(i);
                    APIMethod nw = newerMethods.get(j);
                    if (nw.pairsPerfectly(older)) {
                        methods.remove(i);
                        newerMethods.remove(j);
                        i--;
                        break;
                    }
                }
            }
            
            boolean anyFinal = false;
            
            for (APIMethod meth : methods) {
                anyFinal |= !meth.isBeta;
            }
            
            if (anyFinal) {
                incMajor = true;
                return;
            }
            incMinor = true;
        }
    }
    
    public class APIMethod {
        public final String name, descriptor;
        public final boolean isBeta;
        
        public APIMethod(String name, String descriptor, boolean isBeta) {
            this.name = name;
            this.descriptor = descriptor;
            this.isBeta = isBeta;
        }
        
        public boolean pairsPerfectly(APIMethod older) {
            return older.name.equals(name) && older.descriptor.equals(descriptor);
        }
    }
}
