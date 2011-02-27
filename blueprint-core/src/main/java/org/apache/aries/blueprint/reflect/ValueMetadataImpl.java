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

import org.apache.aries.blueprint.mutable.MutableValueMetadata;
import org.osgi.service.blueprint.reflect.ValueMetadata;

/**
 * Implementation of ValueMetadata 
 *
 * @version $Rev: 896324 $, $Date: 2010-01-06 06:05:04 +0000 (Wed, 06 Jan 2010) $
 */
public class ValueMetadataImpl implements MutableValueMetadata {

    private String stringValue;
    private String type;

    public ValueMetadataImpl() {
    }

    public ValueMetadataImpl(String stringValue) {
        this.stringValue = stringValue;
    }

    public ValueMetadataImpl(String stringValue, String type) {
        this.stringValue = stringValue;
        this.type = type;
    }

    public ValueMetadataImpl(ValueMetadata source) {
        this.stringValue = source.getStringValue();
        this.type = source.getType();
    }

    public String getStringValue() {
        return stringValue;
    }

    public void setStringValue(String stringValue) {
        this.stringValue = stringValue;
    }

    public String getType() {
        return type;
    }

    public void setType(String typeName) {
        this.type = typeName;
    }

    @Override
    public String toString() {
        return "ValueMetadata[" +
                "stringValue='" + stringValue + '\'' +
                ", type='" + type + '\'' +
                ']';
    }
}
