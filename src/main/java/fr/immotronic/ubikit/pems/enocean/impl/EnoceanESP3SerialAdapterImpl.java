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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Vector;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.ubikit.Logger;
import org.ubikit.event.EventGate;
import org.ubikit.pem.event.HardwareLinkStatusEvent;
import org.ubikit.service.PhysicalEnvironmentModelService.HardwareLinkStatus;

import fr.immotronic.rxtx.SerialPort;
import fr.immotronic.rxtx.SerialPortConstants;
import fr.immotronic.rxtx.SerialPortFactory;
import fr.immotronic.rxtx.SerialPortListener;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanTCMManager.TCMSettingResponse;

/**
 * Serial Adapter for EnOcean Serial Protocol 3.0 (ESP3). The ESP3 defines the 
 * serial communication between a host and EnOcean modules.
 * 
 * @author Kevin Planchet
 */
public class EnoceanESP3SerialAdapterImpl implements SerialPortListener, EnoceanSerialAdapter 
{	
	private SerialPort serialPort;
	private boolean connected;
	private String portName;
	
	private int enoceanTranceiverID;
	
	private final ByteBuffer buffer;
	int dataLength = 0;
	int optionalLength = 0;
	byte[] serialSynchronizationBytes = null;
	
	private final BlockingQueue<EnoceanTelegram> receivedTelegrams;
	private final BlockingQueue<EnoceanESP3Telegram> responseTelegrams;
	private final CountDownLatch connectionLatch;
	private final DiagnosticManager diagnosticManager;
	private final TCMConnectionTask tcmConnectionTask;
	private final EventGate higherLevelEventGate;
	
	private boolean tcmOnUSB;
	
	public enum GetPacketState {
		// Waiting for the synchronization byte 0x55
		GET_SYNC_STATE,
		// Copying the 4 after sync byte: raw data length (2 bytes), optional data length (1), type (1).
		GET_HEADER_STATE,
		// Checking the header CRC8 checksum. Re-synchronization test is also done here.
		CHECK_CRC8H_STATE,
		// Copying the data and optional data bytes to the packet buffer.
		GET_DATA_STATE,
		// Checking the info CRC8 checksum.
		CHECK_CRC8D_STATE,
	}
	
	private GetPacketState getPacketState = GetPacketState.GET_SYNC_STATE;
	
	private class TCMConnectionTask implements Runnable
	{
		private ScheduledExecutorService executor;
		
		private TCMConnectionTask(ScheduledExecutorService executor)
		{
			this.executor = executor;
		}
		
		public void start()
		{
			try 
			{
				executor.schedule(this, 1, TimeUnit.SECONDS);
			}
			catch(Exception e)
			{
				Logger.error(LC.gi(), this, "While launching the TCM connection task.", e);
			}
		}
		
		@Override
		public void run() 
		{
			// Now, all is ready to start listening telegram from Enocean devices
			
				if(connect()) // Try to open the serial link with the transceiver.
				{
					if(LC.debug) {
						Logger.debug(LC.gi(), this, "Connected on port "+getPortName());
					}
					
					/*if(pemStatus != Status.INVALID_LICENSE) {
						pemStatus = Status.RUNNING;
					}*/
				}
				else
				{
					Logger.error(LC.gi(), this, "Serial adapter connection failed. No Enocean tranceiver was found.");
					//pemStatus = Status.NO_TCM_DETECTED;
				}
		}
	}
	
	public EnoceanESP3SerialAdapterImpl(boolean tcmOnUSB, DiagnosticManager diagnosticManager, ScheduledExecutorService executor, EventGate higherLevelEventGate)
	{
		this.tcmOnUSB = tcmOnUSB;
		this.diagnosticManager = diagnosticManager;
		this.higherLevelEventGate = higherLevelEventGate;
		
		tcmConnectionTask = new TCMConnectionTask(executor);
		buffer = ByteBuffer.allocate(EnoceanESP3Telegram.TELEGRAM_STANDARD_SIZE * 100);
		
		receivedTelegrams = new ArrayBlockingQueue<EnoceanTelegram>(10);
		responseTelegrams = new ArrayBlockingQueue<EnoceanESP3Telegram>(1);
		
		connectionLatch = new CountDownLatch(1);
		
		connected = false;
		portName = null;
		serialPort = null;
		
		tcmConnectionTask.start();
	}
	
