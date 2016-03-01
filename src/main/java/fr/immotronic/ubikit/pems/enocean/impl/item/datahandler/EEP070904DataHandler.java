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

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.ubikit.event.AbstractEvent;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.EnoceanSensorData;
import fr.immotronic.ubikit.pems.enocean.data.EEP070904Data;
import fr.immotronic.ubikit.pems.enocean.event.out.CO2ConcentrationEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.RelativeHumidityEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.TemperatureEvent;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanData;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEP070904DataImpl;

public final class EEP070904DataHandler implements EnoceanDataHandler 
{
	private final float temperatureMultiplier;
	private final float temperatureLowerRange;
	private final int concentrationMultiplier;
	private final float humidityMultiplier;
	
	private EventGate eventGate;
	private String itemUID;
	
	private EEP070904Data sensorData;
	
	public EEP070904DataHandler(EventGate eventGate, String itemUID, int EEPType, JSONObject lastKnownData)
	{
		this.eventGate = eventGate;
		this.itemUID = itemUID;
		
		concentrationMultiplier = 10;
		humidityMultiplier = .5f;
		temperatureMultiplier = (51f / 255f);
		temperatureLowerRange = 0;
		
		sensorData = EEP070904DataImpl.constructDataFromRecord(lastKnownData);
	}

	@Override
	public void processNewIncomingData(int transmitterID, EnoceanData data) 
	{
		List<AbstractEvent> events = new ArrayList<AbstractEvent>();
		
		byte[] dataBytes = data.getBytes();
		
		// Checking if telegram is a teach-in telegram. If so, ignore it.
		if((dataBytes[0] & 0x8) == 0x0) {
			return;
		}
		
		// Reading supply voltage data		
		float relativeHumidity = (float)Math.round((float)(dataBytes[3] & 0xff) * humidityMultiplier); 
		
		// Generate a supply voltage event.
		events.add(new RelativeHumidityEvent(itemUID, relativeHumidity, data.getDate()));
		
		// Reading illumination data
		int C02ppm = (dataBytes[2] & 0xff) * concentrationMultiplier; 
		
		// Generate an Illumination event.
		events.add(new CO2ConcentrationEvent(itemUID, C02ppm, data.getDate()));
		
		// Reading temperature data
		float temperature = ((float)(dataBytes[1] & 0xff) * temperatureMultiplier) - temperatureLowerRange; 
		
		// Generate a Temperature event.
		events.add(new TemperatureEvent(itemUID, temperature, data.getDate()));
		
		// Remembering received data
		sensorData = new EEP070904DataImpl(relativeHumidity, C02ppm, temperature, data.getDate());
		
		// And then, send all generated events. Sending MUST be done after remembering received data, otherwise if an event listener performs
		// a getLastKnownData() on event reception, it might obtain non up-to-date data.
		eventGate.postEvents(events);
	}
	
	@Override
	public EnoceanSensorData getLastKnownData()
	{
		return sensorData;
	}
}
