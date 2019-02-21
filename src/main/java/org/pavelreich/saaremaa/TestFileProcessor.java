package org.pavelreich.saaremaa;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spoon.Launcher;
import spoon.compiler.SpoonResource;
import spoon.compiler.SpoonResourceHelper;
import spoon.processing.AbstractProcessor;
import spoon.reflect.CtModel;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtExpression;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.*;
import spoon.reflect.reference.CtExecutableReference;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Analyse test classes,
 * extract methods with annotations
 * extract fields with types and annotations.
 *
 */
public class TestFileProcessor extends AbstractProcessor<CtClass> {

    private static final Logger LOG = LoggerFactory.getLogger(TestFileProcessor.class);

    private List<MyClass> elements = new CopyOnWriteArrayList<>();

    public List<MyClass> getElements() {
        return elements;
    }

    @Override
    public void process(CtClass ctClass) {
        MyClass myClass = new MyClass(ctClass);
        if (myClass.hasTests()) {
            LOG.info("myClass:" + myClass);
            this.elements.add(myClass);
        }

    }

    public static void main(String[] args) {
        try {
            run(args[0]);
        } catch (FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    public static TestFileProcessor run(String pathname) throws FileNotFoundException {
        Launcher launcher = new Launcher();
        SpoonResource resource = SpoonResourceHelper.createResource(new File(pathname));
        launcher.addInputResource(resource);
        launcher.buildModel();

        CtModel model = launcher.getModel();
        TestFileProcessor processor = new TestFileProcessor();
        model.processWith(processor);
        return processor;
    }



    static class MyClass {

        private final CtClass ctClass;
        private Map<String, MyMethod> methods = new HashMap<>();
        private Set<String> annotations = new HashSet<>();
        private List<MyField> fields = new ArrayList<>();

        public MyClass(CtClass ctClass) {
            this.ctClass = ctClass;
            try {
                this.annotations = getAnnotations(ctClass);
            } catch (Exception e) {
                LOG.error("Error occured for annotations of class:" + ctClass + ", error: " + e.getMessage(), e);
            }
            try {
                Set<CtMethod> allMethods = ctClass.getAllMethods();
                this.methods = allMethods.stream().map(x -> new MyMethod(x)).filter(p -> p.isPublicVoidMethod()).collect(Collectors.toMap(e -> e.simpleName, e -> e));
            } catch (Exception e) {
                LOG.error("Error occured for methods of class:" + ctClass + ", error: " + e.getMessage(), e);
            }
            try {
                List<CtField> fields = ctClass.getFields();
                this.fields = fields.stream().map(x -> new MyField(x)).collect(Collectors.toList());
            } catch (Exception e) {
                LOG.error("Error occured for fields of class:" + ctClass + ", error: " + e.getMessage(), e);
            }
        }

        boolean hasTests() {
            return this.methods.values().stream().anyMatch(p->p.isTest());
        }

        List<MyMethod> getTestMethods() {
            return this.methods.values().stream().filter(p->p.isTest()).collect(Collectors.toList());
        }

        List<MyField> getMockFields() {
            return this.fields.stream().filter(p->!p.getMockType().isEmpty()).collect(Collectors.toList());
        }

        @Override
        public String toString() {
            List<MyMethod> testMethods = getTestMethods();
            List<MyField> mockFields = getMockFields();
            return "ctClass=" + ctClass.getQualifiedName() + ", methods.size=" + testMethods.size() + ":" + testMethods + ", fields.size=" + mockFields.size() + ":" + mockFields;
        }
    }

    static class MyField {

        private final CtField ctField;
        private final Set<String> annotations;
        private final String simpleName;
        private final String typeName;
        private final CtExpression defaultExpression;

        public MyField(CtField ctField) {
            this.ctField = ctField;
            this.simpleName = ctField.getSimpleName();
            this.annotations = getAnnotations(ctField);
            this.typeName = ctField.getType().getQualifiedName();
            this.defaultExpression = ctField.getDefaultExpression();
        }

        public String getMockType() {
            if (defaultExpression instanceof CtInvocation) {
                CtExecutableReference exec = ((CtInvocation) defaultExpression).getExecutable();
                if (!exec.getSimpleName().contains("mock")) {
                    return "";
                }
                List arguments = ((CtInvocation) defaultExpression).getArguments();
                Optional found = arguments.stream().filter(p -> p.toString().contains(".class")).findFirst();
                if (found.isPresent()) {
                    return String.valueOf(found.get()).replace(".class","");
                }
            }
            if (annotations.contains("Mock")) {
                return typeName;
            }
            return "";
        }

        @Override
        public String toString() {
            return "MyField[simpleName=" + simpleName + ", typeName=" + typeName + ", annotations=" + annotations + ", mockType=" + getMockType() + "]";
        }
    }

    static class MyMethod {

        final Set<String> annotations;
        final String simpleName;
        final CtMethod method;

        public MyMethod(CtMethod e) {
            this.simpleName = e.getSimpleName();
            this.annotations = getAnnotations(e);
            this.method = e;
        }


        private boolean isPublicVoidMethod() {
            CtMethod p = method;
            return p.getParameters().isEmpty() && p.isPublic() && isVoid(p);
        }

        boolean isTest() {
            return annotations.contains("Test");
        }
        private boolean isVoid(CtMethod p) {
            String simpleName = p.getType().getSimpleName();
            return simpleName.contains("void");
        }

        int lineCount() {
            CtBlock body = this.method.getBody();
            int loc = body.toString().split("\n").length;
            return loc;
        }

        int statementCount() {
            List<CtStatement> statements = method.getBody().getStatements();
            return statements.size();
        }

        @Override
        public String toString() {
            return "MyMethod[simpleName=" + simpleName + ", annotations=" + annotations + ", LOC=" + lineCount() + ", statementCount=" + statementCount() + "]";
        }
    }

    private static Set<String> getAnnotations(CtElement element) {
        return element.getAnnotations().stream().map(a -> a.getAnnotationType().getSimpleName()).collect(Collectors.toSet());
    }

}
