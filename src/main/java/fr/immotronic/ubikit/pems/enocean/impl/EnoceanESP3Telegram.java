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

import java.util.Arrays;
import java.util.Date;

import org.ubikit.Logger;

public class EnoceanESP3Telegram extends EnoceanTelegram
{
	////////////////////
	/* Main Constants */
	////////////////////
	
	public static final int TELEGRAM_MAX_SIZE = 65535;
	public static final int TELEGRAM_STANDARD_SIZE = 24; // size of 4BS telegram
	public static final int SERIAL_SYNC_LENGTH = 6; // size in bytes of the synchronization part of the message
	public static final int HEADER_FIRST_BYTE = 1; // index of the first significant byte of header in a telegram
	public static final int HEADER_LAST_BYTE = 4; // index of the last significant byte of header in a telegram
	public static final int DATA_FIRST_BYTE = 6 ; // index of the first byte of data in a telegram

	/////////////////////////////////////////
	/* Constants for Packet Type 1 : RADIO */
	/////////////////////////////////////////
	
	public static final byte ORG_RPS = (byte) 0xf6; // telegram from PTM (Piezo Transmitter Module)
	public static final byte ORG_1BS = (byte) 0xd5; // 1 byte data telegram from STM (Solar Transmitter Module)
	public static final byte ORG_4BS = (byte) 0xa5; // 4 byte data telegram from STM (Solar Transmitter Module)
	public static final byte ORG_UTE = (byte) 0xd4; // Universal Uni-and Bidirectional Teach-in
	public static final byte ORG_VLD = (byte) 0xd2; // Variable Length Data telegram
	
	public static final int CHOICE_BYTE = 0;
	public static final int DBM_BYTE = 5;
	public static final int ENCRYPTION_BYTE = 6;
	
	////////////////////////////////////////////
	/* Constants for Packet Type 2 : RESPONSE */
	////////////////////////////////////////////
	
	public static final byte RET_OK = 0;
	public static final byte RET_ERROR = 1;
	public static final byte RET_NOT_SUPPORTED = 2;
	public static final byte RET_WRONG_PARAM = 3;
	public static final byte RET_OPERATION_DENIED = 4;
	public static final byte FLASH_HW_ERROR = (byte) 0x82;
	public static final byte BASEID_OUT_OF_RANGE = (byte) 0x90;
	public static final byte BASEID_MAX_REACHED = (byte) 0x91;
	
	public static final int RETURN_CODE_BYTE = 0;
	
	/////////////////////////////////////////
	/* Constants for Packet Type 4 : EVENT */
	/////////////////////////////////////////
	
	//public static final byte SA_RECLAIM_NO_SUCCESSFUL = 1;  // not supported by TCM310 module
	public static final byte SA_CONFIRM_LEARN = 2;
	//public static final byte SA_LEARN_ACK = 3;  // not supported by TCM310 module
	public static final byte CO_READY = 4;
	
	//////////////////////////////////////////////////
	/* Constants for Packet Type 5 : COMMON_COMMAND */
	//////////////////////////////////////////////////
	
	/* Commented commands codes are not yet used in this implementation */
	
	//public static final byte CO_WR_SLEEP = 1;
	//public static final byte CO_WR_RESET = 2;
	//public static final byte CO_RD_VERSION = 3;
	//public static final byte CO_RD_SYS_LOG = 4;
	//public static final byte CO_WR_SYS_LOG = 5;
	//public static final byte CO_WR_BIST = 6;
	public static final byte CO_WR_IDBASE = 7;
	public static final byte CO_RD_IDBASE = 8;
	//public static final byte CO_WR_REPEATER = 9;
	//public static final byte CO_RD_REPEATER = 10;
	//public static final byte CO_WR_FILTER_ADD = 11;
	//public static final byte CO_WR_FILTER_DEL = 12;
	//public static final byte CO_WR_FILTER_DEL_ALL = 13;
	//public static final byte CO_WR_FILTER_ENABLE = 14;
	//public static final byte CO_RD_FILTER = 15;
	//public static final byte CO_WR_WAIT_MATURITY = 16;
	//public static final byte CO_WR_SUBTEL = 17; // not supported by TCM310 module
	//public static final byte CO_WR_MEM = 18;
	//public static final byte CO_RD_MEM = 19;
	//public static final byte CO_RD_MEM_ADDRESS = 20;
	//public static final byte CO_RD_SECURITY = 21; // not supported by TCM310 module
	//public static final byte CO_WR_SECURITY = 22; // not supported by TCM310 module
	
