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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem.JSONValueField;

import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData;
import fr.immotronic.ubikit.pems.enocean.impl.LC;

public class EEPD201xxDataImpl implements EEPD201xxData
{	
	private final int EEPType;
	
	private final SwitchState[] switchStates; // For Switches
	private final int dimmerValue; // For Dimmers
	private final int value;
	private final MeasurementUnit unit;
	private final Date[] switchDates;
	private final Date valueDate;
	private final Mode mode;
	
	public static EEPD201xxDataImpl constructDataFromRecord(int EEPType, JSONObject lastKnownData)
	{
		if (lastKnownData == null) {
			return new EEPD201xxDataImpl(EEPType, null, -1, null, Integer.MIN_VALUE, MeasurementUnit.UNKNOWN, null, null);
		}		
		
		try
		{
			SwitchState[] switchStates = new SwitchState[2];
			if(lastKnownData.has("switchState")) {
				switchStates[0] = SwitchState.valueOf(lastKnownData.getString("switchState"));
				switchStates[1] = SwitchState.UNKNOWN;
			}
			else 
			{
				switchStates[0] = SwitchState.valueOf(lastKnownData.getString("switchState_0"));
				switchStates[1] = SwitchState.valueOf(lastKnownData.getString("switchState_1"));
			}
			
			int dimmerValue = lastKnownData.getInt("dimmerValue");
			int value = lastKnownData.getInt("value");
			MeasurementUnit unit = MeasurementUnit.valueOf(lastKnownData.getString("unit"));
			
			Date[] switchStateDates = new Date[2];
			if(lastKnownData.has("switchDate")) {
				long switchDate = lastKnownData.optLong("switchDate");
				switchStateDates[0] = (switchDate == 0) ? null : new Date(switchDate);
				switchStateDates[1] = null;
			}
			else
			{
				long switchDate = lastKnownData.optLong("switchDate_0");
				switchStateDates[0] = (switchDate == 0) ? null : new Date(switchDate);
				
				switchDate = lastKnownData.optLong("switchDate_1");
				switchStateDates[1] = (switchDate == 0) ? null : new Date(switchDate);
			}
			
			long valueDate = lastKnownData.optLong("valueDate");
			
			Mode mode = null;
			if(lastKnownData.has("mode")) {
				mode = Mode.valueOf(lastKnownData.getString("mode"));
			}
			
			
			return new EEPD201xxDataImpl(EEPType, switchStates, dimmerValue, switchStateDates, value, unit, (valueDate == 0) ? null : new Date(valueDate), mode);
		}
		catch (JSONException e)
		{
			Logger.error(LC.gi(), null, "constructDataFromRecord(): An exception while building a sensorData from a JSONObject SHOULD never happen. Check the code !", e);
			return new EEPD201xxDataImpl(EEPType, null, -1, null, Integer.MIN_VALUE, MeasurementUnit.UNKNOWN, null, null);
		}
	}
	
	public EEPD201xxDataImpl(int EEPType, Mode mode)
	{
		this(EEPType, null, -1, null, Integer.MIN_VALUE, MeasurementUnit.UNKNOWN, null, mode);
	}
	
	public EEPD201xxDataImpl(int EEPType, SwitchState[] switchStates, Date[] switchStateDates, int value, MeasurementUnit unit, Date valueDate, Mode mode)
	{
		this(EEPType, switchStates, Integer.MIN_VALUE, switchStateDates, value, unit, valueDate, mode);
	}
	
	public EEPD201xxDataImpl(int EEPType, int dimmerValue, Date[] switchStateDates, int value, MeasurementUnit unit, Date valueDate)
	{
		this(EEPType, null, dimmerValue, switchStateDates, value, unit, valueDate, Mode.NOT_APPLICABLE);
	}
	
	private EEPD201xxDataImpl(int EEPType, SwitchState[] switchStates, int dimmerValue, Date[] switchStateDates, int value, MeasurementUnit unit, Date valueDate, Mode mode)
	{
		this.EEPType = EEPType;
		
		this.dimmerValue = dimmerValue;
		
		if(switchStates == null) 
		{
			SwitchState[] sstates = new SwitchState[2];
			sstates[0] = SwitchState.UNKNOWN;
			sstates[1] = SwitchState.UNKNOWN;
			this.switchStates = sstates;
		}
		else {
			this.switchStates = switchStates;
		}
		
		if(switchStateDates == null) 
		{
			Date[] sdates = new Date[2];
			sdates[0] = null;
			sdates[1] = null;
			this.switchDates = sdates;
		}
		else {
			this.switchDates = switchStateDates;
		}
		
		this.value = value;
		this.unit = unit;
		this.valueDate = valueDate;
		
		if(mode != null) {
			this.mode = mode;
		}
		else {
			if(EEPType == 0x11) {
				this.mode = Mode.RELAY;
			}
			else {
				this.mode = Mode.NOT_APPLICABLE;
			}
		}
	}
	
	@Override
	public SwitchState getSwitchState() 
	{
		return switchStates[0];
	}
	
	@Override
	public int getDimmerValue()
	{
		return dimmerValue;
	}
	
