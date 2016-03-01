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

import fr.immotronic.ubikit.pems.enocean.data.EEP0503xxData;
import fr.immotronic.ubikit.pems.enocean.impl.LC;

public final class EEP0503xxDataImpl implements EEP0503xxData 
{
	public final static class SwitchEventImpl implements SwitchEvent
	{
		private final Date date;
		private final SwitchEventType eventType;
		
		public SwitchEventImpl(Date date, SwitchEventType eventType)
		{
			this.date = date;
			this.eventType = eventType;
		}
		
		public SwitchEventImpl(JSONObject switchEventJSON) throws JSONException 
		{
			eventType = SwitchEventType.valueOf(switchEventJSON.getString("eventType"));
			date = new Date(switchEventJSON.getLong("date"));
		}
		
		public Date getDate()
		{
			return date;
		}
		
		public SwitchEventType getSwitchEventType()
		{
			return eventType;
		}
		
		public JSONObject toJSON()
		{
			try
			{
				JSONObject o = new JSONObject();
				o.put("eventType", eventType.name());
				o.put("date", date.getTime());
				return o;
			}
			catch (JSONException e)
			{
				Logger.error(LC.gi(), this, "toJSON(): An exception while building this JSON object SHOULD never happen. Check the code !", e);
				return null;
			}
		}
	}
	
	//private static DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	
	private final SwitchEvent[] switchEvents;
	private final Date date;
	
	public static EEP0503xxDataImpl constructDataFromRecord(JSONObject lastKnownData)
	{
		if (lastKnownData == null)
			return new EEP0503xxDataImpl(null, null);
		
		try
		{
			SwitchEvent[] switchEvents = new SwitchEvent[4];
			for(int i = 0; i < switchEvents.length; i++) 
			{
				JSONObject o = lastKnownData.optJSONObject("switchEvent"+i);
				if (o == null)
					switchEvents[i] = null;
				else
					switchEvents[i] = new SwitchEventImpl(o);
			}
				
			Date date = new Date(lastKnownData.getLong("date"));
			
			return new EEP0503xxDataImpl(switchEvents, date);
		}
		catch (JSONException e)
		{
			Logger.error(LC.gi(), null, "constructDataFromRecord(): An exception while building a sensorData from a JSONObject SHOULD never happen. Check the code !", e);
			return new EEP0503xxDataImpl(null, null);
		}
	}
	
	public EEP0503xxDataImpl(SwitchEvent[] switches, Date date)
	{
		this.switchEvents = new SwitchEvent[4];
		
		if(switches != null && switches.length == 4) {
			this.switchEvents[0] = switches[0];
			this.switchEvents[1] = switches[1];
			this.switchEvents[2] = switches[2];
			this.switchEvents[3] = switches[3];
		}
		
		this.date = date;
	}
	
	public SwitchEvent[] getSwitchEvents()
	{
		return switchEvents;
	}
	
	@Override
	public SwitchEvent getSwitchEvent(int switchNumber)
	{
		return switchEvents[switchNumber];
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
			
			for(int i = 0; i < switchEvents.length; i++) 
			{
				if(switchEvents[i] != null)
				{
					o = new JSONObject();
			
					o.put(JSONValueField.value.name(), switchEvents[i].getSwitchEventType().name());
					o.put(JSONValueField.uiValue.name(), switchEvents[i].getSwitchEventType().name());
					o.put(JSONValueField.timestamp.name(), switchEvents[i].getDate().getTime());

					res.put("switch"+i, o);
				}
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
		if(date == null) 
			return null;
		
		try
		{
			JSONObject o = new JSONObject();
			for(int i = 0; i < switchEvents.length; i++) 
			{
				if(switchEvents[i] != null)
				{
					o.put("switchEvent"+i, ((SwitchEventImpl)switchEvents[i]).toJSON());
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
