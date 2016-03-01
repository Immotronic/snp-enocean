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
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.EnoceanSensorData;
import fr.immotronic.ubikit.pems.enocean.event.out.MeteringCounterEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.MeteringEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.MeteringEvent.MeasurementUnit;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanData;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEPA512xxDataImpl;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEPA512xxDataImpl.ValueType;

public class EEPA512xxDataHandler implements EnoceanDataHandler 
{
	private EventGate eventGate;
	private String itemUID;
	private int EEPType;
	
	private EEPA512xxDataImpl sensorData;
	
	public EEPA512xxDataHandler(EventGate eventGate, String itemUID, int EEPType, JSONObject lastKnownData)
	{
		this.eventGate = eventGate;
		this.itemUID = itemUID;
		this.EEPType = EEPType;
		
		sensorData = EEPA512xxDataImpl.constructDataFromRecord(EEPType, lastKnownData);
	}
	
	@Override
	public void processNewIncomingData(int transmitterID, EnoceanData data) 
	{
		byte[] dataBytes = data.getBytes();
		
		// Checking if telegram is a teach-in telegram. If so, ignore it.
		if((dataBytes[0] & 0x8) == 0x0) {
			return;
		}
		
		// Decode measurement channel (EEPType 00) OR tariff info (other EEPType)
		int info = (dataBytes[0] & 0xf0) >> 4;
		
		// Decode Data Type (0 : cumulative value, 1 current value)
		int dataType = (dataBytes[0] & 0x4) >> 2;
		ValueType type = (dataType == 0) ? ValueType.CUMULATIVE : ValueType.CURRENT;
		
		// Decode divisor
		int divisor = dataBytes[0] & 0x3;
		
		// Decode meter reading
		int meterReading = (dataBytes[3] & 0xff) << 16 | (dataBytes[2] & 0xff) << 8 | (dataBytes[1] & 0xff);
		
		// Calculating measured value
		float value = meterReading/getDivisorValue(divisor);
		
		switch(EEPType)
		{
			case 0x00:
				sensorData.setCounterValue(info, type, value, data.getDate());
				eventGate.postEvent(new MeteringCounterEvent(itemUID, info, value, (dataType == 0), data.getDate()));
				break;
			case 0x01:
				sensorData = new EEPA512xxDataImpl(EEPType, value, type, info, sensorData.getFirstChannel() , data.getDate());
				eventGate.postEvent(new MeteringEvent(itemUID, value, (dataType == 0) ? MeasurementUnit.KILOWATT_HOUR : MeasurementUnit.KILOWATT, info, data.getDate()));
				break;
			case 0x02:
			case 0x03:
				sensorData = new EEPA512xxDataImpl(EEPType, value, type, info, sensorData.getFirstChannel() , data.getDate());
				eventGate.postEvent(new MeteringEvent(itemUID, value, (dataType == 0) ? MeasurementUnit.CUBIC_METRE : MeasurementUnit.LITRE_PER_SECOND, info, data.getDate()));
				break;
		}		
	}

	@Override
	public EnoceanSensorData getLastKnownData() 
	{
		return sensorData;
	}
	
	private float getDivisorValue(int divisor)
	{
		if (divisor < 0 || divisor > 3)
			throw new IllegalArgumentException(divisor + " is not a valid divisor identifier for EEP_A5_12_xx enocean equipment.");
		
		switch (divisor)
		{
			case 0: return 1;
			case 1: return 10;
			case 2: return 100;
			case 3: return 1000;
			default: return 1;
		}
	}
}
