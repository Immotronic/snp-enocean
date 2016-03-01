/*
 * Copyright (c) Immotronic, 2012
 *
 * Contributors:
 *
 *  	Lionel Balme (lbalme@immotronic.fr)
 *  	Kevin Planchet (kplanchet@immotronic.fr)
 *
 * This file is part of ubikit-core, a component of the UBIKIT project.
 *
 * This software is a computer program whose purpose is to host third-
 * parties applications that make use of sensor and actuator networks.
 *
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use,
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * As a counterpart to the access to the source code and  rights to copy,
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and,  more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 *
 * CeCILL-C licence is fully compliant with the GNU Lesser GPL v2 and v3.
 *
 */

package fr.immotronic.ubikit.pems.enocean.impl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.AbstractPhysicalEnvironmentModelEvent;
import org.ubikit.Logger;
import org.ubikit.pem.event.ItemAddedEvent;
import org.ubikit.pem.event.ItemAddingFailedEvent;
import org.ubikit.tools.http.WebApiCommons;

import fr.immotronic.ubikit.pems.enocean.EnoceanActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorAndActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.actuator.HVACDevice;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.Mode;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.VanePosition;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.FanSpeed;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.OnOffStatus;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.Disablement;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.WindowsStatus;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanTCMManager.TCMSettingResponse;
import fr.immotronic.ubikit.pems.enocean.impl.item.BlindAndShutterMotorDevice;
import fr.immotronic.ubikit.pems.enocean.impl.item.ElectronicSwitchWithEnergyMeasurement;
import fr.immotronic.ubikit.pems.enocean.impl.item.IntesisBoxDKRCENO1i1iCDevice;
import fr.immotronic.ubikit.pems.enocean.impl.item.OnOffDevice;
import fr.immotronic.ubikit.pems.enocean.impl.item.RHDevice;

public class APIServlet extends HttpServlet 
{
	private static final long serialVersionUID = -6372127057898997808L;

	private final DeviceManager deviceManager;
	private final EnoceanTCMManager tcmManager;
	