	public ESP getSupportedESP()
	{
		return ESP.ESP3;
	}
	
	private boolean connect()
	{
		int attemps = 0;
		while(!connected/* && attemps < 10*/)
		{
			Vector<String> portList = SerialPortFactory.list(tcmOnUSB); // true == Only list USB serial port, false == List every serial port available (std, bluetooth and USB)
			for(String portName : portList)
			{
				Logger.info(LC.gi(), this, "[Attemp "+attemps+"] Probing "+portName+" serial port...");
			
				try 
				{
					serialPort = SerialPortFactory.open(
									portName, 
									57600, 
									SerialPortConstants.DATABITS_8, 
									SerialPortConstants.STOPBITS_1, 
									SerialPortConstants.PARITY_NONE);
										
					serialPort.addSerialPortListener(this);
					
					if(readEnoceanTransceiverIDFromTCM() != 0)
					{	
						Logger.info(LC.gi(), this, "Enocean transceiver found. Its base ID is " + Integer.toHexString(enoceanTranceiverID));
						
						connected = true;
						this.portName = portName;
						higherLevelEventGate.postEvent(new HardwareLinkStatusEvent(HardwareLinkStatus.CONNECTED, LC.gi().bundleName()));
						connectionLatch.countDown();						
						
						return true;
					}
					else {
						disconnect();
					}
					
				} 
				catch (Exception e) 
				{
					Logger.error(LC.gi(), this, "while opening a serial connection on a Enocean tranceiver", e);
				} 
			}
			
			Logger.debug(LC.gi(), this, "Enocean tranceiver NOT connected !");
			
			try { Thread.sleep(1000); } catch(Exception e) { Logger.error(LC.gi(), this, "While waiting between two TCM connection attemps"); }
			
			attemps++;
		}
		
		return false;
	}
	
	@Override
	public void disconnect()
	{
		connected = false;
		
		higherLevelEventGate.postEvent(new HardwareLinkStatusEvent(HardwareLinkStatus.DISCONNECTED, LC.gi().bundleName()));
		
		if(serialPort != null) {
			serialPort.removeSerialPortListener(this);
			serialPort = null;
		}
	}

	@Override
	public int readEnoceanTransceiverIDFromTCM()
	{
		EnoceanESP3Telegram response = (EnoceanESP3Telegram) emitRawTelegram(EnoceanESP3Telegram.createRawTransmitCommandTelegram(EnoceanESP3Telegram.CO_RD_IDBASE, null, null));
		// Does the request succeed ?
		if (response != null && response.getResponseCode() == EnoceanESP3Telegram.RET_OK)
		{
			// Is the response in the expected format ?
			byte[] data = response.getRawData();
			if (data != null && data.length == 5)
			{
				enoceanTranceiverID = EnoceanESP3Telegram.getIntValue(data[1], data[2], data[3], data[4]);
				diagnosticManager.setTcmUID(Integer.toHexString(enoceanTranceiverID));
				/* TODO use remainingWriteCycle somewhere
				byte[] optionalData = response.getRawOptionalData();
				if (optionalData != null && optionalData.length == 1)
				{
					byte remainingWriteCycle = optionalData[0];
				}
				*/
				return enoceanTranceiverID;
			}
		}
		return 0;
	}

