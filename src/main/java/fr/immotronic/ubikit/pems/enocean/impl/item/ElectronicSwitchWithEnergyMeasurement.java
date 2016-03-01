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

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.ControllerProfile;
import fr.immotronic.ubikit.pems.enocean.EnoceanControllerDevice;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.impl.DatabaseManager;
import fr.immotronic.ubikit.pems.enocean.impl.DatabaseManager.Record;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEPD201xxDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.DeviceManager;
import fr.immotronic.ubikit.pems.enocean.impl.DeviceParameters;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanSensorAndActuatorDeviceImpl;
import fr.immotronic.ubikit.pems.enocean.impl.LC;
import fr.immotronic.ubikit.pems.enocean.impl.NoControllerChannelLeftException;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData.Mode;
import fr.immotronic.ubikit.pems.enocean.event.in.MoveDownBlindOrShutterEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.MoveUpBlindOrShutterEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.StopBlindOrShutterEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.TurnOffActuatorEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.TurnOnActuatorEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.ActuatorUpdateEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.SetOutputLevelEvent;

public class ElectronicSwitchWithEnergyMeasurement extends EnoceanSensorAndActuatorDeviceImpl 
	implements 	ActuatorUpdateEvent.Listener, 
				TurnOffActuatorEvent.Listener, 
				TurnOnActuatorEvent.Listener, 
				SetOutputLevelEvent.Listener,
				MoveUpBlindOrShutterEvent.Listener, 
				MoveDownBlindOrShutterEvent.Listener, 
				StopBlindOrShutterEvent.Listener
{
	private final VariableLengthDataController controller;
	private final SensorActuatorProfile profile;
	
	public ElectronicSwitchWithEnergyMeasurement(EventGate eventGate, DatabaseManager.Record record, SensorActuatorProfile profile, DeviceManager deviceManager)
	{
		super(record.getUID(), record.getEnoceanUID(), profile, record.getManufacturerUID(), deviceManager, record.getConfigurationAsJSON());
		
		setPropertiesFromJSONObject(record.getPropertiesAsJSON());
		addCapability(profile.name());
		this.profile = profile;
		
		// Create Data handler
		EEPD201xxDataHandler dh = null;
		switch(profile)
		{
			case EEP_D2_01_00: dh = new EEPD201xxDataHandler(eventGate, getUID(), 0, record.getLastKnownData()); break;
			case EEP_D2_01_02: dh = new EEPD201xxDataHandler(eventGate, getUID(), 2, record.getLastKnownData()); break;
			case EEP_D2_01_06: dh = new EEPD201xxDataHandler(eventGate, getUID(), 6, record.getLastKnownData()); break;
			case EEP_D2_01_11: dh = new EEPD201xxDataHandler(eventGate, getUID(), 0x11, record.getLastKnownData()); break;
		}
		setDataHandler(dh);
		
		// Create controllers
		int controllerUID = Integer.parseInt(record.getData());
		controller = (VariableLengthDataController) deviceManager.getController(controllerUID);
		controller.markAsUsed();
	}
	
	public ElectronicSwitchWithEnergyMeasurement(EventGate eventGate, int enoceanUID, int manufacturerID, SensorActuatorProfile profile, DeviceManager deviceManager, JSONObject userProperties, DeviceParameters params) throws NoControllerChannelLeftException 
	{
		super(enoceanUID, profile, manufacturerID, deviceManager, params.toJSON());
		
		setPropertiesFromJSONObject(userProperties);
		addCapability(profile.name());
		this.profile = profile;
		
		// Create Data handler
		EEPD201xxDataHandler dh = null;
		Mode mode = null;
		switch(profile)
		{
			case EEP_D2_01_00: dh = new EEPD201xxDataHandler(eventGate, getUID(), 0, Mode.NOT_APPLICABLE); break;
			case EEP_D2_01_02: dh = new EEPD201xxDataHandler(eventGate, getUID(), 2, Mode.NOT_APPLICABLE); break;
			case EEP_D2_01_06: dh = new EEPD201xxDataHandler(eventGate, getUID(), 6, Mode.NOT_APPLICABLE); break;
			case EEP_D2_01_11:
				try
				{
					mode = Mode.valueOf(params.get("mode"));
				}
				catch(Exception e) {
					throw new IllegalArgumentException("the expected DeviceParameter 'mode' was not found in "+params.toString());
				}
				
				dh = new EEPD201xxDataHandler(eventGate, getUID(), 0x11, mode); 
				break;
		}
		setDataHandler(dh);
		
		// Create controllers
		controller = (VariableLengthDataController) deviceManager.getController(ControllerProfile.VARIABLE_LENGTH_DATA_CONTROLLER, 1);
		if(controller == null)
		{
			throw new NoControllerChannelLeftException();
		}
		controller.markAsUsed();
	}

	@Override
	public EnoceanControllerDevice[] releaseController()
	{
		if(controller != null)
		{
			VariableLengthDataController[] controllers = new VariableLengthDataController[1];
			
			controller.releaseChannel(1);
			controllers[0] = controller;
			return controllers;
		}
		
		return null;
	}
	
	@Override
	public void sendPairingSignals(int nbOfSignals)
	{
		// No pairing to do, sending telegram with destination address.
		return; 
	}
	
	public void switchOff(int channel)
	{
		if(channel == -1) {
			channel = 0;
		}
		
		EEPD201xxData data = ((EEPD201xxData) ((EEPD201xxDataHandler) this.getDataHandler()).getLastKnownData());
		if(data.getMode() != Mode.MOTOR)
		{
			byte[] dataBytes = new byte[] { 0x01, (byte)(channel & 0x1f), 0x00};
			controller.sendDataTelegram(dataBytes, getEnoceanUID());
		}
	}
	
	public void switchOn(int channel)
	{
		if(channel == -1) {
			channel = 0;
		}
		
		EEPD201xxData data = ((EEPD201xxData) ((EEPD201xxDataHandler) this.getDataHandler()).getLastKnownData());
		if(data.getMode() != Mode.MOTOR)
		{
			byte[] dataBytes = null;
			switch (profile)
			{
				case EEP_D2_01_00:
				case EEP_D2_01_06:
				case EEP_D2_01_11:
					dataBytes = new byte[] { 0x01, (byte)(channel & 0x1f), 0x01};
					controller.sendDataTelegram(dataBytes, getEnoceanUID());
					break;
				case EEP_D2_01_02:
					switchToValue(100);
					break;
			}
		}
	}
	
	public void moveUp()
	{
		EEPD201xxData data = ((EEPD201xxData) ((EEPD201xxDataHandler) this.getDataHandler()).getLastKnownData());
		if(data.getMode() == Mode.MOTOR)
		{
			byte[] dataBytes = new byte[] { 0x01, 0x01, 0x00};
			controller.sendDataTelegram(dataBytes, getEnoceanUID());
			dataBytes = new byte[] { 0x01, 0x00, 0x01};
			controller.sendDataTelegram(dataBytes, getEnoceanUID());
		}
	}
	
	public void stop()
	{
		EEPD201xxData data = ((EEPD201xxData) ((EEPD201xxDataHandler) this.getDataHandler()).getLastKnownData());
		if(data.getMode() == Mode.MOTOR)
		{
			byte[] dataBytes = new byte[] { 0x01, 0x01, 0x00};
			controller.sendDataTelegram(dataBytes, getEnoceanUID());
			dataBytes = new byte[] { 0x01, 0x00, 0x00};
			controller.sendDataTelegram(dataBytes, getEnoceanUID());
		}
	}
	
	public void moveDown()
	{
		EEPD201xxData data = ((EEPD201xxData) ((EEPD201xxDataHandler) this.getDataHandler()).getLastKnownData());
		if(data.getMode() == Mode.MOTOR)
		{
			byte[] dataBytes = new byte[] { 0x01, 0x00, 0x00};
			controller.sendDataTelegram(dataBytes, getEnoceanUID());
			dataBytes = new byte[] { 0x01, 0x01, 0x01};
			controller.sendDataTelegram(dataBytes, getEnoceanUID());
		}
	}
	
	public void switchToValue(int value)
	{
		if (value < 0 || value > 0x64 || profile != SensorActuatorProfile.EEP_D2_01_02)
			return;
		
		byte[] dataBytes = null;
		dataBytes = new byte[] { 0x01, 0x00, (byte) (value & 0xff) };
		controller.sendDataTelegram(dataBytes, getEnoceanUID());
		dataBytes = new byte[] { 0x01, (byte) 0x80, (byte) (value & 0xff) };
		controller.sendDataTelegram(dataBytes, getEnoceanUID());
	}
	
	public void sendStatusQuery()
	{
		byte[] dataBytes = new byte[] { 0x03, 0x00};
		controller.sendDataTelegram(dataBytes, getEnoceanUID());
	}
	
	public void sendMeasurementQuery()
	{
		byte[] dataBytes = new byte[] { 0x06, 0x1f};
		controller.sendDataTelegram(dataBytes, getEnoceanUID());
	}
	
	@Override
	public void onEvent(TurnOnActuatorEvent event)
	{
		if(event.getSourceItemUID().equals(getUID()) && controller != null)
		{
			switchOn(event.getChannel());
		}		
	}

	@Override
	public void onEvent(TurnOffActuatorEvent event)
	{
		if(event.getSourceItemUID().equals(getUID()) && controller != null)
		{
			switchOff(event.getChannel());
		}
	}
	
	@Override
	public void onEvent(SetOutputLevelEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()) && controller != null)
		{
			switchToValue(event.getOutputLevel());
		}
	}
	
	@Override
	public void onEvent(MoveUpBlindOrShutterEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()) && controller != null)
		{
			moveUp();
		}
	}
	
	@Override
	public void onEvent(StopBlindOrShutterEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()) && controller != null)
		{
			stop();
		}
	}

	@Override
	public void onEvent(MoveDownBlindOrShutterEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()) && controller != null)
		{
			moveDown();
		}
	}

	@Override
	public void onEvent(ActuatorUpdateEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()) && controller != null)
		{
			sendMeasurementQuery();
			sendStatusQuery();
		}
	}

	@Override
	public Record getDeviceRecord()
	{
		DatabaseManager.Record rec = this.getPreFilledRecordWithoutData();
		rec.setData(Integer.toString(controller.getControllerIndex()));
		rec.setLastKnownData(getDataHandler().getLastKnownData().getRecordAsJSON());
		rec.setConfiguration(getConfigurationAsJSON());
		return rec;
	}
	
	public JSONObject getDimmerValueAsJSON()
	{
		JSONObject o = new JSONObject();
		
		if (profile == SensorActuatorProfile.EEP_D2_01_02)
		{
			try {
				EEPD201xxData data = (EEPD201xxData) getDataHandler().getLastKnownData();
				o.put("dimmerValue", data.getDimmerValue() == Integer.MIN_VALUE ? 50 : data.getDimmerValue());
			} 
			catch (JSONException e) {
				Logger.error(LC.gi(), this, "While building JSON value object.", e);
				return null;
			}
		}
		
		return o;
	}

	@Override
	protected void terminate()
	{
		// Release channel should be done here, not in deviceManager.
	}

}
