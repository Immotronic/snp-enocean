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

import fr.immotronic.ubikit.pems.enocean.data.EEP0702xxData;
import fr.immotronic.ubikit.pems.enocean.impl.LC;

public final class EEP0702xxDataImpl implements EEP0702xxData 
{
	private final static DecimalFormat df = new DecimalFormat("#######.#");
	
	private final float temperature;
	private final Date date;
	
	public static EEP0702xxDataImpl constructDataFromRecord(JSONObject lastKnownData)
	{
		if (lastKnownData == null)
			return new EEP0702xxDataImpl(Float.MIN_VALUE, null);
		
		try
		{
			float temperature = (float) lastKnownData.getDouble("temperature");
			Date date = new Date(lastKnownData.getLong("date"));
			
			return new EEP0702xxDataImpl(temperature, date);
		}
		catch (JSONException e)
		{
			Logger.error(LC.gi(), null, "constructDataFromRecord(): An exception while building a sensorData from a JSONObject SHOULD never happen. Check the code !", e);
			return new EEP0702xxDataImpl(Float.MIN_VALUE, null);
		}
	}
	
	public EEP0702xxDataImpl(float temperature, Date date)
	{
		this.temperature = temperature;
		this.date = date;
	}

	@Override
	public float getTemperature() 
	{
		return temperature;
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
			
			o = new JSONObject();
			o.put(JSONValueField.value.name(), temperature);
			o.put(JSONValueField.uiValue.name(), df.format(temperature));
			o.put(JSONValueField.unit.name(), " &deg;C");
			o.put(JSONValueField.timestamp.name(), timestamp);
			res.put("temperature", o);
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
			o.put("temperature", temperature);
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
