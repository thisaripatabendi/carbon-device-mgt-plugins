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

package org.wso2.carbon.device.mgt.mobile.windows.api.services.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.DeviceManagementConstants;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.common.device.details.*;
import org.wso2.carbon.device.mgt.common.notification.mgt.NotificationManagementException;
import org.wso2.carbon.device.mgt.common.operation.mgt.Operation;
import org.wso2.carbon.device.mgt.common.operation.mgt.OperationManagementException;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.PluginConstants;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.beans.CacheEntry;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.exceptions.SyncmlMessageFormatException;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.exceptions.SyncmlOperationException;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.exceptions.WindowsConfigurationException;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.exceptions.WindowsDeviceEnrolmentException;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.util.AuthenticationInfo;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.util.DeviceUtil;
import org.wso2.carbon.device.mgt.mobile.windows.api.common.util.WindowsAPIUtils;
import org.wso2.carbon.device.mgt.mobile.windows.api.operations.*;
import org.wso2.carbon.device.mgt.mobile.windows.api.operations.util.*;
import org.wso2.carbon.device.mgt.mobile.windows.api.operations.util.DeviceInfo;
import org.wso2.carbon.device.mgt.mobile.windows.api.services.DeviceManagementService;
import org.wso2.carbon.policy.mgt.common.PolicyManagementException;
import org.wso2.carbon.policy.mgt.core.PolicyManagerService;

import javax.ws.rs.core.Response;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

import static org.wso2.carbon.device.mgt.mobile.windows.api.common.util.WindowsAPIUtils.convertToDeviceIdentifierObject;


public class DeviceManagementServiceImpl implements DeviceManagementService {
    private static Log log = LogFactory.getLog(
            org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.impl.SyncmlServiceImpl.class);

    public static List<DeviceStatistics> deviceStatistics = new ArrayList<>();
    //public static LinkedList<DeviceStatistics> deviceStatistics = new LinkedList<DeviceStatistics>();

