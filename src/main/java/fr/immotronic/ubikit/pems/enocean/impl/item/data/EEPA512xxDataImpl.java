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

import java.text.DecimalFormat;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem.JSONValueField;

import fr.immotronic.ubikit.pems.enocean.data.EEPA51200Data;
import fr.immotronic.ubikit.pems.enocean.data.EEPA512xxData;
import fr.immotronic.ubikit.pems.enocean.impl.LC;

public class EEPA512xxDataImpl implements EEPA51200Data, EEPA512xxData
{
	public enum ValueType
	{
		CUMULATIVE,
		CURRENT
	}
	
	private final static class Channel
	{
		private float cumulativeValue = Integer.MIN_VALUE;
		private Date cumulativeValueDate = null;
		private float currentValue = Integer.MIN_VALUE;
		private Date currentValueDate = null;
		
		public Channel(float cumulativeValue, Date cumulativeValueDate, float currentValue, Date currentValueDate)
		{
			this.cumulativeValue = cumulativeValue;
			this.cumulativeValueDate = cumulativeValueDate;
			this.currentValue = currentValue;
			this.currentValueDate = currentValueDate;
		}
		
		public Channel(JSONObject channelJSON)
		{
			try
			{
				cumulativeValue = (float) channelJSON.getDouble("cumulValue");
				currentValue = (float) channelJSON.getDouble("currentValue");			
				long date = channelJSON.optLong("cumulValueDate");
				cumulativeValueDate = (date == 0) ? null : new Date(date);	
				date = channelJSON.optLong("currentValueDate");
				currentValueDate = (date == 0) ? null : new Date(date);
			}
			catch (JSONException e)
			{
				Logger.error(LC.gi(), this, "Channel(): A JSONException while instanciating Channel SHOULD never happen. Check the code !", e);
				cumulativeValue = Integer.MIN_VALUE;  cumulativeValueDate = null; currentValue = Integer.MIN_VALUE; currentValueDate = null;
			}
		}
		
		protected float getCumulativeValue() {
			return cumulativeValue;
		}
		
		protected float getCurrentValue() {
			return currentValue;
		}
		
		protected Date getCumulativeValueDate() {
			return cumulativeValueDate;
		}
		
		protected Date getCurrentValueDate() {
			return currentValueDate;
		}
		
		protected void setCumulativeValue(float cumulativeValue, Date cumulativeValueDate) {
			this.cumulativeValue = cumulativeValue;
			this.cumulativeValueDate = cumulativeValueDate;
		}
		
		protected void setCurrentValue(float currentValue, Date currentValueDate) {
			this.currentValue = currentValue;
			this.currentValueDate = currentValueDate;
		}
		
		protected JSONObject toJSON()
		{
			try
			{
				JSONObject o = new JSONObject();
				o.put("cumulValue", cumulativeValue);
				o.put("currentValue", currentValue);
				if (cumulativeValueDate != null) o.put("cumulValueDate", cumulativeValueDate.getTime());
				if (currentValueDate != null) o.put("currentValueDate", currentValueDate.getTime());
				return o;
			}
			catch (JSONException e)
			{
				Logger.error(LC.gi(), this, "toJSON(): An exception while building this JSON object SHOULD never happen. Check the code !", e);
				return null;
			}
		}
		
	}
	
	private final static DecimalFormat df = new DecimalFormat("########.#");
	
	private final int EEPType;
	private final Channel channels[];
	private final int tariffInfo; // Has only a meaning for EEPType != 00
	private Date date;   
	
	public static EEPA512xxDataImpl constructDataFromRecord(int EEPType, JSONObject lastKnownData)
	{
		if (lastKnownData == null)
			return new EEPA512xxDataImpl(EEPType, null, null);
		
		Channel[] lastChannels;
		switch (EEPType)
		{
			case 0x00: 
				lastChannels = new Channel[15];
				break;
			default:
				lastChannels = new Channel[1];
				break;
		}
		
		try
		{
			for(int i = 0; i < lastChannels.length; i++)
			{
				JSONObject o = lastKnownData.optJSONObject("channel"+i);
				if (o == null)
					lastChannels[i] = null;
				else
					lastChannels[i] = new Channel(o);
			}
				
			Date date = new Date(lastKnownData.getLong("date"));
			
			return new EEPA512xxDataImpl(EEPType, lastChannels, date);
		}
		catch (JSONException e)
		{
			Logger.error(LC.gi(), null, "constructDataFromRecord(): An exception while building a sensorData from a JSONObject SHOULD never happen. Check the code !", e);
			return new EEPA512xxDataImpl(EEPType, null, null);
		}
	}
	
	public EEPA512xxDataImpl(int EEPType, Channel channels[], Date date)
	{
		switch (EEPType)
		{
			case 0x00: 
				this.channels = new Channel[15];
				for (int i = 0; i < 15; i++) {
					if (channels == null || channels[i] == null)
						this.channels[i] = null;
					else
						this.channels[i] = new Channel(channels[i].toJSON());
				}
				break;
			default:
				this.channels = new Channel[1];
				if (channels == null)
					this.channels[0] = new Channel(Integer.MIN_VALUE, null, Integer.MIN_VALUE, null);
				else
					this.channels[0] = new Channel(channels[0].toJSON());
				break;
		}
		
		
		this.EEPType = EEPType;
		this.tariffInfo = 0;
		this.date = date;
	}
	
