/*
 *
 *  Copyright (c) 2014, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package org.wso2.das.javaagent.instrumentation;

import javassist.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.das.javaagent.schema.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.List;

/**
 * InstrumentationClassTransformer handles the main instrumentation of classes loaded onto JVM.
 * When a class is loaded it checks for a match in the Map created by the InstrumentingAgent.
 * Bytecode of the selected class will be modified using Javassist.
 * CtMethod object will be obtained by matching the method name and method signature.
 * Each instrumenting method will be injected at three different locations to publish events
 * to DAS server. A single event will contain correlationId, payload data and
 * an arbitraryMap with required values.
 */
public class InstrumentationClassTransformer implements ClassFileTransformer {

    private static final Log log = LogFactory.getLog(InstrumentationClassTransformer.class);

    /**
     * Create a copy of currently processing class. Since javassist instrument methods with body,
     * for each Class iterate through all the methods defined to find respective methods.
     * Instrument method body by injecting required code and return
     * the class file of the modified class.
     *
     * @param loader the defining loader of the class to be transformed, may be null if the
     *            bootstrap loader
     * @param className the name of the class in the internal form of fully qualified class
     *            and interface names as defined in The Java Virtual Machine Specification
     * @param classBeingRedefined if this is triggered by a redefine or retransform,
     *            the class being redefined or retransformed;
     *            if this is a class load, null
     * @param protectionDomain the protection domain of the class being defined or redefined
     * @param classfileBuffer the input byte buffer in class file format
     * @return a well-formed class file buffer (the result of the transform),
     *         or null if no transform is performed
     * @throws IllegalClassFormatException
     */
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
            ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
        if (log.isDebugEnabled()) {
            log.debug("Loading class : " + className.replace('/', '.'));
        }
        ByteArrayInputStream currentClass = null;
        CtClass ctClass = null;
        byte[] transformedBytes = classfileBuffer;
        InstrumentationDataHolder instDataHolder = InstrumentationDataHolder.getInstance();
        try {
            boolean transformed = false;
            ClassPool classPool = ClassPool.getDefault();
            currentClass = new ByteArrayInputStream(classfileBuffer);
            ctClass = classPool.makeClass(currentClass);
            if (!ctClass.isInterface()) {
                // if the class given is an interface
                CtClass[] interfaces = ctClass.getInterfaces();
                if (interfaces.length != 0) {
                    for (CtClass baseClass : interfaces) {
                        if (instDataHolder.getClassMap().keySet().contains(baseClass.getName())) {
                            instrumentClass(ctClass, baseClass.getName());
                            transformed = true;
                            break;
                        }
                    }
                }
                // class provided is Superclass
                try {
                    if (!transformed) {
                        CtClass extendedClass = ctClass.getSuperclass();
                        if (extendedClass != null
                                && instDataHolder.getClassMap().keySet().contains(extendedClass.getName())) {
                            instrumentClass(ctClass, extendedClass.getName());
                            transformed = true;
                        }
                    }
                } catch (NotFoundException ignored) {
                } catch (Exception e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Unable to instrument class due to : " + e.getMessage());
                    }
                }
                // Class provided is the exact class
                if (!transformed && instDataHolder.getClassMap().keySet().contains(ctClass.getName())) {
                    instrumentClass(ctClass, ctClass.getName());
                }
            }
            transformedBytes = ctClass.toBytecode();
        } catch (NotFoundException e) {
            if (log.isDebugEnabled()) {
                log.debug("Unable to find " + className.replace('/', '.') + "for instrumentation : " + e.getMessage());
            }
        } catch (CannotCompileException | IOException e) {
            if (log.isDebugEnabled()) {
                log.debug("Intrumentation of " + className.replace('/', '.') + "failed : " + e.getMessage());
            }
        } finally {
            if (currentClass != null) {
                try {
                    currentClass.close();
                } catch (IOException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("Failed to close the connection : " + e.getMessage());
                    }
                }
            }
            if (ctClass != null) {
                ctClass.detach();
            }
        }
        return transformedBytes;
    }

    public void instrumentClass(CtClass ctClass, String name) throws NotFoundException, CannotCompileException {
        if (log.isDebugEnabled()) {
            log.debug("Instrumenting " + ctClass.getName());
        }
        InstrumentationDataHolder instDataHolder = InstrumentationDataHolder.getInstance();
        List<InstrumentationClassData> instMethods = instDataHolder.getClassMap().get(name);
        for (InstrumentationClassData instMethodData : instMethods) {
            CtMethod method = ctClass.getMethod(instMethodData.getInstrumentationMethod().getMethodName(),
                    instMethodData.getInstrumentationMethod().getMethodSignature());
            instrumentMethod(createPayloadData(instMethodData.getScenarioName(), ctClass.getName(), method.getName()),
                    instMethodData.getInstrumentationMethod(), method);
        }
    }

    public void instrumentMethod(String payloadData, InstrumentationMethod instMethod, CtMethod method)
            throws NotFoundException, CannotCompileException {
        createInsertBefore(payloadData, method, instMethod.getInsertBefore());
        List<InsertAt> insertAts = instMethod.getInsertAts();
        if (insertAts != null && !insertAts.isEmpty()) {
            for (InsertAt insertAt : insertAts) {
                if (insertAt != null) {
                    createInsertAt(payloadData, method, insertAt);
                }
            }
        }
        createInsertAfter(payloadData, method, instMethod.getInsertAfter());
    }

    public void createInsertAt(String payloadData, CtMethod method, InsertAt insertAt) throws CannotCompileException {
        StringBuilder atBuilder = new StringBuilder();
        if (!insertAt.getParameterNames().isEmpty()) {
            createArbitraryMap(insertAt.getParameterNames(), atBuilder, "insertAtMap" + insertAt.getLineNo());
        }
        atBuilder.append("org.wso2.das.javaagent.instrumentation.AgentPublisherHolder.getInstance().publishEvents(");
        atBuilder.append("System.currentTimeMillis(),");
        atBuilder.append("instMethod_correlationId,\"");
        atBuilder.append(payloadData);
        atBuilder.append(":line ");
        atBuilder.append(insertAt.getLineNo());
        atBuilder.append(": \",insertAtMap");
        atBuilder.append(insertAt.getLineNo());
        atBuilder.append(");");
        method.insertAt(insertAt.getLineNo(), atBuilder.toString());
    }

    public void createInsertBefore(String payloadData, CtMethod method, InsertBefore beforeList)
            throws CannotCompileException {
        boolean addMap = false;
        StringBuilder beforeBuilder = new StringBuilder();
        method.addLocalVariable("instMethod_startTime", CtClass.longType);
        method.addLocalVariable("instMethod_correlationId", CtClass.longType);
        beforeBuilder.append("instMethod_startTime = System.nanoTime();");
        beforeBuilder.append("instMethod_correlationId = System.nanoTime()+Math.round(Math.random() * 123456789);");
        if (beforeList != null && !beforeList.getParameterNames().isEmpty()) {
            createArbitraryMap(beforeList.getParameterNames(), beforeBuilder, "insertBeforeMap");
            addMap = true;
        }
        beforeBuilder
                .append("org.wso2.das.javaagent.instrumentation.AgentPublisherHolder.getInstance().publishEvents(");
        beforeBuilder.append("System.currentTimeMillis(),");
        beforeBuilder.append("instMethod_correlationId,\"");
        beforeBuilder.append(payloadData);
        beforeBuilder.append(":start: \"");
        if (addMap) {
            beforeBuilder.append(",insertBeforeMap");
        }
        beforeBuilder.append(");");
        method.insertBefore(beforeBuilder.toString());
    }

    public void createInsertAfter(String payloadData, CtMethod method, InsertAfter afterList)
            throws CannotCompileException {
        boolean addMap = false;
        StringBuilder afterBuilder = new StringBuilder();
        if (afterList != null && !afterList.getParameterNames().isEmpty()) {
            createArbitraryMap(afterList.getParameterNames(), afterBuilder, "insertAfterMap");
            addMap = true;
        }
        afterBuilder.append("org.wso2.das.javaagent.instrumentation.AgentPublisherHolder.getInstance().publishEvents(");
        afterBuilder.append("System.currentTimeMillis(),");
        afterBuilder.append("instMethod_correlationId,\"");
        afterBuilder.append(payloadData);
        afterBuilder.append(":end:\"+");
        afterBuilder.append("String.valueOf(System.nanoTime()-instMethod_startTime)");
        if (addMap) {
            afterBuilder.append(",insertAfterMap");
        }
        afterBuilder.append(");");
        method.insertAfter(afterBuilder.toString());
    }

    private void createArbitraryMap(List<ParameterName> insertMapList, StringBuilder sBuilder, String mapName) {
        sBuilder.append("java.util.Map/*<String,String>*/ ");
        sBuilder.append(mapName);
        sBuilder.append("= new java.util.HashMap/*<String,String>*/();");
        for (ParameterName parameter : insertMapList) {
            sBuilder.append(mapName);
            sBuilder.append(".put(\"");
            sBuilder.append(parameter.getKey());
            sBuilder.append("\",");
            sBuilder.append(parameter.getParameterValue());
            sBuilder.append(");");
        }
    }

    private String createPayloadData(String scenarioName, String className, String methodName) {
        return scenarioName + ":" + className + ":" + methodName;
    }
}