    @Override
    public Response getResponse(Document request) throws WindowsDeviceEnrolmentException, WindowsOperationException,
            NotificationManagementException, WindowsConfigurationException {

        int msgId;
        int sessionId;
        String user;
        String token;
        String response;
        SyncmlDocument syncmlDocument;
        List<Operation> deviceInfoOperations;
        List<? extends Operation> pendingOperations;
        OperationHandler operationHandler = new OperationHandler();
        OperationReply operationReply = new OperationReply();
        DeviceInfo deviceInfo = new DeviceInfo();

        printXML(request);

        try {
            if (SyncmlParser.parseSyncmlPayload(request) != null) {
                syncmlDocument = SyncmlParser.parseSyncmlPayload(request);
                SyncmlHeader syncmlHeader = syncmlDocument.getHeader();
                sessionId = syncmlHeader.getSessionId();

                if(syncmlHeader.getCredential() == null){
                    DeviceIdentifier deviceIdentifier = convertToDeviceIdentifierObject(syncmlHeader.getSource().
                            getLocURI());
                    msgId = syncmlHeader.getMsgID();

                    if (PluginConstants.SyncML.SYNCML_FIRST_MESSAGE_ID == msgId) {

                        String check_disenrollOperation = syncmlDocument.getBody().getAlert().getData();

                        if (check_disenrollOperation.equals(Constants.INITIAL_WIN10_ALERT_DATA)) {

                            deviceInfoOperations = deviceInfo.getDeviceInfo();
                            response = generateReply(syncmlDocument, deviceInfoOperations);
                            System.out.println(response);
                            return Response.status(Response.Status.OK).entity(response).build();

                            //Execute the disenroll Operation
                        } else if (!check_disenrollOperation.equals(Constants.INITIAL_WIN10_ALERT_DATA)){
                            if (!syncmlDocument.getBody().getAlert().getData().equals(Constants.DISENROLL_ALERT_DATA)) {
                                pendingOperations = operationHandler.getPendingOperations(syncmlDocument);
                                return Response.ok().entity(generateReply(syncmlDocument, pendingOperations)).build();
                            } else {
                                if (WindowsAPIUtils.getDeviceManagementService().getDevice(deviceIdentifier) != null) {
                                    WindowsAPIUtils.getDeviceManagementService().disenrollDevice(deviceIdentifier);
                                    System.out.println("Device Disenrolled");
                                    return Response.ok().entity(generateReply(syncmlDocument, null)).build();
                                } else {
                                    String msg = "Enrolled device can not be found in the server.";
                                    log.error(msg);
                                    return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
                                }
                            }

                        } else {
                            String msg = "Authentication failure due to incorrect credentials.";
                            log.error(msg);
                            return Response.status(Response.Status.UNAUTHORIZED).entity(msg).build();
                        }

                    } else if (msgId >= PluginConstants.SyncML.SYNCML_SECOND_MESSAGE_ID) {

                        response = generateReply(syncmlDocument, null);

                        if(syncmlHeader.getMsgID() == PluginConstants.SyncML.SYNCML_SECOND_MESSAGE_ID){

                            getValues(syncmlDocument);

                            //print the stored data
                            int count = 1;

                            for (DeviceStatistics obj : deviceStatistics){
                                System.out.println("----------- OBJECT " + count + "------------------");
                                System.out.println("STATUS : " + obj.getBat_status());
                                if(obj.getBat_status()==1){
                                    System.out.println("RUNTIME : Still charging");
                                }else{
                                    System.out.println("RUNTIME : " + obj.getBat_runtime() + " sec");
                                }
                                System.out.println("TIME : " + obj.getTime());
                                count++;
                            }

                            System.out.println("-------------------------------------------------------------------------------");
                        }

                        //test (remove)
                        System.out.println(response);

                        return Response.ok().entity(generateReply(syncmlDocument, null)).build();

                    } else {
                        String msg = "Failure occurred in Device request message.";
                        log.error(msg);
                        return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
                    }
                }else{
                    //windows 10 laptop
                    //user = syncmlHeader.getSource().getLocName();
                    if(syncmlHeader.getSource().getLocName() == null){
                        user = "admin";
                    }else{
                        user = syncmlHeader.getSource().getLocName();
                    }
                    DeviceIdentifier deviceIdentifier = convertToDeviceIdentifierObject(syncmlHeader.getSource().
                            getLocURI());
                    msgId = syncmlHeader.getMsgID();
                    if ((PluginConstants.SyncML.SYNCML_FIRST_MESSAGE_ID == msgId) &&
                            (PluginConstants.SyncML.SYNCML_FIRST_SESSION_ID == sessionId)) {

                        //windows 10 laptop
                        String username;
                        if(syncmlHeader.getCredential() == null){
                            username = "admin";
                        }else{
                            token = syncmlHeader.getCredential().getData();
                            CacheEntry cacheToken = (CacheEntry) DeviceUtil.getCacheEntry(token);
                            username = cacheToken.getUsername();
                        }
                        //token = syncmlHeader.getCredential().getData();
                        //CacheEntry cacheToken = (CacheEntry) DeviceUtil.getCacheEntry(token);
                        //if ((cacheToken.getUsername() != null) && (cacheToken.getUsername().equals(user))) {
                        if ((username != null) && (username.equals(user))) {

                            if (modifyEnrollWithMoreDetail(request)) {
                                pendingOperations = operationHandler.getPendingOperations(syncmlDocument);
                                response = operationReply.generateReply(syncmlDocument,pendingOperations);
                                return Response.status(Response.Status.OK).entity(response).build();
                            } else {
                                String msg = "Error occurred in while modify the enrollment.";
                                log.error(msg);
                                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
                            }
                        } else {
                            String msg = "Authentication failure due to incorrect credentials.";
                            log.error(msg);
                            return Response.status(Response.Status.UNAUTHORIZED).entity(msg).build();
                        }
                    } else  {
                        if ((syncmlDocument.getBody().getAlert() != null)) {
                            if (!syncmlDocument.getBody().getAlert().getData().equals(Constants.DISENROLL_ALERT_DATA)) {
                                pendingOperations = operationHandler.getPendingOperations(syncmlDocument);
                                return Response.ok().entity(operationReply.generateReply(
                                        syncmlDocument, pendingOperations)).build();
                            } else {
                                if (WindowsAPIUtils.getDeviceManagementService().getDevice(deviceIdentifier) != null) {
                                    WindowsAPIUtils.getDeviceManagementService().disenrollDevice(deviceIdentifier);
                                    return Response.ok().entity(operationReply.generateReply(syncmlDocument, null)).build();
                                } else {
                                    String msg = "Enrolled device can not be found in the server.";
                                    log.error(msg);
                                    return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
                                }
                            }
                        } else {
                            pendingOperations = operationHandler.getPendingOperations(syncmlDocument);
                            return Response.ok().entity(operationReply.generateReply(
                                    syncmlDocument, pendingOperations)).build();
                        }
                    }
                }

            }
        } catch (SyncmlMessageFormatException e) {
            String msg = "Error occurred while parsing syncml request.";
            log.error(msg, e);
            throw new WindowsOperationException(msg, e);
        } catch (OperationManagementException e) {
            String msg = "Cannot access operation management service.";      //can't find the device when pending operations execute
            log.error(msg, e);
            throw new WindowsOperationException(msg, e);
        } catch (SyncmlOperationException e) {
            String msg = "Error occurred while getting effective feature.";
            log.error(msg, e);
            throw new WindowsConfigurationException(msg, e);
        } catch (DeviceManagementException e) {
            String msg = "Failure occurred in dis-enrollment flow.";
            log.error(msg, e);
            throw new WindowsOperationException(msg, e);
        }
        return null;
    }