	@Override
	public TCMSettingResponse setTransceiverBaseID(String baseID) 
	{
		try
		{
			int newBaseID = (int)Long.parseLong(baseID, 16);
			
			if(newBaseID == getEnoceanTransceiverID())
			{
				return TCMSettingResponse.OK;
			}
			
			byte[] data = { (byte)((newBaseID >> 24) & 0xff), (byte)((newBaseID >> 16) & 0xff), (byte)((newBaseID >> 8) & 0xff), (byte)(newBaseID & 0xff) };
			
			EnoceanESP3Telegram response = (EnoceanESP3Telegram) emitRawTelegram(EnoceanESP3Telegram.createRawTransmitCommandTelegram(EnoceanESP3Telegram.CO_WR_IDBASE, data, null));
			// Does the request succeed ?
			if (response != null)
			{
				switch (response.getResponseCode())
				{
					case EnoceanESP3Telegram.RET_OK:
						int writtenID = readEnoceanTransceiverIDFromTCM();
						if(writtenID != newBaseID)
						{
							return TCMSettingResponse.WRITING_FAILED;
						}
						return TCMSettingResponse.OK;
					case EnoceanESP3Telegram.RET_NOT_SUPPORTED:
					case EnoceanESP3Telegram.FLASH_HW_ERROR:
						return TCMSettingResponse.WRITING_FAILED;
					case EnoceanESP3Telegram.BASEID_OUT_OF_RANGE:
						return TCMSettingResponse.ERR_RANGE;
					case EnoceanESP3Telegram.BASEID_MAX_REACHED:
						return TCMSettingResponse.ERR_MORE_THAN_10_TIMES;
					default:
						return TCMSettingResponse.WRITING_FAILED; // should never happen.
				}
			}
			else
			{
				return TCMSettingResponse.NOT_READY;
			}
		}
		catch(NumberFormatException e)
		{
			Logger.error(LC.gi(), this, "Cannot convert "+baseID+" into int.", e);
			return TCMSettingResponse.INVALID_VALUE;
		}
	}

	/**
	 * Return the Enocean base ID of the connected Enocean transceiver.
	 * 
	 * @return a 32 bits integer that is the base ID of the connected Enocean transceiver.
	 */
	@Override
	public int getEnoceanTransceiverID()
	{
		try 
		{
			connectionLatch.await(2, TimeUnit.SECONDS);
		} 
		catch (InterruptedException e) 
		{
			Logger.warn(LC.gi(), this, "getEnoceanTranceiverID(): serial port connection awaiting was interrupted");
			return 0;
		}
		
		return enoceanTranceiverID;
	}
		
	@Override
	public BlockingQueue<EnoceanTelegram> getTelegramReceptionQueue()
	{
		return receivedTelegrams;
	}

	@Override
	public void serialPortHasDisappeared(String portName)
	{
		Logger.info(LC.gi(), this, "It seems that the serial port has disappeared. We are trying to reconnect...");
		serialPort = null;
		disconnect();
		tcmConnectionTask.start();
	}

