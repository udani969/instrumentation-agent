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

import org.wso2.das.javaagent.schema.InstrumentationMethod;

public class InstrumentationClassData {
    private String scenarioName;
    private InstrumentationMethod instrumentationMethod;

    public InstrumentationClassData(String scenarioName, InstrumentationMethod instrumentationMethod) {
        this.scenarioName = scenarioName;
        this.instrumentationMethod = instrumentationMethod;
    }

    public String getScenarioName() {
        return scenarioName;
    }

    public void setScenarioName(String scenarioName) {
        this.scenarioName = scenarioName;
    }

    public InstrumentationMethod getInstrumentationMethod() {
        return instrumentationMethod;
    }

    public void setInstrumentationMethod(InstrumentationMethod instrumentationMethod) {
        this.instrumentationMethod = instrumentationMethod;
    }
}
