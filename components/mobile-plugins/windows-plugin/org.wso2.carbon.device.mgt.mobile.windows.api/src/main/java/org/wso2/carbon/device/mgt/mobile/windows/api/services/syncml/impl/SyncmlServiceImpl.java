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

package org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.w3c.dom.Document;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.device.mgt.common.*;
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
import org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.SyncmlService;
import org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.beans.DeviceStatistics;
import org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.beans.GraphingData;
import org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.beans.WindowsDevice;
import org.wso2.carbon.device.mgt.mobile.windows.api.operations.*;
import org.wso2.carbon.device.mgt.mobile.windows.api.operations.util.*;
import org.wso2.carbon.policy.mgt.common.PolicyManagementException;
import org.wso2.carbon.policy.mgt.core.PolicyManagerService;

import javax.swing.*;
import javax.ws.rs.core.Response;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import java.io.*;
import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

import static org.wso2.carbon.device.mgt.mobile.windows.api.common.util.WindowsAPIUtils.convertToDeviceIdentifierObject;

/**
 * Implementing class of SyncmlImpl interface.
 */
public class SyncmlServiceImpl implements SyncmlService{

    private static Log log = LogFactory.getLog(
            org.wso2.carbon.device.mgt.mobile.windows.api.services.syncml.impl.SyncmlServiceImpl.class);

    //test graph
    public static List<DeviceStatistics> getDeviceStatistics() {
        return deviceStatistics;
    }

    public static List<DeviceStatistics> deviceStatistics = new ArrayList<>();
    int count;

    /**
     * This method is used to generate and return Device object from the received information at
     * the Syncml step.
     *
     * @param windowsDevice Windows specific property object.
     * @return - Generated device object.
     */
    private Device generateDevice(WindowsDevice windowsDevice) {

        Device generatedDevice = new Device();

        Device.Property OSVersionProperty = new Device.Property();
        OSVersionProperty.setName(PluginConstants.SyncML.OS_VERSION);
        OSVersionProperty.setValue(windowsDevice.getOsVersion());

        Device.Property IMSEIProperty = new Device.Property();
        IMSEIProperty.setName(PluginConstants.SyncML.IMSI);
        IMSEIProperty.setValue(windowsDevice.getImsi());

        Device.Property IMEIProperty = new Device.Property();
        IMEIProperty.setName(PluginConstants.SyncML.IMEI);
        IMEIProperty.setValue(windowsDevice.getImei());

        Device.Property DevManProperty = new Device.Property();
        DevManProperty.setName(PluginConstants.SyncML.VENDOR);
        DevManProperty.setValue(windowsDevice.getManufacturer());

        Device.Property DevModProperty = new Device.Property();
        DevModProperty.setName(PluginConstants.SyncML.MODEL);
        DevModProperty.setValue(windowsDevice.getModel());

        List<Device.Property> propertyList = new ArrayList<>();
        propertyList.add(OSVersionProperty);
        propertyList.add(IMSEIProperty);
        propertyList.add(IMEIProperty);
        propertyList.add(DevManProperty);
        propertyList.add(DevModProperty);

        EnrolmentInfo enrolmentInfo = new EnrolmentInfo();
        enrolmentInfo.setOwner(windowsDevice.getUser());
        enrolmentInfo.setOwnership(EnrolmentInfo.OwnerShip.BYOD);
        enrolmentInfo.setStatus(EnrolmentInfo.Status.ACTIVE);

        generatedDevice.setEnrolmentInfo(enrolmentInfo);
        generatedDevice.setDeviceIdentifier(windowsDevice.getDeviceId());
        generatedDevice.setProperties(propertyList);
        generatedDevice.setType(windowsDevice.getDeviceType());

        return generatedDevice;
    }