	/////////////////////////////////////////////////////
	/* Constants for Packet Type 6 : SMART_ACK_COMMAND */
	/////////////////////////////////////////////////////
	
	public static final byte SA_WR_LEARNMODE = 1;
	public static final byte SA_RD_LEARNMODE = 2;
	public static final byte SA_WR_LEARNCONFIRM = 3;
	//public static final byte SA_WR_CLIENTLEARNRQ = 4; // not supported by TCM310 module
	public static final byte SA_WR_RESET = 5;
	public static final byte SA_RD_LEARNEDCLIENTD = 6;
	//public static final byte SA_WR_RECLAIMS = 7;  // not supported by TCM310 module
	public static final byte SA_WR_POSTMASTER = 8;	
	
	//////////////////////////////
	/* Other Constants and Enum */
	//////////////////////////////
	
	public static final int RESPONSE_TIMEOUT = 1000; // in ms
	
	public static final byte[] CRC8Table = {
		(byte) 0x00, (byte) 0x07, (byte) 0x0e, (byte) 0x09, (byte) 0x1c, (byte) 0x1b, (byte) 0x12, (byte) 0x15, 
		(byte) 0x38, (byte) 0x3f, (byte) 0x36, (byte) 0x31, (byte) 0x24, (byte) 0x23, (byte) 0x2a, (byte) 0x2d,  
		(byte) 0x70, (byte) 0x77, (byte) 0x7e, (byte) 0x79, (byte) 0x6c, (byte) 0x6b, (byte) 0x62, (byte) 0x65,
		(byte) 0x48, (byte) 0x4f, (byte) 0x46, (byte) 0x41, (byte) 0x54, (byte) 0x53, (byte) 0x5a, (byte) 0x5d,
		(byte) 0xe0, (byte) 0xe7, (byte) 0xee, (byte) 0xe9, (byte) 0xfc, (byte) 0xfb, (byte) 0xf2, (byte) 0xf5,
		(byte) 0xd8, (byte) 0xdf, (byte) 0xd6, (byte) 0xd1, (byte) 0xc4, (byte) 0xc3, (byte) 0xca, (byte) 0xcd,
		(byte) 0x90, (byte) 0x97, (byte) 0x9e, (byte) 0x99, (byte) 0x8c, (byte) 0x8b, (byte) 0x82, (byte) 0x85,
		(byte) 0xa8, (byte) 0xaf, (byte) 0xa6, (byte) 0xa1, (byte) 0xb4, (byte) 0xb3, (byte) 0xba, (byte) 0xbd,
		(byte) 0xc7, (byte) 0xc0, (byte) 0xc9, (byte) 0xce, (byte) 0xdb, (byte) 0xdc, (byte) 0xd5, (byte) 0xd2,
		(byte) 0xff, (byte) 0xf8, (byte) 0xf1, (byte) 0xf6, (byte) 0xe3, (byte) 0xe4, (byte) 0xed, (byte) 0xea,
		(byte) 0xb7, (byte) 0xb0, (byte) 0xb9, (byte) 0xbe, (byte) 0xab, (byte) 0xac, (byte) 0xa5, (byte) 0xa2,
		(byte) 0x8f, (byte) 0x88, (byte) 0x81, (byte) 0x86, (byte) 0x93, (byte) 0x94, (byte) 0x9d, (byte) 0x9a,
		(byte) 0x27, (byte) 0x20, (byte) 0x29, (byte) 0x2e, (byte) 0x3b, (byte) 0x3c, (byte) 0x35, (byte) 0x32,
		(byte) 0x1f, (byte) 0x18, (byte) 0x11, (byte) 0x16, (byte) 0x03, (byte) 0x04, (byte) 0x0d, (byte) 0x0a,
		(byte) 0x57, (byte) 0x50, (byte) 0x59, (byte) 0x5e, (byte) 0x4b, (byte) 0x4c, (byte) 0x45, (byte) 0x42,
		(byte) 0x6f, (byte) 0x68, (byte) 0x61, (byte) 0x66, (byte) 0x73, (byte) 0x74, (byte) 0x7d, (byte) 0x7a,
		(byte) 0x89, (byte) 0x8e, (byte) 0x87, (byte) 0x80, (byte) 0x95, (byte) 0x92, (byte) 0x9b, (byte) 0x9c,
		(byte) 0xb1, (byte) 0xb6, (byte) 0xbf, (byte) 0xb8, (byte) 0xad, (byte) 0xaa, (byte) 0xa3, (byte) 0xa4,
		(byte) 0xf9, (byte) 0xfe, (byte) 0xf7, (byte) 0xf0, (byte) 0xe5, (byte) 0xe2, (byte) 0xeb, (byte) 0xec,
		(byte) 0xc1, (byte) 0xc6, (byte) 0xcf, (byte) 0xc8, (byte) 0xdd, (byte) 0xda, (byte) 0xd3, (byte) 0xd4,
		(byte) 0x69, (byte) 0x6e, (byte) 0x67, (byte) 0x60, (byte) 0x75, (byte) 0x72, (byte) 0x7b, (byte) 0x7c,
		(byte) 0x51, (byte) 0x56, (byte) 0x5f, (byte) 0x58, (byte) 0x4d, (byte) 0x4a, (byte) 0x43, (byte) 0x44,
		(byte) 0x19, (byte) 0x1e, (byte) 0x17, (byte) 0x10, (byte) 0x05, (byte) 0x02, (byte) 0x0b, (byte) 0x0c,
		(byte) 0x21, (byte) 0x26, (byte) 0x2f, (byte) 0x28, (byte) 0x3d, (byte) 0x3a, (byte) 0x33, (byte) 0x34,
		(byte) 0x4e, (byte) 0x49, (byte) 0x40, (byte) 0x47, (byte) 0x52, (byte) 0x55, (byte) 0x5c, (byte) 0x5b,
		(byte) 0x76, (byte) 0x71, (byte) 0x78, (byte) 0x7f, (byte) 0x6A, (byte) 0x6d, (byte) 0x64, (byte) 0x63,
		(byte) 0x3e, (byte) 0x39, (byte) 0x30, (byte) 0x37, (byte) 0x22, (byte) 0x25, (byte) 0x2c, (byte) 0x2b,
		(byte) 0x06, (byte) 0x01, (byte) 0x08, (byte) 0x0f, (byte) 0x1a, (byte) 0x1d, (byte) 0x14, (byte) 0x13,
		(byte) 0xae, (byte) 0xa9, (byte) 0xa0, (byte) 0xa7, (byte) 0xb2, (byte) 0xb5, (byte) 0xbc, (byte) 0xbb,
		(byte) 0x96, (byte) 0x91, (byte) 0x98, (byte) 0x9f, (byte) 0x8a, (byte) 0x8D, (byte) 0x84, (byte) 0x83,
		(byte) 0xde, (byte) 0xd9, (byte) 0xd0, (byte) 0xd7, (byte) 0xc2, (byte) 0xc5, (byte) 0xcc, (byte) 0xcb,
		(byte) 0xe6, (byte) 0xe1, (byte) 0xe8, (byte) 0xef, (byte) 0xfa, (byte) 0xfd, (byte) 0xf4, (byte) 0xf3  
	};
	
