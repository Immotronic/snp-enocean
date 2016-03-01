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

package fr.immotronic.ubikit.pems.enocean.impl.item;

import org.json.JSONObject;
import org.ubikit.Logger;
import org.ubikit.event.EventGate;

import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.impl.DatabaseManager;
import fr.immotronic.ubikit.pems.enocean.impl.DeviceManager;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanSensorDeviceImpl;
import fr.immotronic.ubikit.pems.enocean.impl.LC;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP0503xxDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP050401DataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP051000DataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP060001DataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP0702xxDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP070401DataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP070402DataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP0706xxDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP070701DataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP0708xxDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP070904DataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP070905DataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEP0710xxDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.EEPA512xxDataHandler;
import fr.immotronic.ubikit.pems.enocean.impl.item.datahandler.Eltako_FRW_ws_SmokeAlarmDataHandler;

public final class EnoceanStandardSensorDevice extends EnoceanSensorDeviceImpl 
{
	public EnoceanStandardSensorDevice(EventGate eventGate, DatabaseManager.Record record, DeviceManager deviceManager)
	{
		super(record.getUID(), record.getEnoceanUID(), record.getEnoceanEquipmentProfile(), record.getManufacturerUID(), deviceManager);
		this.setPropertiesFromJSONObject(record.getPropertiesAsJSON());
		createDataHandler(record.getEnoceanEquipmentProfile(), record.getLastKnownData(), eventGate);
	}
	
	public EnoceanStandardSensorDevice(EventGate eventGate, int enoceanUID, EnoceanEquipmentProfileV20 eep, DeviceManager deviceManager) 
	{
		super(enoceanUID, eep, 0, deviceManager);
		createDataHandler(eep, null, eventGate);
	}

	private void createDataHandler(EnoceanEquipmentProfileV20 eep, JSONObject lastKnownData, EventGate eventGate)
	{
		addCapability(eep.name());
		EnoceanDataHandler dataHandler = null;
		switch(eep)
		{
			case EEP_05_02_01:
				dataHandler = new EEP0503xxDataHandler(eventGate, getUID(), lastKnownData);
				break;
			case EEP_05_04_01:
				dataHandler = new EEP050401DataHandler(eventGate, getUID(), lastKnownData);
				break;
			case EEP_05_10_00:
				dataHandler = new EEP051000DataHandler(eventGate, getUID(), lastKnownData);
				break;
			case EEP_06_00_01:
				dataHandler = new EEP060001DataHandler(eventGate, getUID(), lastKnownData);
				break;
			case EEP_07_02_01:
			case EEP_07_02_02:
			case EEP_07_02_03:
			case EEP_07_02_04:
			case EEP_07_02_05:
			case EEP_07_02_06:
			case EEP_07_02_07:
			case EEP_07_02_08:
			case EEP_07_02_09:
			case EEP_07_02_0A:
			case EEP_07_02_0B:
			case EEP_07_02_10:
			case EEP_07_02_11:
			case EEP_07_02_12:
			case EEP_07_02_13:
			case EEP_07_02_14:
			case EEP_07_02_15:
			case EEP_07_02_16:
			case EEP_07_02_17:
			case EEP_07_02_18:
			case EEP_07_02_19:
			case EEP_07_02_1A:
			case EEP_07_02_1B:
				dataHandler = new EEP0702xxDataHandler(eventGate, getUID(), eep.getType(), lastKnownData);
				break;
			case EEP_07_04_01:
				dataHandler = new EEP070401DataHandler(eventGate, getUID(), eep.getType(), lastKnownData);
				break;
			case EEP_07_04_02:
				dataHandler = new EEP070402DataHandler(eventGate, getUID(), eep.getType(), lastKnownData);
				break;
			case EEP_07_06_01:
			case EEP_07_06_02:
				dataHandler = new EEP0706xxDataHandler(eventGate, getUID(), eep.getType(), lastKnownData);
				break;
			case EEP_07_07_01:
				dataHandler = new EEP070701DataHandler(eventGate, getUID(), eep.getType(), lastKnownData);
				break;
			case EEP_07_08_01:
				dataHandler = new EEP0708xxDataHandler(eventGate, getUID(), eep.getType(), lastKnownData);
				break;
			case EEP_07_09_04:
				dataHandler = new EEP070904DataHandler(eventGate, getUID(), eep.getType(), lastKnownData);
				break;
			case EEP_07_09_05:
				dataHandler = new EEP070905DataHandler(eventGate, getUID(), lastKnownData);
				break;
			case EEP_07_10_01:
			case EEP_07_10_02:
			case EEP_07_10_03:
			case EEP_07_10_04:
			case EEP_07_10_05:
			case EEP_07_10_06:
			case EEP_07_10_07:
			case EEP_07_10_08:
			case EEP_07_10_09:
			case EEP_07_10_10:
			case EEP_07_10_11:
			case EEP_07_10_12:
			case EEP_07_10_13:
			case EEP_07_10_14:
				dataHandler = new EEP0710xxDataHandler(eventGate, getUID(), eep.getType(), lastKnownData);
				break;
			case EEP_A5_12_00:
			case EEP_A5_12_01:
			case EEP_A5_12_02:
			case EEP_A5_12_03:
				dataHandler = new EEPA512xxDataHandler(eventGate, getUID(), eep.getType(), lastKnownData);
				break;
			case ELTAKO_FRW_WS_SMOKE_ALARM:
				dataHandler = new Eltako_FRW_ws_SmokeAlarmDataHandler(eventGate, getUID(), lastKnownData);
				break;
			default:
				dataHandler = null;
				break;
		}
		
		setDataHandler(dataHandler);
		
		if(dataHandler == null) {
			Logger.warn(LC.gi(), this, eep.toString()+" devices are not yet supported in this implementation");
		}
	}
	
	@Override
	protected void terminate() 
	{
		//TODO: dataHandler must no more be an event listener 
	}
}
