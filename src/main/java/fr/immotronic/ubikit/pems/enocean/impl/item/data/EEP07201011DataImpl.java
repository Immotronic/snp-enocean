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

package fr.immotronic.ubikit.pems.enocean.impl.item.data;

import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem.JSONValueField;

import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data;
import fr.immotronic.ubikit.pems.enocean.impl.LC;

public final class EEP07201011DataImpl implements EEP07201011Data 
{
	// EEP_07_20_10
	private final Mode mode;
	private final VanePosition vanePosition;
	private final FanSpeed fanSpeed;
	private final RoomOccupancy roomOccupancy;
	private final OnOffStatus onOffStatus;
	
	// EEP_07_20_11
	private final int errorCode;
	private final Disablement windowContactDisablement;
	private final Disablement keyCardDisablement;
	private final Disablement externalDisablement;
	private final Disablement remoteControllerDisablement;
	private final WindowsStatus windowContact;
	private final AlarmState alarmState;
	
	private final Date date;
	
	public static EEP07201011DataImpl constructDataFromRecord(JSONObject lastKnownData)
	{
		if (lastKnownData == null)
			return new EEP07201011DataImpl(Mode.NO_ACTION, VanePosition.NO_ACTION, FanSpeed.NO_ACTION, RoomOccupancy.OCCUPIED, OnOffStatus.ON, 0, Disablement.NOT_DISABLED, Disablement.NOT_DISABLED, Disablement.NOT_DISABLED, Disablement.NOT_DISABLED, WindowsStatus.WINDOWS_CLOSED, AlarmState.OK, null);
		
		try
		{
			Mode mode = Mode.valueOf(lastKnownData.getString("mode"));
			VanePosition vanePosition = VanePosition.valueOf(lastKnownData.getString("vanePosition"));
			FanSpeed fanSpeed =  FanSpeed.valueOf(lastKnownData.getString("fanSpeed"));
			RoomOccupancy roomOccupancy = RoomOccupancy.valueOf(lastKnownData.getString("roomOccupancy"));
			OnOffStatus onOffStatus = OnOffStatus.valueOf(lastKnownData.getString("onOffStatus"));
			
			int errorCode = lastKnownData.getInt("errorCode");
			Disablement windowContactDisablement = Disablement.valueOf(lastKnownData.getString("windowContactD"));
			Disablement keyCardDisablement = Disablement.valueOf(lastKnownData.getString("keyCardD"));
			Disablement externalDisablement = Disablement.valueOf(lastKnownData.getString("externalD"));
			Disablement remoteControllerDisablement = Disablement.valueOf(lastKnownData.getString("remoteControllerD"));
			WindowsStatus windowContact = WindowsStatus.valueOf(lastKnownData.getString("windowContact"));
			AlarmState alarmState = AlarmState.valueOf(lastKnownData.getString("alarmState"));
			
			Date date = new Date(lastKnownData.getLong("date"));
			
			return new EEP07201011DataImpl(mode, vanePosition, fanSpeed, roomOccupancy, onOffStatus, errorCode, windowContactDisablement, keyCardDisablement, externalDisablement, remoteControllerDisablement, windowContact, alarmState, date);
		}
		catch (JSONException e)
		{
			Logger.error(LC.gi(), null, "constructDataFromRecord(): An exception while building a sensorData from a JSONObject SHOULD never happen. Check the code !", e);
			return new EEP07201011DataImpl(Mode.NO_ACTION, VanePosition.NO_ACTION, FanSpeed.NO_ACTION, RoomOccupancy.OCCUPIED, OnOffStatus.ON, 0, Disablement.NOT_DISABLED, Disablement.NOT_DISABLED, Disablement.NOT_DISABLED, Disablement.NOT_DISABLED, WindowsStatus.WINDOWS_CLOSED, AlarmState.OK, null);
		}
	}
	
	public EEP07201011DataImpl(Mode mode, VanePosition vanePosition, FanSpeed fanSpeed, RoomOccupancy roomOccupancy, OnOffStatus onOffStatus, int errorCode, Disablement windowContactDisablement, Disablement keyCardDisablement, Disablement externalDisablement, Disablement remoteControllerDisablement, WindowsStatus windowContact, AlarmState alarmState, Date date)
	{
		// EEP_07_20_10
		this.mode = mode;
		this.vanePosition = vanePosition;
		this.fanSpeed = fanSpeed;
		this.roomOccupancy = roomOccupancy;
		this.onOffStatus = onOffStatus;
		
		// EEP_07_20_11
		this.errorCode = errorCode;
		this.windowContactDisablement = windowContactDisablement;
		this.keyCardDisablement = keyCardDisablement;
		this.externalDisablement = externalDisablement;
		this.remoteControllerDisablement = remoteControllerDisablement;
		this.windowContact = windowContact;
		this.alarmState = alarmState;

		this.date = date;
	}

