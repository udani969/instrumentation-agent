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
import org.wso2.das.javaagent.schema.InsertAt;
import org.wso2.das.javaagent.schema.InstrumentationMethod;
import org.wso2.das.javaagent.schema.ParameterName;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InstrumentationClassTransformer implements ClassFileTransformer {
//    private static final Log log = LogFactory.getLog(InstrumentationClassTransformer.class);

    /**
     * Create a copy of currently processing class. Since javassist instrument methods with body,
     * for each Class iterate through all the methods defined to find respective methods.
     * Instrument method body by injecting required code and return
     * the class file of the modified class.
     *
     * @param loader the defining loader of the class to be transformed, may be null if the
     *               bootstrap loader
     * @param className the name of the class in the internal form of fully qualified class
     *            and interface names as defined in The Java Virtual Machine Specification
     * @param classBeingRedefined if this is triggered by a redefine or retransform,
     *            the class being redefined or retransformed;
     *            if this is a class load, null
     * @param protectionDomain the protection domain of the class being defined or redefined
     * @param classfileBuffer the input byte buffer in class file format
     * @return a well-formed class file buffer (the result of the transform),
     * or null if no transform is performed
     * @throws IllegalClassFormatException
     */
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classfileBuffer)
            throws IllegalClassFormatException {
        byte[] transformedBytes = null;

        if(AgentPublisher.getClassMap().keySet().contains(className.replace('/','.'))){
            ClassPool classPool = ClassPool.getDefault();
            try {
                CtClass ctClass = classPool.makeClass(new ByteArrayInputStream(classfileBuffer));

                List<InstrumentationClassData> instrumentationMethods =
                        AgentPublisher.getClassMap().get(className.replace('/','.'));
                for(InstrumentationClassData instrumentationMethodData : instrumentationMethods){
                    CtMethod method = ctClass.getMethod(
                            instrumentationMethodData.getInstrumentationMethod().getMethodName(),
                            instrumentationMethodData.getInstrumentationMethod().getMethodSignature());

                    instrumentMethod(createPayloadData(instrumentationMethodData.getScenarioName(),
                                                    className.replace('/','.'), method.getName()),
                            instrumentationMethodData.getInstrumentationMethod(), method);
                }

                transformedBytes = ctClass.toBytecode();
                ctClass.detach();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CannotCompileException e) {
                e.printStackTrace();
            } catch (NotFoundException e) {
                e.printStackTrace();
            }
        }

        return transformedBytes;

    }

    public void instrumentMethod(String payloadData, InstrumentationMethod instrumentationMethod,
                                 CtMethod method) throws NotFoundException, CannotCompileException {
        createInsertBefore(payloadData, method, instrumentationMethod.getinsertBefore());

        List<InsertAt> insertAts = instrumentationMethod.getInsertAts();
        if(insertAts!=null && !insertAts.isEmpty()){
            for(InsertAt insertAt : insertAts){
                List<ParameterName> parameterNames = insertAt.getParameterNames();
                Map<String,String> parameterMap = new HashMap<String,String>();
                if(!parameterNames.isEmpty()){
                    for(ParameterName parameterName : parameterNames){
                        parameterMap.put(parameterName.getKey(), parameterName.getParameterValue());
                    }
                    createInsertAt(payloadData, method, insertAt.getLineNo(), parameterMap);
                    parameterMap.clear();
                }
            }
        }
        createInsertAfter(payloadData, method, instrumentationMethod.getInsertAfter());
    }

    public void createInsertAt(String payloadData, CtMethod method, int lineNo,
                               Map<String,String> parameterMap)
            throws CannotCompileException {
        StringBuilder atBuilder = new StringBuilder();
        atBuilder.append("java.util.Map/*<String,String>*/ insertAtMap"+lineNo+" " +
                "= new java.util.HashMap/*<String,String>*/();");
        for (Map.Entry<String,String> entry : parameterMap.entrySet()) {
            atBuilder.append("insertAtMap"+lineNo+".put(\"" + entry.getKey() + "\","
                    + entry.getValue() + ");");
        }

        atBuilder.append("org.wso2.das.javaagent.instrumentation.AgentPublisher.publishEvents(" +
                "System.currentTimeMillis(),\"" + payloadData + ":line "+lineNo+": \",insertAtMap" + lineNo + ");");

        method.insertAt(lineNo, atBuilder.toString());
    }

    public void createInsertBefore(String payloadData, CtMethod method,
                                   String insertBefore) throws CannotCompileException {
        method.addLocalVariable("startTime", CtClass.longType);
        StringBuilder beforeBuilder = new StringBuilder();
        beforeBuilder.append("startTime = System.nanoTime();");
        if((insertBefore!=null) && !insertBefore.isEmpty()){
            beforeBuilder.append(insertBefore);
        }

        beforeBuilder.append("org.wso2.das.javaagent.instrumentation.AgentPublisher.publishEvents(" +
                "System.currentTimeMillis(),\"" + payloadData + ":start: \");");
        method.insertBefore(beforeBuilder.toString());
    }

    public void createInsertAfter(String payloadData, CtMethod method,
                                  String insertAfter) throws CannotCompileException {
        StringBuilder afterBuilder = new StringBuilder();
        if(insertAfter!=null && !insertAfter.isEmpty()) {
            afterBuilder.append(insertAfter);
        }
        afterBuilder.append("org.wso2.das.javaagent.instrumentation.AgentPublisher.publishEvents(" +
                "System.currentTimeMillis(),\"" + payloadData + ":end:\"+" +
                "String.valueOf(System.nanoTime()-startTime));");
        method.insertAfter(afterBuilder.toString());
    }

    private String createPayloadData (String scenarioName, String className, String methodName){
        return scenarioName+":"+className+":"+methodName;
    }
}
