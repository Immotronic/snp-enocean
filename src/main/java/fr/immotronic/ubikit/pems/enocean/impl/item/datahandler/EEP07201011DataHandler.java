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

package fr.immotronic.ubikit.pems.enocean.impl.item.datahandler;

import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.event.AbstractEvent;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorData;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.AlarmState;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.Disablement;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.FanSpeed;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.Mode;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.OnOffStatus;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.RoomOccupancy;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.VanePosition;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.WindowsStatus;
import fr.immotronic.ubikit.pems.enocean.event.out.GenericHVACInterfaceErrorControlEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.GenericHVACInterfaceReportEvent;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanData;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.LC;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEP07201011DataImpl;

public final class EEP07201011DataHandler implements EnoceanDataHandler 
{
	private EventGate eventGate;
	private String itemUID;
	
	private int EEP072010transmitterUID = 0;
	private int EEP072011transmitterUID = 0;	
	
	private EEP07201011Data sensorData;
	
	public EEP07201011DataHandler(EventGate eventGate, String itemUID, JSONObject lastKnownData)
	{
		this.eventGate = eventGate;
		this.itemUID = itemUID;
		
		sensorData = EEP07201011DataImpl.constructDataFromRecord(lastKnownData);
	}
	
	public void setTransmitterUID(EnoceanEquipmentProfileV20 eep, int transmitterUID)
	{
		if (eep == EnoceanEquipmentProfileV20.EEP_07_20_10) {
			this.EEP072010transmitterUID = transmitterUID;
		}
		else if (eep == EnoceanEquipmentProfileV20.EEP_07_20_11) {
			this.EEP072011transmitterUID = transmitterUID;
		}
	}

	@Override
	public void processNewIncomingData(int transmitterID, EnoceanData data) 
	{	
		byte[] dataBytes = data.getBytes();
		AbstractEvent event = null;
		
		// Checking if telegram is a teach-in telegram. If so, ignore it.
		if((dataBytes[0] & 0x8) == 0x0) {
			Logger.debug(LC.gi(), this, "this is a teach-in telegram. It will be ignored.");
			return;
		}
		
		// If the transmitter ID correspond to the EEP 07_20_10
		if (transmitterID == EEP072010transmitterUID)
		{
			// Reading Mode data.
			Mode mode = Mode.getValueOf((byte)(dataBytes[3] & 0xff));
			
			// Reading Vane position data.
			VanePosition vanePosition = VanePosition.getValueOf((byte)((dataBytes[2] & 0xf0) >> 4));
			
			// Reading Fan speed data.
			FanSpeed fanSpeed = FanSpeed.getValueOf((byte)(dataBytes[2] & 0x0f));
			
			// Reading Room occupancy data.
			RoomOccupancy roomOccupancy = RoomOccupancy.getValueOf((byte)((dataBytes[0] & 0x06) >> 1));
					
			// Reading OnOff status data.
			OnOffStatus onOffStatus = OnOffStatus.getValueOf((byte)(dataBytes[0] & 0x01));
			
			// Generate and send a Generic HVAC Interface New Data event.
			//if (sensorData.getMode() != mode || sensorData.getVanePosition() != vanePosition || sensorData.getFanSpeed() != fanSpeed || sensorData.getRoomOccupancy() != roomOccupancy || sensorData.getOnOffStatus() != onOffStatus)
			//{
				event = new GenericHVACInterfaceReportEvent(itemUID, mode, vanePosition, fanSpeed, roomOccupancy, onOffStatus, data.getDate());
			//}
			
			// Remembering received data
			sensorData = new EEP07201011DataImpl(mode, vanePosition, fanSpeed, roomOccupancy, onOffStatus, sensorData.getErrorCode(), sensorData.getWindowContactDisablement(), sensorData.getKeyCardDisablement(), sensorData.getExternalDisablement(), sensorData.getRemoteControllerDisablement(), sensorData.getWindowContact(), sensorData.getAlarmState(), data.getDate());
		}
		else if (transmitterID == EEP072011transmitterUID) // If the transmitter ID correspond to the EEP 07_20_11
		{	
			// Reading disablements
			Disablement isWindowContactDisable = ((dataBytes[1] & 0x4) != 0) ? Disablement.DISABLED : Disablement.NOT_DISABLED;
			Disablement isKeyCardDisable = ((dataBytes[1] & 0x2) != 0) ? Disablement.DISABLED : Disablement.NOT_DISABLED;
			Disablement hasExternalDisablement = ((dataBytes[1] & 0x1) != 0) ? Disablement.DISABLED : Disablement.NOT_DISABLED;
			
			// Reading status
			Disablement isRemoteControllerDisable = ((dataBytes[0] & 0x4) != 0) ? Disablement.DISABLED : Disablement.NOT_DISABLED;
			WindowsStatus isWindowClosed = ((dataBytes[0] & 0x2) != 0) ? WindowsStatus.WINDOWS_CLOSED : WindowsStatus.WINDOWS_OPENED;
			
			AlarmState alarmState = AlarmState.OK;
			int errorCode = 0;
			if((dataBytes[0] & 0x1) != 0)
			{
				errorCode = ((((dataBytes[3] & 0xff) << 8) + (dataBytes[2] & 0xff)) & 0xffff);
				Logger.debug(LC.gi(), this, "error in HVAC system: code = "+errorCode+" ("+dataBytes[3]+", "+dataBytes[2]+")");
				alarmState = AlarmState.ERROR;
			}
			
			// Generate and send a Generic HVAC Interface New Data event.
			//if (sensorData.getErrorCode() != errorCode || sensorData.getWindowContactDisablement() != isWindowContactDisable || sensorData.getKeyCardDisablement() != isKeyCardDisable || sensorData.getExternalDisablement() != hasExternalDisablement || sensorData.getRemoteControllerDisablement() != isRemoteControllerDisable || sensorData.getWindowContact() != isWindowClosed || sensorData.getAlarmState() != alarmState)
			//{
				event = new GenericHVACInterfaceErrorControlEvent(itemUID, errorCode, isWindowContactDisable, isKeyCardDisable, hasExternalDisablement, isRemoteControllerDisable, isWindowClosed, alarmState);
			//}
			
			// Remembering received data
			sensorData = new EEP07201011DataImpl(sensorData.getMode(), sensorData.getVanePosition(), sensorData.getFanSpeed(), sensorData.getRoomOccupancy(), sensorData.getOnOffStatus(), errorCode, isWindowContactDisable, isKeyCardDisable, hasExternalDisablement, isRemoteControllerDisable, isWindowClosed, alarmState, data.getDate());
		}
		else 
		{
			// Return, the transmitter ID is not known.
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "processNewIncomingData() : transmitterID "+transmitterID+"is not known");
			}
			return;
		}		
		
		// And then, send all generated events. Sending MUST be done after remembering received data, otherwise if an event listener performs
		// a getLastKnownData() on event reception, it might obtain non up-to-date data.
		if (event != null) {
			eventGate.postEvent(event);
		}
	}
	
	@Override
	public EnoceanSensorData getLastKnownData()
	{
		return sensorData;
	}
	
	public void setData(EEP07201011Data data)
	{
		sensorData = data;
	}
}