	public enum ESP3PacketType
	{
		RADIO((byte) 0x1),
		RESPONSE((byte) 0x2),
		// RADIO_SUB_TEL((byte) 0x3),   // not used, functionality internal to EnOcean
		EVENT((byte) 0x4),
		COMMON_COMMAND((byte) 0x5),
		SMART_ACK_COMMAND((byte) 0x6),
		REMOTE_MAN_COMMAND((byte) 0x7),
		NOT_SUPPORTED((byte) 0xff);
		
		public byte value;
		
		private ESP3PacketType(byte value)
		{
			this.value = value;
		}
		
		public static ESP3PacketType getValueOf(byte value)
		{
			switch (value) {
				case 1 : return RADIO;
				case 2 : return RESPONSE;
				// case 3 : return RADIO_SUB_TEL;  // not used, functionality internal to EnOcean
				case 4 : return EVENT;
				case 5 : return COMMON_COMMAND;
				case 6 : return SMART_ACK_COMMAND;
				case 7 : return REMOTE_MAN_COMMAND;
				default : return NOT_SUPPORTED;
			}
		}
	}

	///////////////
	/* Body Part */
	///////////////
	
	private final byte[] header;
	private final byte[] data;
	private final byte[] optionalData;
	private final byte CRC8D;
	