	@Override
	public void serialPortReceptionEvent(byte[] rawData, int length) 
	{
		if(LC.debug){
			Logger.debug(LC.gi(), this, "In PEM serial event listener: "+length+" byte(s) to process.");
		}
		
		try
		{
			diagnosticManager.increaseReceivedByteCounter(length);
			int syncBytePosition = 0; // This variable will mark the position of the first byte of the telegram to decode
			int dataBytePosition = 0; // This variable will mark the position of the first byte of the data of telegram to decode
			int offset = 0;
			
			int rawDataLength = 0;
			
			boolean packetReceptionCompleted = false;
			boolean wasInSyncState = false;
			
			byte[] dataBytes = null;
			byte CRC8D = 0;
			
			if(LC.debug) {
				StringBuffer sb = new StringBuffer("Analysing: < ");
				for(int k = syncBytePosition; k < length; k++) {
					sb.append(Integer.toHexString(rawData[k] & 0xff));
					sb.append(" ");
				}
				sb.append(">");
				Logger.debug(LC.gi(), this, sb.toString());
			}
			
			while(syncBytePosition < length) // While all received data are not entirely processed
			{
				// State machine to load incoming packet bytes
				switch(getPacketState)
				{
					// Waiting for packet sync byte 0x55
					case GET_SYNC_STATE:
						wasInSyncState = true;
						for(int i = syncBytePosition; i < length;  i++) 
						{
							// Is the byte a synchronization byte ?
							if (rawData[i] == (byte) 0x55)
							{
								// Yes, it is.
								// We need now to try to decode the header.
								getPacketState = GetPacketState.GET_HEADER_STATE;
								break;
							}
							else 
							{
								// No, we discard the byte.
								syncBytePosition++;
								diagnosticManager.increaseDiscardedBytesCounter(1);
							}
						}
						
						// Received data does NOT contains any enocean telegram data,
						// no more processing are needed on these data.
						if (getPacketState != GetPacketState.GET_HEADER_STATE)
						{
							Logger.debug(LC.gi(), this,"Received data does NOT contains any enocean telegram data");
						}
						break;
						
					// Read the header bytes (including CRC8 byte).
					case GET_HEADER_STATE:
						rawDataLength = length - syncBytePosition;
						
						// Is there some previously received data that contains the header ?
						if (buffer.position() == 0)
						{
							// No, there is not. 
							// Is there, at least, enough data for a complete header (with CRC8 byte) ?
							if (rawDataLength >= EnoceanESP3Telegram.SERIAL_SYNC_LENGTH)
							{
								// Yes, there is.
								// Data are stored to do the CRC8 check (we do not store the 0x55 byte).
								serialSynchronizationBytes = Arrays.copyOfRange(rawData, syncBytePosition, EnoceanESP3Telegram.SERIAL_SYNC_LENGTH + syncBytePosition);
								
								// State is changed to process CRC8 check.
								dataBytePosition = syncBytePosition + EnoceanESP3Telegram.SERIAL_SYNC_LENGTH;
								getPacketState = GetPacketState.CHECK_CRC8H_STATE;
							}
							else 
							{
								// No, there is not.
								// Data are buffered and will be processed later.
								buffer.put(rawData, syncBytePosition, rawDataLength);
								return;
							}
						}
						else
						{
							// Yes, there is some previously received data.
							// How many bytes are missing ?
							int missingBytes = EnoceanESP3Telegram.SERIAL_SYNC_LENGTH - buffer.position();
	
							// Are they enough available data to complete the on going header ?
							if(rawDataLength >= missingBytes) 
							{
								// Yes, they are. Partial data are completed
								buffer.put(rawData, syncBytePosition, missingBytes);
								
								// At this point, a complete header is arrived.
								// Data are stored to do the CRC8 check (we do not store the 0x55 byte).
								serialSynchronizationBytes = new byte[EnoceanESP3Telegram.SERIAL_SYNC_LENGTH];
								buffer.flip(); // set the reading position at position 0
								buffer.get(serialSynchronizationBytes, 0, EnoceanESP3Telegram.SERIAL_SYNC_LENGTH);
								
								buffer.clear(); // Buffer is emptied to be read to host new data from next telegram
								
								// State is changed to process CRC8 check.
								dataBytePosition = syncBytePosition + missingBytes;
								getPacketState = GetPacketState.CHECK_CRC8H_STATE;
							}
							else 
							{
								// No, they are not. 
								// Complete already received data with new ones and return.
								buffer.put(rawData, syncBytePosition, rawDataLength);
								return;
							}
						}
						break;
						
					// Check header checksum & try to re-synchronize if error happened.
					case CHECK_CRC8H_STATE:
						byte CRC8value = EnoceanESP3Telegram.computeCRC8(serialSynchronizationBytes, EnoceanESP3Telegram.HEADER_FIRST_BYTE, EnoceanESP3Telegram.HEADER_LAST_BYTE);
						byte CRC8byte = serialSynchronizationBytes[EnoceanESP3Telegram.SERIAL_SYNC_LENGTH-1];
						
						// Header CRC correct?
						if (CRC8value != CRC8byte)
						{
							Logger.debug(LC.gi(), this, "CRC8H code not correct (received: 0x"+Integer.toHexString(CRC8byte & 0xff)+", calculated: 0x"+Integer.toHexString(CRC8value & 0xff)+").");
							// No, header CRC is not correct.
							// We increased the syncBytePosition and get back to synchronization state.
							if (wasInSyncState)
								syncBytePosition++;
							diagnosticManager.increaseDiscardedBytesCounter(1);
							getPacketState = GetPacketState.GET_SYNC_STATE;
						}
						else
						{
							Logger.debug(LC.gi(), this,"CRC8H correct");
							// Yes, the CRC is correct.
							dataLength = ((serialSynchronizationBytes[1] << 8) + serialSynchronizationBytes[2]) & 0xffff;
							optionalLength = (serialSynchronizationBytes[3] & 0xff);
							
							// Are length fields values correct ?
							if (dataLength + optionalLength == 0)
							{
								Logger.debug(LC.gi(), this,"Packet header contains invalid length fields (both having a 0 value)");
								// No, packet with correct CRC8H but wrong length fields.
								if (wasInSyncState)
									syncBytePosition++;
								//diagnosticManager.increaseDiscardedBytesCounter(1);
								getPacketState = GetPacketState.GET_SYNC_STATE;
								break;
							}
							
							// Correct Header. So we can do the reception of data.
							getPacketState = GetPacketState.GET_DATA_STATE;
						}
						break;
						
					case GET_DATA_STATE:
						rawDataLength = length - dataBytePosition;
						int dataPayloadLength = dataLength + optionalLength;
						
						// Is there some previously received data that contains data payload ?
						if (buffer.position() == 0)
						{
							// No, there is not. 
							// Is there, at least, enough data to decode this telegram (with CRC8 byte) ?
							if (rawDataLength >= dataLength + optionalLength + 1)
							{
								// Yes, there is.
								// Data are stored to do the CRC8 check (we do not store the 0x55 byte).
								dataBytes = Arrays.copyOfRange(rawData, dataBytePosition, dataBytePosition + dataPayloadLength);
								CRC8D = rawData[dataBytePosition + dataPayloadLength];
								
								offset =  dataLength + optionalLength + 1;
								
								// State is changed to process CRC8 check.
								getPacketState = GetPacketState.CHECK_CRC8D_STATE;
							}
							else 
							{
								// No, there is not.
								// Data are buffered and will be processed later.
								buffer.put(rawData, dataBytePosition, rawDataLength);
								return;
							}
						}
						else
						{
							// Yes, there is some previously received data.
							// How many bytes are missing ?
							int missingBytes = dataPayloadLength + 1 - buffer.position();
							
							// Are they enough available data to complete the on going data ?
							if(rawDataLength >= missingBytes) 
							{
								// Yes, they are. Partial data are completed
								buffer.put(rawData, dataBytePosition, missingBytes - 1);
								
								// At this point, a complete header is arrived.
								// Data are stored to do the CRC8 check (we do not store the 0x55 byte).
								dataBytes = new byte[dataPayloadLength];
								buffer.flip(); // set the reading position at position 0
								buffer.get(dataBytes, 0, dataPayloadLength);
								CRC8D = rawData[dataBytePosition + missingBytes - 1];
								
								buffer.clear(); // Buffer is emptied to be read to host new data from next telegram
	
								offset = missingBytes;
								
								// State is changed to process CRC8 check.
								getPacketState = GetPacketState.CHECK_CRC8D_STATE;
							}
							else 
							{
								// No, they are not. 
								// Complete already received data with new ones and return.
								buffer.put(rawData, syncBytePosition, rawDataLength);
								return;
							}
						}
						break;
						
					case CHECK_CRC8D_STATE:
						CRC8value = EnoceanESP3Telegram.computeCRC8(dataBytes, dataLength + optionalLength);
						
						// Header CRC correct?
						if (CRC8value != CRC8D)
						{
							Logger.debug(LC.gi(), this, "CRC8D code not correct (received: 0x"+Integer.toHexString(CRC8D & 0xff)+", calculated: 0x"+Integer.toHexString(CRC8value & 0xff)+").");
							// No, header CRC is not correct.
							diagnosticManager.increaseDiscardedBytesCounter(EnoceanESP3Telegram.SERIAL_SYNC_LENGTH + dataLength + optionalLength + 1);
							dataBytes = null;
						}
						else
						{
							Logger.debug(LC.gi(), this, "CRC8D code correct.");
							// Yes, the CRC is correct.	
							// We can now read the packet.
							packetReceptionCompleted = true;
						}
						
						// In all cases, we return in the initial state.
						getPacketState = GetPacketState.GET_SYNC_STATE;
						
						// syncBytePosition is set to point onto data that come after the processed telegram
						syncBytePosition = dataBytePosition + offset;
						break;
				}
				
				// Is there any telegram data to decode ?
				if (packetReceptionCompleted && dataBytes != null)
				{
					// Yes, they are.
					long startDecodingTime = System.nanoTime();
					
					Logger.debug(LC.gi(), this, "Reception of telegram completed.");
					try 
					{	
						EnoceanESP3Telegram telegram = new EnoceanESP3Telegram(serialSynchronizationBytes, dataBytes, CRC8D);
						diagnosticManager.logTelegram(telegram);
						
						switch (telegram.getPacketType())
						{
							case RADIO:
								receivedTelegrams.put(telegram);
								diagnosticManager.telegramReceptionSuccess();
								break;
							case RESPONSE:
								if(!responseTelegrams.offer(telegram, 1, TimeUnit.SECONDS))
								{
									Logger.error(LC.gi(), this, "Could not store a response telegram into the response blocking queue.");
								}
								break;
							case REMOTE_MAN_COMMAND:
							case EVENT:
								Logger.warn(LC.gi(), this, "The reception of this type of messages are not yet supported. Test it before using it.");
								receivedTelegrams.put(telegram);
								diagnosticManager.telegramReceptionSuccess();
								break;
							case COMMON_COMMAND:
							case SMART_ACK_COMMAND:
								Logger.warn(LC.gi(), this, "This type of message is not supported. And for a good reason: we should never received them.");
								break;
							case NOT_SUPPORTED:
								Logger.warn(LC.gi(), this, "Packet Type not supported.");
								break;
						}			
					}
					catch(IllegalArgumentException e)
					{
						diagnosticManager.telegramReceptionFailure();
						diagnosticManager.increaseDiscardedBytesCounter(EnoceanESP3Telegram.SERIAL_SYNC_LENGTH + dataLength + optionalLength + 1);
						diagnosticManager.logRawData(dataBytes, dataBytes.length);
						Logger.warn(LC.gi(), this, "Error while decoding a raw telegram", e);
					} 
					catch (InterruptedException e) 
					{
						Logger.warn(LC.gi(), this, "A received telegram could NOT have been put in queue to be processed", e);
					}
					finally
					{
						CRC8D = 0;
						packetReceptionCompleted = false;
						dataBytes = null;
						serialSynchronizationBytes = null;
					}
					
					long decodingTime = System.nanoTime() - startDecodingTime;
					diagnosticManager.timeToDecodeTelegram(decodingTime);
				}
				
				// Any more data to process ? Will loop if any. (see looping condition)
			}
		}
		finally 
		{
			if(LC.debug){
				Logger.debug(LC.gi(), this, "Exiting PEM serial event listener.");
			}
		}
		// no more serial data to process, end of job !
	}
	
