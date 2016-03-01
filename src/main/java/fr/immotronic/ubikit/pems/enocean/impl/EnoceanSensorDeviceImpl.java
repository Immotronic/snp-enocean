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

package fr.immotronic.ubikit.pems.enocean.impl;

import org.json.JSONObject;
import org.ubikit.PhysicalEnvironmentItem;

import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorDevice;
import fr.immotronic.ubikit.pems.enocean.impl.DatabaseManager;

public abstract class EnoceanSensorDeviceImpl extends EnoceanDeviceImpl implements EnoceanSensorDevice
{
	private EnoceanEquipmentProfileV20 eep;
	private int manufacturerUID;
	private EnoceanDataHandler dataHandler = null;
	private int lastRSSI;
	
	protected EnoceanSensorDeviceImpl(long enoceanUID, EnoceanEquipmentProfileV20 eep, int manufacturerUID, DeviceManager deviceManager) 
	{
		this((int)enoceanUID, eep, manufacturerUID, deviceManager);
	}
	
	protected EnoceanSensorDeviceImpl(int enoceanUID, EnoceanEquipmentProfileV20 eep, int manufacturerUID, DeviceManager deviceManager) 
	{
		super(enoceanUID, PhysicalEnvironmentItem.Type.SENSOR, deviceManager, null);
		this.eep = eep;
		this.manufacturerUID = manufacturerUID;
		this.lastRSSI = 0;
	}

	protected EnoceanSensorDeviceImpl(String UID, int enoceanUID, EnoceanEquipmentProfileV20 eep, int manufacturerUID, DeviceManager deviceManager) 
	{
		super(UID, enoceanUID, PhysicalEnvironmentItem.Type.SENSOR, deviceManager, null);
		this.eep = eep;
		this.manufacturerUID = manufacturerUID;
		this.lastRSSI = 0;
	}
	
	protected DatabaseManager.Record getPreFilledRecordWithoutData()
	{
		return new DatabaseManager.Record(getUID(), manufacturerUID, getPropertiesAsJSONObject().toString(), getEnoceanUID(), eep, null, null);
	}
	
	protected void setDataHandler(EnoceanDataHandler dataHandler)
	{
		this.dataHandler = dataHandler;
	}

	@Override
	public EnoceanEquipmentProfileV20 getEnoceanEquipmentProfile() 
	{
		return eep;
	}
	
	@Override
	public int getManufacturerUID() 
	{
		return manufacturerUID;
	}
	
	@Override
	public EnoceanDataHandler getDataHandler() 
	{
		return dataHandler;
	}
	
	@Override
	public DatabaseManager.Record getDeviceRecord()
	{
		DatabaseManager.Record rec = this.getPreFilledRecordWithoutData();
		rec.setLastKnownData(dataHandler.getLastKnownData().getRecordAsJSON());
		
		return rec;
	}
	
	@Override
	public int getDeviceLastRSSI()
	{
		return lastRSSI;
	}
	
	public void setDeviceLastRSSI(int RSSI)
	{
		lastRSSI = RSSI;
	}
	
	@Override
	public Object getValue() 
	{
		if(dataHandler != null) {
			return dataHandler.getLastKnownData();
		}
		
		return null;
	}
	
	@Override
	public JSONObject getValueAsJSON()
	{
		if(dataHandler != null) {
			return dataHandler.getLastKnownData().toJSON();
		}
		
		return null;
	}
}
