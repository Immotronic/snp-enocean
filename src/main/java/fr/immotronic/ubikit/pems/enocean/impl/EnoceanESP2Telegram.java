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

import java.util.Date;

import org.ubikit.Logger;

/** 
 * A class that decode an EnOcean telegram from raw data. 
 */
public final class EnoceanESP2Telegram extends EnoceanTelegram
{
	public static final int TELEGRAM_SIZE = 14;
	public static final byte ORG_RPS = 0x05; // telegram from PTM (Piezo Transmitter Module)
	public static final byte ORG_1BS = 0x06; // 1 byte data telegram from STM (Solar Transmitter Module)
	public static final byte ORG_4BS = 0x07; // 4 byte data telegram from STM (Solar Transmitter Module)
	public static final byte ORG_HRC = 0x08; // Hand remote control
	
	public static final byte SET_IDBASE = 				0x18;
	public static final byte RD_IDBASE = 				0x58;
	public static final byte SET_RX_SENSITIVITY = 		0x08;
	public static final byte RD_RX_SENSITIVITY = 		0x48;
	public static final byte SLEEP = 					0x09;
	public static final byte WAKE = 					(byte)0xaa;
	public static final byte RESET = 					0x0a;
	public static final byte MODEM_ON = 				0x28;
	public static final byte MODEM_OFF = 				0x2a;
	public static final byte RD_MODEM_STATUS = 			0x68;
	public static final byte RD_SW_VERSION = 			0x4b;	
	
	private static final int SYNC_BYTE_0 = 	0;
	private static final int SYNC_BYTE_1 = 	1;
	private static final int H_SEQ_LENGTH =	2;
	private static final int ORG_BYTE =		3;
	private static final int DATA_BYTE_3 =	4;
	private static final int DATA_BYTE_2 =	5;
	private static final int DATA_BYTE_1 =	6;
	private static final int DATA_BYTE_0 =	7;
	private static final int ID_BYTE_3 =	8;
	private static final int ID_BYTE_2 =	9;
	private static final int ID_BYTE_1 =	10;
	private static final int ID_BYTE_0 = 	11;
	@SuppressWarnings("unused")
	private static final int STATUS =		12;
	private static final int CHECKSUM =		13;
	
	// ORG byte value signification for acknowledgment telegram
	public static final byte OK =						(byte) 0x58;
	public static final byte INF_INIT = 				(byte) 0x89;
	public static final byte ERR = 						(byte) 0x19;
	public static final byte INF_IDBASE = 				(byte) 0x98;
	public static final byte INF_RX_SENSITIVITY = 		(byte) 0x88;
	public static final byte INF_MODEM_STATUS = 		(byte) 0xa8;
	public static final byte INF_SW_VERSION = 			(byte) 0x8c;
	public static final byte ERR_MODEM_NOTWANTEDACK =	(byte) 0x28;
	public static final byte ERR_MODEM_NOTACK =			(byte) 0x29;
	public static final byte ERR_MODEM_DUP_ID =			(byte) 0x0c;
	public static final byte ERR_SYNTAX_H_SEQ =			(byte) 0x08;
	public static final byte ERR_SYNTAX_ORG =			(byte) 0x0b;
	public static final byte ERR_SYNTAX_LENGTH =		(byte) 0x09;
	public static final byte ERR_SYNTAX_CHECKSUM =		(byte) 0x0a;
	public static final byte ERR_TX_IDRANGE =			(byte) 0x22;
	public static final byte ERR_ID_RANGE =				(byte) 0x1a;
	
	
	
	public static byte[] createRawTransmitRadioTelegram(byte type, int id, byte[] data, byte status)
	{
		byte[] rawTelegram = null;
		switch (type)
		{
			case ORG_RPS:
			case ORG_1BS:
			case ORG_4BS:
			case ORG_HRC:
				if(data == null || data.length != 4) {
					Logger.debug(LC.gi(), null, "Invalid data !");
					return null;
				}
			
				rawTelegram = new byte[] {	
					(byte)0xa5, 0x5a, 0x6b, type, data[3], data[2], data[1], data[0], 
					(byte)((id >> 24) & 0xff), (byte)((id >> 16) & 0xff), (byte)((id >> 8) & 0xff), 
					(byte)(id & 0xff), status, 0x0
				};
				
				break;
			default :
				Logger.debug(LC.gi(), null, "Unsupported org type ("+Integer.toHexString(type)+").");
				return null;
		}
		
		rawTelegram[CHECKSUM] = computeChecksum(rawTelegram);
		
		if(LC.debug) {
			String s = "TRT : generated raw data are <";
			for(byte b : rawTelegram) {
				s += " "+Integer.toHexString(b & 0xff);
			}
			s += " >";
			Logger.debug(LC.gi(), null, s);
		}
		
		return rawTelegram;
	}
	
	@Deprecated
	public static byte[] createRawTransmitRadioTelegram(byte type, int id, int data, byte status)
	{
		byte[] dataBytes = {	(byte)(data & 0xff), (byte)((data >> 8) & 0xff), 
								(byte)((data >> 16) & 0xff), (byte)((data >> 24) & 0xff) };
		
		return createRawTransmitRadioTelegram(type, id, dataBytes, status);
	}	
	