	/**
	 * Emit a raw telegram to Enocean devices through the Enocean transceiver. 
	 * Return the acknowledgment telegram generated by the Enocean transceiver.
	 * 
	 * @param rawTelegram
	 * @return
	 */
	@Override
	public synchronized EnoceanTelegram emitRawTelegram(byte[] rawTelegram)
	{
		if(serialPort != null && rawTelegram != null) 
		{
			try 
			{
				int emissionFailureCount = 0;
				EnoceanESP3Telegram response = null;
				while(emissionFailureCount < 5)
				{
					diagnosticManager.logEmittedRawTelegram(rawTelegram);
					serialPort.write(rawTelegram);
					
					response = responseTelegrams.poll(EnoceanESP3Telegram.RESPONSE_TIMEOUT, TimeUnit.MILLISECONDS);
					if(response == null) 
					{
						diagnosticManager.telegramEmissionFailure();
						diagnosticManager.logNoEmissionAck();
						emissionFailureCount++;
					}
					else {
						switch (response.getResponseCode())
						{
							case EnoceanESP3Telegram.RET_OK:
							case EnoceanESP3Telegram.RET_NOT_SUPPORTED:
							case EnoceanESP3Telegram.RET_WRONG_PARAM:
							case EnoceanESP3Telegram.RET_OPERATION_DENIED:
								diagnosticManager.telegramEmissionSuccess();
								diagnosticManager.increaseEmittedByteCounter(rawTelegram.length);
								return (EnoceanTelegram) response;
							default :
								Logger.error(LC.gi(), this, "While emitting a telegram: the received ACK is not the one expected: response_code="+response.getResponseCode());
								emissionFailureCount++;
								break;
						}
					}
				}
				return (EnoceanTelegram) response;
			} 
			catch (IOException e) {
				diagnosticManager.telegramEmissionFailure();
				Logger.error(LC.gi(), this, "Failed to emit a telegram through the serial port", e);
			} 
			catch (InterruptedException e) { 
				Logger.error(LC.gi(), this, "The ack waiting was interrupted", e);
			}
		}
		
		if(rawTelegram == null) {
			Logger.error(LC.gi(), this, "emitRawTelegram called with a null raw telegram. This is a bug to fix.");
		}
		return null;
	}
	
