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
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem.Type;
import org.ubikit.event.EventGate;
import org.ubikit.pem.event.AddItemEvent;
import org.ubikit.pem.event.EnterPairingModeEvent;
import org.ubikit.pem.event.ExitPairingModeEvent;
import org.ubikit.pem.event.ItemAddingFailedEvent;
import org.ubikit.pem.event.NewItemEvent;
import org.ubikit.pem.event.NewItemEvent.CapabilitySelection;
import org.ubikit.pem.event.UnsupportedNewItemEvent;

//import fr.immotronic.license.LicenseManager;
import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData;

/*
 * 
 * TODO: Consider adding a SelectionErrorEvent (for SelectionEvent matching
 * no element in the toPair map, and SelectionApprovedEvent to give a positive
 * feedback to the SelectionEvent sender.
 * 
 *  TODO: Consider adding a PairingCancelledEvent management, when the user wish
 *  to dismissed an ongoing pairing.
*/

public final class PairingManagerImpl implements AddItemEvent.Listener, EnterPairingModeEvent.Listener, ExitPairingModeEvent.Listener
{
	private static final long CLEANING_THREAD_SLEEPING_PERIOD = 60; // 1 hour
	private static final long PAIRING_TIMEOUT = 600000; // 10 minutes
	
	private final class PairingEntry
	{
		private long arrivalTime;
		private int enoceanUID;
		private String itemUID;
		private EnoceanEquipmentProfileV20 eep;
		private int manufacturerUID;
		private JSONObject properties = null;
		
		PairingEntry(int enoceanUID, EnoceanEquipmentProfileV20 eep, int manufacturerUID)
		{
			arrivalTime = System.currentTimeMillis();
			this.enoceanUID = enoceanUID;
			this.itemUID = EnoceanDeviceImpl.makeEnoceanItemUID(enoceanUID);
			this.eep = eep;
			this.manufacturerUID = manufacturerUID;
		}
		
		public boolean isExpired(long currentTime)
		{
			return (arrivalTime + PAIRING_TIMEOUT) < currentTime;
		}
	}
	
	private final DeviceManager deviceManager;
	//private final LicenseManager licenseManager;
	private final EventGate eventGateToHigherAbstractionModelLevels;
	private final String pemUID; 
	private final Map<String, PairingEntry> toPair;
	private boolean pairingMode;
	
	private class CleaningThread implements Runnable
	{	
		public void run()
		{
			ArrayList<String> toBeRemoved = new ArrayList<String>();
			
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "cleaning task start...");
			}
			
			// Get current system time
			long referenceTime = System.currentTimeMillis();
			
			synchronized(toPair)
			{
				Iterator<PairingEntry> entries = toPair.values().iterator();
				
				// For each entry of arrival times
				while(entries.hasNext())
				{
					PairingEntry entry = entries.next();
					if(entry.isExpired(referenceTime)) // if the entry expired
					{
						// Mark it as "entry to remove"
						toBeRemoved.add(entry.itemUID);
						
						if(LC.debug) {
							Logger.debug(LC.gi(), this, "cleaning thread mark "+entry.enoceanUID+" item to be removed.");
						}
					}
				}
			}
			
			// Remove marked entry
			for(String key : toBeRemoved)
			{
				toPair.remove(key);
			}
			