	private final ESP3PacketType packetType;
	
	/* Fields for Packet Type 1 : RADIO  */
	private byte radioType;
	private EnoceanData userData;
	private int id;
	private Date date;
	
	private int rssi = Integer.MIN_VALUE;
	private int encryptionLevel = Integer.MIN_VALUE;
	
	/* Fields for Packet Type 2 (RESPONSE) & 4 (EVENT) */
	private byte packetCode; // corresponds to Return Code for RESPONSE packet & Event Code for EVENT packet
	
	public EnoceanESP3Telegram(byte[] header, byte[] dataBytes, byte CRC8D) throws IllegalArgumentException
	{
		if(LC.debug) {
			String s = "EnoceanESP3Telegram constructor: provided raw data are <";
			for(byte b : header) {
				s += " "+Integer.toHexString(b & 0xff);
			}
			s += " |";
			for(byte b : dataBytes) {
				s += " "+Integer.toHexString(b & 0xff);
			}
			s += " >";
			Logger.debug(LC.gi(), this, s);
		}
		
		// Check sync byte. MUST be equal to 0x55. See EnOcean specs.
		if(header.length == 6 && header[0] == 0x55)
		{
			// Check CRC8H
			if (computeCRC8(header, EnoceanESP3Telegram.HEADER_FIRST_BYTE, EnoceanESP3Telegram.HEADER_LAST_BYTE) == header[SERIAL_SYNC_LENGTH-1])
			{
				int dataLength = (((header[1] << 8) + header[2]) & 0xffff);
				int optionalLength = header[3];
					
				//  Check data length (can't have telegram with no data). See EnOcean specs.
				if (dataLength + optionalLength != 0)
				{
					// Check data length
					if (dataBytes.length == dataLength + optionalLength)
					{
						// Check CRC8D
						if (computeCRC8(dataBytes, dataLength + optionalLength) == CRC8D)
						{
							// Here we know that we have a correct Enocean telegram.
							if(LC.debug) {
								Logger.debug(LC.gi(), this, "Correct Enocean telegram: start decoding.");
							}
							
							this.header = Arrays.copyOf(header, header.length);
							this.data = Arrays.copyOf(dataBytes, dataLength);
							this.optionalData = Arrays.copyOfRange(dataBytes, dataLength, dataLength + optionalLength);
							this.packetType = ESP3PacketType.getValueOf(header[4]);
							this.CRC8D = CRC8D;
							
							switch(packetType)
							{
								case RADIO:
									switch (data[CHOICE_BYTE]) // Choice byte : define Radio type (4BS, 1BS, RPS, VLD...)
									{
										case ORG_RPS:
										case ORG_1BS:
											date = new Date(); 
											
											radioType = data[0];
											
											id = getIntValue(data[2], data[3], data[4], data[5]);
											
											userData = new EnoceanData(new byte[]{0x00, 0x00, 0x00, data[1]}, date);
											
											break;
										case ORG_4BS:
											date = new Date(); 
											
											radioType = data[0];
											
											id = getIntValue(data[5], data[6], data[7], data[8]);
											
											userData = new EnoceanData(new byte[]{data[4], data[3], data[2], data[1]}, date);
											
											break;
										case ORG_UTE:
											date = new Date();
											
											radioType = data[0];
											
											id = getIntValue(data[8], data[9], data[10], data[11]);
											
											userData = new EnoceanData(new byte[]{data[7], data[6], data[5], data[4], data[3], data[2], data[1]}, date);
											
											break;
										case ORG_VLD:
											date = new Date();
											
											radioType = data[0];
											
											int userDataLength = data.length - 6;
											int senderIdByte = userDataLength + 1;
											
											id = getIntValue(data[senderIdByte], data[senderIdByte+1], data[senderIdByte+2], data[senderIdByte+3]);
											
											byte[] userDataBytes = new byte[userDataLength];
											for (int i = 0; i < userDataLength; i++)
												userDataBytes[i] = data[userDataLength-i];
											userData = new EnoceanData(userDataBytes , date);
											
											break;
										default:
											Logger.warn(LC.gi(), this, "Constructor: choice field not supported ("+Integer.toHexString(data[0] & 0xff)+").");
											throw new IllegalArgumentException();
									}
									if (optionalData.length == 7)
									{
										rssi = optionalData[DBM_BYTE];
										encryptionLevel = optionalData[ENCRYPTION_BYTE];							
									}
									break;
								
								case RESPONSE:
								case EVENT:
									packetCode = data[RETURN_CODE_BYTE];
									break;								
									
								case REMOTE_MAN_COMMAND:
									Logger.warn(LC.gi(), this, "Constructor: Packet type "+packetType+" is not yet supported.");
									break;
									
								case COMMON_COMMAND:
								case SMART_ACK_COMMAND:
									Logger.warn(LC.gi(), this, "Constructor: We should not received packets of type "+packetType+". Weird.");
									throw new IllegalArgumentException();
								case NOT_SUPPORTED:
									Logger.warn(LC.gi(), this, "Constructor: Unsupported packet type (value: "+Integer.toHexString(header[4] & 0xff)+").");
									throw new IllegalArgumentException();
							}
							if(LC.debug) {
								Logger.debug(LC.gi(), this, "Correct Enocean telegram: decoding succeeds.");						
							}
							return;
						}
						else {
							Logger.error(LC.gi(), this, "Constructor: failed on CRC8D verification.");
						}
					}
					else {
						Logger.error(LC.gi(), this, "Constructor: lengths fields does not correspond to the data size. Failed.");
					}
				}
				else {
					Logger.error(LC.gi(), this, "Constructor: lengths fields are null. Failed.");
				}
			}
			else {
				Logger.error(LC.gi(), this, "Constructor: failed on CRC8H verification.");
			}
		}
		else {
			Logger.error(LC.gi(), this, "Constructor: failed on sync byte (0x55) verification.");
		}
		
		throw new IllegalArgumentException();
	}
	
