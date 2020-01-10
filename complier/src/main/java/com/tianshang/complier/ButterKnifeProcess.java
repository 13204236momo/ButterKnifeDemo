package com.tianshang.complier;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.tianshang.annotation.BindView;
import com.tianshang.annotation.OnClick;
import com.tianshang.complier.utils.Constants;
import com.tianshang.complier.utils.EmptyUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;

//用来生成 META-INF/services/javax.annotation.processing.Processor 文件
@AutoService(Processor.class)
//允许/支持的注解类型，让注解处理器处理
@SupportedAnnotationTypes({Constants.BINDVIEW_ANNOTATION_TYPES, Constants.ONCLICK_ANNOTATION_TYPES})
//指定jdk的编译版本
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class ButterKnifeProcess extends AbstractProcessor {

    //操作Element工具类（类，函数，属性都是Element）
    private Elements elementUtils;
    //type(类信息)工具类，包含用于操作TypeMirror的工具方法
    private Types typeUtils;
    //Messager 用来报告错误，警告和其他提示信息
    private Messager messager;
    //文件生成器 类、资源，Filter用来创建新的类，class文件以及辅助文件
    private Filer filer;
    //key:类节点，value：被@BindView注解的属性集合
    private Map<TypeElement, List<VariableElement>> tempBindViewMap = new HashMap<>();
    //key:类节点，value：被@OnClick注解的方法集合
    private Map<TypeElement, List<ExecutableElement>> tempOnClickMap = new HashMap<>();

    /**
     * 用于一些初始化的操作，通过processingEnvironment参数可以获取一些有用的工具类
     *
     * @param processingEnvironment
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        //初始化
        elementUtils = processingEnvironment.getElementUtils();
        messager = processingEnvironment.getMessager();
        filer = processingEnvironment.getFiler();
        typeUtils = processingEnvironment.getTypeUtils();
        messager.printMessage(Diagnostic.Kind.NOTE,
                "注解处理器初始化完成，开始处理注解------------------------------->");

    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //允许处理的注解的元素不为空（即有元素被@BindView或@OnClick标记）
        if (!EmptyUtils.isEmpty(set)) {
            //获取所有被@BindView注解的元素集合
            Set<? extends Element> bindViewElements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
            Set<? extends Element> onClickElements = roundEnvironment.getElementsAnnotatedWith(OnClick.class);

            if (!EmptyUtils.isEmpty(bindViewElements) || !EmptyUtils.isEmpty(onClickElements)) {
                //赋值临时map存储，用来存储被注解的属性集合
//                比如MainActivity中有两个控件
//                @BindView(R.id.tv_1)
//                TextView tv1;
//                @BindView(R.id.tv_2)
//                TextView tv2;
//                以这种方式存储下来
//                {"MainActivity_ViewBinding":列表{R.id.tv_1,R.id.tv_2}}
//                onClick同理
                valueOfMap(bindViewElements, onClickElements);
                // 生成类文件，如：
                try {
                    createJavaFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
        }
        return false;
    }

    private void createJavaFile() throws IOException {
//判断是否有需要生成的类文件
        if (!EmptyUtils.isEmpty(tempBindViewMap)) {

            //获取接口的类型
            TypeElement viewBinderType = elementUtils.getTypeElement(Constants.VIEWBINDER);

            //从下往上写（javaPoet的技巧）
            for (Map.Entry<TypeElement, List<VariableElement>> entry : tempBindViewMap.entrySet()) {

                //类名（TypeElement）
                ClassName className = ClassName.get(entry.getKey());
                //2.实现接口泛型(implements ViewBinder<MainActivity>)
                ParameterizedTypeName typeName = ParameterizedTypeName.get(ClassName.get(viewBinderType), ClassName.get(entry.getKey()));

                //4.方法体参数(final MainActivity target)
                ParameterSpec parameterSpec = ParameterSpec.builder(ClassName.get(entry.getKey()), //参数类型（Mainactivity）
                        Constants.TARGET_PARAMETER_NAME) //参数名target
                        .addModifiers(Modifier.FINAL)
                        .build();

                //3.方法体： public void bind(final MainActivity target) {
                MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(Constants.BIND_METHOD_NAME) //bind方法
                        .addAnnotation(Override.class)  //方法注解
                        .addModifiers(Modifier.PUBLIC) //方法类型
                        .addParameter(parameterSpec);//方法创建完成

                //5.方法内容
                for (VariableElement fieldElement : entry.getValue()) {
                    //获取属性名
                    String fieldName = fieldElement.getSimpleName().toString();
                    //获取注解的值(如：R.id.tv_1)
                    int annotationValue = fieldElement.getAnnotation(BindView.class).value();
                    // target.tv1.findViewById(R.id.tv_1);
                    String methodContent = "$N."+fieldName+"=$N.findViewById($L)";
                    //加入方法内容
                    methodBuilder.addStatement(methodContent,Constants.TARGET_PARAMETER_NAME,Constants.TARGET_PARAMETER_NAME,annotationValue);

                }

                //1.生成必须是同包;(属性的修饰符是缺失的)
                JavaFile.builder(className.packageName(), //包名
                        TypeSpec.classBuilder(className.simpleName() + "$ViewBinder")  //类名
                                .addSuperinterface(typeName)  //实现ViewBinder接口
                                .addModifiers(Modifier.PUBLIC) //类的类型为public
                                .addMethod(methodBuilder.build())  //方法体
                                .build()) //类构件完成
                        .build()
                        .writeTo(filer);
            }

        }
    }

    private void valueOfMap(Set<? extends Element> bindViewElements, Set<? extends Element> onClickElements) {

        if (!EmptyUtils.isEmpty(bindViewElements)) {
            for (Element element : bindViewElements) {
                messager.printMessage(Diagnostic.Kind.NOTE, "@BindView >>> " + element.getSimpleName());
                if (element.getKind() == ElementKind.FIELD) {
                    VariableElement fieldElement = (VariableElement) element;
                    //属性节点的上层是类节点
                    TypeElement enClosingElement = (TypeElement) element.getEnclosingElement();
                    if (tempBindViewMap.containsKey(enClosingElement)) {
                        tempBindViewMap.get(enClosingElement).add(fieldElement);
                    } else {
                        List<VariableElement> fields = new ArrayList<>();
                        fields.add(fieldElement);
                        tempBindViewMap.put(enClosingElement, fields);
                    }
                }
            }
        }

        if (!EmptyUtils.isEmpty(onClickElements)) {
            for (Element element : onClickElements) {
                messager.printMessage(Diagnostic.Kind.NOTE, "@OnClick >>> " + element.getSimpleName());
                if (element.getKind() == ElementKind.METHOD) {
                    ExecutableElement executableElement = (ExecutableElement) element;
                    //属性节点的上层是类节点
                    TypeElement enClosingElement = (TypeElement) element.getEnclosingElement();
                    if (tempOnClickMap.containsKey(enClosingElement)) {
                        tempOnClickMap.get(enClosingElement).add(executableElement);
                    } else {
                        List<ExecutableElement> executes = new ArrayList<>();
                        executes.add(executableElement);
                        tempOnClickMap.put(enClosingElement, executes);
                    }
                }
            }
        }

    }
}
