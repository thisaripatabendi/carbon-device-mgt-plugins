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

package org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.beans;

import java.text.SimpleDateFormat;

/**
 * Created by thisari on 11/22/16.
 * this class is used to store the data related to battery of the device
 */
public class DeviceStatistics {

    int bat_status ;
    int bat_runtime;
    //time
    String time;

    public DeviceStatistics(int d1, int d2, String d3) {
        bat_status = d1;
        bat_runtime = d2;
        time = d3;
    }


    public int getBat_status() {
        return bat_status;
    }

    public void setBat_status(int bat_status) {
        this.bat_status = bat_status;
    }

    public int getBat_runtime() {
        return bat_runtime;
    }

    public void setBat_runtime(int bat_runtime) {
        this.bat_runtime = bat_runtime;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

}
