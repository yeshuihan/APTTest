package com.yeshuihan.clickbindprocessor;

import static java.util.Objects.requireNonNull;

import com.squareup.javapoet.CodeBlock;
import com.sun.source.util.Trees;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.processing.JavacFiler;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeScanner;
import com.yeshuihan.clickbindannotation.Click;
import com.yeshuihan.clickbindannotation.LongClick;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;

@SupportedAnnotationTypes({"com.yeshuihan.clickbindannotation.Click","com.yeshuihan.clickbindannotation.LongClick"})
public class ClickBindProcessor extends AbstractProcessor {

    List<String> createdFile = new ArrayList<>();
    Messager messager;
    Trees trees;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        messager = processingEnv.getMessager();
        trees = Trees.instance(processingEnv);
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        Filer filer = processingEnv.getFiler();
        if (!roundEnvironment.processingOver()) {
            Map<String, List<Element>> classAnnotationMethod = new HashMap<>();
            List<Element> classCache = new ArrayList<>();
            analysisElement(roundEnvironment, Click.class, classCache, classAnnotationMethod);  //分析出类与方法
            for (Element element : classCache) {
                createClickJavaFile((TypeElement) element, classAnnotationMethod.get(element.toString()));
            }

            classAnnotationMethod.clear();
            classCache.clear();
            analysisElement(roundEnvironment, LongClick.class, classCache, classAnnotationMethod);  //分析出类与方法
            for (Element element : classCache) {
                createLongClickJavaFile((TypeElement) element, classAnnotationMethod.get(element.toString()));
            }
        }
        return true;
    }



    private void analysisElement(RoundEnvironment roundEnvironment, Class<? extends Annotation> classes, List<Element> classCache, Map<String, List<Element>> classAnnotationMethod) {
        Set<? extends Element> set2 = roundEnvironment.getElementsAnnotatedWith(classes);
        Element typeElement;
        List<Element> methodElementList;
        for (Element element : set2) {
            if (element.getKind() == ElementKind.METHOD) {
                typeElement = element.getEnclosingElement();
                methodElementList = classAnnotationMethod.get(typeElement.toString());
                if (methodElementList == null) {
                    classCache.add(typeElement);
                    methodElementList = new ArrayList<>();
                    classAnnotationMethod.put(typeElement.toString(), methodElementList);
                }
                methodElementList.add(element);
            } else {
                // 其他情况异常
                throw new RuntimeException();
            }
        }
    }


    private void createClickJavaFile(TypeElement classElement, List<Element> methodElement) {
        if (methodElement.isEmpty()) {
            return;
        }

        String className = classElement.getSimpleName().toString();
        String packageName = classElement.getEnclosingElement().toString();
        String createClassName = className + "ClickBind";
        String createFileName = packageName+"." + createClassName;


        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("package ");
        stringBuilder.append(packageName);
        stringBuilder.append(";\n\n");
        stringBuilder.append("import android.view.View;\n");

        stringBuilder.append("public class " + createClassName + " implements View.OnClickListener {\n");
        stringBuilder.append("    " + className + " target;\n\n");
        stringBuilder.append("    public " + createClassName + "("+className+" target){\n");
        stringBuilder.append("        this.target = target;\n");
        for (Element element : methodElement) {
            Click annotation = element.getAnnotation(Click.class);
            stringBuilder.append("        target.findViewById("+annotation.value()+").setOnClickListener(this);\n");
        }
        stringBuilder.append("    }\n\n");

        stringBuilder.append(
                "    @Override\n" +
                "    public void onClick(View v) {\n" +
                        "        int id = v.getId();\n"
        );

        for (Element element : methodElement) {
            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) element;
            Click annotation = element.getAnnotation(Click.class);
            String methodName = methodSymbol.name.toString();
            com.sun.tools.javac.util.List<Symbol.VarSymbol> params = methodSymbol.params();

            Set<Modifier> modifiers = methodSymbol.getModifiers();
            if (!modifiers.contains(Modifier.PUBLIC)) {
                messager.printMessage(Diagnostic.Kind.WARNING, className + "." + methodName + "为非public 方法，不支持");
                continue;
            }

            if (modifiers.contains(Modifier.STATIC)) {
                messager.printMessage(Diagnostic.Kind.WARNING, className + "." + methodName + "为 static 方法，不支持");
                continue;
            }

            if (params.size() > 1 || (params.size() == 1 && !params.get(0).type.toString().equals("android.view.View"))) {
                messager.printMessage(Diagnostic.Kind.ERROR, "Click 修饰的方法，最多支持一个参数，并且该参数必须为android.view.View类型");
                return;
            }  else {
                if (params.size() == 0) {
                    stringBuilder.append(
                            "        if (id == "+annotation.value()+") {\n" +
                                    "            target."+methodName+"();\n" +
                                    "        }\n"
                    );
                } else {
                    stringBuilder.append(
                            "        if (id == "+annotation.value()+") {\n" +
                                    "            target."+methodName+"(v);\n" +
                                    "        }\n"
                    );
                }
            }
        }
        stringBuilder.append("    }\n");
        stringBuilder.append("}\n");

        writeJavaFile(stringBuilder.toString(), createFileName);
    }


    private void createLongClickJavaFile(TypeElement classElement, List<Element> methodElement) {
        if (methodElement.isEmpty()) {
            return;
        }

        String className = classElement.getSimpleName().toString();
        String packageName = classElement.getEnclosingElement().toString();
        String createClassName = className + "LongClickBind";
        String createFileName = packageName+"." + createClassName;


        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("package ");
        stringBuilder.append(packageName);
        stringBuilder.append(";\n\n");
        stringBuilder.append("import android.view.View;\n");

        stringBuilder.append("public class " + createClassName + " implements View.OnLongClickListener {\n");
        stringBuilder.append("    " + className + " target;\n\n");
        stringBuilder.append("    public " + createClassName + "("+className+" target){\n");
        stringBuilder.append("        this.target = target;\n");
        for (Element element : methodElement) {
            LongClick annotation = element.getAnnotation(LongClick.class);
            stringBuilder.append("        target.findViewById("+annotation.value()+").setOnLongClickListener(this);\n");
        }
        stringBuilder.append("    }\n\n");

        stringBuilder.append(
                "    @Override\n" +
                        "    public boolean onLongClick(View v) {\n" +
                        "        int id = v.getId();\n"
        );

        for (Element element : methodElement) {
            Symbol.MethodSymbol methodSymbol = (Symbol.MethodSymbol) element;
            LongClick annotation = element.getAnnotation(LongClick.class);
            String methodName = methodSymbol.name.toString();
            com.sun.tools.javac.util.List<Symbol.VarSymbol> params = methodSymbol.params();
            if (params.size() > 1 || (params.size() == 1 && !params.get(0).type.toString().equals("android.view.View"))) {
                messager.printMessage(Diagnostic.Kind.ERROR, "LongClick 修饰的方法，最多支持一个参数，并且该参数必须为android.view.View类型");
                return;
            } else {
                if (params.size() == 0) {
                    stringBuilder.append(
                            "        if (id == "+annotation.value()+") {\n" +
                            "            target."+methodName+"();\n" +
                            "        }\n"
                    );

                } else {
                    stringBuilder.append(
                            "        if (id == "+annotation.value()+") {\n" +
                            "            target."+methodName+"(v);\n" +
                            "        }\n"
                    );
                }
            }
        }

        stringBuilder.append("        return true;\n");
        stringBuilder.append("    }\n");
        stringBuilder.append("}\n");

        writeJavaFile(stringBuilder.toString(), createFileName);
    }


    public void writeJavaFile(String str, String fileName) {
        BufferedWriter writer = null;
        try {
            JavacFiler filer = (JavacFiler) processingEnv.getFiler();
            FileObject fileObject = filer.createSourceFile(fileName);
            writer = new BufferedWriter(fileObject.openWriter());
            writer.write(str);
            writer.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }



}
