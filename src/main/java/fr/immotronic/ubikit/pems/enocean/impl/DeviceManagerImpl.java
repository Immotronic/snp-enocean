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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONObject;
import org.ubikit.AbstractPhysicalEnvironmentModelEvent;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.PhysicalEnvironmentItem.Property;
import org.ubikit.event.EventGate;
import org.ubikit.event.EventListener;
import org.ubikit.pem.event.ItemDroppedEvent;
import org.ubikit.pem.event.ItemAddedEvent;
import org.ubikit.pem.event.ItemAddingFailedEvent;
import org.ubikit.pem.event.ItemPropertiesUpdatedEvent;

//import fr.immotronic.license.LicenseManager;
import fr.immotronic.ubikit.pems.enocean.ActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.ControllerProfile;
import fr.immotronic.ubikit.pems.enocean.EnoceanActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanControllerDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorAndActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorDevice;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.impl.item.BlindAndShutterMotorDevice;
import fr.immotronic.ubikit.pems.enocean.impl.item.ElectronicSwitchWithEnergyMeasurement;
import fr.immotronic.ubikit.pems.enocean.impl.item.EnoceanStandardSensorDevice;
import fr.immotronic.ubikit.pems.enocean.impl.item.FourByteSensorController;
import fr.immotronic.ubikit.pems.enocean.impl.item.FourChannelPTMController;
import fr.immotronic.ubikit.pems.enocean.impl.item.IntesisBoxDKRCENO1i1iCDevice;
import fr.immotronic.ubikit.pems.enocean.impl.item.OnOffDevice;
import fr.immotronic.ubikit.pems.enocean.impl.item.RHController;
import fr.immotronic.ubikit.pems.enocean.impl.item.RHDevice;
import fr.immotronic.ubikit.pems.enocean.impl.item.VariableLengthDataController;

public final class DeviceManagerImpl implements DeviceManager 
{	
	private final Map<Integer, EnoceanDevice> devices;	// List of managed devices (sensors and actuators)
	private final Map<Integer, Integer> additionalUIDs;	// Some complex device could be represented by several UID. This map stores association between those additional UIDs and their device. 
	private final EnoceanControllerDevice[] actuatorControllers;
	private final EnoceanSerialAdapter enoceanSerialAdapter;

	private final DatabaseManager databaseManager;
	private final PemLauncher pem;
	private final EventGate higherAbstractionLevelEventGate;
	
	//private final LicenseManager licenseManager;
	
	private int actuatorCount;
	
	public DeviceManagerImpl(PemLauncher pem, DatabaseManager databaseManager, EnoceanSerialAdapter enoceanSerialAdapter/*, LicenseManager licenseManager*/)
	{
		this.databaseManager = databaseManager;
		this.pem = pem;
		higherAbstractionLevelEventGate = pem.getHigherAbstractionLevelEventGate();
		this.enoceanSerialAdapter = enoceanSerialAdapter;
		devices = Collections.synchronizedMap(new HashMap<Integer, EnoceanDevice>()); 
		additionalUIDs = Collections.synchronizedMap(new HashMap<Integer, Integer>()); 
		actuatorControllers = new EnoceanControllerDevice[0x80]; // 0x80 match the maximum number of IDs for a TCM120/320/300 transceiver
		actuatorCount = 0;
		//this.licenseManager = licenseManager;
		
		loadKnownDevicesFromDatabase(databaseManager.getAllItems());
	}
	
	
	/*===================================================================================*
	 * Controller Management methods         /    PUBLIC                                 *
	 *===================================================================================*/
	
	@Override
	public EnoceanControllerDevice getController(int controllerUID)
	{
		return actuatorControllers[controllerUID];
	}
	
