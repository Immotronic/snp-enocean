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
import org.ubikit.event.AbstractEvent;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.EnoceanSensorData;
import fr.immotronic.ubikit.pems.enocean.data.EEP051000Data;
import fr.immotronic.ubikit.pems.enocean.data.EEP051000Data.HandleState;
import fr.immotronic.ubikit.pems.enocean.event.out.WindowHandleClosedEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.WindowHandleOpenedHorizontallyEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.WindowHandleOpenedVerticallyEvent;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanData;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEP051000DataImpl;

public final class EEP051000DataHandler implements EnoceanDataHandler 
{
	private EventGate eventGate;
	private String itemUID;
	private EEP051000Data sensorData;
	
	public EEP051000DataHandler(EventGate eventGate, String itemUID, JSONObject lastKnownData)
	{
		this.eventGate = eventGate;
		this.itemUID = itemUID;
		
		sensorData = EEP051000DataImpl.constructDataFromRecord(lastKnownData);
	}

	@Override
	public void processNewIncomingData(int transmitterID, EnoceanData data) 
	{		
		// Reading contact information: 
		HandleState receivedHandleState = HandleState.UNKNOWN;
		int c = data.getBytes()[3];
		if((c & 0xf0) == 0xf0) 
		{
			receivedHandleState = HandleState.CLOSED;
		}
		else if((c & 0xc0) == 0xc0) {
			if((c & 0x10) == 0x10) {
				receivedHandleState = HandleState.OPEN_VERTICAL;
			}
			else {
				receivedHandleState = HandleState.OPEN_HORIZONTAL;
			}
		}
		
		AbstractEvent event = null;
		
		// If state has changed since last time
		//if(receivedHandleState != sensorData.getHandleState())
		//{
			// Generate and send the appropriate event
			switch(receivedHandleState)
			{
				case CLOSED:
					event = new WindowHandleClosedEvent(itemUID, data.getDate());
					break;
					
				case OPEN_HORIZONTAL:
					event = new WindowHandleOpenedHorizontallyEvent(itemUID, data.getDate());
					break;
					
				case OPEN_VERTICAL:
					event = new WindowHandleOpenedVerticallyEvent(itemUID, data.getDate());
					break;
				default:
					return;
			}
		//}
		
		// Remembering the new state
		sensorData = new EEP051000DataImpl(receivedHandleState, data.getDate());
		
		// And then, send the event, if any. Sending MUST be done after remembering received data, otherwise if an event listener performs
		// a getLastKnownData() on event reception, it might obtain non up-to-date data.
		if(event != null) {
			eventGate.postEvent(event);
		}
	}

	@Override
	public EnoceanSensorData getLastKnownData() 
	{
		return sensorData;
	}
}
