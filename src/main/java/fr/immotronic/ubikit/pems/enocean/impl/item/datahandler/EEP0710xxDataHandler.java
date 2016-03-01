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
import java.util.Date;
import java.util.List;

import org.json.JSONObject;
import org.ubikit.event.AbstractEvent;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.EnoceanSensorData;
import fr.immotronic.ubikit.pems.enocean.data.EEP0710xxData;
import fr.immotronic.ubikit.pems.enocean.data.EEP0710xxData.DayNightState;
import fr.immotronic.ubikit.pems.enocean.data.EEP0710xxData.FanSpeed;
import fr.immotronic.ubikit.pems.enocean.event.out.DayNightSlideSwitchChangedEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.FanSpeedSelectionChangedEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.OccupancyButtonPressedEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.RelativeHumidityEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.SetPointChangedEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.TemperatureEvent;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanData;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEP0710xxDataImpl;

public final class EEP0710xxDataHandler implements EnoceanDataHandler 
{
	private static final float temperatureMultiplier_1 = (40f / 255f);
	private static final float temperatureMultiplier_2 = (40f / 250f);
	private static final float relativeHumidityMultiplier = (100f / 250f);
	
	private EventGate eventGate;
	private String itemUID;
	private EEP0710xxData sensorData;
	private int EEPType;
	
	public EEP0710xxDataHandler(EventGate eventGate, String itemUID, int EEPType, JSONObject lastKnownData)
	{
		this.eventGate = eventGate;
		this.itemUID = itemUID;
		this.EEPType = EEPType;
		
		sensorData = EEP0710xxDataImpl.constructDataFromRecord(lastKnownData);
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
		
		// Reading Fan Speed data, if any
		FanSpeed fanSpeed = FanSpeed.UNKNOWN;
		switch(EEPType)
		{
			case 0x1:
			case 0x2:
			case 0x4:
			case 0x7:
			case 0x8:
			case 0x9:
				int v = dataBytes[3] & 0xff;
				if(v >= 210) {
					fanSpeed = FanSpeed.AUTO;
				}
				else if(v >= 190 && v <= 209) {
					fanSpeed = FanSpeed.STAGE_0;
				}
				else if(v >= 165 && v <= 189) {
					fanSpeed = FanSpeed.STAGE_1;
				}
				else if(v >= 145 && v <= 164) {
					fanSpeed = FanSpeed.STAGE_2;
				}
				else if(v <= 144) {
					fanSpeed = FanSpeed.STAGE_3;
				}
				
				// Check if a change has occurred since last received data
				//if(fanSpeed != sensorData.getFanSpeed()) {
					// A change occurred, generate and send an event.
					events.add(new FanSpeedSelectionChangedEvent(itemUID, fanSpeed, data.getDate()));
				//}
				
				break;
		}
		
		// Reading Set Point data, if any
		int setPoint = -1;
		switch(EEPType)
		{
			case 0x1:
			case 0x2:
			case 0x3:
			case 0x4:	
			case 0x5:
			case 0x6:
			case 0xA:
				setPoint = dataBytes[2] & 0xff;
				// Check if a change has occurred since last received data
				//if(setPoint != sensorData.getSetPoint()) {
					// A change occurred, generate and send an event.
					events.add(new SetPointChangedEvent(itemUID, setPoint, data.getDate()));
				//}
				break;
				
			case 0x10:
			case 0x11:
			case 0x12:
				setPoint = dataBytes[3] & 0xff;
				// Check if a change has occurred since last received data
				//if(setPoint != sensorData.getSetPoint()) {
					// A change occurred, generate and send an event.
					events.add(new SetPointChangedEvent(itemUID, setPoint, data.getDate()));
				//}
				break;
		}
		
		float temperature = Float.MIN_VALUE;
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
			case 0xC:
			case 0xD:
				// According to Enocean Equipment Profile, temperature = 0..40°C, DB1 = 255..0, linear
				temperature = (float)(255 - (int)(dataBytes[1] & 0xff)) * temperatureMultiplier_1; 
				
				// Generate and send an temperature event.
				events.add(new TemperatureEvent(itemUID, temperature, data.getDate()));
				break;
				
			case 0x10:
			case 0x11:
			case 0x12:
			case 0x13:
			case 0x14:
				// According to Enocean Equipment Profile, temperature = 0..40°C, DB1 = 0..250, linear
				temperature = (float)(dataBytes[1] & 0xff) * temperatureMultiplier_2; 
				
				// Generate and send an temperature event.
				events.add(new TemperatureEvent(itemUID, temperature, data.getDate()));
				break;
		}
		
		float relativeHumidity = Float.MIN_VALUE;
		switch(EEPType)
		{
			case 0x10:	
			case 0x11:
			case 0x12:
			case 0x13:
			case 0x14:
				// According to Enocean Equipment Profile, relativeHumidity = 0..100%, DB2 = 0..250, linear
				relativeHumidity = (float)(dataBytes[2] & 0xff) * relativeHumidityMultiplier; 
				
				// Generate and send an relative humidity event.
				events.add(new RelativeHumidityEvent(itemUID, relativeHumidity, data.getDate()));
				break;
		}
		
		// Reading occupancy button data, if any
		Date lastOccupancyButtonPressDate = sensorData.getLastOccupancyButtonPressDate(); // Retain the old date, if occupancy button has not been pressed this time.
		switch(EEPType)
		{
			case 0x1:
			case 0x5:
			case 0x8:
			case 0xC:
			case 0x10:
			case 0x13:
				boolean occupancyButton = (dataBytes[0] & 0x1) == 0x0; // Worth true if the occupancy button is pressed
				if(occupancyButton) {
					lastOccupancyButtonPressDate = data.getDate();
					events.add(new OccupancyButtonPressedEvent(itemUID, data.getDate()));
				}
				break;
		}
		
		// Reading the day/night slide switch, if any
		DayNightState dayNightState = sensorData.getDayNightState(); // Get the old state, if day/night button change, an event will be sent.;
		switch(EEPType)
		{
			case 0x2:
			case 0x6:
			case 0x9:
			case 0xD:
			case 0x11:
			case 0x14:
				boolean dayNightSlideSwitch = (dataBytes[0] & 0x1) == 0x0; // Worth true if the day/night slide switch is Night
				if(dayNightSlideSwitch) 
				{
					//if(dayNightState != DayNightState.NIGHT) {
						events.add(new DayNightSlideSwitchChangedEvent(itemUID, dayNightState, data.getDate()));
					//}
					dayNightState = DayNightState.NIGHT;
				}
				else 
				{
					//if(dayNightState != DayNightState.DAY) {
						events.add(new DayNightSlideSwitchChangedEvent(itemUID, dayNightState, data.getDate()));
					//}
					dayNightState = DayNightState.DAY;
				}
				break;
		}
		
		// Remembering received data
		sensorData = new EEP0710xxDataImpl(temperature, relativeHumidity, fanSpeed, setPoint, lastOccupancyButtonPressDate, dayNightState, data.getDate());
		
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
