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

package org.wso2.carbon.device.mgt.mobile.windows.api.operations;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wso2.carbon.device.mgt.mobile.windows.api.operations.util.Constants;

/**
 * Challenge data pass through the device and Device Management server for the security purpose.
 */
public class ChallengeTag {
    MetaTag meta;

    //windows 10
    public ChallengeTag(){
        this.meta = new MetaTag();
        meta.setFormat("syncml:auth-basic");
        meta.setType("b64");
    }

    public MetaTag getMeta() {
        return meta;
    }

    public void setMeta(MetaTag meta) {
        this.meta = meta;
    }

    public void buildChallengeElement(Document doc, Element rootElement) {
        Element challenge = doc.createElement(Constants.CHALLENGE);
        rootElement.appendChild(challenge);
        if (getMeta() != null) {
            getMeta().buildMetaElement(doc, challenge);
        }
    }
}
