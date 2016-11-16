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

package org.wso2.carbon.device.mgt.mobile.windows.api.operations.util;

import org.wso2.carbon.device.mgt.common.operation.mgt.Operation;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.PluginConstants;

import java.util.ArrayList;
import java.util.List;

/**
 * Class generate Info type operation list.
 */
public class DeviceInfo {
    public List<Operation> getDeviceInfo() {

        List<Operation> deviceInfoOperations = new ArrayList<>();

        /*Operation osVersion = new Operation();
        osVersion.setCode(PluginConstants.SyncML.SOFTWARE_VERSION);
        osVersion.setType(Operation.Type.INFO);
        deviceInfoOperations.add(osVersion);

        Operation imsi = new Operation();
        imsi.setCode(PluginConstants.SyncML.IMSI);
        imsi.setType(Operation.Type.INFO);
        deviceInfoOperations.add(imsi);*/

        /*Operation imei = new Operation();
        imei.setCode(PluginConstants.SyncML.IMEI);
        imei.setType(Operation.Type.INFO);
        deviceInfoOperations.add(imei);*/

        Operation deviceID = new Operation();
        deviceID.setCode(PluginConstants.SyncML.DEV_ID);
        deviceID.setType(Operation.Type.INFO);
        deviceInfoOperations.add(deviceID);

        Operation test = new Operation();
        test.setCode(PluginConstants.SyncML.LONGITUDE);
        test.setType(Operation.Type.INFO);
        deviceInfoOperations.add(test);

        /*Operation manufacturer = new Operation();
        manufacturer.setCode(PluginConstants.SyncML.MANUFACTURER);
        manufacturer.setType(Operation.Type.INFO);
        deviceInfoOperations.add(manufacturer);

        Operation model = new Operation();
        model.setCode(PluginConstants.SyncML.MODEL);
        model.setType(Operation.Type.INFO);
        deviceInfoOperations.add(model);*/

        /*Operation bat_charge = new Operation();
        bat_charge.setCode(PluginConstants.SyncML.BATTERY_CHARGE_REMAINING);
        bat_charge.setType(Operation.Type.INFO);
        deviceInfoOperations.add(bat_charge);

        Operation bat_runtime = new Operation();
        bat_runtime.setCode(PluginConstants.SyncML.BATTERY_ESTIMATED_RUNTIME);
        bat_runtime.setType(Operation.Type.INFO);
        deviceInfoOperations.add(bat_runtime);*/

        /*Operation language = new Operation();
        language.setCode(PluginConstants.SyncML.LANGUAGE);
        language.setType(Operation.Type.INFO);
        deviceInfoOperations.add(language);*/

        /*Operation vendor = new Operation();
        vendor.setCode(PluginConstants.SyncML.VENDOR);
        vendor.setType(Operation.Type.INFO);
        deviceInfoOperations.add(vendor);*/

        Operation mobileID = new Operation();
        mobileID.setCode(PluginConstants.SyncML.LATITUDE);
        mobileID.setType(Operation.Type.INFO);
        deviceInfoOperations.add(mobileID);

        /*Operation deviceType = new Operation();
        deviceType.setCode(PluginConstants.SyncML.DEVICE_TYPE);
        deviceType.setType(Operation.Type.INFO);
        deviceInfoOperations.add(deviceType);*/

        Operation totalStorage = new Operation();
        totalStorage.setCode(PluginConstants.SyncML.TOTAL_STORAGE);
        totalStorage.setType(Operation.Type.INFO);
        deviceInfoOperations.add(totalStorage);

        /*Operation macaddress = new Operation();
        macaddress.setCode(PluginConstants.SyncML.MAC_ADDRESS);
        macaddress.setType(Operation.Type.INFO);
        deviceInfoOperations.add(macaddress);

        Operation osPlatform = new Operation();
        osPlatform.setCode(PluginConstants.SyncML.OS_PLATFORM);
        osPlatform.setType(Operation.Type.INFO);
        deviceInfoOperations.add(osPlatform);*/

        /*Operation resolution = new Operation();
        resolution.setCode(PluginConstants.SyncML.RESOLUTION);
        resolution.setType(Operation.Type.INFO);
        deviceInfoOperations.add(resolution);

        Operation deviceName = new Operation();
        deviceName.setCode(PluginConstants.SyncML.DEVICE_NAME);
        deviceName.setType(Operation.Type.INFO);
        deviceInfoOperations.add(deviceName);*/

        //new in windows 10
        // used to get deice statistics - RAM
        // returns an integer that specifies the total available memory in MB on the device (may be less than total physical memory).
        Operation totalRAM = new Operation();
        totalRAM.setCode(PluginConstants.SyncML.TOTAL_RAM);
        totalRAM.setType(Operation.Type.INFO);
        deviceInfoOperations.add(totalRAM);

        /*Operation localTime = new Operation();
        localTime.setCode(PluginConstants.SyncML.LOCAL_TIME);
        localTime.setType(Operation.Type.INFO);
        deviceInfoOperations.add(localTime);*/

        /*Operation maxdepth = new Operation();
        maxdepth.setCode(PluginConstants.SyncML.MAX_DEPTH);
        maxdepth.setType(Operation.Type.INFO);
        deviceInfoOperations.add(maxdepth);*/

        /*Operation osVersion = new Operation();
        osVersion.setCode(PluginConstants.SyncML.SOFTWARE_VERSION);
        osVersion.setType(Operation.Type.INFO);
        deviceInfoOperations.add(osVersion);
*/
        return deviceInfoOperations;
    }
}