    /**
     * Method for calling SyncML engine for producing the Syncml response. For the first SyncML message comes from
     * the device, this method produces a response to retrieve device information for enrolling the device.
     *
     * @param request - SyncML request
     * @return - SyncML response
     * @throws WindowsOperationException
     * @throws WindowsDeviceEnrolmentException
     */
    @Override
    public Response getResponse(Document request)
            throws WindowsDeviceEnrolmentException, WindowsOperationException, NotificationManagementException,
            WindowsConfigurationException {
        int msgId;
        int sessionId;
        String user;
        String token;
        String response;
        SyncmlDocument syncmlDocument;
        List<Operation> deviceInfoOperations;
        List<? extends Operation> pendingOperations;
        OperationHandler operationHandler = new OperationHandler();
        DeviceInfo deviceInfo = new DeviceInfo();

        printXML(request);

        try {
            if (SyncmlParser.parseSyncmlPayload(request) != null) {
                syncmlDocument = SyncmlParser.parseSyncmlPayload(request);
                SyncmlHeader syncmlHeader = syncmlDocument.getHeader();
                sessionId = syncmlHeader.getSessionId();

                //windows 10 --------
                //check whether the credentials is there in the request header
                //if not the response body requests the credentials from the device

                //if the Request header contains credentials
                if(syncmlHeader.getCredential()!=null){
                    System.out.println("Credentials ok");

                    user = syncmlHeader.getSource().getLocName();
                    DeviceIdentifier deviceIdentifier = convertToDeviceIdentifierObject(syncmlHeader.getSource().
                            getLocURI());
                    msgId = syncmlHeader.getMsgID();
                    if ((PluginConstants.SyncML.SYNCML_FIRST_MESSAGE_ID == msgId) &&
                            (PluginConstants.SyncML.SYNCML_FIRST_SESSION_ID == sessionId)) {
                        //windows 10
                        token = syncmlHeader.getCredential().getData();
                        CacheEntry cacheToken = (CacheEntry) DeviceUtil.getCacheEntry(token);

                        if ((cacheToken.getUsername() != null) && (cacheToken.getUsername().equals(user))) {
                            if (enrollDevice(request)) {
                                deviceInfoOperations = deviceInfo.getDeviceInfo();
                                response = generateReply(syncmlDocument, deviceInfoOperations);
                                System.out.println(response);
                                return Response.status(Response.Status.OK).entity(response).build();
                            } else {
                                String msg = "Error occurred in device enrollment.";
                                log.error(msg);
                                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
                            }
                        } else {
                            String msg = "Authentication failure due to incorrect credentials.";
                            log.error(msg);
                            return Response.status(Response.Status.UNAUTHORIZED).entity(msg).build();
                        }
                    } else if (PluginConstants.SyncML.SYNCML_SECOND_MESSAGE_ID == msgId &&
                            PluginConstants.SyncML.SYNCML_FIRST_SESSION_ID == sessionId) {
                        if (enrollDevice(request)) {
                            response = generateReply(syncmlDocument, null);
                            System.out.println(response);
                            return Response.ok().entity(generateReply(syncmlDocument, null)).build();
                        } else {
                            String msg = "Error occurred in modify enrollment.";
                            log.error(msg);
                            return Response.status(Response.Status.NOT_MODIFIED).entity(msg).build();
                        }
                    } else if (sessionId >= PluginConstants.SyncML.SYNCML_SECOND_SESSION_ID) {
                        if ((syncmlDocument.getBody().getAlert() != null)) {
                            if (!syncmlDocument.getBody().getAlert().getData().equals(Constants.DISENROLL_ALERT_DATA)) {
                                pendingOperations = operationHandler.getPendingOperations(syncmlDocument);
                                return Response.ok().entity(generateReply(syncmlDocument, pendingOperations)).build();
                            } else {
                                if (WindowsAPIUtils.getDeviceManagementService().getDevice(deviceIdentifier) != null) {
                                    WindowsAPIUtils.getDeviceManagementService().disenrollDevice(deviceIdentifier);
                                    return Response.ok().entity(generateReply(syncmlDocument, null)).build();
                                } else {
                                    String msg = "Enrolled device can not be found in the server.";
                                    log.error(msg);
                                    return Response.status(Response.Status.NOT_FOUND).entity(msg).build();
                                }
                            }
                        } else {
                            pendingOperations = operationHandler.getPendingOperations(syncmlDocument);
                            return Response.ok().entity(generateReply(syncmlDocument, pendingOperations)).build();
                        }
                    } else {
                        String msg = "Failure occurred in Device request message.";
                        log.error(msg);
                        return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
                    }

                //if the Request body does not contain the credentials
                //in windows 10 laptop devices the initial Request does not contain credentials
                }else{

                    System.out.println("No credentials");
                    user = Constants.USER_WITHOUT_CREDENTIALS;
                    DeviceIdentifier deviceIdentifier = convertToDeviceIdentifierObject(syncmlHeader.getSource().
                            getLocURI());
                    msgId = syncmlHeader.getMsgID();

                    if (PluginConstants.SyncML.SYNCML_FIRST_MESSAGE_ID == msgId) {

                        String username = Constants.USER_WITHOUT_CREDENTIALS;

                        String disenrollOperation = syncmlDocument.getBody().getAlert().getData();

                        if (disenrollOperation.equals(Constants.NOT_OPERATION)) {
                        //if ((username != null) && (username.equals(user) && (disenrollOperation.equals(Constants.NOT_OPERATION)))) {

                            if (enrollDevice(request)) {
                                deviceInfoOperations = deviceInfo.getDeviceInfo();
                                response = generateReply(syncmlDocument, deviceInfoOperations);
                                //System.out.println("MSGID = 1 -----------------------" + count + "---------------");
                                //System.out.println(response);
                                return Response.status(Response.Status.OK).entity(response).build();
                            } else {
                                String msg = "Error occurred in device enrollment.";
                                log.error(msg);
                                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(msg).build();
                            }

                            //Execute the disenroll Operation
                        } else if (!disenrollOperation.equals(Constants.NOT_OPERATION)){
                            if (!syncmlDocument.getBody().getAlert().getData().equals(Constants.DISENROLL_ALERT_DATA)) {
                                pendingOperations = operationHandler.getPendingOperations(syncmlDocument);
                                return Response.ok().entity(generateReply(syncmlDocument, pendingOperations)).build();
                            } else {
                                if (WindowsAPIUtils.getDeviceManagementService().getDevice(deviceIdentifier) != null) {
                                    WindowsAPIUtils.getDeviceManagementService().disenrollDevice(deviceIdentifier);
                                    System.out.println("Device disenrolled");
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
                                        System.out.println("RUNTIME : " + obj.getBat_runtime());
                                    }
                                    System.out.println("TIME : " + obj.getTime());
                                    count++;
                                }

                                System.out.println("-------------------------------------------------------------------------------");

                                //generateGraph();
                                /*JFrame f = new JFrame();
                                f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                                f.add(new GraphingData());
                                f.setSize(800,800);
                                f.setLocation(200,200);
                                f.setVisible(true);*/

                                //System.out.println(response);

                            }

                            //System.out.println(response);
                            return Response.ok().entity(generateReply(syncmlDocument, null)).build();

                    } else {
                        String msg = "Failure occurred in Device request message.";
                        log.error(msg);
                        return Response.status(Response.Status.BAD_REQUEST).entity(msg).build();
                    }
                }
            }
        } catch (SyncmlMessageFormatException e) {
            String msg = "Error occurred while parsing syncml request.";
            log.error(msg, e);
            throw new WindowsOperationException(msg, e);
        } catch (OperationManagementException e) {
            String msg = "Cannot access operation management service.";
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
    private boolean enrollDevice(Document request) throws WindowsDeviceEnrolmentException,
            WindowsOperationException {

        String osVersion;
        String imsi = null;
        String imei = null;
        String devID;
        String devMan;
        String devMod;
        String devLang;
        String vendor;
        String macAddress;
        String resolution;
        String modVersion;
        boolean status = false;
        String user;
        String deviceName;
        int msgID;
        SyncmlDocument syncmlDocument;

        try {
            syncmlDocument = SyncmlParser.parseSyncmlPayload(request);
            msgID = syncmlDocument.getHeader().getMsgID();
            if (msgID == PluginConstants.SyncML.SYNCML_FIRST_MESSAGE_ID) {
                ReplaceTag replace = syncmlDocument.getBody().getReplace();
                List<ItemTag> itemList = replace.getItems();
                devID = itemList.get(PluginConstants.SyncML.DEVICE_ID_POSITION).getData();
                devMan = itemList.get(PluginConstants.SyncML.DEVICE_MAN_POSITION).getData();
                devMod = itemList.get(PluginConstants.SyncML.DEVICE_MODEL_POSITION).getData();
                modVersion = itemList.get(PluginConstants.SyncML.DEVICE_MOD_VER_POSITION).getData();
                devLang = itemList.get(PluginConstants.SyncML.DEVICE_LANG_POSITION).getData();

                //windows 10
                //user = syncmlDocument.getHeader().getSource().getLocName();
                user = Constants.USER_WITHOUT_CREDENTIALS;

                AuthenticationInfo authenticationInfo = new AuthenticationInfo();
                authenticationInfo.setUsername(user);
                WindowsAPIUtils.startTenantFlow(authenticationInfo);

                if (log.isDebugEnabled()) {
                    log.debug(
                            "OS Version:" + modVersion + ", DevID: " + devID + ", DevMan: " + devMan +
                                    ", DevMod: " + devMod + ", DevLang: " + devLang);
                }
                WindowsDevice windowsDevice = new WindowsDevice();
                windowsDevice.setDeviceType(DeviceManagementConstants.MobileDeviceTypes.
                        MOBILE_DEVICE_TYPE_WINDOWS);
                windowsDevice.setDeviceId(devID);
                windowsDevice.setImei(imei);
                windowsDevice.setImsi(imsi);
                windowsDevice.setManufacturer(devMan);
                windowsDevice.setOsVersion(modVersion);
                windowsDevice.setModel(devMod);
                windowsDevice.setUser(user);
                Device device = generateDevice(windowsDevice);
                status = WindowsAPIUtils.getDeviceManagementService().enrollDevice(device);
                return status;

            } else if (msgID == PluginConstants.SyncML.SYNCML_SECOND_MESSAGE_ID) {

                List<ItemTag> itemList = syncmlDocument.getBody().getResults().getItem();
                osVersion = itemList.get(PluginConstants.SyncML.OSVERSION_POSITION).getData();
                imsi = itemList.get(PluginConstants.SyncML.IMSI_POSITION).getData();
                imei = itemList.get(PluginConstants.SyncML.IMEI_POSITION).getData();
                vendor = itemList.get(PluginConstants.SyncML.VENDOR_POSITION).getData();
                devMod = itemList.get(PluginConstants.SyncML.MODEL_POSITION).getData();
                macAddress = itemList.get(PluginConstants.SyncML.MAC_ADDRESS_POSITION).getData();
                resolution = itemList.get(PluginConstants.SyncML.RESOLUTION_POSITION).getData();
                deviceName = itemList.get(PluginConstants.SyncML.DEVICE_NAME_POSITION).getData();
                DeviceIdentifier deviceIdentifier = convertToDeviceIdentifierObject(syncmlDocument.
                        getHeader().getSource().getLocURI());
                Device existingDevice = WindowsAPIUtils.getDeviceManagementService().getDevice(deviceIdentifier);
                if (!existingDevice.getProperties().isEmpty()) {
                    List<Device.Property> existingProperties = new ArrayList<>();

                    Device.Property imeiProperty = new Device.Property();
                    imeiProperty.setName(PluginConstants.SyncML.IMEI);
                    imeiProperty.setValue(imei);
                    existingProperties.add(imeiProperty);

                    Device.Property osVersionProperty = new Device.Property();
                    osVersionProperty.setName(PluginConstants.SyncML.OS_VERSION);
                    osVersionProperty.setValue(osVersion);
                    existingProperties.add(osVersionProperty);

                    Device.Property imsiProperty = new Device.Property();
                    imsiProperty.setName(PluginConstants.SyncML.IMSI);
                    imsiProperty.setValue(imsi);
                    existingProperties.add(imsiProperty);

                    Device.Property vendorProperty = new Device.Property();
                    vendorProperty.setName(PluginConstants.SyncML.VENDOR);
                    vendorProperty.setValue(vendor);
                    existingProperties.add(vendorProperty);

                    Device.Property macAddressProperty = new Device.Property();
                    macAddressProperty.setName(PluginConstants.SyncML.MAC_ADDRESS);
                    macAddressProperty.setValue(macAddress);
                    existingProperties.add(macAddressProperty);

//                    Device.Property resolutionProperty = new Device.Property();
//                    resolutionProperty.setName(PluginConstants.SyncML.DEVICE_INFO);
//                    resolutionProperty.setValue("null");
//                    existingProperties.add(resolutionProperty);

                    Device.Property deviceNameProperty = new Device.Property();
                    deviceNameProperty.setName(PluginConstants.SyncML.DEVICE_NAME);
                    deviceNameProperty.setValue(deviceName);
                    existingProperties.add(deviceNameProperty);

                    Device.Property deviceModelProperty = new Device.Property();
                    deviceNameProperty.setName(PluginConstants.SyncML.MODEL);
                    deviceNameProperty.setValue(devMod);
                    existingProperties.add(deviceModelProperty);

                    existingDevice.setProperties(existingProperties);
                    existingDevice.setDeviceIdentifier(syncmlDocument.getHeader().getSource().getLocURI());
                    existingDevice.setType(DeviceManagementConstants.MobileDeviceTypes.MOBILE_DEVICE_TYPE_WINDOWS);
                    status = WindowsAPIUtils.getDeviceManagementService().modifyEnrollment(existingDevice);
                    // call effective policy for the enrolling device.
                    PolicyManagerService policyManagerService = WindowsAPIUtils.getPolicyManagerService();
                    policyManagerService.getEffectivePolicy(deviceIdentifier);
                    return status;
                }
            }
        } catch (DeviceManagementException e) {
            throw new WindowsDeviceEnrolmentException("Failure occurred while enrolling device.", e);
        } catch (PolicyManagementException e) {
            throw new WindowsOperationException("Error occurred while getting effective policy.", e);
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

    public void printXML(Document request){

        try
        {
            DOMSource domSource = new DOMSource(request);
            StringWriter writer = new StringWriter();
            StreamResult result = new StreamResult(writer);
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.transform(domSource, result);

            String check = writer.toString();
            count ++;

            try {
                //Whatever the file path is.
                String filepath = "/home/thisari/Documents/thisaripata/" + count + "Request.xml";
                //String filepath = "/home/thisari/Documents/thisaripata/Request.xml";
                File statText = new File(filepath);
                FileOutputStream is = new FileOutputStream(statText);
                OutputStreamWriter osw = new OutputStreamWriter(is);
                Writer w = new BufferedWriter(osw);
                w.write(check);
                w.close();
            } catch (IOException e) {
                System.err.println("Problem writing to the file statsTest.txt");
            }

            //System.out.print(check);
        }
        catch(TransformerException ex)
        {
            ex.printStackTrace();
        }
    }

    public void getValues (SyncmlDocument syncmlDocument){

        List<ItemTag> deviceResults = syncmlDocument.getBody().getResults().getItem();

        for(int i = 0 ; i < deviceResults.size() ; i++){
            ItemTag currentItem = deviceResults.get(i);
            String syncmlPath = currentItem.getSource().getLocURI();

            if(syncmlPath.equals("./Vendor/MSFT/DeviceStatus/Battery/Status")){
                String stringValue_batStatus = currentItem.getData();
                int valueStatus = Integer.parseInt(stringValue_batStatus);
                int valueRuntime = 0;

                if(valueStatus == 0){                                //if not plugged into charge
                    //get the estimated runtime
                    int next = i+1;
                    ItemTag estimatedRuntime = deviceResults.get(next);
                    String stringValue_runtime = estimatedRuntime.getData();
                    int temp_runtime = Integer.parseInt(stringValue_runtime);
                    valueRuntime = (temp_runtime * 100)/14400;
                }

                String time = new SimpleDateFormat("HH:mm:ss").format(Calendar.getInstance().getTime());

                DeviceStatistics storeData = new DeviceStatistics(valueStatus, valueRuntime, time);
                deviceStatistics.add(storeData);

                //test win 10 graph split
                for(DeviceStatistics obj : deviceStatistics){
                    String time2 = obj.getTime();

                    //convert time to int (minuts)
                    String[] timeSplit = time2.split(":");
                    int h = 60*(Integer.parseInt(timeSplit[0]));
                    int m = Integer.parseInt(timeSplit[1]);
                    int total_minuts = h+m;
                    /*System.out.println("h : " + timeSplit[0] + " m : " + timeSplit[1]);
                    System.out.println("h(int) : " + h + " m(int) : " + m);
                    System.out.println("/////////////////////////////////");*/
                }
            }
        }
    }

}