	@Override
	public byte[] createRawTransmitRadioTelegram(EnoceanTelegram.RORG rorg, int senderId, byte[] data)
	{
		return createRawTransmitRadioTelegram(rorg, senderId, data, (byte) 0x00, 0xffffffff);
	}
	
	public byte[] createRawTransmitRadioTelegram(EnoceanTelegram.RORG rorg, int senderId, byte[] data, int destinationID)
	{
		return createRawTransmitRadioTelegram(rorg, senderId, data, (byte) 0x00, destinationID);
	}
	
	@Override
	public byte[] createRawTransmitRadioTelegram(EnoceanTelegram.RORG rorg, int senderId, byte[] data, byte status)
	{
		return createRawTransmitRadioTelegram(rorg, senderId, data, status, 0xffffffff);
	}
	
	public byte[] createRawTransmitRadioTelegram(EnoceanTelegram.RORG rorg, int senderId, byte[] data, byte status, int destinationID)
	{
		byte choice;
		switch (rorg)
		{
			case RORG_RPS: 
				choice = EnoceanESP3Telegram.ORG_RPS; break;
			case RORG_1BS: 
				choice = EnoceanESP3Telegram.ORG_1BS; break;
			case RORG_4BS: 
				choice = EnoceanESP3Telegram.ORG_4BS; break;
			case RORG_UTE:
				choice = EnoceanESP3Telegram.ORG_UTE; break;
			case RORG_VLD : 
				choice = EnoceanESP3Telegram.ORG_VLD; break;
			default:
				Logger.error(LC.gi(), this, "Unsupported RORG. This should never happen. This is a bug to fix.");
				return null;
		}
		return EnoceanESP3Telegram.createRawTransmitRadioTelegram(choice, senderId, data, status, destinationID);
	}
	
	public byte[] createRawTransmitResponseTelegram(byte returnCode, byte[] responseData)
	{
		return EnoceanESP3Telegram.createRawTransmitResponseTelegram(returnCode, responseData);
	}
	
	public byte[] createRawTransmitCommandTelegram(byte commonCommandCode, byte[] commonCommandData, byte optionalData[])
	{
		return EnoceanESP3Telegram.createRawTransmitCommandTelegram(commonCommandCode, commonCommandData, optionalData);
	}
	
	public byte[] createRawTransmitSmartAckCommandTelegram(byte smartAckCode, byte[] smartAckData)
	{
		return EnoceanESP3Telegram.createRawTransmitSmartAckCommandTelegram(smartAckCode, smartAckData);
	}

	@Override
	public String getPortName() 
	{
		try 
		{
			connectionLatch.await(2, TimeUnit.SECONDS);
		} 
		catch (InterruptedException e) 
		{
			Logger.warn(LC.gi(), this, "getPortName(): serial port connection awaiting was interrupted");
			return null;
		}
		
		return portName;
	}

	@Override
	public boolean isConnected()
	{
		return connected;
	}
}