	private static byte[] addAll(byte[] array1, byte[] array2) 
	{
		if (array1 == null && array2 == null) return null;
		if (array1 == null) return array2.clone();
		if (array2 == null) return array1.clone();
	      
		byte[] joinedArray = new byte[array1.length + array2.length];

		System.arraycopy(array1, 0, joinedArray, 0, array1.length);
		System.arraycopy(array2, 0, joinedArray, array1.length, array2.length);
		return joinedArray;
	}
	
	public static byte[] createRawTransmitRadioTelegram(byte choice, int senderId, byte[] data, byte status, int destinationID)
	{
		byte[] rawTelegram = null;
		switch (choice)
		{
			case ORG_RPS:	
			case ORG_1BS:
				if(data == null || data.length != 4) {
					Logger.debug(LC.gi(), null, "Invalid data !");
					return null;
				}
				
				rawTelegram = new byte[] {
					0x55, 0x00, 0x07, 0x07, ESP3PacketType.RADIO.value, 0x00, /* Header */
					choice, data[3], /* Data : choice byte + data */
					(byte)((senderId >> 24) & 0xff), (byte)((senderId >> 16) & 0xff), (byte)((senderId >> 8) & 0xff), (byte)(senderId & 0xff), status, /* Data : senderID + status */
					0x03, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x0, 0x0 /* Optional Data (see enocean specs) and CRC byte */
				};
				break;				
			case ORG_4BS:
				if(data == null || data.length != 4) {
					Logger.debug(LC.gi(), null, "Invalid data !");
					return null;
				}
				
				rawTelegram = new byte[] {
					0x55, 0x00, 0x0a, 0x07, ESP3PacketType.RADIO.value, 0x00, /* Header */
					choice, data[3], data[2], data[1], data[0], /* Data : choice byte + data */
					(byte)((senderId >> 24) & 0xff), (byte)((senderId >> 16) & 0xff), (byte)((senderId >> 8) & 0xff), (byte)(senderId & 0xff), status, /* Data : senderID + status */
					0x03, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x0, 0x0 /* Optional Data (see enocean specs) and CRC byte */
				};
				
				break;
			case ORG_UTE:
				if(data == null || data.length != 7) {
					Logger.debug(LC.gi(), null, "Invalid data !");
					return null;
				}
					
				rawTelegram = new byte[] {	
					0x55, 0x00, 0x0d, 0x07, ESP3PacketType.RADIO.value, 0x00, /* Header */
					choice, data[6], data[5], data[4], data[3], data[2], data[1], data[0], /* Data : choice byte + data */
					(byte)((senderId >> 24) & 0xff), (byte)((senderId >> 16) & 0xff), (byte)((senderId >> 8) & 0xff), (byte)(senderId & 0xff), status, /* Data : senderID + status */
					0x03, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff, 0x0, 0x0 /* Optional Data (see enocean specs) and CRC byte */
				};
				
				break;
			case ORG_VLD:
				int dataLength = data.length + 6;
				
				if(data == null || dataLength > 0xffff) {
					Logger.debug(LC.gi(), null, "Invalid data !");
					return null;
				}
				
				byte dalaLengthHigh = (byte) ((dataLength >> 8) & 0xff);
				byte dalaLengthLow = (byte) (dataLength & 0xff);
				
				byte[] beginningOfTelegram = new byte[] { 
					0x55, dalaLengthHigh, dalaLengthLow, 0x07, ESP3PacketType.RADIO.value, 0x00, /* Header */
					choice /* Choice byte */
				};
				byte[] endOfTelegram =  new byte[] { 
					(byte)((senderId >> 24) & 0xff), (byte)((senderId >> 16) & 0xff), (byte)((senderId >> 8) & 0xff), (byte)(senderId & 0xff), status, /* Data : senderID + status */
					0x03, (byte)((destinationID >> 24) & 0xff), (byte)((destinationID >> 16) & 0xff), (byte)((destinationID >> 8) & 0xff), (byte)(destinationID & 0xff), (byte) 0xff, 0x0, 0x0 /* Optional Data (see enocean specs) and CRC byte */ 
				};
				
				/* Building telegram */
				rawTelegram = addAll(beginningOfTelegram, data);
				rawTelegram = addAll(rawTelegram, endOfTelegram);
				
				break;
			default :
				Logger.debug(LC.gi(), null, "Unsupported radio type ("+Integer.toHexString(choice)+").");
				return null;
		}
		
		// Here we have a correct telegram, we just have to compute CRC8.
		rawTelegram[SERIAL_SYNC_LENGTH-1] = computeCRC8(rawTelegram, HEADER_FIRST_BYTE, HEADER_LAST_BYTE);
		rawTelegram[rawTelegram.length-1] = computeCRC8(rawTelegram, DATA_FIRST_BYTE, rawTelegram.length-2);
		
		if (LC.debug) {
			String s = "RADIO TELEGRAM : generated raw data are < ";
			for(byte b : rawTelegram) {
				s += " "+Integer.toHexString(b & 0xff);
			}
			s += " >";
			Logger.debug(LC.gi(), null, s);
		}
		
		return rawTelegram;
	}
	