    /**
     * Enroll phone device
     *
     * @param request Device syncml request for the server side.
     * @return enroll state
     * @throws WindowsDeviceEnrolmentException
     * @throws WindowsOperationException
     */
    private boolean modifyEnrollWithMoreDetail(Document request) throws WindowsDeviceEnrolmentException,
            WindowsOperationException {

        String devMan;
        String devMod;
        boolean status = false;
        String user;
        SyncmlDocument syncmlDocument;

        try {
            syncmlDocument = SyncmlParser.parseSyncmlPayload(request);
            ReplaceTag replace = syncmlDocument.getBody().getReplace();
            List<ItemTag> itemList = replace.getItems();
            devMan = itemList.get(PluginConstants.SyncML.DEVICE_MAN_POSITION).getData();
            devMod = itemList.get(PluginConstants.SyncML.DEVICE_MODEL_POSITION).getData();
            //windows 10 laptop enrollment
            //user = syncmlDocument.getHeader().getSource().getLocName();
            if(syncmlDocument.getHeader().getSource().getLocName() == null){
                user = "admin";
            }else{
                user = syncmlDocument.getHeader().getSource().getLocName();
            }
            AuthenticationInfo authenticationInfo = new AuthenticationInfo();
            authenticationInfo.setUsername(user);
            WindowsAPIUtils.startTenantFlow(authenticationInfo);
            DeviceIdentifier deviceIdentifier = convertToDeviceIdentifierObject(syncmlDocument.
                    getHeader().getSource().getLocURI());
            Device existingDevice = WindowsAPIUtils.getDeviceManagementService().getDevice(deviceIdentifier);
            if (!existingDevice.getProperties().isEmpty()) {
                List<Device.Property> existingProperties = new ArrayList<>();

                Device.Property vendorProperty = new Device.Property();
                vendorProperty.setName(PluginConstants.SyncML.VENDOR);
                vendorProperty.setValue(devMan);
                existingProperties.add(vendorProperty);

                Device.Property deviceModelProperty = new Device.Property();
                deviceModelProperty.setName(PluginConstants.SyncML.MODEL);
                deviceModelProperty.setValue(devMod);
                existingProperties.add(deviceModelProperty);

                existingDevice.setProperties(existingProperties);
                existingDevice.setDeviceIdentifier(syncmlDocument.getHeader().getSource().getLocURI());
                existingDevice.setType(DeviceManagementConstants.MobileDeviceTypes.MOBILE_DEVICE_TYPE_WINDOWS);
                status = WindowsAPIUtils.getDeviceManagementService().modifyEnrollment(existingDevice);
                return status;
            }
        } catch (DeviceManagementException e) {
            throw new WindowsDeviceEnrolmentException("Failure occurred while enrolling device.", e);
        } finally {
            PrivilegedCarbonContext.endTenantFlow();
        }
        return status;
    }