	@Override
	public int getNbOfChannel() 
	{
		return (EEPType == 0x10 || EEPType == 0x11)?2:1;
	}

	@Override
	public SwitchState getSwitchState(int channel) 
	{
		int nbOfChannels = (EEPType == 0x10 || EEPType == 0x11)?2:1;
		if(channel >= 0 || channel < nbOfChannels)
		{
			return switchStates[channel];
		}
		
		throw new IllegalArgumentException("Invalid channel argument. Its value must be >= 0 and < "+nbOfChannels);
	}
	
	@Override
	public SwitchState[] getSwitchStates() 
	{
		return switchStates;
	}

	@Override
	public MeasurementUnit getMeasurementUnit() 
	{
		return unit;
	}

	@Override
	public int getMeasurementValue()
	{
		return value;
	}

	@Override
	public Date getDate() 
	{
		if (switchDates[0] == null && switchDates[1] == null && valueDate == null)
			return null;
		
		ArrayList<Date> dates = new ArrayList<Date>();
		if(switchDates[0] != null) dates.add(switchDates[0]);
		if(switchDates[1] != null) dates.add(switchDates[1]);
		if(valueDate != null) dates.add(valueDate);
		
		Collections.sort(dates);
		
		return dates.get(dates.size()-1);
	}
	
	public Date[] getSwitchStateDates() 
	{
		return switchDates;
	}
	
	public Date getValueDate() 
	{
		return valueDate;
	}

	@Override
	public JSONObject toJSON()
	{
		JSONObject res = new JSONObject();
		JSONObject o = null;
		
		try 
		{
			if(switchDates[0] == null && switchDates[1] == null && valueDate == null) 
			{
				res.put("noDataAvailable", true);
				return res;
			}

			switch (EEPType)
			{
				case 0x0:
				case 0x6:
					o = new JSONObject();
					o.put(JSONValueField.value.name(), switchStates[0].name());
					o.put(JSONValueField.uiValue.name(), switchStates[0].name());
					o.put(JSONValueField.timestamp.name(), switchDates[0].getTime());
					res.put("switchState", o);
					break;
				case 0x2:
					o = new JSONObject();
					o.put(JSONValueField.value.name(), dimmerValue);
					o.put(JSONValueField.uiValue.name(), dimmerValue);
					o.put(JSONValueField.timestamp.name(), switchDates[0].getTime());
					o.put(JSONValueField.unit.name(), " %");
					res.put("dimmerValue", o);
					break;
				case 0x11:
					o = new JSONObject();
					o.put(JSONValueField.value.name(), switchStates[0].name());
					o.put(JSONValueField.uiValue.name(), switchStates[0].name());
					if(switchDates[0] != null) {
						o.put(JSONValueField.timestamp.name(), switchDates[0].getTime());
					}
					res.put("channel_1", o);
					
					o = new JSONObject();
					o.put(JSONValueField.value.name(), switchStates[1].name());
					o.put(JSONValueField.uiValue.name(), switchStates[1].name());
					if(switchDates[1] != null) {
						o.put(JSONValueField.timestamp.name(), switchDates[1].getTime());
					}
					res.put("channel_2", o);
					
					o = new JSONObject();
					o.put(JSONValueField.value.name(), mode.name());
					o.put(JSONValueField.uiValue.name(), mode.name());
					res.put("mode", o);
					break;
			}
			
			if(value != Float.MIN_VALUE && unit != MeasurementUnit.UNKNOWN)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), value);
				o.put(JSONValueField.uiValue.name(), value);
				o.put(JSONValueField.unit.name(), toText(unit));
				o.put(JSONValueField.timestamp.name(), valueDate.getTime());
				res.put("energyValue", o);
			}
		}
		catch (JSONException e) {
			Logger.error(LC.gi(), this, "toJSON(): An exception while building a JSON view of a EnoceanData SHOULD never happen. Check the code !", e);
			return null;
		}
		
		return res;
	}
	
	private String toText(MeasurementUnit unit)
	{
		switch (unit)
		{
			case KILOWATT_HOUR: return " kWh";
			case WATT_HOUR: return " Wh";
			case WATT_SECOND: return " Ws";
			default: return "";
		}
	}
	
	@Override
	public JSONObject getRecordAsJSON() 
	{
		if(switchDates[0] == null && switchDates[1] == null && valueDate == null)
			return null;
		
		try
		{
			JSONObject o = new JSONObject();
			o.put("switchState_0", switchStates[0].name());
			o.put("switchState_1", switchStates[1].name());
			o.put("dimmerValue", dimmerValue);
			o.put("value", value);
			o.put("unit", unit);
			if (switchDates[0] != null)
				o.put("switchDate_0", switchDates[0].getTime());
			if (switchDates[1] != null)
				o.put("switchDate_1", switchDates[1].getTime());
			if (valueDate != null)
				o.put("valueDate", valueDate.getTime());
			o.put("mode", mode.name());
			return o;
		}
		catch (JSONException e)
		{
			Logger.error(LC.gi(), this, "getRecordAsJSON(): An exception while building a JSON record of a EnoceanData SHOULD never happen. Check the code !", e);
			return null;
		}
	}
	
	@Override
	public Mode getMode()
	{
		return mode;
	}
}