	public static byte[] createRawTransmitResponseTelegram(byte returnCode, byte[] responseData)
	{
		return createRawTransmitGenericTelegram(ESP3PacketType.RESPONSE, returnCode, responseData, null);
	}
	
	public static byte[] createRawTransmitCommandTelegram(byte commonCommandCode, byte[] commonCommandData, byte optionalData[])
	{
		return createRawTransmitGenericTelegram(ESP3PacketType.COMMON_COMMAND, commonCommandCode, commonCommandData, optionalData);
	}
	
	public static byte[] createRawTransmitSmartAckCommandTelegram(byte smartAckCode, byte[] smartAckData)
	{
		return createRawTransmitGenericTelegram(ESP3PacketType.SMART_ACK_COMMAND, smartAckCode, smartAckData, null);
	}
	
	private static byte[] createRawTransmitGenericTelegram(ESP3PacketType packetType, byte code, byte[] data, byte optionalData[])
	{
		int dataLength = 1;
		byte optionalDataLength = 0;
		if (data != null)
			dataLength += data.length;
		if (optionalData != null)
			optionalDataLength += optionalData.length;
		
		byte[] rawTelegram = new byte[SERIAL_SYNC_LENGTH+dataLength+optionalDataLength+1];
		/* Filling the header */
		rawTelegram[0] = 0x55;
		rawTelegram[1] = (byte) ((dataLength >> 8) & 0xff);
		rawTelegram[2] = (byte) (dataLength & 0xff);
		rawTelegram[3] = optionalDataLength;
		rawTelegram[4] = packetType.value;
		
		/* Compute CRC8H */
		rawTelegram[5] = computeCRC8(rawTelegram, HEADER_FIRST_BYTE, HEADER_LAST_BYTE);
		
		/* Filling Data */
		rawTelegram[DATA_FIRST_BYTE] = code;
		if (data != null)
		{
			for (int i = 0; i < data.length; i++)
			{
				rawTelegram[DATA_FIRST_BYTE+1+i] = data[i];
			}
		}
		if (optionalData != null)
		{
			for (int i = 0; i < optionalData.length; i++)
			{
				rawTelegram[DATA_FIRST_BYTE+dataLength+i] = optionalData[i];
			}
		} 
		
		/* Compute CRC8D */
		rawTelegram[SERIAL_SYNC_LENGTH+dataLength+optionalDataLength] = computeCRC8(rawTelegram, DATA_FIRST_BYTE, SERIAL_SYNC_LENGTH+dataLength+optionalDataLength-1);
		
		if (LC.debug) {
			String s = packetType.name()+" : generated raw data are <";
			for(byte b : rawTelegram) {
				s += " "+Integer.toHexString(b & 0xff);
			}
			s += " >";
			Logger.debug(LC.gi(), null, s);
		}
		
		return rawTelegram;
	}
	
