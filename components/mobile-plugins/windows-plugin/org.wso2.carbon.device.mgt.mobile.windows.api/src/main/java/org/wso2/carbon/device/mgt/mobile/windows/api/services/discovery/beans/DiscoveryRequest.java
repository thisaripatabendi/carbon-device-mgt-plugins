/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * you may obtain a copy of the License at
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

package org.wso2.carbon.device.mgt.mobile.windows.api.services.discovery.beans;


import org.wso2.carbon.device.mgt.mobile.windows.api.common.PluginConstants;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
//@XmlType(name = "DiscoveryRequest")
@XmlType(name = "DiscoveryRequest", namespace = "http://schemas.microsoft.com/windows/management/2012/01/enrollment")
@SuppressWarnings("unused")

public class DiscoveryRequest implements Serializable {

    @XmlElement(name = "EmailAddress", required = true)
    private String emailId;

    @XmlElement(name = "RequestVersion")
    private String version;

    @XmlElement(name = "DeviceType")
    private String deviceType;

    //new in windows 10
    @XmlElement(name = "ApplicationVersion")
    private String applicationVersion;

    //new in windows 10
    @XmlElement(name = "OSEdition")
    private String osEdition;

    //new in windows 10
    @XmlElementWrapper(name = "AuthPolicies")
    @XmlElement(name = "AuthPolicy")
    private List<String> authPolicies;

    public List<String> getAuthPolicies() {
        return authPolicies;
    }
    public void setAuthPolicies(List<String> authPolicies) {
        this.authPolicies = authPolicies;
    }

    public String getEmailId() {
        return emailId;
    }

    public String getVersion() {
        return version;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public String getApplicationVersion() {
        return applicationVersion;
    }

    public String getOsEdition() {
        return osEdition;
    }

    public void setEmailId(String emailId) {
        this.emailId = emailId;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public void setApplicationVersion(String applicationVersion) {
        this.applicationVersion = applicationVersion;
    }

    public void setOsEdition(String osEdition) {
        this.osEdition = osEdition;
    }

}