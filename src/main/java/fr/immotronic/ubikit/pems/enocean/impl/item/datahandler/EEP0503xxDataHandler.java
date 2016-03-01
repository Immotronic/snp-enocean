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
import fr.immotronic.ubikit.pems.enocean.data.EEP0503xxData;
import fr.immotronic.ubikit.pems.enocean.event.out.SwitchOffEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.SwitchOnEvent;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanData;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEP0503xxDataImpl;

public final class EEP0503xxDataHandler implements EnoceanDataHandler 
{
	private EventGate eventGate;
	private String itemUID;
	
	private EEP0503xxDataImpl sensorData;
	
	public EEP0503xxDataHandler(EventGate eventGate, String itemUID, JSONObject lastKnownData)
	{
		this.eventGate = eventGate;
		this.itemUID = itemUID;
		
		sensorData = EEP0503xxDataImpl.constructDataFromRecord(lastKnownData);
	}

	@Override
	public void processNewIncomingData(int transmitterID, EnoceanData data) 
	{
		List<AbstractEvent> events = new ArrayList<AbstractEvent>();
		
		EEP0503xxData.SwitchEvent[] switchEvents = sensorData.getSwitchEvents();
		
		// Getting data to decode from telegram data
		byte dataByte3 = data.getBytes()[3];
		
		// Reading first button information, if any
		if((dataByte3 & 0xF0) != 0)
		{
			byte rockerID = (byte)((dataByte3 >> 6) & 0x3); // RockerID will worth 0, 1, 2 or 3.
			boolean button =  (dataByte3 & 0x20) == 0x0; // true if button 1 is pressed, false if button 0 is pressed
			
			// Generating and emit the matching switch event
			if(button) {
				switchEvents[rockerID] = new EEP0503xxDataImpl.SwitchEventImpl(data.getDate(), EEP0503xxData.SwitchEventType.ON);
				events.add(new SwitchOnEvent(itemUID, rockerID, data.getDate()));
			}
			else {
				switchEvents[rockerID] = new EEP0503xxDataImpl.SwitchEventImpl(data.getDate(), EEP0503xxData.SwitchEventType.OFF);
				events.add(new SwitchOffEvent(itemUID, rockerID, data.getDate()));
			}
		}
		
		// Reading second button information, if any
		if((dataByte3 & 0xF) != 0)
		{
			byte rockerID = (byte)((dataByte3 >> 2) & 0x3); // RockerID will worth 0, 1, 2 or 3.
			boolean button =  (dataByte3 & 0x2) == 0x0; // true if button 1 is pressed, false if button 0 is pressed
			
			// Generating and emit the matching switch event
			if(button) {
				switchEvents[rockerID] = new EEP0503xxDataImpl.SwitchEventImpl(data.getDate(), EEP0503xxData.SwitchEventType.ON);
				events.add(new SwitchOnEvent(itemUID, rockerID, data.getDate()));
			}
			else {
				switchEvents[rockerID] = new EEP0503xxDataImpl.SwitchEventImpl(data.getDate(), EEP0503xxData.SwitchEventType.OFF);
				events.add(new SwitchOffEvent(itemUID, rockerID, data.getDate()));
			}
		}
		
		// Remembering received data
		sensorData = new EEP0503xxDataImpl(switchEvents, data.getDate());
		
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
