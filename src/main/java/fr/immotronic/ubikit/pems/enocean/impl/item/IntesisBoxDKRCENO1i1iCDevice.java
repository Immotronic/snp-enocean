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

package fr.immotronic.ubikit.pems.enocean.impl.item;

import org.json.JSONObject;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.ControllerProfile;
import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.actuator.HVACDevice;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.Disablement;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.FanSpeed;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.Mode;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.OnOffStatus;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.RoomOccupancy;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.VanePosition;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.WindowsStatus;
import fr.immotronic.ubikit.pems.enocean.event.in.GenericHVACInterfaceControlEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.GenericHVACInterfaceDisablementMessageEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.TemperatureAndSetPointEvent;
import fr.immotronic.ubikit.pems.enocean.impl.DatabaseManager;
import fr.immotronic.ubikit.pems.enocean.impl.DeviceManager;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanSensorAndActuatorDeviceImpl;
import fr.immotronic.ubikit.pems.enocean.impl.NoControllerChannelLeftException;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP07201011DataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEP07201011DataImpl;

public final class IntesisBoxDKRCENO1i1iCDevice 
						extends EnoceanSensorAndActuatorDeviceImpl 
						implements 	TemperatureAndSetPointEvent.Listener, 
									GenericHVACInterfaceDisablementMessageEvent.Listener, 
									GenericHVACInterfaceControlEvent.Listener, 
									HVACDevice
{	
	private float temperatureMultiplier = (255f/40f);
	
	private FourByteSensorController controller_20_10;
	private FourByteSensorController controller_20_11;
	private FourByteSensorController controller_10_03;
	
	private float setPointTemperature = 20f;
	private float ambientTemperature = 20f;
	
	public IntesisBoxDKRCENO1i1iCDevice(EventGate eventGate, DatabaseManager.Record record, DeviceManager deviceManager)
	{
		super(record.getUID(), record.getEnoceanUID(), SensorActuatorProfile.INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE, record.getManufacturerUID(), deviceManager, null);
		
		setPropertiesFromJSONObject(record.getPropertiesAsJSON());
		addCapability(SensorActuatorProfile.INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE.name());
		
		int eep2010id = (int)Long.parseLong(getPropertyValue(EnoceanEquipmentProfileV20.EEP_07_20_10.name()), 16);
		int eep2011id = (int)Long.parseLong(getPropertyValue(EnoceanEquipmentProfileV20.EEP_07_20_11.name()), 16);
	
		// Registering additional Enocean UID in device manager
		if(getEnoceanUID() == eep2010id) {
			deviceManager.registerAdditionalEnoceanUID(getEnoceanUID(), eep2011id);
		}
		else {
			deviceManager.registerAdditionalEnoceanUID(getEnoceanUID(), eep2010id);
		}
		
		// Create Data handler
		EEP07201011DataHandler dh = new EEP07201011DataHandler(eventGate, getUID(), record.getLastKnownData());
		dh.setTransmitterUID(EnoceanEquipmentProfileV20.EEP_07_20_10, eep2010id);
		dh.setTransmitterUID(EnoceanEquipmentProfileV20.EEP_07_20_11, eep2011id);
		setDataHandler(dh);
		
		// Create controllers
		String[] controllers = record.getData().split(" ");
		controller_20_10 = (FourByteSensorController) deviceManager.getController(Integer.parseInt(controllers[0]));
		controller_20_10.markAsUsed();
		controller_20_11 = (FourByteSensorController) deviceManager.getController(Integer.parseInt(controllers[1]));
		controller_20_11.markAsUsed();
		controller_10_03 = (FourByteSensorController) deviceManager.getController(Integer.parseInt(controllers[2]));
		controller_10_03.markAsUsed();
	}
	
	public IntesisBoxDKRCENO1i1iCDevice(EventGate eventGate, int enoceanUID, DeviceManager deviceManager, JSONObject userProperties) throws NoControllerChannelLeftException 
	{
		super(enoceanUID, SensorActuatorProfile.INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE, 0x19, deviceManager, null);
		
		setPropertiesFromJSONObject(userProperties);
		addCapability(SensorActuatorProfile.INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE.name());
		
		int eep2010id = (int)Long.parseLong(getPropertyValue(EnoceanEquipmentProfileV20.EEP_07_20_10.name()), 16);
		int eep2011id = (int)Long.parseLong(getPropertyValue(EnoceanEquipmentProfileV20.EEP_07_20_11.name()), 16);
		
		// Registering additional Enocean UID in device manager
		if(getEnoceanUID() == eep2010id) {
			deviceManager.registerAdditionalEnoceanUID(getEnoceanUID(), eep2011id);
		}
		else {
			deviceManager.registerAdditionalEnoceanUID(getEnoceanUID(), eep2010id);
		}
		
		// Create Data handler
		EEP07201011DataHandler dh = new EEP07201011DataHandler(eventGate, getUID(), null);
		dh.setTransmitterUID(EnoceanEquipmentProfileV20.EEP_07_20_10, eep2010id);
		dh.setTransmitterUID(EnoceanEquipmentProfileV20.EEP_07_20_11, eep2011id);
		setDataHandler(dh);
		
		// Create controllers
		controller_20_10 = (FourByteSensorController) deviceManager.getController(ControllerProfile.FOUR_BYTE_SENSOR_CONTROLLER, 1);
		if(controller_20_10 == null)
		{
			throw new NoControllerChannelLeftException();
		}
		controller_20_10.markAsUsed();
		
		controller_20_11 = (FourByteSensorController) deviceManager.getController(ControllerProfile.FOUR_BYTE_SENSOR_CONTROLLER, 1);
		if(controller_20_11 == null)
		{
			throw new NoControllerChannelLeftException();
		}
		controller_20_11.markAsUsed();
		
		controller_10_03 = (FourByteSensorController) deviceManager.getController(ControllerProfile.FOUR_BYTE_SENSOR_CONTROLLER, 1);
		if(controller_10_03 == null)
		{
			throw new NoControllerChannelLeftException();
		}
		controller_10_03.markAsUsed();
	}
	
	@Override
	public FourByteSensorController[] releaseController()
	{
		if(controller_20_10 != null && controller_20_11 != null && controller_10_03 != null)
		{
			FourByteSensorController[] controllers = new FourByteSensorController[3];
		
			controller_20_10.releaseChannel(1);
			controllers[0] = controller_20_10;
			controller_20_10 = null;
			
			controller_20_11.releaseChannel(1);
			controllers[1] = controller_20_11;
			controller_20_11 = null;
			
			controller_10_03.releaseChannel(1);
			controllers[2] = controller_10_03;
			controller_10_03 = null;
			
			return controllers;
		}
				
		return null;
	}
	
	@Override
	public void onEvent(GenericHVACInterfaceControlEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()) && controller_20_10 != null)
		{
			sendCommand(event.getMode(), event.getVanePosition(), event.getFanSpeed(), event.getRoomOccupancy(), event.getOnOrOff());
		}
	}
	
	public void setMode(Mode mode)
	{
		sendCommand(mode, VanePosition.NO_ACTION, FanSpeed.NO_ACTION, getLastKnownData().getRoomOccupancy(), getLastKnownData().getOnOffStatus());
	}
	
	public void setVanePosition(VanePosition vanePosition)
	{
		sendCommand(Mode.NO_ACTION, vanePosition, FanSpeed.NO_ACTION, getLastKnownData().getRoomOccupancy(), getLastKnownData().getOnOffStatus());
	}
	
	public void setFanSpeed(FanSpeed fanSpeed)
	{
		sendCommand(Mode.NO_ACTION, VanePosition.NO_ACTION, fanSpeed, getLastKnownData().getRoomOccupancy(), getLastKnownData().getOnOffStatus());
	}
	
	public void setOnOrOff(OnOffStatus onOffStatus)
	{
		sendCommand(Mode.NO_ACTION, VanePosition.NO_ACTION, FanSpeed.NO_ACTION, getLastKnownData().getRoomOccupancy(), onOffStatus);
	}
	
	private void sendCommand(Mode mode, VanePosition position, FanSpeed speed, RoomOccupancy roomOccupancy, OnOffStatus turnOnOff)
	{
		if(controller_20_10 != null)
		{
			EEP07201011Data data = getLastKnownData();
			data = new EEP07201011DataImpl(	(mode != Mode.NO_ACTION)?mode:data.getMode(), (position != VanePosition.NO_ACTION)?position:data.getVanePosition(), (speed != FanSpeed.NO_ACTION)?speed:data.getFanSpeed(), data.getRoomOccupancy(), turnOnOff, 
											data.getErrorCode(), data.getWindowContactDisablement(), data.getKeyCardDisablement(), 
											data.getExternalDisablement(), data.getRemoteControllerDisablement(), data.getWindowContact(), 
											data.getAlarmState(), data.getDate());
			((EEP07201011DataHandler) getDataHandler()).setData(data);
			
			byte[] bytes = { (byte) (((roomOccupancy.getValue() << 1) | (turnOnOff.getValue())) & 0x7),    // DB_0 : RoomOccupancy & On/Off data
							 0x00,                    // DB_1 : Not used
							 (byte) ((position.getValue() << 4 | speed.getValue()) & 0xff), // DB_2 : Vane position (4 MSB) & Fan speed (4 LSB)
							 mode.getValue() 		  // DB_3 : Mode
							};
			controller_20_10.sendDataTelegram(bytes);
		}
	}
	
	@Override
	public void onEvent(GenericHVACInterfaceDisablementMessageEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()) && controller_20_11 != null)
		{
			sendDisablementMessage(event.hasExternalDisablement(), event.isRemoteControllerDisable(), event.isWindowClosed());
		}
	}
	
	public void setExternalDisablement(Disablement disablement)
	{
		sendDisablementMessage(disablement, getLastKnownData().getRemoteControllerDisablement(), getLastKnownData().getWindowContact());
	}
	
	public void setRemoteControllerDisablement(Disablement disablement)
	{
		sendDisablementMessage(getLastKnownData().getExternalDisablement(), disablement, getLastKnownData().getWindowContact());
	}
	
	public void setWindowsStatus(WindowsStatus windowsStatus)
	{
		sendDisablementMessage(getLastKnownData().getExternalDisablement(), getLastKnownData().getRemoteControllerDisablement(), windowsStatus);
	}
	
	private void sendDisablementMessage(Disablement hasExternalDisablement, Disablement remoteControllerDisable, WindowsStatus windowStatus)
	{
		if(controller_20_11 != null)
		{
			EEP07201011Data data = getLastKnownData();
			data = new EEP07201011DataImpl(	data.getMode(), data.getVanePosition(), data.getFanSpeed(), data.getRoomOccupancy(), data.getOnOffStatus(), 
											data.getErrorCode(), data.getWindowContactDisablement(), data.getKeyCardDisablement(), 
											hasExternalDisablement, remoteControllerDisable, windowStatus, 
											data.getAlarmState(), data.getDate());
			((EEP07201011DataHandler) getDataHandler()).setData(data);
			byte[] bytes = { (byte) (((windowStatus.getValue() << 1) | (remoteControllerDisable.getValue() << 2)) & 0x6),   // DB_0
							 hasExternalDisablement.getValue(),    // DB_1                
							 0x00,   // DB_2 : Not used
							 0x00	 // DB_3 : Not used
							};
			controller_20_11.sendDataTelegram(bytes);
		}
	}
	
	@Override
	public void onEvent(TemperatureAndSetPointEvent event)
	{
		if(event.getSourceItemUID().equals(getUID()) && controller_10_03 != null)
		{
			sendDataTemperature(event.getSetPoint(), event.getCurrentTemperature());
		}
	}
	
	@Override
	public void setSetPointTemperature(float setPointTemperature)
	{
		sendDataTemperature(setPointTemperature, this.ambientTemperature);
	}
	
	@Override
	public void setAmbientTemperature(float ambientTemperature)
	{
		sendDataTemperature(this.setPointTemperature, ambientTemperature);
	}
	
	private void sendDataTemperature(float setPointTemperature, float ambientTemperature)
	{
		if(setPointTemperature != Float.MIN_VALUE) {
			this.setPointTemperature = setPointTemperature;
		}
		
		if(ambientTemperature != Float.MIN_VALUE) {
			this.ambientTemperature = ambientTemperature;
		}
		
		if(controller_10_03 != null)
		{
			byte setPoint;
			if (this.setPointTemperature < 0)
				setPoint = 0x00;
			else if (this.setPointTemperature > 40) 
				setPoint = (byte) 0xff;
			else
				setPoint = (byte) ((int)(this.setPointTemperature*temperatureMultiplier) & 0xff);
			

			byte temp;
			if (this.ambientTemperature < 0)
				temp = (byte) 0xff;
			else if (this.ambientTemperature > 40) 
				temp = 0x00;
			else
				temp = (byte) ((int)((255-(this.ambientTemperature*temperatureMultiplier))) & 0xff);

			byte[] bytes = { 0x8, temp, setPoint, 0x0};
			controller_10_03.sendDataTelegram(bytes);
		}
	}
	
	private EEP07201011DataImpl getLastKnownData()
	{
		return (EEP07201011DataImpl) getDataHandler().getLastKnownData();
	}
	
	@Override
	public DatabaseManager.Record getDeviceRecord()
	{
		DatabaseManager.Record rec = this.getPreFilledRecordWithoutData();
		rec.setData(Integer.toString(controller_20_10.getControllerIndex()) + " " + Integer.toString(controller_20_11.getControllerIndex()) + " " +Integer.toString(controller_10_03.getControllerIndex()));
		rec.setLastKnownData(getDataHandler().getLastKnownData().getRecordAsJSON());
		return rec;
	}
	
	@Override
	public void sendPairingSignals(int signalID)
	{
		if(controller_20_10 != null && controller_20_11 != null && controller_10_03 != null)
		{
			controller_20_10.sendPairingSignal((byte) 0x20, (byte) 0x10, (byte) 0x00, (byte) 0x80);
			controller_20_11.sendPairingSignal((byte) 0x20, (byte) 0x11, (byte) 0x00, (byte) 0x80);
			controller_10_03.sendPairingSignal((byte) 0x10, (byte) 0x03, (byte) 0x00, (byte) 0x80);
		}
	}
	
	@Override
	public boolean isModeSupported(Mode mode) 
	{
		switch(mode)
		{
			case AUTO:
			case HEAT:
			case COOL:
			case FAN_ONLY:
			case DRY:
			case NO_ACTION:
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean isFanSpeedSupported(FanSpeed fanSpeed) 
	{
		switch(fanSpeed)
		{
			case SPEED_1:
			case SPEED_2:
			case SPEED_3:
			case NO_ACTION:
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean isVanePositionSupported(VanePosition vanePosition) 
	{
		switch(vanePosition)
		{
			case HORIZONTAL:
			case POSITION_2:
			case POSITION_3:
			case POSITION_4:
			case VERTICAL:
			case VERTICAL_SWING:
			case NO_ACTION:
				return true;
			default:
				return false;
		}
	}

	@Override
	public boolean isRoomOccupancySupported(RoomOccupancy fanSpeed) 
	{
		return true;
	}
	
	@Override
	protected void terminate() 
	{
		// Release channel should be done here, not in deviceManager.
	}
}
