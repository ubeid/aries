/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.aries.blueprint.reflect;

import org.apache.aries.blueprint.mutable.MutableRegistrationListener;
import org.osgi.service.blueprint.reflect.RegistrationListener;
import org.osgi.service.blueprint.reflect.Target;

/**
 * Implementation of RegistrationListener.
 *
 * @version $Rev: 896324 $, $Date: 2010-01-06 06:05:04 +0000 (Wed, 06 Jan 2010) $
 */
public class RegistrationListenerImpl implements MutableRegistrationListener {

    private Target listenerComponent;
    private String registrationMethod;
    private String unregistrationMethod;

    public RegistrationListenerImpl() {
    }

    public RegistrationListenerImpl(Target listenerComponent, String registrationMethod, String unregistrationMethod) {
        this.listenerComponent = listenerComponent;
        this.registrationMethod = registrationMethod;
        this.unregistrationMethod = unregistrationMethod;
    }

    public RegistrationListenerImpl(RegistrationListener source) {
        listenerComponent = MetadataUtil.cloneTarget(source.getListenerComponent());
        registrationMethod = source.getRegistrationMethod();
        unregistrationMethod = source.getUnregistrationMethod();
    }

    public Target getListenerComponent() {
        return listenerComponent;
    }

    public void setListenerComponent(Target listenerComponent) {
        this.listenerComponent = listenerComponent;
    }

    public String getRegistrationMethod() {
        return registrationMethod;
    }

    public void setRegistrationMethod(String registrationMethod) {
        this.registrationMethod = registrationMethod;
    }

    public String getUnregistrationMethod() {
        return unregistrationMethod;
    }

    public void setUnregistrationMethod(String unregistrationMethod) {
        this.unregistrationMethod = unregistrationMethod;
    }

    @Override
    public String toString() {
        return "RegistrationListener[" +
                "listenerComponent=" + listenerComponent +
                ", registrationMethodName='" + registrationMethod + '\'' +
                ", unregistrationMethodName='" + unregistrationMethod + '\'' +
                ']';
    }
}
