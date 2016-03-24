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
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.device.mgt.iot.digitaldisplay.manager.api;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.apimgt.application.extension.APIManagementProviderService;
import org.wso2.carbon.apimgt.application.extension.dto.ApiApplicationKey;
import org.wso2.carbon.apimgt.application.extension.exception.APIManagerException;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.device.mgt.common.Device;
import org.wso2.carbon.device.mgt.common.DeviceIdentifier;
import org.wso2.carbon.device.mgt.common.DeviceManagementException;
import org.wso2.carbon.device.mgt.common.EnrolmentInfo;
import org.wso2.carbon.device.mgt.iot.digitaldisplay.manager.api.util.APIUtil;
import org.wso2.carbon.device.mgt.iot.exception.DeviceControllerException;
import org.wso2.carbon.device.mgt.iot.util.ZipArchive;
import org.wso2.carbon.device.mgt.iot.util.ZipUtil;
import org.wso2.carbon.device.mgt.iot.digitaldisplay.plugin.constants.DigitalDisplayConstants;
import org.wso2.carbon.device.mgt.jwt.client.extension.JWTClient;
import org.wso2.carbon.device.mgt.jwt.client.extension.JWTClientManager;
import org.wso2.carbon.device.mgt.jwt.client.extension.dto.AccessTokenInfo;
import org.wso2.carbon.device.mgt.jwt.client.extension.exception.JWTClientException;
import org.wso2.carbon.user.api.UserStoreException;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class DigitalDisplayManagerService {

	private static Log log = LogFactory.getLog(DigitalDisplayManagerService.class);
	@Context  //injected response proxy supporting multiple thread
	private HttpServletResponse response;
	private static final String KEY_TYPE = "PRODUCTION";
	private static ApiApplicationKey apiApplicationKey;

	@Path("manager/device")
	@POST
	public boolean register(@QueryParam("deviceId") String deviceId, @QueryParam("name") String name) {
		DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
		deviceIdentifier.setId(deviceId);
		deviceIdentifier.setType(DigitalDisplayConstants.DEVICE_TYPE);
		try {
			if (APIUtil.getDeviceManagementService().isEnrolled(deviceIdentifier)) {
				response.setStatus(Response.Status.CONFLICT.getStatusCode());
				return false;
			}
			Device device = new Device();
			device.setDeviceIdentifier(deviceId);
			EnrolmentInfo enrolmentInfo = new EnrolmentInfo();
			enrolmentInfo.setDateOfEnrolment(new Date().getTime());
			enrolmentInfo.setDateOfLastUpdate(new Date().getTime());
			enrolmentInfo.setStatus(EnrolmentInfo.Status.ACTIVE);
			device.setName(name);
			device.setType(DigitalDisplayConstants.DEVICE_TYPE);
			enrolmentInfo.setOwner(APIUtil.getAuthenticatedUser());
			device.setEnrolmentInfo(enrolmentInfo);
			boolean added = APIUtil.getDeviceManagementService().enrollDevice(device);
			if (added) {
				response.setStatus(Response.Status.OK.getStatusCode());
			} else {
				response.setStatus(Response.Status.NOT_ACCEPTABLE.getStatusCode());
			}

			return added;
		} catch (DeviceManagementException e) {
			response.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
			return false;
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}

	@Path("manager/device/{device_id}")
	@DELETE
	public void removeDevice(@PathParam("device_id") String deviceId, @Context HttpServletResponse response) {
		DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
		deviceIdentifier.setId(deviceId);
		deviceIdentifier.setType(DigitalDisplayConstants.DEVICE_TYPE);
		try {
			boolean removed = APIUtil.getDeviceManagementService().disenrollDevice(
					deviceIdentifier);
			if (removed) {
				response.setStatus(Response.Status.OK.getStatusCode());
			} else {
				response.setStatus(Response.Status.NOT_ACCEPTABLE.getStatusCode());
			}
		} catch (DeviceManagementException e) {
			response.setStatus(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}

	@Path("manager/device/{device_id}")
	@PUT
	public boolean updateDevice(@PathParam("device_id") String deviceId,
								@QueryParam("name") String name,
								@Context HttpServletResponse response) {
		DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
		deviceIdentifier.setId(deviceId);
		deviceIdentifier.setType(DigitalDisplayConstants.DEVICE_TYPE);
		try {
			Device device = APIUtil.getDeviceManagementService().getDevice(deviceIdentifier);
			device.setDeviceIdentifier(deviceId);
			device.getEnrolmentInfo().setDateOfLastUpdate(new Date().getTime());
			device.setName(name);
			device.setType(DigitalDisplayConstants.DEVICE_TYPE);
			boolean updated = APIUtil.getDeviceManagementService().modifyEnrollment(device);
			if (updated) {
				response.setStatus(Response.Status.OK.getStatusCode());

			} else {
				response.setStatus(Response.Status.NOT_ACCEPTABLE.getStatusCode());
			}
			return updated;
		} catch (DeviceManagementException e) {
			log.error(e.getErrorMessage());
			return false;
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}

	@Path("manager/device/{device_id}")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Device getDevice(@PathParam("device_id") String deviceId) {
		DeviceIdentifier deviceIdentifier = new DeviceIdentifier();
		deviceIdentifier.setId(deviceId);
		deviceIdentifier.setType(DigitalDisplayConstants.DEVICE_TYPE);
		try {
			return APIUtil.getDeviceManagementService().getDevice(deviceIdentifier);
		} catch (DeviceManagementException ex) {
			log.error("Error occurred while retrieving device with Id " + deviceId + "\n" + ex);
			return null;
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}

	@Path("manager/device/{sketch_type}/download")
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response downloadSketch(@QueryParam("deviceName") String deviceName,
								   @PathParam("sketch_type") String
										   sketchType) {
		try {
			ZipArchive zipFile = createDownloadFile(APIUtil.getAuthenticatedUser(), deviceName, sketchType);
			Response.ResponseBuilder response = Response.ok(FileUtils.readFileToByteArray(zipFile.getZipFile()));
			response.type("application/zip");
			response.header("Content-Disposition", "attachment; filename=\"" + zipFile.getFileName() + "\"");
			return response.build();
		} catch (IllegalArgumentException ex) {
			return Response.status(400).entity(ex.getMessage()).build();//bad request
		} catch (DeviceManagementException ex) {
			return Response.status(500).entity(ex.getMessage()).build();
		} catch (JWTClientException ex) {
			return Response.status(500).entity(ex.getMessage()).build();
		} catch (DeviceControllerException ex) {
			return Response.status(500).entity(ex.getMessage()).build();
		} catch (APIManagerException ex) {
			return Response.status(500).entity(ex.getMessage()).build();
		} catch (IOException ex) {
			return Response.status(500).entity(ex.getMessage()).build();
		} catch (UserStoreException ex) {
			return Response.status(500).entity(ex.getMessage()).build();
		} finally {
			PrivilegedCarbonContext.endTenantFlow();
		}
	}

	private ZipArchive createDownloadFile(String owner, String deviceName, String sketchType)
			throws DeviceManagementException, JWTClientException, DeviceControllerException, APIManagerException,
				   UserStoreException {
		if (owner == null) {
			throw new IllegalArgumentException("Error on createDownloadFile() Owner is null!");
		}
		//create new device id
		String deviceId = shortUUID();
		if (apiApplicationKey == null) {
			String applicationUsername =
					PrivilegedCarbonContext.getThreadLocalCarbonContext().getUserRealm().getRealmConfiguration().getAdminUserName();
			APIManagementProviderService apiManagementProviderService = APIUtil.getAPIManagementProviderService();
			String[] tags = {DigitalDisplayConstants.DEVICE_TYPE};
			apiApplicationKey = apiManagementProviderService.generateAndRetrieveApplicationKeys(
					DigitalDisplayConstants.DEVICE_TYPE, tags, KEY_TYPE, applicationUsername, true);
		}
		JWTClient jwtClient = JWTClientManager.getInstance().getJWTClient();
		String scopes = "device_type_" + DigitalDisplayConstants.DEVICE_TYPE + " device_" + deviceId;
		AccessTokenInfo accessTokenInfo = jwtClient.getAccessToken(apiApplicationKey.getConsumerKey(),
																   apiApplicationKey.getConsumerSecret(), owner, scopes);
		//create token
		String accessToken = accessTokenInfo.getAccess_token();
		String refreshToken = accessTokenInfo.getRefresh_token();
		//adding registering data
		boolean status;
		//Register the device with CDMF
		status = register(deviceId, deviceName);
		if (!status) {
			String msg = "Error occurred while registering the device with " + "id: " + deviceId + " owner:" + owner;
			throw new DeviceManagementException(msg);
		}
		ZipUtil ziputil = new ZipUtil();
		ZipArchive zipFile = ziputil.createZipFile(owner, APIUtil.getTenantDomainOftheUser(), sketchType, deviceId,
												   deviceName, accessToken, refreshToken);
		zipFile.setDeviceId(deviceId);
		return zipFile;
	}

	private static String shortUUID() {
		UUID uuid = UUID.randomUUID();
		long l = ByteBuffer.wrap(uuid.toString().getBytes(StandardCharsets.UTF_8)).getLong();
		return Long.toString(l, Character.MAX_RADIX);
	}

}