	public APIServlet(DeviceManager deviceManager, EnoceanTCMManager tcmManager)
	{
		this.deviceManager = deviceManager;
		this.tcmManager = tcmManager;
	}
	
	
	
	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)throws ServletException, IOException 
	{
		String[] pathInfo = req.getPathInfo().split("/"); // Expected path info is /command/command_param_1/command_param_2/...etc. 	
		// Notes: if the request is valid (e.g. looks like /command/command_params), 
		//		pathInfo[0] contains an empty string,
		//		pathInfo[1] contains "command",
		//		pathInfo[2] and next each contains a command parameter. Command parameters are "/"-separated.
		if(pathInfo != null && pathInfo.length > 1)
		{
			if(pathInfo[1].equals("gettcmbaseid"))
			{
				JSONObject o = new JSONObject();
				
				try 
				{
					o.put("tcmBaseID", tcmManager.getTransceiverBaseID());
				}
				catch (JSONException e) 
				{
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
					return;
				}
				
				resp.getWriter().write(WebApiCommons.okMessage(o));
				return;
			}
			else if(pathInfo[1].equals("getProperties"))
			{
				String itemUID = pathInfo[2];
				try 
				{
					BlindAndShutterMotorDevice device = (BlindAndShutterMotorDevice) deviceManager.getDevice(itemUID);
					resp.getWriter().write(WebApiCommons.okMessage(device.getValueAsJSON()));
					return;
				}
				catch(Exception e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
					return;
				}
			}
			else if(pathInfo[1].equals("getDimmerValue"))
			{
				String itemUID = pathInfo[2];
				try 
				{
					ElectronicSwitchWithEnergyMeasurement device = (ElectronicSwitchWithEnergyMeasurement) deviceManager.getDevice(itemUID);
					resp.getWriter().write(WebApiCommons.okMessage(device.getDimmerValueAsJSON()));
					return;
				}
				catch(Exception e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
					return;
				}
			}
		}
		
		resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.invalid_query));
	}


	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException 
	{
		String[] pathInfo = req.getPathInfo().split("/"); // Expected path info is /command/command_param_1/command_param_2/...etc. 	
		// Notes: if the request is valid (e.g. looks like /command/command_params), 
		//		pathInfo[0] contains an empty string,
		//		pathInfo[1] contains "command",
		//		pathInfo[2] and next each contains a command parameter. Command parameters are "/"-separated.
		if(pathInfo != null && pathInfo.length > 1)
		{
			if(pathInfo[1].equals("writetcmbaseid"))
			{
				switch(tcmManager.setTransceiverBaseID(req.getParameter("tcmBaseID")))
				{
					case OK:
						resp.getWriter().write(WebApiCommons.okMessage(null));
						return;
						
					case ERR_MORE_THAN_10_TIMES:
						resp.getWriter().write(WebApiCommons.errorMessage(TCMSettingResponse.ERR_MORE_THAN_10_TIMES.name(), WebApiCommons.Errors.invalid_query));
						return;
						
					case ERR_RANGE:
						resp.getWriter().write(WebApiCommons.errorMessage(TCMSettingResponse.ERR_RANGE.name(), WebApiCommons.Errors.invalid_query));
						return;
						
					case INVALID_VALUE:
						resp.getWriter().write(WebApiCommons.errorMessage(TCMSettingResponse.INVALID_VALUE.name(), WebApiCommons.Errors.invalid_query));
						return;
						
					case NOT_READY:
						resp.getWriter().write(WebApiCommons.errorMessage(TCMSettingResponse.NOT_READY.name(),WebApiCommons.Errors.internal_error));
						return;
						
					case WRITING_FAILED:
						resp.getWriter().write(WebApiCommons.errorMessage(TCMSettingResponse.WRITING_FAILED.name(),WebApiCommons.Errors.internal_error));
						return;
				}
			}
			else if(pathInfo[1].equals("add"))
			{	
				AbstractPhysicalEnvironmentModelEvent addingResponseEvent = deviceManager.createNewActuator(req.getParameter("actuatorProfile"), req.getParameter("customName"), req.getParameter("location"));
				
				if(addingResponseEvent instanceof ItemAddedEvent)
				{
					JSONObject o = new JSONObject();
					
					try {
						o.put("actuatorUID", addingResponseEvent.getSourceItemUID());
					}
					catch (JSONException e) {
						Logger.error(LC.gi(), this, "doPost/addActuator: While building the response object.");
						resp.getWriter().write(WebApiCommons.errorMessage(WebApiCommons.Errors.internal_error));
						return;
					}
					
					resp.getWriter().write(WebApiCommons.okMessage(o));
				}
				else if(addingResponseEvent instanceof ItemAddingFailedEvent)
				{
					ItemAddingFailedEvent e = (ItemAddingFailedEvent)addingResponseEvent;
					resp.getWriter().write(WebApiCommons.errorMessage(e.getReason(), e.getErrorCode()));
				}
			}
			else if(pathInfo[1].equals("pair"))
			{
				String actuatorUID = req.getParameter("actuatorUID");
				try
				{
					int nbOfSignals = Integer.parseInt(req.getParameter("nbOfSignals"));
					if(nbOfSignals > 0 && nbOfSignals < 4)
					{
						EnoceanDevice device = deviceManager.getDevice(actuatorUID);
						if(device != null)
						{
							if(device instanceof EnoceanActuatorDevice)
							{
								((EnoceanActuatorDevice)device).sendPairingSignals(nbOfSignals);
							}
							else if(device instanceof EnoceanSensorAndActuatorDevice)
							{
								((EnoceanSensorAndActuatorDevice)device).sendPairingSignals(nbOfSignals);
							}
						}
						
						resp.getWriter().write(WebApiCommons.okMessage(null));
						return;
					}
				}
				catch(NumberFormatException e)
				{ }
				
				resp.getWriter().write(WebApiCommons.errorMessage(req.getParameter("nbOfSignals")+" is not a valid number of paring signals. It MUST be 1, 2 ou 3.", WebApiCommons.Errors.invalid_query));
			}
			else if(pathInfo[1].equals("on"))
			{
				String itemUID = pathInfo[2];
				try
				{
					OnOffDevice device = (OnOffDevice) deviceManager.getDevice(itemUID);
					device.setOn();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an OnOff device", WebApiCommons.Errors.invalid_query));
				}
				
			}
			else if(pathInfo[1].equals("off"))
			{
				String itemUID = pathInfo[2];
				try
				{
					OnOffDevice device = (OnOffDevice) deviceManager.getDevice(itemUID);
					device.setOff();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an OnOff device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("up"))
			{
				String itemUID = pathInfo[2];
				EnoceanDevice device = deviceManager.getDevice(itemUID);
				
				if(device instanceof BlindAndShutterMotorDevice) 
				{
					((BlindAndShutterMotorDevice) device).moveUp();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				else if(device instanceof ElectronicSwitchWithEnergyMeasurement) 
				{
					((ElectronicSwitchWithEnergyMeasurement) device).moveUp();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				else 
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not a motor device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("stop"))
			{
				String itemUID = pathInfo[2];
				EnoceanDevice device = deviceManager.getDevice(itemUID);
				
				if(device instanceof BlindAndShutterMotorDevice) 
				{
					((BlindAndShutterMotorDevice) device).stop();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				else if(device instanceof ElectronicSwitchWithEnergyMeasurement) 
				{
					((ElectronicSwitchWithEnergyMeasurement) device).stop();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				else 
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not a motor device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("down"))
			{
				String itemUID = pathInfo[2];
				EnoceanDevice device = deviceManager.getDevice(itemUID);
				
				if(device instanceof BlindAndShutterMotorDevice) 
				{
					((BlindAndShutterMotorDevice) device).moveDown();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				else if(device instanceof ElectronicSwitchWithEnergyMeasurement) 
				{
					((ElectronicSwitchWithEnergyMeasurement) device).moveDown();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				else 
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not a motor device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("send1"))
			{
				String itemUID = pathInfo[2];
				try
				{
					BlindAndShutterMotorDevice device = (BlindAndShutterMotorDevice) deviceManager.getDevice(itemUID);
					device.send1();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an BlindAndShutterMotor device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("send0"))
			{
				String itemUID = pathInfo[2];
				try
				{
					BlindAndShutterMotorDevice device = (BlindAndShutterMotorDevice) deviceManager.getDevice(itemUID);
					device.send0();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an BlindAndShutterMotor device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("invertMotor"))
			{
				String itemUID = pathInfo[2];
				
				try
				{
					BlindAndShutterMotorDevice device = (BlindAndShutterMotorDevice) deviceManager.getDevice(itemUID);
					boolean inverted = Boolean.parseBoolean(req.getParameter("inverted"));
					device.setInverted(inverted);
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an BlindAndShutterMotor device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("setPressingType"))
			{
				String itemUID = pathInfo[2];
				try
				{
					BlindAndShutterMotorDevice device = (BlindAndShutterMotorDevice) deviceManager.getDevice(itemUID);
					boolean shortPressing = Boolean.parseBoolean(req.getParameter("shortPressing"));
					Logger.debug(LC.gi(), this, "shortPressing for "+itemUID+"="+shortPressing);
					device.setCommandOnShortPressing(shortPressing);
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an BlindAndShutterMotor device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("outputLevel"))
			{
				String itemUID = pathInfo[2];
				int level = Integer.parseInt(req.getParameter("level"));
				try
				{
					RHDevice device = (RHDevice) deviceManager.getDevice(itemUID);
					device.setOutputLevel(level);
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an RHMotor device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("setHVACMode"))
			{
				String itemUID = pathInfo[2];
				String mode = pathInfo[3];
				try
				{
					HVACDevice device = (HVACDevice) deviceManager.getDevice(itemUID);
					device.setMode(Mode.valueOf(mode));
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an IntesisBoxControllerDevice device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("setHVACVanePosition"))
			{
				String itemUID = pathInfo[2];
				String vanePosition = pathInfo[3];
				try
				{
					HVACDevice device = (HVACDevice) deviceManager.getDevice(itemUID);
					device.setVanePosition(VanePosition.valueOf(vanePosition));
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an IntesisBoxControllerDevice device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("setHVACFanSpeed"))
			{
				String itemUID = pathInfo[2];
				String fanSpeed = pathInfo[3];
				try
				{
					HVACDevice device = (HVACDevice) deviceManager.getDevice(itemUID);
					device.setFanSpeed(FanSpeed.valueOf(fanSpeed));
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an IntesisBoxControllerDevice device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("setHVACOnOff"))
			{
				String itemUID = pathInfo[2];
				String onOff = pathInfo[3];
				try
				{
					HVACDevice device = (HVACDevice) deviceManager.getDevice(itemUID);
					device.setOnOrOff(OnOffStatus.valueOf(onOff));
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an IntesisBoxControllerDevice device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("setHVACSetPoint"))
			{
				String itemUID = pathInfo[2];
				String setPointTemperature = pathInfo[3];
				try
				{
					HVACDevice device = (HVACDevice) deviceManager.getDevice(itemUID);
					device.setSetPointTemperature(Float.parseFloat(setPointTemperature));
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an IntesisBoxControllerDevice device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("IBExternalDisablement"))
			{
				String itemUID = pathInfo[2];
				String disablement = pathInfo[3];
				try
				{
					IntesisBoxDKRCENO1i1iCDevice device = (IntesisBoxDKRCENO1i1iCDevice) deviceManager.getDevice(itemUID);
					device.setExternalDisablement(Disablement.valueOf(disablement));
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an IntesisBoxControllerDevice device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("IBRemoteControllerDisablement"))
			{
				String itemUID = pathInfo[2];
				String disablement = pathInfo[3];
				try
				{
					IntesisBoxDKRCENO1i1iCDevice device = (IntesisBoxDKRCENO1i1iCDevice) deviceManager.getDevice(itemUID);
					device.setRemoteControllerDisablement(Disablement.valueOf(disablement));
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an IntesisBoxControllerDevice device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("IBWindowsStatus"))
			{
				String itemUID = pathInfo[2];
				String windowsStatus = pathInfo[3];
				try
				{
					IntesisBoxDKRCENO1i1iCDevice device = (IntesisBoxDKRCENO1i1iCDevice) deviceManager.getDevice(itemUID);
					device.setWindowsStatus(WindowsStatus.valueOf(windowsStatus));
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an IntesisBoxControllerDevice device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("ESWEMOn"))
			{
				String itemUID = pathInfo[2];
				int channel_index = 0;
				if(pathInfo.length > 3) {
					channel_index = Integer.parseInt(pathInfo[3]);
				}
				
				Logger.debug(LC.gi(), this, "pathInfo="+req.getPathInfo()+", pathInfo.length="+pathInfo.length+", channel_index="+channel_index);
				
				try
				{
					ElectronicSwitchWithEnergyMeasurement device = (ElectronicSwitchWithEnergyMeasurement) deviceManager.getDevice(itemUID);
					device.switchOn(channel_index);
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an Electronic Switch with Energy Measurement (Type 6) device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("ESWEMOff"))
			{
				String itemUID = pathInfo[2];
				int channel_index = 0;
				if(pathInfo.length > 3) {
					channel_index = Integer.parseInt(pathInfo[3]);
				}
				
				try
				{
					ElectronicSwitchWithEnergyMeasurement device = (ElectronicSwitchWithEnergyMeasurement) deviceManager.getDevice(itemUID);
					device.switchOff(channel_index);
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an Electronic Switch with Energy Measurement (Type 6) device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("ESWEMDim"))
			{
				String itemUID = pathInfo[2];
				int value = Integer.parseInt(req.getParameter("value"));
				try
				{
					ElectronicSwitchWithEnergyMeasurement device = (ElectronicSwitchWithEnergyMeasurement) deviceManager.getDevice(itemUID);
					device.switchToValue(value);
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an Electronic Switch with Energy Measurement (Type 6) device", WebApiCommons.Errors.invalid_query));
				}
			}
			else if(pathInfo[1].equals("ESWEMUpdateQuery"))
			{
				String itemUID = pathInfo[2];
				try
				{
					ElectronicSwitchWithEnergyMeasurement device = (ElectronicSwitchWithEnergyMeasurement) deviceManager.getDevice(itemUID);
					device.sendStatusQuery();
					device.sendMeasurementQuery();
					resp.getWriter().write(WebApiCommons.okMessage(null));
				}
				catch(ClassCastException e)
				{
					resp.getWriter().write(WebApiCommons.errorMessage(itemUID+" is not an Electronic Switch with Energy Measurement (Type 6) device", WebApiCommons.Errors.invalid_query));
				}
			}
		}
	}
}