	@Override
	public Mode getMode() 
	{
		return mode;		
	}
	
	@Override
	public VanePosition getVanePosition()
	{
		return vanePosition;		
	}
	
	@Override
	public FanSpeed getFanSpeed()
	{
		return fanSpeed;
	}
	
	@Override
	public RoomOccupancy getRoomOccupancy()
	{
		return roomOccupancy;
	}
	
	@Override
	public OnOffStatus getOnOffStatus()
	{
		return onOffStatus;
	}
	
	@Override
	public int getErrorCode() 
	{
		return errorCode;		
	}
	
	@Override
	public Disablement getWindowContactDisablement() 
	{
		return windowContactDisablement;		
	}
	
	@Override
	public Disablement getKeyCardDisablement() 
	{
		return keyCardDisablement;		
	}
	
	@Override
	public Disablement getExternalDisablement() 
	{
		return externalDisablement;		
	}
	
	@Override
	public Disablement getRemoteControllerDisablement() 
	{
		return remoteControllerDisablement;		
	}
	
	@Override
	public WindowsStatus getWindowContact() 
	{
		return windowContact;		
	}
	
	@Override
	public AlarmState getAlarmState() 
	{
		return alarmState;
	}
	
	@Override
	public Date getDate() 
	{
		return date;
	}
	
	@Override
	public JSONObject toJSON()
	{
		JSONObject res = new JSONObject();
		JSONObject o = null;
		
		try 
		{
			if(date == null) 
			{
				res.put("noDataAvailable", true);
				return res;
			}
			
			long timestamp = date.getTime();
		
			if(mode != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), mode.name());
				o.put(JSONValueField.uiValue.name(), mode.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("mode", o);
			}
			
			if(vanePosition != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), vanePosition.name());
				o.put(JSONValueField.uiValue.name(), vanePosition.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("vanePosition", o);
			}
			
			if(fanSpeed != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), fanSpeed.name());
				o.put(JSONValueField.uiValue.name(), fanSpeed.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("fanSpeed", o);
			}
			
			if(roomOccupancy != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), roomOccupancy.name());
				o.put(JSONValueField.uiValue.name(), roomOccupancy.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("roomOccupancy", o);
			}
			
			if(onOffStatus != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), onOffStatus.name());
				o.put(JSONValueField.uiValue.name(), onOffStatus.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("onOffStatus", o);
			}
			
			if(windowContactDisablement != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), windowContactDisablement.name());
				o.put(JSONValueField.uiValue.name(), windowContactDisablement.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("windowContactDisablement", o);
			}
			
			if(keyCardDisablement != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), keyCardDisablement.name());
				o.put(JSONValueField.uiValue.name(), keyCardDisablement.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("keyCardDisablement", o);
			}
			
			if(externalDisablement != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), externalDisablement.name());
				o.put(JSONValueField.uiValue.name(), externalDisablement.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("externalDisablement", o);
			}
			
			if(remoteControllerDisablement != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), remoteControllerDisablement.name());
				o.put(JSONValueField.uiValue.name(), remoteControllerDisablement.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("remoteControllerDisablement", o);
			}
			
			if(windowContact != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), windowContact.name());
				o.put(JSONValueField.uiValue.name(), windowContact.name());
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("windowContact", o);
			}
			
			if(alarmState != null)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), errorCode);
				o.put(JSONValueField.uiValue.name(), errorCode);
				o.put(JSONValueField.timestamp.name(), timestamp);
				res.put("errorCode", o);
			}
		}
		catch (JSONException e) {
			Logger.error(LC.gi(), this, "toJSON(): An exception while building a JSON view of a EnoceanData SHOULD never happen. Check the code !", e);
			return null;
		}
		
		return res;
	}
	
	@Override
	public JSONObject getRecordAsJSON() 
	{
		if (date == null)
			return null;
		
		try
		{
			JSONObject o = new JSONObject();
			o.put("mode", mode.name());
			o.put("vanePosition", vanePosition.name());
			o.put("fanSpeed", fanSpeed.name());
			o.put("onOffStatus", onOffStatus.name());
			o.put("roomOccupancy", roomOccupancy.name());
			o.put("errorCode", errorCode);
			o.put("windowContactD", windowContactDisablement.name());
			o.put("keyCardD", keyCardDisablement.name());
			o.put("externalD", externalDisablement.name());
			o.put("remoteControllerD", remoteControllerDisablement.name());
			o.put("windowContact", windowContact.name());
			o.put("alarmState", alarmState.name());
			o.put("date", date.getTime());
			return o;
		}
		catch (JSONException e)
		{
			Logger.error(LC.gi(), this, "getRecordAsJSON(): An exception while building a JSON record of a EnoceanData SHOULD never happen. Check the code !", e);
			return null;
		}
	}
	
}
