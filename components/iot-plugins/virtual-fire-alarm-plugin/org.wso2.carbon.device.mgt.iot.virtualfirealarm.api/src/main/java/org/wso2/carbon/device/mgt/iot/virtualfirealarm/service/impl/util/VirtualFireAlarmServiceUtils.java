/*
 * Copyright (c) 2016, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.iot.virtualfirealarm.service.impl.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.plugin.exception.VirtualFirealarmDeviceMgtPluginException;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.plugin.impl.util.VirtualFirealarmSecurityManager;
import org.wso2.carbon.device.mgt.iot.virtualfirealarm.service.impl.exception.VirtualFireAlarmException;

import java.lang.*;
import java.security.PrivateKey;

/**
 *
 */
public class VirtualFireAlarmServiceUtils {

    private static final String JSON_MESSAGE_KEY = "Msg";
    private static final String JSON_SIGNATURE_KEY = "Sig";

    public static String prepareSecurePayLoad(String message, PrivateKey signatureKey) throws VirtualFireAlarmException {
        try {
            message = Base64.encodeBase64String(message.getBytes());
            String signedPayload = VirtualFirealarmSecurityManager.signMessage(message, signatureKey);
            JSONObject jsonPayload = new JSONObject();
            jsonPayload.put(JSON_MESSAGE_KEY, message);
            jsonPayload.put(JSON_SIGNATURE_KEY, signedPayload);
            return jsonPayload.toString();
        } catch (VirtualFirealarmDeviceMgtPluginException e) {
            throw new VirtualFireAlarmException(e);
        }

    }
}
