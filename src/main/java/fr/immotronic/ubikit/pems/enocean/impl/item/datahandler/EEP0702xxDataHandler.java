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
import fr.immotronic.ubikit.pems.enocean.data.EEP0702xxData;
import fr.immotronic.ubikit.pems.enocean.event.out.TemperatureEvent;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanData;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEP0702xxDataImpl;

public final class EEP0702xxDataHandler implements EnoceanDataHandler 
{
	private final float temperatureMultiplier;
	private final float temperatureLowerRange;
	
	private EventGate eventGate;
	private String itemUID;
	
	private EEP0702xxData sensorData;
	
	public EEP0702xxDataHandler(EventGate eventGate, String itemUID, int EEPType, JSONObject lastKnownData)
	{
		this.eventGate = eventGate;
		this.itemUID = itemUID;
	
		sensorData = EEP0702xxDataImpl.constructDataFromRecord(lastKnownData);
		
		// Given the EEP type, initialize temperature multiplier and temperature lower range with adequate values
		switch(EEPType)
		{
			case 0x1:
			case 0x2:
			case 0x3:
			case 0x4:
			case 0x5:
			case 0x6:
			case 0x7:
			case 0x8:
			case 0x9:
			case 0xA:
			case 0xB:
				temperatureMultiplier = (40f / 255f);
				break;
				
			case 0x10:
			case 0x11:
			case 0x12:
			case 0x13:
			case 0x14:
			case 0x15:
			case 0x16:
			case 0x17:
			case 0x18:
			case 0x19:
			case 0x1A:
			case 0x1B:
				temperatureMultiplier = (80f / 255f);
				break;
				
			default:
				temperatureMultiplier = 0;
				break;
		}
		
		switch(EEPType)
		{
			case 0x10:
				temperatureLowerRange = -60;
				break;
			case 0x11:
				temperatureLowerRange = -50;
				break;
			case 0x1:
			case 0x12:
				temperatureLowerRange = -40;
				break;
			case 0x2:
			case 0x13:
				temperatureLowerRange = -30;
				break;
			case 0x3:
			case 0x14:
				temperatureLowerRange = -20;
				break;
			case 0x4:
			case 0x15:
				temperatureLowerRange = -10;
				break;
			case 0x5:
			case 0x16:
				temperatureLowerRange = 0;
				break;
			case 0x6:
			case 0x17:
				temperatureLowerRange = 10;
				break;
			case 0x7:
			case 0x18:
				temperatureLowerRange = 20;
				break;
			case 0x8:
			case 0x19:
				temperatureLowerRange = 30;
				break;
			case 0x9:
			case 0x1A:
				temperatureLowerRange = 40;
				break;
			case 0xA:
			case 0x1B:
				temperatureLowerRange = 50;
				break;
			case 0xB:
				temperatureLowerRange = 60;
				break;
			
			default:
				temperatureLowerRange = 0;
				break;
		}
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

		float temperature = Float.MIN_VALUE;
		
		// According to Enocean Equipment Profile: temperature = min..max°C, DB1 = 255..0, linear
		temperature = ((float)(255 - (int)(dataBytes[1] & 0xff)) * temperatureMultiplier) + temperatureLowerRange;
		
		// Generate a Temperature event.
		events.add(new TemperatureEvent(itemUID, temperature, data.getDate()));
		
		// Remembering received data
		sensorData = new EEP0702xxDataImpl(temperature, data.getDate());
		
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