	public static byte[] createRawTransmitCommandTelegram(byte command, byte[] data)
	{
		byte[] rawTelegram = {	(byte)0xa5, 0x5a, (byte)0xab, command, 
								0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0 };
		
		if(data != null && data.length == 4)
		{
			rawTelegram[4] = data[3];
			rawTelegram[5] = data[2];
			rawTelegram[6] = data[1];
			rawTelegram[7] = data[0];
		}
		
		rawTelegram[13] = computeChecksum(rawTelegram);
		
		if(LC.debug) {
			String s = "TCT : generated raw data are <";
			for(byte b : rawTelegram) {
				s += " "+Integer.toHexString(b & 0xff);
			}
			s += " >";
			Logger.debug(LC.gi(), null, s);
		}
		
		return rawTelegram;
	}
	
	
	
	public static byte[] createRawTransmitCommandTelegram(byte command, int data)
	{
		byte[] dataBytes = {	(byte)(data & 0xff), (byte)((data >> 8) & 0xff), 
								(byte)((data >> 16) & 0xff), (byte)((data >> 24) & 0xff) };
		
		return createRawTransmitCommandTelegram(command, dataBytes);
	}
	
	private final byte h_seq;
	private final byte org;
	private final EnoceanData data;
	private final int id;
	private final Date date;
	
	
	/** Create a EnoceanTelegram object from a raw Enocean telegram 
	 *	data. If raw_telegram does not contain a valid Enocean telegram, an
	 *	exception is raised.
	 *
	 * @param raw_telegram : a pointer onto a buffer that contains a 14 bytes raw 
	 *		enocean telegram. This buffer MUST contains at least 14 bytes.
	 *
	 * @throws IllegalArgumentException if raw_telegram is not an Enocean telegram
	 * 		or if its checksum is wrong. 
	 */
	public EnoceanESP2Telegram(byte[] rawTelegram) throws IllegalArgumentException
	{
		if(LC.debug) {
			String s = "EnoceanTelegram constructor: provided raw data are <";
			for(byte b : rawTelegram) {
				s += " "+Integer.toHexString(b & 0xff);
			}
			s += " >";
			Logger.debug(LC.gi(), this, s);
		}
		
		// Check sync bytes. MUST be equal to 0xA5 and 0x5A. See EnOcean specs.
		if(	rawTelegram.length == 14
			&& rawTelegram[SYNC_BYTE_0] == (byte) 0xA5
			&& rawTelegram[SYNC_BYTE_1] == (byte) 0x5A)
		{
			//  Check data length (Must be equal to 11). See EnOcean specs.
			if((rawTelegram[H_SEQ_LENGTH] & (byte) 0x1F) == (byte) 0x0B)
			{
				h_seq = (byte) (((rawTelegram[H_SEQ_LENGTH] & (byte) 0xE0) >> 5) & 0x7);
				
				// Check the telegram checksum.
				if(checkTelegramChecksum(rawTelegram))
				{
					date = new Date(); 
					
					org = rawTelegram[ORG_BYTE];
					
					id = ((rawTelegram[ID_BYTE_3] & 0xff) << 24)
						+ ((rawTelegram[ID_BYTE_2] & 0xff) << 16)
						+ ((rawTelegram[ID_BYTE_1] & 0xff) << 8)
						+ (rawTelegram[ID_BYTE_0] & 0xff);
					
					data = new EnoceanData(new byte[]{rawTelegram[DATA_BYTE_0], rawTelegram[DATA_BYTE_1], rawTelegram[DATA_BYTE_2], rawTelegram[DATA_BYTE_3]}, date);
					
					return;
				}
				else Logger.error(LC.gi(), this, "Constructor: failed on Checksum");
			}
			else Logger.error(LC.gi(), this, "Constructor: failed on H_SEQ bytes");
		}
		else
		{
			Logger.error(LC.gi(), this, "Constructor: failed on SYNC bytes or illegal telegram length");
		}
		
		throw new IllegalArgumentException();
	}
	
	/** 
	 * If this telegram is radio received telegram (RRT), return the type of the device which had emitted this telegram. 
	 * 
	 * @return the device type value : ORG_RPS, ORG_1BS, ORG_4BS or ORG_HRC.
	 */
	public byte getOrgID()
	{
		return org;
	}
	
	/** 
	 * If this telegram is an acknowledgment (ACK), return the error code. 
	 * 
	 * @return the device type value : OK, ERR, ERR_MODEM_NOTWANTEDACK, ERR_MODEM_NOTACK, ERR_MODEM_DUP_ID, ERR_SYNTAX_H_SEQ, ERR_SYNTAX_ORG
	 * 			 ERR_SYNTAX_LENGTH, ERR_SYNTAX_CHECKSUM, ERR_TX_IDRANGE or ERR_ID_RANGE.
	 */
	public byte getErrorID()
	{
		return org;
	}

	/** 
	 * Return the 32 bits id of the device which had emitted this telegram.
	 * 
	 * @return a 32 bits numerical ID of an Enocean device.
	 */
	public int getTransmitterId()
	{
		return id;
	}