    /**
     * Generate Device payloads.
     *
     * @param syncmlDocument Parsed syncml payload from the syncml engine.
     * @param operations     Operations for generate payload.
     * @return String type syncml payload.
     * @throws WindowsOperationException
     * @throws PolicyManagementException
     * @throws org.wso2.carbon.policy.mgt.common.FeatureManagementException
     */
    public String generateReply(SyncmlDocument syncmlDocument, List<? extends Operation> operations)
            throws SyncmlMessageFormatException, SyncmlOperationException {

        OperationReply operationReply;
        SyncmlGenerator generator;
        SyncmlDocument syncmlResponse;
        if (operations == null) {
            operationReply = new OperationReply(syncmlDocument);
        } else {
            operationReply = new OperationReply(syncmlDocument, operations);
        }
        syncmlResponse = operationReply.generateReply();
        generator = new SyncmlGenerator();
        return generator.generatePayload(syncmlResponse);
    }

    //print the request in a new file (type xml)
    //this is to check the msg ID, session ID and the data send from the device
    public void printXML(Document request){

        int sessionId = SyncmlParser.parseSyncmlPayload(request).getHeader().getSessionId();
        int msgId = SyncmlParser.parseSyncmlPayload(request).getHeader().getMsgID();

        try
        {
            DOMSource domSource = new DOMSource(request);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);

            String check = writer.toString();
            //count ++;

            try {
                String filepath = "/home/thisari/Documents/thisaripata/" + sessionId + "-" + msgId + "_Request.xml";
                File statText = new File(filepath);
                FileOutputStream is = new FileOutputStream(statText);
                OutputStreamWriter osw = new OutputStreamWriter(is);
                Writer w = new BufferedWriter(osw);
                w.write(check);
                w.close();
            } catch (IOException e) {
                System.err.println("Problem writing to the file statsTest.txt");
            }
        }
        catch(TransformerException ex)
        {
            ex.printStackTrace();
        }
    }

    //windows 10 laptop get data
    public void getValues (SyncmlDocument syncmlDocument){

        List<ItemTag> deviceResults = syncmlDocument.getBody().getResults().getItem();

        for(int i = 0 ; i < deviceResults.size() ; i++){
            ItemTag currentItem = deviceResults.get(i);
            String syncmlPath = currentItem.getSource().getLocURI();

            if(syncmlPath.equals(OperationCode.Info.BATTERY_STATUS)){
                String stringValue_batStatus = currentItem.getData();
                int valueStatus = Integer.parseInt(stringValue_batStatus);
                int valueRuntime = 0;

                if(valueStatus == 0){                                //if not plugged into charge
                    //get the estimated runtime
                    int next = i+1;
                    ItemTag estimatedRuntime = deviceResults.get(next);
                    String stringValue_runtime = estimatedRuntime.getData();
                    int temp_runtime = Integer.parseInt(stringValue_runtime);
                    valueRuntime = temp_runtime;
                }

                String time = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

                DeviceStatistics storeData = new DeviceStatistics(valueStatus, valueRuntime, time);

                //check the size and move the list forward
                System.out.println("size of linked list" + deviceStatistics.size());

                //the actual size should be 96
                //store data of a whole daya
                //repeats in 15 minuets --> 4 for an hour - 4*24=96
                /*if(deviceStatistics.size() > 2){
                    int length = deviceStatistics.size();
                    DeviceStatistics[] temp_arr = new DeviceStatistics[length];
                    for(int k=0 ; k<length ; k++){
                        temp_arr[k] = deviceStatistics.get(k);
                    }

                    for(int j = 1 ; j <= temp_arr.length; j++){
                        temp_arr[j-1] = temp_arr[j];
                        System.out.println("----- " + j + " to "+ (j-1) + "-------" );
                    }
                }*/
                //temp_arr convert to arraylist

                deviceStatistics.add(storeData);

                //test win 10 graph split
                for(DeviceStatistics obj : deviceStatistics){
                    String time2 = obj.getTime();

                    //convert time to int (minuts)
                    String[] timeSplit = time2.split(":");
                    int h = 60*(Integer.parseInt(timeSplit[0]));
                    int m = Integer.parseInt(timeSplit[1]);
                }
            }
        }
    }

}

