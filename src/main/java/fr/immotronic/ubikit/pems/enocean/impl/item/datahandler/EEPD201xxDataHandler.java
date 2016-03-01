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

import java.util.Date;

import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.event.AbstractEvent;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.EnoceanSensorData;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData.Mode;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData.SwitchState;
import fr.immotronic.ubikit.pems.enocean.data.EEPD201xxData.MeasurementUnit;
import fr.immotronic.ubikit.pems.enocean.event.out.DimmingEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.MeteringEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.SwitchOffEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.SwitchOnEvent;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanData;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.LC;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEPD201xxDataImpl;

public class EEPD201xxDataHandler implements EnoceanDataHandler 
{
	private EventGate eventGate;
	private String itemUID;
	private int EEPType;
	
	private EEPD201xxData sensorData;
	
	public EEPD201xxDataHandler(EventGate eventGate, String itemUID, int EEPType, JSONObject lastKnownData)
	{
		this(eventGate, itemUID, EEPType, EEPD201xxDataImpl.constructDataFromRecord(EEPType, lastKnownData));
	}
	
	public EEPD201xxDataHandler(EventGate eventGate, String itemUID, int EEPType, Mode mode)
	{
		this(eventGate, itemUID, EEPType, new EEPD201xxDataImpl(EEPType, mode));
	}
	
	private EEPD201xxDataHandler(EventGate eventGate, String itemUID, int EEPType, EEPD201xxData sensorData)
	{
		this.eventGate = eventGate;
		this.itemUID = itemUID;
		this.EEPType = EEPType;
		this.sensorData = sensorData;
	}
	
	@Override
	public void processNewIncomingData(int transmitterID, EnoceanData data)
	{
		byte[] dataBytes = data.getBytes();
		
		AbstractEvent event = null;
		SwitchState[] switchStates = sensorData.getSwitchStates();
		Date[] eventDates = ((EEPD201xxDataImpl)sensorData).getSwitchStateDates();
		
		// Reading Command Identifier
		switch (dataBytes[dataBytes.length-1])
		{
			// Actuator Status Response
			case 0x04:
				
				switch(EEPType)
				{
					case 0x0:
					case 0x6:
						// Reading switch state
						switchStates[0] = (dataBytes[0] == 0) ? SwitchState.OFF : SwitchState.UNKNOWN;
						if (switchStates[0] != SwitchState.OFF && dataBytes[0] <= 0x64)
							switchStates[0] = SwitchState.ON;
						
						eventDates[0] = data.getDate();
						
						// Does the state change since last data ?
						//if (switchState != ((EEPD201xxData.EEPD20106Data) sensorData).getSwitchState())
						//{
							if (switchStates[0] == SwitchState.ON)
								event = new SwitchOnEvent(itemUID, (byte) 0, data.getDate());
							else if (switchStates[0] == SwitchState.OFF)
								event = new SwitchOffEvent(itemUID, (byte) 0, data.getDate());
						//}
							
						// Remembering new state.
						sensorData = new EEPD201xxDataImpl(EEPType, switchStates, eventDates, sensorData.getMeasurementValue(), sensorData.getMeasurementUnit(), ((EEPD201xxDataImpl) sensorData).getValueDate(), Mode.NOT_APPLICABLE);
						break;
						
					case 0x2:
						// Reading dimmer value
						int dimmerValue = dataBytes[0];
						if (dimmerValue < 0 || dimmerValue > 0x64)
							dimmerValue = Integer.MIN_VALUE;
						
						eventDates[0] = data.getDate();
						
						// Does the value change since last data ?
						//if (dimmerValue != ((EEPD201xxData.EEPD20102Data) sensorData).getDimmerValue())
						//{
							event = new DimmingEvent(itemUID, dimmerValue, data.getDate());
						//}
							
						// Remembering new state.
						sensorData = new EEPD201xxDataImpl(EEPType, dimmerValue, eventDates, sensorData.getMeasurementValue(), sensorData.getMeasurementUnit(), ((EEPD201xxDataImpl) sensorData).getValueDate());
						break;
						
					case 0x11:
						// Reading switch state
						int channel_index = (dataBytes[1] & 0x1f);
						switchStates[channel_index] = (dataBytes[0] == 0) ? SwitchState.OFF : SwitchState.UNKNOWN;
						
						if (switchStates[channel_index] != SwitchState.OFF && dataBytes[0] <= 0x64) {
							switchStates[channel_index] = SwitchState.ON;
						}
						
						eventDates[channel_index] = data.getDate();
						
						// Send switch event
						if (switchStates[channel_index] == SwitchState.ON)
							event = new SwitchOnEvent(itemUID, (byte) channel_index, data.getDate());
						else if (switchStates[channel_index] == SwitchState.OFF)
							event = new SwitchOffEvent(itemUID, (byte) channel_index, data.getDate());
						
						// Remembering new state.
						sensorData = new EEPD201xxDataImpl(EEPType, switchStates, eventDates, sensorData.getMeasurementValue(), sensorData.getMeasurementUnit(), ((EEPD201xxDataImpl) sensorData).getValueDate(), ((EEPD201xxDataImpl) sensorData).getMode());
						break;
				}
				
				break;
				
			// Actuator Measurement Response
			case 0x07:
				//Reading unit
				MeasurementUnit unit;
				byte unitByte = (byte) ((dataBytes[4] >> 5) & 0x7);
				switch (unitByte)
				{
					case 0x0: unit = MeasurementUnit.WATT_SECOND; break;
					case 0x1: unit = MeasurementUnit.WATT_HOUR; break;
					case 0x2: unit = MeasurementUnit.KILOWATT_HOUR; break;
					case 0x3: 
					case 0x4:
					default: 
						Logger.warn(LC.gi(), this, "Received an unknown unit identifier. Check code and enocean EEP2.5 specs.");
						unit = MeasurementUnit.UNKNOWN; break;
				}
				
				// Reading measured value
				int value = (dataBytes[3] & 0xff) << 24 | (dataBytes[2] & 0xff) << 16 | (dataBytes[1] & 0xff) << 8 | (dataBytes[0] & 0xff);
				
				// Does the value change since last data ?
				//if (unit != sensorData.getMeasurementUnit() || value != sensorData.getMeasurementValue())
				//{
					event = new MeteringEvent(itemUID, value, MeteringEvent.MeasurementUnit.valueOf(unit.name()), 0, data.getDate());
				//}
				
				// Remembering new state.
				switch (EEPType)
				{
					case 0x0:
					case 0x6:
						sensorData = new EEPD201xxDataImpl(EEPType, sensorData.getSwitchStates(), ((EEPD201xxDataImpl) sensorData).getSwitchStateDates(), value, unit, data.getDate(), Mode.NOT_APPLICABLE);
						break;
					case 0x2:
						sensorData = new EEPD201xxDataImpl(EEPType, sensorData.getDimmerValue(), ((EEPD201xxDataImpl) sensorData).getSwitchStateDates(), value, unit, data.getDate());
						break;
				}
				
				break;
				
			default:
				Logger.warn(LC.gi(), this, "Unsupported CMD identifier from EEPD20106 Device received. Check code and enocean EEP2.5 specs.");
				return;
		}

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