	/**
	 * Return an object that contains the data part of an Enocean telegram.
	 * 
	 * @return The data part of the Enocean telegram
	 */
	public EnoceanData getData()
	{
		return data;
	}
	
	/**
	 * Return true if this telegram contains an error message. Error message 
	 * are sent by an Enocean transceiver (TCM120 or TCM320) it received from
	 * the Java code a invalid command telegram.
	 *  
	 * @return true if this telegram is an error telegram, false otherwise.
	 */
	public boolean isErrorTelegram()
	{
		switch(org)
		{
			case OK:
			case INF_INIT:
			case INF_IDBASE:
			case INF_RX_SENSITIVITY:
			case INF_MODEM_STATUS:
			case INF_SW_VERSION:
				return false;
			default:
				return true;
		}
	}
	
	/**
	 * Return true if this telegram is an acknowledgment telegram. These kinds of
	 * telegram are sent by an Enocean transceiver (TCM120 or TCM320) as a response
	 * for a command telegram sent from the Java code.
	 * 
	 * @return true if this telegram is an acknowledgment telegram, false if it is a
	 * telegram from an Enocean device.
	 */
	public boolean isAckTelegram()
	{
		return h_seq == 4;
	}
	
	/**
	 * Return the creation date of this telegram.
	 * @return
	 */
	public Date getDate()
	{
		return date;
	}
	
	public String toString()
	{
		String res = "";
		String id = Long.toHexString(this.getTransmitterId() & 0xFFFFFFFFl);
		byte[] d = data.getBytes();
		String dataString = "MSB [ " + Integer.toHexString(d[3] & 0xFF) + " " + Integer.toHexString(d[2] & 0xFF) + " " + Integer.toHexString(d[1] & 0xFF) + " " + Integer.toHexString(d[0] & 0xFF) + " ] LSB";
		
		
		switch(h_seq) {
			case 0: res = "RRT from " + id +", ";
			case 3: 
				if(h_seq == 3) res = "TRT by " + id + ", ";
				
				switch(org)
				{
					case ORG_RPS: res += "RPS "; break;
					case ORG_1BS: res += "1BS "; break;
					case ORG_4BS: res += "4BS "; break;
					case ORG_HRC: res += "HRC "; break;
				}
				
				res += dataString;
				
				break;
			case 4: res = "RMT message < ";
				switch(org)
				{
					case OK: res += "OK"; break;
					case INF_INIT: res += "INF_INIT"; break;
					case ERR: res += "ERR"; break;
					case INF_IDBASE: res += "INF_IDBASE " + dataString; break;
					case INF_RX_SENSITIVITY: res += "INF_RX_SENSITIVITY " + dataString; break;
					case INF_MODEM_STATUS: res += "INF_MODEM_STATUS " + dataString; break;
					case INF_SW_VERSION: res += "INF_SW_VERSION " + dataString; break;
					case ERR_MODEM_NOTWANTEDACK: res += "ERR_MODEM_NOTWANTEDACK "; break;
					case ERR_MODEM_NOTACK: res += "ERR_MODEM_NOTACK "; break;
					case ERR_MODEM_DUP_ID: res += "ERR_MODEM_DUP_ID "; break;
					case ERR_SYNTAX_H_SEQ: res += "ERR_SYNTAX_H_SEQ "; break;
					case ERR_SYNTAX_ORG: res += "ERR_SYNTAX_ORG "; break;
					case ERR_SYNTAX_LENGTH: res += "ERR_SYNTAX_LENGTH "; break;
					case ERR_SYNTAX_CHECKSUM: res += "ERR_SYNTAX_LENGTH "; break;
					case ERR_TX_IDRANGE: res += "ERR_SYNTAX_LENGTH "; break;
					case ERR_ID_RANGE: res += "ERR_ID_RANGE "; break;
				}
				res += " >";
				break;
			case 5: res = "TCT command "; break;
		}
		
		return res;
	}

	/** 
	 * Compute the telegram checksum and compare it with the expected one. Return 
	 *	true if checksum is correct, false otherwise.
	 *
	 * @param raw_telegram raw telegram data
	 *
	 *@return true if checksum is correct, false otherwise.
	 */
	private boolean checkTelegramChecksum(byte[] rawTelegram)
	{		
		return computeChecksum(rawTelegram) == rawTelegram[CHECKSUM];
	}
	
	private static byte computeChecksum(byte[] rawTelegram)
	{
		int sum = 0;
		
		// Sum all bytes except sync bytes and checksum
		for(int i = H_SEQ_LENGTH; i < CHECKSUM; i++)
		{
			sum += (int) rawTelegram[i];
		}
		
		return (byte)(sum & 0xff);
	}

	@Override
	public RORG getRorgID()
	{
		switch (org)
		{
			case ORG_RPS: 
				return RORG.RORG_RPS;
			case ORG_1BS: 
				return RORG.RORG_1BS;
			case ORG_4BS: 
				return RORG.RORG_4BS;
			case ORG_HRC:
				return RORG.RORG_HRC;
			default: 
				return RORG.UNDEFINED;
		}
	}
}