	/* Global methods */
	
	public ESP3PacketType getPacketType()
	{
		return packetType;
	}
	
	public byte[] getRawData()
	{
		return data;
	}
	
	public byte[] getRawOptionalData()
	{
		return optionalData;
	}
	
	/* Methods for Packet Type 1 (RADIO) */
	
	public byte getRadioType()
	{
		return radioType;
	}
	
	@Override
	public RORG getRorgID()
	{
		switch(radioType)
		{
			case ORG_RPS: 
				return RORG.RORG_RPS;
			case ORG_1BS: 
				return RORG.RORG_1BS;
			case ORG_4BS: 
				return RORG.RORG_4BS;
			case ORG_UTE: 
				return RORG.RORG_UTE;
			case ORG_VLD : 
				return RORG.RORG_VLD;
			default: 
				return RORG.UNDEFINED;
		}
	}
	
	@Override
	public EnoceanData getData()
	{
		return userData;
	}
	
	@Override
	public int getTransmitterId()
	{
		return id;
	}
	
	/**
	 * Returns RSSI value in dBm.
	 * @return RSSI value in dBm.
	 */
	public int getRSSI()
	{
		return - (rssi & 0xff);
	}
	
	public int getEncryptionLevel()
	{
		return (encryptionLevel & 0xff);
	}
	
	/* Methods for Packet Type 2 (RESPONSE) */
	
	public byte getResponseCode()
	{
		return packetCode;
	}
	
	/* Methods for Packet Type 4 (EVENT) */
	
	public byte getEventCode()
	{
		return packetCode;
	}
	
	/**
     * Calculate the CRC value with data from a byte array.
     * @param data  The byte array
     * @param len   Data length (could be lower to byte array length) 
     * @return      The calculated CRC value
     */
	public static byte computeCRC8(byte[] data, int len)
	{
        byte crc = 0;

        for (int i = 0; i < len; i++) {
            crc = CRC8Table[(crc ^ data[i]) & 0xff];
        }

        return crc;
    }
	
