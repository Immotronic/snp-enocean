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

import fr.immotronic.ubikit.pems.enocean.ControllerProfile;
import fr.immotronic.ubikit.pems.enocean.impl.DatabaseManager;
import fr.immotronic.ubikit.pems.enocean.impl.DeviceManager;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanControllerDeviceImpl;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanSerialAdapter;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanTelegram;
import fr.immotronic.ubikit.pems.enocean.impl.LC;
import fr.immotronic.ubikit.pems.enocean.impl.DatabaseManager.Record;

public final class RHController extends EnoceanControllerDeviceImpl 
{
	private boolean used;
	
	public RHController(String controllerUID, int enoceanUID, EnoceanSerialAdapter enoceanSerialAdapter, DeviceManager deviceManager)
	{
		super(controllerUID, enoceanUID, ControllerProfile.RH_CONTROLLER, enoceanSerialAdapter, deviceManager);
	}
	
	public RHController(int controllerUID, EnoceanSerialAdapter enoceanSerialAdapter, DeviceManager deviceManager) 
	{
		super(controllerUID, ControllerProfile.RH_CONTROLLER, enoceanSerialAdapter, deviceManager);
		used = false;
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "Creating a new RH_CONTROLLER: "+getUID()+", enoceanUID="+getEnoceanUID());
		}
	}
	
	public void markAsUsed()
	{
		used = true;
	}
	
	public void setOutputLevel(int percent)
	{
		int d = (percent * 250) / 100;
		emitTelegram((byte)d);
	}
	
	private void emitTelegram(byte data)
	{
		byte[] telegramData = { 0x8, data, data, data };
		Logger.debug(LC.gi(), this, "emit (twice) = "+Integer.toHexString(telegramData[2] & 0xff));
		byte[] rawTelegram = getSerialAdapter().createRawTransmitRadioTelegram(EnoceanTelegram.RORG.RORG_4BS, getEnoceanUID(), telegramData);
		getSerialAdapter().emitRawTelegram(rawTelegram);
		//getSerialAdapter().emitRawTelegram(rawTelegram); // Sending is done twice to minimize RF hazards
	}
	
	@Override
	public boolean isUnused() 
	{
		return !used;
	}

	@Override
	public void releaseChannel(int channelID) 
	{
		used = false;
	}

	@Override
	public boolean checkChannelAvailability(int nbOfRequiredChannels) 
	{
		return !used;
	}

	@Override
	public Object getValue() 
	{
		return null;
	}

	@Override
	public JSONObject getValueAsJSON() 
	{
		return null;
	}

	@Override
	public Record getDeviceRecord() 
	{
		DatabaseManager.Record rec = this.getPreFilledRecordWithoutData();
		rec.setData(Integer.toString(getControllerIndex()));
		
		return rec;
	}

	@Override
	protected void terminate() 
	{
		//TODO: emitTelegram MUST be impossible and reference to enoceanSerialAdapter MUST be set to null
	}

}
