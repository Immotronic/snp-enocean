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
import org.ubikit.Logger;
import org.ubikit.event.AbstractEvent;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.EnoceanSensorData;
import fr.immotronic.ubikit.pems.enocean.data.EEP0706xxData;
import fr.immotronic.ubikit.pems.enocean.event.out.IlluminationEvent;
import fr.immotronic.ubikit.pems.enocean.event.out.SupplyVoltageEvent;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanData;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.LC;
import fr.immotronic.ubikit.pems.enocean.impl.item.data.EEP0706xxDataImpl;

public final class EEP0706xxDataHandler implements EnoceanDataHandler 
{
	private final float illuminationMultiplierSmallRange;
	private final float illuminationMultiplierLargeRange;
	private final int illuminationMinSmallRange;
	private final int illuminationMinLargeRange;
	private final float supplyVoltageMultiplier;
	
	private EventGate eventGate;
	private String itemUID;
	private boolean smallRangeSelected;
	
	private EEP0706xxData sensorData;
	
	public EEP0706xxDataHandler(EventGate eventGate, String itemUID, int EEPType, JSONObject lastKnownData)
	{
		this.eventGate = eventGate;
		this.itemUID = itemUID;
		
		supplyVoltageMultiplier = (5.1f / 255f) * 100f;
		
		// Given the EEP type, initialize illumination & temperature multipliers with adequate values
		switch(EEPType)
		{
			case 0x1:
				illuminationMultiplierSmallRange = 29700f / 255f;
				illuminationMultiplierLargeRange = 59400f / 255f;
				illuminationMinSmallRange = 300;
				illuminationMinLargeRange = 600;
				break;
			
			case 0x2:
				illuminationMultiplierSmallRange = 510f / 255f;
				illuminationMultiplierLargeRange = 1020f / 255f;
				illuminationMinSmallRange = 0;
				illuminationMinLargeRange = 0;
				break;
				
			default:
				illuminationMultiplierSmallRange = 0f;
				illuminationMultiplierLargeRange = 0f;
				illuminationMinSmallRange = 0;
				illuminationMinLargeRange = 0;
				break;
		}
		
		sensorData = EEP0706xxDataImpl.constructDataFromRecord(lastKnownData);
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
		float supplyVoltage = (float)Math.round((float)(dataBytes[3] & 0xff) * supplyVoltageMultiplier) / 100f; 
		
		// Generate a Supply Voltage event.
		events.add(new SupplyVoltageEvent(itemUID, supplyVoltage, data.getDate()));	
		
		// Find out which measurement range is used. According to the EEP specification
		// the range selection is point out by bit 0 of databyte 0. But Omnio device,
		// for instance do not conformed to this standard. Then we assume that the databyte that
		// should contain the measurement for the unselected range is set to 0. Otherwise, we cannot
		// find out which range is selected.
		
		// Reading selected range point out by Bit 0 of Databye 0: 1: small range selected, 0: large range selected
		smallRangeSelected = (dataBytes[0] & 0x1) == 0x1;
		
		// Computing illumination value according the 2 possible ranges
		int illumination = 0;
		int illumination_sr = Math.round((dataBytes[2] & 0xff) * illuminationMultiplierSmallRange);
		int illumination_lr = Math.round((dataBytes[1] & 0xff) * illuminationMultiplierLargeRange);
		
		if(smallRangeSelected && illumination_sr != 0 && illumination_lr == 0) {
			// No doubt, small range is selected
			illumination = illumination_sr + illuminationMinSmallRange;
		}
		else if((smallRangeSelected && illumination_sr == 0 && illumination_lr == 0) ||
				(smallRangeSelected && illumination_sr != 0 && illumination_lr != 0)) {
			// Cannot guess, but there is no reason to consider smallRangeSelected wrong. 
			illumination = illumination_sr + illuminationMinSmallRange;
		}
		else if(smallRangeSelected && illumination_sr == 0 && illumination_lr != 0) {
			// Cannot guess, but smallRangeSelected is probably wrong
			illumination = illumination_lr + illuminationMinLargeRange;
			Logger.warn(LC.gi(), this, "EEP0706xxDataHandler has no choice to guess the light sensor measuring range. The light computed value is maybe wrong. (case 1)");
		}
		else if(!smallRangeSelected && illumination_sr == 0 && illumination_lr != 0) {
			// No doubt, large range is selected
			illumination = illumination_lr + illuminationMinLargeRange;
		}
		else if((!smallRangeSelected && illumination_sr == 0 && illumination_lr == 0) ||
				(!smallRangeSelected && illumination_sr != 0 && illumination_lr != 0)) {
			// Cannot guess, but there is no reason to consider smallRangeSelected wrong. 
			illumination = illumination_lr + illuminationMinLargeRange;
		}
		else if(!smallRangeSelected && illumination_sr != 0 && illumination_lr == 0) {
			// Cannot guess, but smallRangeSelected is probably wrong
			illumination = illumination_sr + illuminationMinSmallRange;
			Logger.warn(LC.gi(), this, "EEP0706xxDataHandler has no choice to guess the light sensor measuring range. The light computed value is maybe wrong. (case 2)");
		}

		Logger.debug(LC.gi(), this, "smallRangeSelected="+smallRangeSelected);
		Logger.debug(LC.gi(), this, "DB2 (ILL2)="+(dataBytes[2] & 0xff)+", illumination_sr="+illumination_sr);
		Logger.debug(LC.gi(), this, "DB1 (ILL1)="+(dataBytes[1] & 0xff)+", illumination_lr="+illumination_lr);
		Logger.debug(LC.gi(), this, "illumination="+illumination+" lux");
		
		// Generate an Illumination event.
		events.add(new IlluminationEvent(itemUID, illumination, data.getDate()));
		
		// Remembering received data
		sensorData = new EEP0706xxDataImpl(supplyVoltage, illumination, data.getDate());
		
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