	/**
	 * Calculate the CRC value between 2 index with data from a byte array.
	 * @param data	The byte array
	 * @param begin Index where CRC calculation begins (included).
	 * @param end   Index where CRC calculation stops (included).
	 * @returnThe	The calculated CRC value
	 */
	public static byte computeCRC8(byte[] data, int begin, int end)
	{
        byte crc = 0;

        for (int i = begin; i <= end; i++) {
            crc = CRC8Table[(crc ^ data[i]) & 0xff];
        }

        return crc;
    }
	
	public static int getIntValue(byte MSB, byte byte2, byte byte3, byte LSB)
	{
		return ((MSB & 0xff) << 24) + ((byte2 & 0xff) << 16) + ((byte3 & 0xff) << 8) + (LSB & 0xff);
	}
	
	public String toReadableBytes()
	{
		StringBuilder sb = new StringBuilder();

		for(int i = 0; i < header.length; i++) {
			sb.append(Integer.toHexString(header[i] & 0xff));
			sb.append(" ");
			if (i == 0 | i == 4 | i == 5)
				sb.append("| ");
		}
		for(byte b : data) {
			sb.append(Integer.toHexString(b & 0xff));
			sb.append(" ");
		}
		for(byte b : optionalData) {
			sb.append(Integer.toHexString(b & 0xff));
			sb.append(" ");
		}
		sb.append("| "+Integer.toHexString(CRC8D  & 0xff));
		return sb.toString();
	}
	
	public String toString()
	{
		String res = "";
		
		switch (getPacketType())
		{
			case RADIO:
				res += "FROM "+Integer.toHexString(getTransmitterId());
				String dataString = "";
				byte[] d = userData.getBytes();
				switch(getRadioType())
				{
					case ORG_RPS : 
						res += ", RPS ";
						dataString = "[ "+Integer.toHexString(d[3])+" ]";
						break;
					case ORG_1BS : 
						res += ", 1BS ";
						dataString = "[ " +Integer.toHexString(d[3])+" ]";
						break;
					case ORG_4BS : 
						res += ", 4BS "; 
						dataString = "[ " + Integer.toHexString(d[3] & 0xFF) + " " + Integer.toHexString(d[2] & 0xFF) + " " + Integer.toHexString(d[1] & 0xFF) + " " + Integer.toHexString(d[0] & 0xFF) + " ]";
						break;
					case ORG_UTE: 
						res += ", UTE "; 
						dataString = "[ " + Integer.toHexString(d[6] & 0xFF) + " " + Integer.toHexString(d[5] & 0xFF) + " " + Integer.toHexString(d[4] & 0xFF) + " " + 
								Integer.toHexString(d[3] & 0xFF) + " " + Integer.toHexString(d[2] & 0xFF) + " " + Integer.toHexString(d[1] & 0xFF) + " " + Integer.toHexString(d[0] & 0xFF) + " ]";
						break;
					case ORG_VLD : 
						res += ", VLD ";
						dataString = "[ ";
						for (int i = d.length-1; i >= 0; i--)
							dataString += Integer.toHexString(d[i] & 0xFF) + " ";
						dataString += "]";
						break;
				}
				res += dataString;
				res += " ("+getRSSI()+" dBm)";
				break;
				
			case RESPONSE:
				res+= "RESPONSE message < ";
				switch(getResponseCode())
				{
					case RET_OK: res+= "OK"; break;
					case RET_ERROR: res+= "ERROR"; break;
					case RET_NOT_SUPPORTED: res+= "NOT_SUPPORTED"; break;
					case RET_WRONG_PARAM: res+= "WRONG_PARAM"; break;
					case RET_OPERATION_DENIED: res+= "OPERATION_DENIED"; break;
				}
				res += " >";
				break;
			case EVENT:
				res+= "EVENT message < ";
				switch(getEventCode())
				{
					case SA_CONFIRM_LEARN: res+= "SA_CONFIRM_LEARN"; break;
					case CO_READY: res+= "SA_CONFIRM_LEARN"; break;
				}
				res += " >";
				break;
			default:
				res += "??? < TODO: handler for that kind of message: "+getPacketType()+" >";
				break;
		}
		return res;
	}
	
}
 