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

public final class FourChannelPTMController extends EnoceanControllerDeviceImpl 
{
	private byte usedChannelBitMask; // 4 lowest bits are used : channel 1 = LSB, channel 4 = MSB.
	
	
	public FourChannelPTMController(String controllerUID, int enoceanUID, EnoceanSerialAdapter enoceanSerialAdapter, DeviceManager deviceManager)
	{
		super(controllerUID, enoceanUID, ControllerProfile.FOUR_CHANNEL_PTM_CONTROLLER, enoceanSerialAdapter, deviceManager);
		
	}
	
	public FourChannelPTMController(int controllerUID, EnoceanSerialAdapter enoceanSerialAdapter, DeviceManager deviceManager) 
	{
		super(controllerUID, ControllerProfile.FOUR_CHANNEL_PTM_CONTROLLER, enoceanSerialAdapter, deviceManager);
		usedChannelBitMask = 0;
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "Creating a new FOUR_CHANNEL_PTM_CONTROLLER: "+getUID()+", enoceanUID="+getEnoceanUID());
		}
	}
	
	/**
	 * 
	 * @return the channel number from 0 to 3. -1 means that no free channel left.
	 */
	public int getFreeChannel()
	{
		Logger.debug(LC.gi(), this, "getFreeChannel(): mask="+usedChannelBitMask+" mask&0x1="+(usedChannelBitMask & 0x1)); 
		if((usedChannelBitMask & 0x1) == 0) { // Is the first channel is free ?
			usedChannelBitMask += 0x1; // Yes, book it.
			return 0; // and return its number.
		}
		if((usedChannelBitMask & 0x2) == 0) {
			usedChannelBitMask += 0x2;
			return 1;
		}
		if((usedChannelBitMask & 0x4) == 0) {
			usedChannelBitMask += 0x4;
			return 2;
		}
		if((usedChannelBitMask & 0x8) == 0) {
			usedChannelBitMask += 0x8;
			return 3;
		}
		
		// No free channel available, returning 0.
		return -1;
	}
	
	/**
	 * 
	 * @return true if this controller is not used by any logical actuator.
	 */
	@Override
	public boolean isUnused()
	{
		return usedChannelBitMask == 0;
	}
	
	/*public boolean isFreeChannelLeft()
	{
		return (usedChannelBitMask & 0xf) != 0xf;
	}*/
	
	@Override
	public boolean checkChannelAvailability(int nbOfRequiredChannels)
	{
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "checkChannelAvailability for "+nbOfRequiredChannels
					+" channel: usedChannelBitMask="+usedChannelBitMask
					+", nb busy channels="+( (usedChannelBitMask & 0x1) + ((usedChannelBitMask & 0x2) >> 1) + ((usedChannelBitMask & 0x4) >> 2) + ((usedChannelBitMask & 0x8) >> 3))
					+", check="+((4 - (usedChannelBitMask & 0x1) - ((usedChannelBitMask & 0x2) >> 1) - ((usedChannelBitMask & 0x4) >> 2) - ((usedChannelBitMask & 0x8) >> 3)) >= nbOfRequiredChannels));
		}
		return (4 - (usedChannelBitMask & 0x1) - ((usedChannelBitMask & 0x2) >> 1) - ((usedChannelBitMask & 0x4) >> 2) - ((usedChannelBitMask & 0x8) >> 3)) >= nbOfRequiredChannels;
	}
	
	@Override
	public void releaseChannel(int channel)
	{
		usedChannelBitMask &= (0xf - (0x1 << channel));
	}
	
	public void markChannelAsUsed(int channel)
	{
		usedChannelBitMask += 0x1 << channel;
	}
	
	public void setChannelOn(int channel)
	{
		byte data = (byte)(((channel << 6) + 0x30) & 0xD0);
		emitTelegram(data, (byte) 0x30);
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "Channel "+getEnoceanUID()+"("+data+") was set ON");
		}
	}
	
	public void setChannelReleased(int channel)
	{
		emitTelegram((byte)0, (byte) 0x20);
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "Channel "+getEnoceanUID()+" was set RELEASED");
		}
	}
	
	public void setChannelOff(int channel)
	{
		byte data = (byte)(((channel << 6) + 0x30) & 0xF0);
		emitTelegram(data, (byte) 0x30);
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "Channel "+getEnoceanUID()+"("+data+") was set OFF");
		}
	}
	
	@Override
	public DatabaseManager.Record getDeviceRecord()
	{
		DatabaseManager.Record rec = this.getPreFilledRecordWithoutData();
		rec.setData(Integer.toString(getControllerIndex()));
		
		return rec;
	}

	private void emitTelegram(byte data, byte status)
	{
		byte[] telegramData = { 0, 0, 0, data };
		Logger.debug(LC.gi(), this, "emit = "+Integer.toHexString(telegramData[3]));
		byte[] rawTelegram =getSerialAdapter().createRawTransmitRadioTelegram(EnoceanTelegram.RORG.RORG_RPS, getEnoceanUID(), telegramData, status);
		getSerialAdapter().emitRawTelegram(rawTelegram);
		//getSerialAdapter().emitRawTelegram(rawTelegram); // Sending is done twice to minimize RF hazards
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
	protected void terminate() 
	{
		//TODO: emitTelegram MUST be impossible and reference to enoceanSerialAdapter MUST be set to null
	}
}