	/* FOR EEPType != 0x00 */
	public EEPA512xxDataImpl(int EEPType, float value, ValueType dataType, int tariffInfo, Channel previousChannel, Date date)
	{
		channels = new Channel[1];
		if (dataType == ValueType.CUMULATIVE)
			channels[0] = new Channel(value, date, previousChannel.getCurrentValue(), previousChannel.getCurrentValueDate());
		else if (dataType == ValueType.CURRENT)
			channels[0] = new Channel(previousChannel.getCumulativeValue(), previousChannel.getCumulativeValueDate(), value, date);
		
		this.EEPType = EEPType;
		this.date = date;
		this.tariffInfo = tariffInfo;
	}
	
	public Channel getFirstChannel()
	{
		return channels[0];
	}
	
	public void setCounterValue(int channel, ValueType dataType, float value, Date date)
	{
		if (channel < 0 || channel > 15)
			return;
		
		if (channels[channel] == null)
			channels[channel] = new Channel(Integer.MIN_VALUE, null, Integer.MIN_VALUE, null);
		
		if (dataType == ValueType.CUMULATIVE)
			channels[channel].setCumulativeValue(value, date);
		else if (dataType == ValueType.CURRENT)
			channels[channel].setCurrentValue(value, date);
		
		this.date = date;
	}
	
	@Override
	public float getCumulativeValue()
	{
		return channels[0].getCumulativeValue();
	}
	
	@Override
	public float getInstantValue()
	{
		return channels[0].getCurrentValue();
	}
	
	@Override
	public int getTariffInfo()
	{  
		return tariffInfo;
	}
	
	@Override
	public MeterReadingType getMeterReadingType()
	{
		switch(EEPType)
		{
			case 0x1 : return MeterReadingType.Electricity;
			case 0x2 : return MeterReadingType.Gas;
			case 0x3 : return MeterReadingType.Water;
		}
		return null;
	}
	
	@Override
	public float getCumulativeValue(int channel)
	{
		if (channel < 0 || channel > 15 || channels[channel] == null)
			return Integer.MIN_VALUE;
		
		return channels[channel].getCumulativeValue();
	}
	
	@Override
	public float getCurrentValue(int channel)
	{
		if (channel < 0 || channel > 15 || channels[channel] == null)
			return Integer.MIN_VALUE;
		
		return channels[channel].getCurrentValue();
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
			
			for(int i = 0; i < channels.length; i++) 
			{				
				if (channels[i] != null)
				{
					if (channels[i].getCumulativeValue() != Integer.MIN_VALUE)
					{
						o = new JSONObject();
						o.put(JSONValueField.value.name(), channels[i].getCumulativeValue());
						o.put(JSONValueField.uiValue.name(), df.format(channels[i].getCumulativeValue()));
						o.put(JSONValueField.unit.name(), getUnitFromEEPType(ValueType.CUMULATIVE));
						o.put(JSONValueField.timestamp.name(), channels[i].getCumulativeValueDate().getTime());
						
						if(EEPType == 0)
							res.put("cumulativeValue_channel"+i, o);
						else
							res.put("cumulativeValue", o); 
					}
					
					if (channels[i].getCurrentValue() != Integer.MIN_VALUE)
					{
						o = new JSONObject();
						o.put(JSONValueField.value.name(), channels[i].getCurrentValue());
						o.put(JSONValueField.uiValue.name(), df.format(channels[i].getCurrentValue()));
						o.put(JSONValueField.unit.name(), getUnitFromEEPType(ValueType.CURRENT));
						o.put(JSONValueField.timestamp.name(), channels[i].getCurrentValueDate().getTime());
						
						if(EEPType == 0)
							res.put("currentValue_channel"+i, o);
						else
							res.put("currentValue", o); 
					}
				}
			}
			
			if (EEPType != 0 && tariffInfo != 0)
			{
				o = new JSONObject();
				o.put(JSONValueField.value.name(), tariffInfo);
				o.put(JSONValueField.uiValue.name(), tariffInfo);
				o.put(JSONValueField.timestamp.name(), date.getTime());
				res.put("tariff", o);
			}
		}
		catch (JSONException e) {
			Logger.error(LC.gi(), this, "toJSON(): An exception while building a JSON view of a EnoceanData SHOULD never happen. Check the code !", e);
			return null;
		}
		
		return res;
	}
	
	private String getUnitFromEEPType(ValueType type)
	{
		switch (EEPType)
		{
			case 0: 
				if (type == ValueType.CUMULATIVE)
					return "";
				else
					return " /s";
			case 1:
				if (type == ValueType.CUMULATIVE)
					return " kWh";
				else
					return " W";
			case 2:
			case 3:
				if (type == ValueType.CUMULATIVE)
					return " m<sup>3</sup>";
				else
					return " l/s";
		}
		return "";
	}
	
	@Override
	public Date getDate() 
	{
		return date;
	}

	@Override
	public JSONObject getRecordAsJSON() 
	{
		if(date == null) 
			return null;
		
		try
		{
			JSONObject o = new JSONObject();
			for(int i = 0; i < channels.length; i++) 
			{
				if(channels[i] != null)
				{
					o.put("channel"+i, channels[i].toJSON());
				}
			}
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