	@Override
	public EnoceanControllerDevice getController(ControllerProfile profile, int nbOfRequiredChannel)
	{
		// First, search for a existing controller with sufficient free channels left.
		for(int k = 0; k < getMaxNumberOfActuatorController(); k++)
		{
			EnoceanControllerDevice controller = getController(k);
			if(controller != null && controller.getControllerProfile() == profile && controller.checkChannelAvailability(nbOfRequiredChannel))
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), this,	"getController(): Using an existing controller: "+controller.getUID());
				}
				
				return controller;
			}
		}
				
		// At this point, no controller with sufficient free channels left was found.
		// A new one is created and hosted in the first available slot.
		if(LC.debug) {
			Logger.debug(LC.gi(), this,	"getController(): A new controller is needed...");
		}
		
		for(int k = 0; k < getMaxNumberOfActuatorController(); k++)
		{
			EnoceanControllerDevice controller = getController(k);
			if(controller == null)
			{
				switch(profile)
				{
					case FOUR_CHANNEL_PTM_CONTROLLER: {
						FourChannelPTMController c = new FourChannelPTMController(k, enoceanSerialAdapter, DeviceManagerImpl.this);
						actuatorControllers[k] = c;
						databaseManager.updateRecord(c.getDeviceRecord());
						return c; 
					}
						
					case RH_CONTROLLER: {
						RHController c = new RHController(k, enoceanSerialAdapter, DeviceManagerImpl.this);
						actuatorControllers[k] = c;
						databaseManager.updateRecord(c.getDeviceRecord());
						return c;
					}
					
					case FOUR_BYTE_SENSOR_CONTROLLER: {
						FourByteSensorController c = new FourByteSensorController(k, enoceanSerialAdapter, DeviceManagerImpl.this);
						actuatorControllers[k] = c;
						databaseManager.updateRecord(c.getDeviceRecord());
						return c;
					}
					
					case VARIABLE_LENGTH_DATA_CONTROLLER: {
						VariableLengthDataController c = new VariableLengthDataController(k, enoceanSerialAdapter, DeviceManagerImpl.this);
						actuatorControllers[k] = c;
						databaseManager.updateRecord(c.getDeviceRecord());
						return c;
					}						
						
					default:
						Logger.error(LC.gi(), this, "getController(): Unsupported controller profile: "+profile.name());
						return null;
				}
			}
		}
				
		// At this point, no controller with sufficient free channels left was found
		// and there is no room left to create a new controller.
		Logger.error(LC.gi(), this, "No room left for an new actuator controller.");
		return null;
	}
	
	@Override
	public void setController(ControllerProfile profile, String controllerUID, int enoceanUID)
	{
		int controllerIndex = EnoceanControllerDeviceImpl.getControllerIndexFromEnoceanUID(enoceanUID);
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "Setting a Controller: "+profile.name()+" controllerUID="+controllerUID+", enoceanUID="+enoceanUID);
		}
		
		if(actuatorControllers[controllerIndex] != null)
		{
			Logger.warn(LC.gi(), this, "Try to set a controller at a non empty place. Should never happen in theory. Check the setController() caller.");
		}
		
		switch(profile)
		{
			case FOUR_CHANNEL_PTM_CONTROLLER: {
				FourChannelPTMController c = new FourChannelPTMController(controllerUID, enoceanUID, enoceanSerialAdapter, this);
				actuatorControllers[controllerIndex] = c;
				break;
			}
			
			case RH_CONTROLLER: {
				RHController c = new RHController(controllerUID, enoceanUID, enoceanSerialAdapter, this);
				actuatorControllers[controllerIndex] = c;
				break;
			}
			
			case FOUR_BYTE_SENSOR_CONTROLLER: {
				FourByteSensorController c = new FourByteSensorController(controllerUID, enoceanUID, enoceanSerialAdapter, this);
				actuatorControllers[controllerIndex] = c;
				break;
			}
			
			case VARIABLE_LENGTH_DATA_CONTROLLER: {
				VariableLengthDataController c = new VariableLengthDataController(controllerUID, enoceanUID, enoceanSerialAdapter, this);
				actuatorControllers[controllerIndex] = c;
				break;
			}
			
			default:
				Logger.error(LC.gi(), this, "setController(): Unsupported controller profile: "+profile.name());
				break;
		}
	}
	
	
	
	
	/*===================================================================================*
	 * Actuator count  & UID Management methods         /    PRIVATE                     *
	 *===================================================================================*/
	
	private void setActuatorCount(String actuatorUID)
	{
		int index = Integer.parseInt(actuatorUID.substring(7), 16);
		if(actuatorCount < index) {
			actuatorCount = index;
		}
	}
	
	private String createActuatorUID()
	{
		actuatorCount++;
		return "ENOACTx"+Integer.toHexString(actuatorCount);
	}
	
	private int getMaxNumberOfActuatorController()
	{
		return actuatorControllers.length;
	}
	
	
	
	
	/*===================================================================================*
	 * Actuator & Sensor Management methods         /    PRIVATE                         *
	 *===================================================================================*/
	
	@Override
	public void removeDevice(String UID)
	{
		Logger.info(LC.gi(), this, "removeDevice("+UID+")");
		// Get the device object
		EnoceanDevice device = (EnoceanDevice) pem.getItem(UID);
		
		if(device != null)
		{
			// Removing it according to its class. Controllers are not directly removed from here,
			// but are removed through actuators remove methods.
			boolean removed = false;
			
			if(device instanceof EnoceanSensorDevice) {
				removeDevice((EnoceanSensorDevice) device);
				removed = true;
			}
			else if(device instanceof EnoceanActuatorDevice) {
				removeDevice((EnoceanActuatorDevice) device);
				removed = true;
			}
			else if(device instanceof EnoceanSensorAndActuatorDevice) {
				removeDevice((EnoceanSensorAndActuatorDevice) device);
				removed = true;
			}
			
			if(removed) {
				Logger.info(LC.gi(), this, "removeDevice("+UID+"): posting ItemDroppedEvent.");
				higherAbstractionLevelEventGate.postEvent(new ItemDroppedEvent(device.getUID(), pem.getUID()));
			}
		}
	}
	
	private void removeDevice(EnoceanSensorDevice device)
	{
		// Remove the device from the device map
		devices.remove(device.getEnoceanUID());
		
		// Remove the device entry from the database
		databaseManager.removeRecord(device.getUID());
	}
	
	private void removeDevice(EnoceanActuatorDevice device)
	{
		EnoceanControllerDevice controller = device.releaseController();
		
		if(controller.isUnused()) {
			// If the controller is now unused, remove it
			
			// Remove the controller from the controller array
			actuatorControllers[controller.getControllerIndex()] = null;
			
			// Remove the device entry from the database
			databaseManager.removeRecord(controller.getUID());
		}
		
		devices.remove(device.getEnoceanUID());
		
		// Remove the actuator as a listener for TurnOn/TurnOff event messages
		pem.getHigherAbstractionLevelEventGate().removeListener((EventListener) device);
		
		// Remove the device entry from the database
		databaseManager.removeRecord(device.getUID());
	}
	
	private void removeDevice(EnoceanSensorAndActuatorDevice device)
	{
		// Remove additional enocean UID associated to the device to remove, if needed.
		Iterator<Integer> itr = additionalUIDs.keySet().iterator();
		while(itr.hasNext())
		{
			Integer d = itr.next();
			if(additionalUIDs.get(d) == device.getEnoceanUID()) {
				itr.remove();
			}
		}
		
		EnoceanControllerDevice[] controllers = device.releaseController();
		
		for(EnoceanControllerDevice controller : controllers)
		{
			if(controller.isUnused()) {
				// If the controller is now unused, remove it
				
				// Remove the controller from the controller array
				actuatorControllers[controller.getControllerIndex()] = null;
				
				// Remove the device entry from the database
				databaseManager.removeRecord(controller.getUID());
			}
		}
		
		devices.remove(device.getEnoceanUID());
		
		// Remove the actuator as a listener for TurnOn/TurnOff event messages
		pem.getHigherAbstractionLevelEventGate().removeListener((EventListener) device);
		
		// Remove the device entry from the database
		databaseManager.removeRecord(device.getUID());
	}

	@Override
	public EnoceanDevice getDevice(String UID) 
	{
		return (EnoceanDevice) pem.getItem(UID);
	}

	@Override
	public EnoceanSensorDevice getSensorDevice(int enoceanUID) 
	{
		try 
		{
			EnoceanSensorDevice device = (EnoceanSensorDevice) devices.get(Integer.valueOf(enoceanUID));
			return device;
		}
		catch(ClassCastException e) 
		{
			return null;
		}
	}
	
	@Override
	public EnoceanSensorAndActuatorDevice getSensorActuatorDevice(int enoceanUID)
	{
		try 
		{
			EnoceanSensorAndActuatorDevice device = (EnoceanSensorAndActuatorDevice) devices.get(Integer.valueOf(enoceanUID));
			// Have we found a device with this enoceanUID ?
			if(device == null)
			{
				// No, we do not.
				// Is the this enoceanUID be an additional one ?
				Integer primaryEnoceanUID = additionalUIDs.get(Integer.valueOf(enoceanUID));
				if(primaryEnoceanUID == null)
				{
					// No, enoceanUID is not an additional id of an existing device.
					return null;
				}
				else
				{
					device =  (EnoceanSensorAndActuatorDevice) devices.get(primaryEnoceanUID);
				}
			}
			
			return device;
		}
		catch(ClassCastException e) {}
		
		return null;
	}
	
	@Override
	public EnoceanDevice createNewDevice(int enoceanUID, EnoceanEquipmentProfileV20 eep, int manufacturerUID, JSONObject userProperties, DeviceParameters params) 
	{
		// Here, we know that the device will be a standard Enocean sensor device.
		EnoceanSensorDevice device = new EnoceanStandardSensorDevice(higherAbstractionLevelEventGate, enoceanUID, eep, this);
		
		// Is the maximum number of allowed devices is reached ?
		if(devices.size() < /*licenseManager.getMaximalNumberOfItemsAllowed()*/10000)
		{
			// No, the maximum number of allowed devices is not yet reached.
			device.setPropertiesFromJSONObject(userProperties);
			
			if(device.getDataHandler() != null) {
				// Adding it to the Enocean device list, and update the database (meaning of the boolean parameter)
				addDevice(device, true);
				ItemAddedEvent ia = new ItemAddedEvent(device.getUID(), pem.getUID(), device.getType(), device.getPropertiesAsJSONObject(), device.getCapabilities(), device.getConfigurationAsJSON());
				higherAbstractionLevelEventGate.postEvent(ia);
				return device;
			}
			
			ItemAddingFailedEvent iaf = new ItemAddingFailedEvent(device.getUID(), "NO_DATA_HANDLER", 2);
			higherAbstractionLevelEventGate.postEvent(iaf);
			
			return null;
		}
		
		ItemAddingFailedEvent iaf = new ItemAddingFailedEvent(device.getUID(), "MAXIMUM_NUMBER_OF_DEVICES_REACHED", 2);
		higherAbstractionLevelEventGate.postEvent(iaf);
				
		return null;
	}
	
	@Override
	public EnoceanDevice createNewDevice(int enoceanUID, SensorActuatorProfile sap, int manufacturerUID, JSONObject userProperties, DeviceParameters params) 
	{
		EnoceanSensorAndActuatorDevice device = null;
		
		// Is the maximum number of allowed devices is reached ?
		if(devices.size() < /*licenseManager.getMaximalNumberOfItemsAllowed()*/10000)
		{
			try
			{
				switch(sap)
				{
					case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
						device = new IntesisBoxDKRCENO1i1iCDevice(higherAbstractionLevelEventGate, enoceanUID, this, userProperties);
						higherAbstractionLevelEventGate.addListener((IntesisBoxDKRCENO1i1iCDevice) device);
						break;
					case EEP_D2_01_00:
					case EEP_D2_01_02:
					case EEP_D2_01_06:
					case EEP_D2_01_11:
						device = new ElectronicSwitchWithEnergyMeasurement(higherAbstractionLevelEventGate, enoceanUID, manufacturerUID, sap, this, userProperties, params);
						higherAbstractionLevelEventGate.addListener((ElectronicSwitchWithEnergyMeasurement) device);
						break;
				}
				
				addDevice(device, true);
				ItemAddedEvent ia = new ItemAddedEvent(device.getUID(), pem.getUID(), device.getType(), device.getPropertiesAsJSONObject(), device.getCapabilities(), device.getConfigurationAsJSON());
				higherAbstractionLevelEventGate.postEvent(ia);

				return device;
			}
			catch(NoControllerChannelLeftException e) 
			{
				ItemAddingFailedEvent iaf = new ItemAddingFailedEvent(sap.name(), "NO_OUTPUT_CHANNEL_LEFT", 1);
				higherAbstractionLevelEventGate.postEvent(iaf);
			}
			
			ItemAddingFailedEvent iaf = new ItemAddingFailedEvent(sap.name(), "UNSUPPORTED_DEVICE_PROFILE", 2);
			higherAbstractionLevelEventGate.postEvent(iaf);
			
			return null;
		}
		
		ItemAddingFailedEvent iaf = new ItemAddingFailedEvent(sap.name(), "MAXIMUM_NUMBER_OF_DEVICES_REACHED", 2);
		higherAbstractionLevelEventGate.postEvent(iaf);
				
		return null;
	}
	
	@Override
	public AbstractPhysicalEnvironmentModelEvent createNewActuator(String actuatorProfile, String customName, String location)
	{
		assert actuatorProfile != null : "actuatorProfile MUST NOT be null";
		assert customName != null : "customName MUST NOT be null";
		
		// Is the maximum number of allowed devices is reached ?
		if(devices.size() < /*licenseManager.getMaximalNumberOfItemsAllowed()*/10000)
		{
			ActuatorProfile profile = ActuatorProfile.valueOf(actuatorProfile);
			if(profile != null)
			{
				EnoceanActuatorDevice actuator = null;
				try
				{
					switch(profile)
					{
						case ONOFF_DEVICE:
							OnOffDevice onoffDevice = new OnOffDevice(createActuatorUID(), this);
							higherAbstractionLevelEventGate.addListener(onoffDevice);
							actuator = onoffDevice;
							break;
							
						case BLIND_AND_SHUTTER_MOTOR_DEVICE:
							BlindAndShutterMotorDevice blindAndShutterMotorDevice = new BlindAndShutterMotorDevice(createActuatorUID(), this);
							higherAbstractionLevelEventGate.addListener(blindAndShutterMotorDevice);
							actuator = blindAndShutterMotorDevice;
							break;
						
						case RH_DEVICE:
							RHDevice rhDevice = new RHDevice(createActuatorUID(), this);
							higherAbstractionLevelEventGate.addListener(rhDevice);
							actuator = rhDevice;
							break;
					}
				}
				catch(NoControllerChannelLeftException e) 
				{
					ItemAddingFailedEvent iaf = new ItemAddingFailedEvent(customName, "NO_OUTPUT_CHANNEL_LEFT", 1);
					return iaf;
				}
							
				if(actuator != null)
				{
					// Setting the actuator properties with the given user defined custom name & location
					actuator.setPropertyValue(Property.CustomName.name(), customName);
					actuator.setPropertyValue(Property.Location.name(), location);
					
					// Adding it to the Enocean device to the device manager
					addDevice(actuator, true);
	
					// Returning the actuator UID
					ItemAddedEvent ia = new ItemAddedEvent(actuator.getUID(), pem.getUID(), actuator.getType(), actuator.getPropertiesAsJSONObject(), actuator.getCapabilities(), actuator.getConfigurationAsJSON());
					pem.getHigherAbstractionLevelEventGate().postEvent(ia);
					return ia;
				}
				else
				{
					Logger.debug(LC.gi(), this, "Actuator was not instanciated. Profile not supported.");
				}
			}
			
			ItemAddingFailedEvent iaf = new ItemAddingFailedEvent(customName, "UNKNOWN_OR_UNSUPPORTED_PROFILE", 2);
			return iaf;
		}
		
		ItemAddingFailedEvent iaf = new ItemAddingFailedEvent(customName, "MAXIMUM_NUMBER_OF_DEVICES_REACHED", 2);
		higherAbstractionLevelEventGate.postEvent(iaf);
				
		return null;
	}
	
	private void addDevice(EnoceanSensorDevice device, boolean storeInDatabase)
	{
		devices.put(device.getEnoceanUID(), device);
		pem.addItem((PhysicalEnvironmentItem) device);
		if(storeInDatabase) {
			databaseManager.updateRecord(device.getDeviceRecord());
		}
	}
	
	private void addDevice(EnoceanActuatorDevice device, boolean storeInDatabase)
	{
		devices.put(device.getEnoceanUID(), device);
		pem.addItem((PhysicalEnvironmentItem) device);
		
		if(storeInDatabase) {
			databaseManager.updateRecord(device.getDeviceRecord());
		}
	}
	
	private void addDevice(EnoceanSensorAndActuatorDevice device, boolean storeInDatabase)
	{
		devices.put(device.getEnoceanUID(), device);
		pem.addItem((PhysicalEnvironmentItem) device);
		
		if(storeInDatabase) {
			databaseManager.updateRecord(device.getDeviceRecord());
		}
	}
	
	@Override
	public void registerAdditionalEnoceanUID(int devicePrimaryEnoceanUID, int additionalEnoceanUID)
	{
		this.additionalUIDs.put(Integer.valueOf(additionalEnoceanUID), Integer.valueOf(devicePrimaryEnoceanUID));
	}
	
	@Override
	public boolean updateDeviceProperties(String UID, String[] propertiesName)
	{
		// Get the device object
		EnoceanDevice device = (EnoceanDevice) pem.getItem(UID);
		if(device != null)
		{
			databaseManager.updateRecord(device.getDeviceRecord());
			
			if (propertiesName != null) {
				higherAbstractionLevelEventGate.postEvent(new ItemPropertiesUpdatedEvent(UID, propertiesName));
			}
		}
		
		return false;
	}
	
	@Override
	public boolean updateDeviceLastKnownData(String UID)
	{
		// Get the device object
		EnoceanDevice device = (EnoceanDevice) pem.getItem(UID);
		if(device != null)
		{
			databaseManager.updateDeviceLastKnownData(device.getDeviceRecord());
		}
		
		return false;
	}
	
	
	
	private void loadKnownDevicesFromDatabase(ArrayList<DatabaseManager.Record> knownDevices)
	{
		int nbDevices = 0;
		int nbAllowedDevices = /*licenseManager.getMaximalNumberOfItemsAllowed()*/10000;
		// First, all controller and sensor items are processed.
		// Actuators will be process in a second loop, because they could have some link to
		// one or more controllers. Then Controllers MUST preexist in the item list.
		for(DatabaseManager.Record record : knownDevices)
		{
			if(record.isSensor() && !record.isActuator())
			{
				if(nbDevices < nbAllowedDevices)
				{
					EnoceanSensorDevice device = new EnoceanStandardSensorDevice(higherAbstractionLevelEventGate, record, this);
					
					if(device.getDataHandler() != null) {
						addDevice(device, false); // Add the device in the system
						nbDevices++;
					}
					else {
						// SHOULD NEVER HAPPEN. Check matching between references enumeration and the switch structure.
						if(LC.debug) {
							Logger.warn(LC.gi(), this, "loadKnownDevicesFromDatabase(): Database entry does not match a supported EEP. Weird.");
						}
					}
				}
			}
			else if(record.isController())
			{
				setController(record.getControllerProfile(), record.getUID(), record.getEnoceanUID());
			}
		}
		
		// Now, a second loop is done to add actuators.
		for(DatabaseManager.Record record : knownDevices)
		{
			if(record.isSensor() && record.isActuator())
			{
				if(nbDevices < nbAllowedDevices)
				{
					setActuatorCount(record.getUID());
					switch(record.getSensorActuatorProfile()) // According the actuator description, instantiate the appropriate object
					{
						case INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE:
							IntesisBoxDKRCENO1i1iCDevice ibcd = new IntesisBoxDKRCENO1i1iCDevice(higherAbstractionLevelEventGate, record, this);
							higherAbstractionLevelEventGate.addListener(ibcd);
							addDevice(ibcd, false);
							nbDevices++;
							break;
							
						case EEP_D2_01_00:
						case EEP_D2_01_02:
						case EEP_D2_01_06:
						case EEP_D2_01_11:
							ElectronicSwitchWithEnergyMeasurement eswem = new ElectronicSwitchWithEnergyMeasurement(higherAbstractionLevelEventGate, record, record.getSensorActuatorProfile(), this);
							higherAbstractionLevelEventGate.addListener(eswem);
							addDevice(eswem, false);
							nbDevices++;
							break;
					}
				}
			}
			else if(record.isActuator())
			{
				if(nbDevices < nbAllowedDevices)
				{
					setActuatorCount(record.getUID());
					switch(record.getActuatorProfile()) // According the actuator description, instantiate the appropriate object
					{
						case ONOFF_DEVICE:
							OnOffDevice ood = new OnOffDevice(record, this);
							higherAbstractionLevelEventGate.addListener(ood);
							addDevice(ood, false);
							nbDevices++;
							break;
							
						case BLIND_AND_SHUTTER_MOTOR_DEVICE:
							BlindAndShutterMotorDevice basmd = new BlindAndShutterMotorDevice(record, this);
							higherAbstractionLevelEventGate.addListener(basmd);
							addDevice(basmd, false);
							nbDevices++;
							break;
						
						case RH_DEVICE:
							RHDevice rd = new RHDevice(record, this);
							higherAbstractionLevelEventGate.addListener(rd);
							addDevice(rd, false);
							nbDevices++;
							break;
					}
				}
			}
		}
	}
	
}