			// Cleaning the toBeRemoved list. It will be ready for the next cleaning.
			toBeRemoved.clear();
			
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "cleaning task stopped.");
			}
		}
	}
	
	public PairingManagerImpl(DeviceManager deviceManager, /*LicenseManager licenseManager,*/ EventGate eventGateToHigherAbstractionModelLevels, ScheduledExecutorService executorService, String pemUID)
	{
		pairingMode = false;
		this.pemUID = pemUID;
		toPair = Collections.synchronizedMap(new HashMap<String, PairingEntry>());
		executorService.scheduleAtFixedRate(new CleaningThread(), CLEANING_THREAD_SLEEPING_PERIOD, CLEANING_THREAD_SLEEPING_PERIOD, TimeUnit.MINUTES);
		
		this.deviceManager = deviceManager;
		//this.licenseManager = licenseManager;
		this.eventGateToHigherAbstractionModelLevels = eventGateToHigherAbstractionModelLevels;
		eventGateToHigherAbstractionModelLevels.addListener(this);
	}
	
	public boolean getPairingMode()
	{
		return pairingMode;
	}
	
	/**
	 * 
	 * @param telegram
	 * @return false if the telegram was not a teach-in telegram and should be ignore. Return true otherwise.
	 */
	public boolean pairNewDevice(EnoceanTelegram telegram)
	{
		//
		// PRE-CONDITION: This method is called ONLY if the PEM pairing mode is ON.
		//
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "Try to pair a new device");
		}
		
		int uid = telegram.getTransmitterId(); // Get the device Enocean UID
		byte[] databytes = telegram.getData().getBytes();
		PairingEntry pairingEntry = null;
		
		// Determine the ORG parameter in the telegram, and instantiate the appropriate PairingEntry object 
		switch(telegram.getRorgID())
		{
			case RORG_RPS:	
				// Manufacturer could not be known.
				if((databytes[3] & 0xFF) >= 0xc0) {
					// The device is a Window Handle
					pairingEntry = new PairingEntry(uid, EnoceanEquipmentProfileV20.EEP_05_10_00, 0);
				}
				else {
					// An pairing entry with a generic EEP information is created. The final user will have to
					// indicate, via the HCI, the exact EEP to use for this sensor.
					pairingEntry = new PairingEntry(uid, EnoceanEquipmentProfileV20.EEP_05_xx_xx, 0);
					break;
				}
				break;
				
			case RORG_1BS:
				// For ORG == 1BS, only on profile match: EEP_06_00_01. Manufacturer could not be known.
				// The data byte 3, bit 3 indicate a teach-in telegram. Check it.
				if((databytes[3] & 0x8) == 0x0)
				{
					// This is a teach-in telegram
					pairingEntry = new PairingEntry(uid, EnoceanEquipmentProfileV20.EEP_06_00_01, 0);
					break;
				}
				
				// This is not a teach-in telegram, stop here.
				if(LC.debug) {
					Logger.debug(LC.gi(), this, "This was not a 1BS teach-in telegram.");
				}
				return false;
			
			case RORG_4BS:
				// For ORG == 4BS, profile could be determined using data bytes 2 & 3. Manufacturer ID is on data bytes 2 & 1
				// The data byte 0, bit 3 indicate a teach-in telegram. Check it.
				if((databytes[0] & 0x8) == 0x0)
				{
					// This is a teach-in telegram
					if((databytes[0] & 0x80) == 0x80) // If bit 7 is 0, function, type and manufacturer info are provided
					{
						int function = ((databytes[3] & 0xff) >> 2) & 0x3f;
						int type = ((databytes[3] & 0x3) << 5) + ((databytes[2] & 0xff) >> 3) & 0x1f;
						int manufacturerID = ((databytes[2] & 0x7) << 8) + (databytes[1] & 0xff);
						
						if(LC.debug) {
							Logger.debug(LC.gi(), this, "4BS teach-in telegram: function="+Integer.toHexString(function)
												+", type="+Integer.toHexString(type)
												+", manufacturer="+manufacturerID);
						}
						
						// Try to figure out witch is the matching EEP.
						EnoceanEquipmentProfileV20 eep = EnoceanEquipmentProfileV20.selectEEP(0x7, function, type);
						if(eep == null)
						{
							// No EEP match: an UnsupportedDeviceEvent message and processing stop here. 
							if(LC.debug) {
								Logger.debug(LC.gi(), this, "4BS teach-in telegram function & types information does not match a valid EEP: " + telegram);
							}
							UnsupportedNewItemEvent uni = new UnsupportedNewItemEvent(EnoceanDeviceImpl.makeEnoceanItemUID(telegram.getTransmitterId()), pemUID, Type.SENSOR, null);
							eventGateToHigherAbstractionModelLevels.postEvent(uni);
							pairingEntry = new PairingEntry(uid, eep, manufacturerID);
							toPair.put(pairingEntry.itemUID, pairingEntry);
							return false;
						}
						else
						{
							if(LC.debug) {
								Logger.debug(LC.gi(), this, "Matching EEP is "+eep.name());
							}
						}
						
						pairingEntry = new PairingEntry(uid, eep, manufacturerID);
						break;
					}
					else 
					{
						// Function and type cannot be determined. That's happen when sensor manufacturer does not conformed to Enocean Standard.
						if(LC.debug) {
							Logger.debug(LC.gi(), this, "4BS teach-in telegram does not contains function & types information: " + telegram);
						}
						
						// An pairing entry without EEP information is created. The final user will have to
						// indicate, via the HCI, the EEP to use for this sensor.
						pairingEntry = new PairingEntry(uid, EnoceanEquipmentProfileV20.NONE, 0);
						break;
					}
				}
				
				// This is not a teach-in telegram, stop here.
				if(LC.debug) {
					Logger.debug(LC.gi(), this, "This was not a 4BS teach-in telegram.");
				}
				return false;
			case RORG_UTE:
				// For ORG == UTE, data byte 0 provide RORG of EEP. The profile could be determined using data bytes 1 & 2. Manufacturer ID is on data bytes 3 & 4.
				// The data byte 6, bit 3..0 indicate a teach-in query telegram. Check it.
				if((databytes[6] & 0xf) == 0x0)
				{
					// Data byte 6, bit 5..4 indicates if it's a teach-in request or a teach-in deletion request. Check it.
					if ((databytes[6] & 0x10) == 0x0)
					{
						// Yes, it is a teach-in request.
						// Test if the devide need a teach-in response
						if ((databytes[6] & 0x50) == 0x0)
							Logger.warn(LC.gi(), this, "The device you're trying to pair expect a Teach-In-Response message but this is not yet implemented. The pairing could failed.");
						
						// NB : DB6 contains also info on Uni/Bidirectional information & info on the need of a response.
						// 		DB5 contains the number of channel supported by the device.
						
						int org = databytes[0] & 0xff;
						int function = databytes[1] & 0xff;
						int type = databytes[2] & 0xff;
						int manufacturerID = ((databytes[3] & 0x7) << 8) + (databytes[4] & 0xff);
						
						// Try to figure out witch is the matching EEP.
						EnoceanEquipmentProfileV20 eep = EnoceanEquipmentProfileV20.selectEEP(org, function, type);
						if(eep == null)
						{
							// No EEP match: an UnsupportedDeviceEvent message and processing stop here. 
							if(LC.debug) {
								Logger.debug(LC.gi(), this, "UTE teach-in telegram org & function & types information does not match a valid EEP: " + telegram);
							}
							UnsupportedNewItemEvent uni = new UnsupportedNewItemEvent(EnoceanDeviceImpl.makeEnoceanItemUID(telegram.getTransmitterId()), pemUID, Type.SENSOR, null);
							eventGateToHigherAbstractionModelLevels.postEvent(uni);
							pairingEntry = new PairingEntry(uid, eep, manufacturerID);
							toPair.put(pairingEntry.itemUID, pairingEntry);
							return false;
						}
						else
						{
							if(LC.debug) {
								Logger.debug(LC.gi(), this, "Matching EEP is "+eep.name());
							}
						}
						
						pairingEntry = new PairingEntry(uid, eep, manufacturerID);
						break;
					}
				}
				
				// This was not a correct teach-in telegram, stop here.
				if(LC.debug) {
					Logger.debug(LC.gi(), this, "This was not a UTE teach-in query telegram.");
				}
				return false;
			default:
				// This telegram is not yet supported: an UnsupportedDeviceEvent message and processing stop here.
				if(LC.debug) {
					Logger.debug(LC.gi(), this, "Unmanaged telegram type: " + telegram);
				}
				UnsupportedNewItemEvent uni = new UnsupportedNewItemEvent(EnoceanDeviceImpl.makeEnoceanItemUID(telegram.getTransmitterId()), pemUID, Type.SENSOR, null);
				eventGateToHigherAbstractionModelLevels.postEvent(uni);
				return true;
		}

		// The device can be associated to an existing one in some case (EEP 07-20-10 and EEP 07-20-11 for instance) so we check this.
		if(isDeviceExistWithAnotherUID(pairingEntry))
		{
			return true;
		}
		
		// Adding the pairing entry to the "toPair" queue
		toPair.put(pairingEntry.itemUID, pairingEntry);
		
		// and send the confirmation request message
		String[] capabilities = null;
		CapabilitySelection cs = null;
		//if((pairingEntry.eep.getFunction() == 0 && pairingEntry.eep != EnoceanEquipmentProfileV20.EEP_06_00_01) || (pairingEntry.eep.getFunction() == 0x20)) {
		//	cs = CapabilitySelection.SINGLE;
			switch(pairingEntry.eep)
			{
				case EEP_05_xx_xx:
					capabilities = new String[3];
					capabilities[0] = EnoceanEquipmentProfileV20.EEP_05_02_01.name();
					capabilities[1] = EnoceanEquipmentProfileV20.EEP_05_04_01.name();
					capabilities[2] = EnoceanEquipmentProfileV20.ELTAKO_FRW_WS_SMOKE_ALARM.name();
					cs = CapabilitySelection.SINGLE;
					break;
				case EEP_07_20_10:
				case EEP_07_20_11:
					capabilities = new String[1];
					capabilities[0] = SensorActuatorProfile.INTESIS_BOX_DK_RC_ENO_1i1iC_DEVICE.name();
					cs = CapabilitySelection.SINGLE;
					break;
				case EEP_D2_01_11:
					capabilities = new String[2];
					capabilities[0] = SensorActuatorProfile.EEP_D2_01_11.name()+"?mode="+EEPD201xxData.Mode.RELAY;
					capabilities[1] = SensorActuatorProfile.EEP_D2_01_11.name()+"?mode="+EEPD201xxData.Mode.MOTOR;
					cs = CapabilitySelection.SINGLE;
					break;
				case NONE:
					capabilities = new String[3];
					capabilities[0] = EnoceanEquipmentProfileV20.EEP_07_06_01.name();
					capabilities[1] = EnoceanEquipmentProfileV20.EEP_07_10_03.name();
					capabilities[2] = EnoceanEquipmentProfileV20.EEP_A5_12_00.name();
					cs = CapabilitySelection.SINGLE;
					break;
				default:
					//capabilities = new String[0];
					capabilities = new String[1];
					capabilities[0] = pairingEntry.eep.name();
					cs = CapabilitySelection.NO;
					break;
			}
		//}
		
		// Defining type (SENSOR or SENSOR_AND_ACTUATOR)
		Type type = getSensorTypeFromEEP(pairingEntry.eep);

		NewItemEvent ni = new NewItemEvent(EnoceanDeviceImpl.makeEnoceanItemUID(uid), pemUID, type, capabilities, cs);
		eventGateToHigherAbstractionModelLevels.postEvent(ni);
		
		return true;
	}
	
	private Type getSensorTypeFromEEP(EnoceanEquipmentProfileV20 eep)
	{
		Type type = Type.SENSOR;
		
		switch(eep) {
			case EEP_07_20_11:
			case EEP_07_20_10:
			case EEP_D2_01_00:
			case EEP_D2_01_02:
			case EEP_D2_01_06:
			case EEP_D2_01_11:
				type = Type.SENSOR_AND_ACTUATOR;
				break;
			default:
				break;
		}
		
		return type;
	}

	private boolean isDeviceExistWithAnotherUID(PairingEntry pairingEntry)
	{
		try
		{
			if(	pairingEntry.eep == EnoceanEquipmentProfileV20.EEP_07_20_10 || 
				pairingEntry.eep == EnoceanEquipmentProfileV20.EEP_07_20_11) 
			{
				PairingEntry existingEntry = getPairingEntryWithSameBaseUID(pairingEntry.enoceanUID);
				
				if (existingEntry != null)
				{
					// INVARIANT HERE : existingEntry.properties could not be null : at least, a first property has been created
					// when the first EEP of this device was received.
					existingEntry.properties.put(pairingEntry.eep.name(), Integer.toHexString(pairingEntry.enoceanUID));
					return true;
				}
				else 
				{
					JSONObject properties = pairingEntry.properties;
					if(properties == null)
					{
						properties = new JSONObject();
						pairingEntry.properties = properties;
					}
					
					properties.put(pairingEntry.eep.name(), Integer.toHexString(pairingEntry.enoceanUID));
				}
			}
		} 
		catch (JSONException e) {
			Logger.error(LC.gi(), this, "isDeviceExistWithAnotherUID() : Trouble while building JSON Object. SHOULD NEVER HAPPEN. Check code.", e);
			return false;
		}
		catch (Exception e) {
			Logger.error(LC.gi(), this, "isDeviceExistWithAnotherUID() : SHOULD NEVER HAPPEN. Check code.", e);
			return false;
		}
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "isDeviceExistWithAnotherUID() will return false. Entry was "+pairingEntry.enoceanUID+"/"+pairingEntry.eep);
		}
		return false;
	}
	
	private PairingEntry getPairingEntryWithSameBaseUID(int enoceanUID)
	{
		int baseUID = enoceanUID & 0x80;
		for(PairingEntry pe : toPair.values())
		{
			if((pe.enoceanUID & 0x80) == baseUID) {
				return pe;
			}
		}
		
		return null;
	}

	@Override
	public void onEvent(AddItemEvent event) 
	{
		PairingEntry pe = toPair.remove(event.getSourceItemUID());
		
		if(pe != null)
		{
			String capability = null;
			String params = null;
			String[] capabilities = event.getCapabilities();
			if(capabilities == null || capabilities.length == 0) {
				capability = pe.eep.name();
			}
			else {
				capability = capabilities[0];
				int paramStringIndex = capability.indexOf('?');
				if(paramStringIndex > 0) {
					params = capability.substring(paramStringIndex+1);
					capability = capability.substring(0, paramStringIndex);
				}
			}
			
			if(pe.properties == null) 
			{
				pe.properties = event.getUserProperties();
			}
			else
			{
				Iterator<?> itr = event.getUserProperties().keys();
				while(itr.hasNext())
				{
					String key = (String) itr.next();
					try 
					{
						pe.properties.put(key, event.getUserProperties().get(key));
					} 
					catch (JSONException e) 
					{
						Logger.error(LC.gi(), this, "isDeviceExistWithAnotherUID() : Trouble while building JSON Object. SHOULD NEVER HAPPEN. Check code.", e);
					}
				}
			}
			
			// TODO: séparer la partie "paramètre" de la partie EEP de la capability, et la transmettre à createNewDevice. 
			
			try 
			{
				EnoceanEquipmentProfileV20 eep = EnoceanEquipmentProfileV20.valueOf(capability);
				if (getSensorTypeFromEEP(eep) != Type.SENSOR_AND_ACTUATOR)
					deviceManager.createNewDevice(pe.enoceanUID, eep, pe.manufacturerUID, pe.properties, new DeviceParameters(params));
				
				Logger.debug(LC.gi(), this, "Confirmation for "+event.getSourceItemUID()+" as sensor with profile "+eep);
			}
			catch(IllegalArgumentException e) {}
				
			try 
			{
				SensorActuatorProfile sap = SensorActuatorProfile.valueOf(capability);
				deviceManager.createNewDevice(pe.enoceanUID, sap, pe.manufacturerUID, pe.properties, new DeviceParameters(params));
				Logger.debug(LC.gi(), this, "Confirmation for "+event.getSourceItemUID()+" as sensor/actuator with profile "+sap);
			}
			catch(IllegalArgumentException e) {}
			return;
		}
		
		ItemAddingFailedEvent iaf = new ItemAddingFailedEvent(event.getSourceItemUID(), "UNKNOWN_ITEM", 1);
		eventGateToHigherAbstractionModelLevels.postEvent(iaf);
	}

	@Override
	public void onEvent(ExitPairingModeEvent event) 
	{
		pairingMode = false;
	}

	@Override
	public void onEvent(EnterPairingModeEvent event) 
	{
		//licenseManager.checkLicenceFile();
		pairingMode = true;
	}
}
