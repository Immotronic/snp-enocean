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

public final class EnoceanESP2SerialAdapterImpl implements SerialPortListener, EnoceanSerialAdapter
{
	private SerialPort serialPort;
	private boolean connected;
	private String portName;
	
	//private final byte[] rawTelegram;
	private final ByteBuffer buffer;
	private boolean telegramReceptionOnGoing;
	private boolean sync0WasReceived;
	
	private int enoceanTranceiverID;
	private int enoceanTranceiverSensitivity;
	
	private final BlockingQueue<EnoceanTelegram> receivedTelegrams;
	private final BlockingQueue<EnoceanESP2Telegram> ackTelegrams;
	private final CountDownLatch connectionLatch;
	private final DiagnosticManager diagnosticManager;
	private final TCMConnectionTask tcmConnectionTask;
	private final EventGate higherLevelEventGate;
	
	private boolean tcmOnUSB;
	
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
	
	public EnoceanESP2SerialAdapterImpl(boolean tcmOnUSB, DiagnosticManager diagnosticManager, ScheduledExecutorService executor, EventGate higherLevelEventGate)
	{
		this.tcmOnUSB = tcmOnUSB;
		this.diagnosticManager = diagnosticManager;
		this.higherLevelEventGate = higherLevelEventGate;
		
		tcmConnectionTask = new TCMConnectionTask(executor);
		buffer = ByteBuffer.allocate(EnoceanESP2Telegram.TELEGRAM_SIZE * 100);
		
		receivedTelegrams = new ArrayBlockingQueue<EnoceanTelegram>(10);
		ackTelegrams = new ArrayBlockingQueue<EnoceanESP2Telegram>(1);
		
		connectionLatch = new CountDownLatch(1);
		
		connected = false;
		portName = null;
		serialPort = null;
		
		tcmConnectionTask.start();
	}
	
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
	
	public BlockingQueue<EnoceanTelegram> getTelegramReceptionQueue()
	{
		return receivedTelegrams;
	}
	
	public ESP getSupportedESP()
	{
		return ESP.ESP2;
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
									9600, 
									SerialPortConstants.DATABITS_8, 
									SerialPortConstants.STOPBITS_1, 
									SerialPortConstants.PARITY_NONE);
					
					telegramReceptionOnGoing = false;
					sync0WasReceived = false;
					
					serialPort.addSerialPortListener(this);
					
					if(readEnoceanTransceiverIDFromTCM() != 0)
					{	
						Logger.info(LC.gi(), this, "Enocean transceiver found. Its base ID is " + Integer.toHexString(enoceanTranceiverID));
						
						EnoceanESP2Telegram ack = emitRawTelegram(EnoceanESP2Telegram.createRawTransmitCommandTelegram(EnoceanESP2Telegram.RD_RX_SENSITIVITY, null));
						enoceanTranceiverSensitivity = ack.getData().getBytes()[3];
						diagnosticManager.setTcmSensitivity(enoceanTranceiverSensitivity);
						
						connected = true;
						this.portName = portName;
						higherLevelEventGate.postEvent(new HardwareLinkStatusEvent(HardwareLinkStatus.CONNECTED, LC.gi().bundleName()));
						connectionLatch.countDown();
						
						return true;
					}
					else {
						Logger.debug(LC.gi(), this, "Enocean tranceiver NOT connected !");
						disconnect();
					}
					
				} 
				catch (Exception e) 
				{
					Logger.error(LC.gi(), this, "while opening a serial connection on a Enocean tranceiver", e);
				} 
			}
			
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
	public void serialPortHasDisappeared(String arg0) 
	{
		serialPort = null;
		disconnect();
		tcmConnectionTask.start();
	}

	/**
	 * Feed the internal buffer with new received data. When enough data are available, 
	 * a telegram will be decoded and transmit to an EnoceanEventSource object.
	 * 
	 * @param rawData raw data to decode
	 * @param length The size of raw data available  
	 */
	@Override
	public void serialPortReceptionEvent(byte[] rawData, int length) 
	{
		diagnosticManager.increaseReceivedByteCounter(length);
		byte[] rawTelegram = null; // This array will host telegram data to decode.
		int sync0position = 0; // This variable will mark the position of the first byte of the telegram to decode
		boolean firstTelegramInThisEvent = true;
		
		if(LC.debug) {
			Logger.debug(LC.gi(), this, "___...[serialPortReceptionEvent]...___");
		}
		
		while(sync0position < length) // While all received data are not entirely processed
		{
			// they are (length - sync0position) bytes of serial data to process
			
			if(LC.debug) {
				StringBuffer sb = new StringBuffer("Analysing: < ");
				for(int k = sync0position; k < length; k++) {
					sb.append(Integer.toHexString(rawData[k] & 0xff));
					sb.append(" ");
				}
				sb.append(">");
				Logger.debug(LC.gi(), this, sb.toString());
			}
			
			if(!telegramReceptionOnGoing) 
			{	 
				// If no telegram reception is on going, all bytes before
				// the 0xa5 0x5a sequence have to be discarded
				for(int i = sync0position; i < length;  i++) 
				{
					switch(rawData[i])
					{
						case (byte)(0xA5):
							
							if(!sync0WasReceived) {
								// a first A5 was received. Mark it.
								sync0WasReceived = true;
							}
							else {
								// A second A5 was received, just after the last one.
								// The first one is discarded.
								sync0position++;
							}
							break;
							
						case (byte)(0x5A):
							
							if(sync0WasReceived) {
								// The sequence A5 5A has been received,
								// sync0position contains the position of
								// the A5 byte : a telegram reception is
								// now on going.
								telegramReceptionOnGoing = true;
								sync0WasReceived = false;
							}
							else {
								// No A5 was received just before, so this 5A
								// byte is discarded.
								sync0position++;
							}
							break;
							
						default:
							
							if(sync0WasReceived) {
								// the A5 byte received is not followed by a 5A
								// byte. Then, this A5 byte was not the good one,
								// sync0WasReceived is set back to false, and these
								// two bytes are discarded.
								sync0WasReceived = false;
								
								if(sync0position != 0) {
									// If the wrong A5 byte was part of this event,
									// it have to be discarded.
									sync0position ++;
								}
								else {
									// The wrong A5 was buffered. So, buffer have to be cleared.
									buffer.clear();
								}
							}

							sync0position++; // this byte is discarded
							break;
					}
					
					if(telegramReceptionOnGoing) 
					{
						// Telegram sync bytes were successfully detected. Telegram data will be processed
						break;
					}
				}
				
				if(!telegramReceptionOnGoing) 
				{
					if(sync0WasReceived)
					{
						// an isolated A5 was detected (received data ended 
						// with a A5. 5A will probably follow in the next serial event)
						// Then, this A5 is buffered.
						buffer.put((byte)0xA5);
					}
					else 
					{
						// Received data does NOT contains any enocean telegram data,
						// no more processing are needed on these data.
						
						diagnosticManager.increaseDiscardedBytesCounter(length);
						diagnosticManager.logRawData(rawData, length);
					}
					
					return;
				}
			}
			
			// At this point, in rawData, Enocean data begins at sync0position.
			int dataLength = length - sync0position; // dataLength contains the length of non discarded data
			
			if(sync0position != 0)
			{
				// If firstTelegramInThisEvent is true, then the first sync0position bytes
				// have been discarded and dataLength bytes need to be processed.
				// If !firstTelegramInThisEvent, then the first sync0position bytes belong to
				// the previous telegram and are just silently ignored. 
				
				if(firstTelegramInThisEvent) {
					diagnosticManager.increaseDiscardedBytesCounter(sync0position);
					diagnosticManager.logRawData(rawData, sync0position);
				}
			}
			
			// Is there some previously received data that contains the beginning of the
			// telegram ?
			if(buffer.position() != 0)
			{
				// Yes, there is.
				// How many bytes are missing ?
				int missingBytes = EnoceanESP2Telegram.TELEGRAM_SIZE - buffer.position();
				
				// Are they enough available data to complete the on going telegram ?
				if(dataLength >= missingBytes) 
				{
					// Yes, they are. Partial data are completed
					buffer.put(rawData, sync0position, missingBytes);
					
					// At this point, a complete telegram is arrived. Its data are put in rawTelegram to be decoded.
					rawTelegram = new byte[EnoceanESP2Telegram.TELEGRAM_SIZE];
					buffer.flip(); // set the reading position at position 0
					buffer.get(rawTelegram, 0, EnoceanESP2Telegram.TELEGRAM_SIZE);
					
					// telegram reception is marked as done
					telegramReceptionOnGoing = false;
					firstTelegramInThisEvent = false;
					
					// sync0position is set to point onto data that come after
					// the processed telegram
					sync0position += missingBytes;
					
					buffer.clear(); // Buffer is emptied to be read to host new data from next telegram
					
					// At this point, a new telegram is now ready for decoding
				}
				else 
				{
					// No; they are not. Complete already received data with new ones
					// and return.
					buffer.put(rawData, sync0position, dataLength);
					return;
				}
			}
			else 
			{
				// No, there is not. 
				// Is there, at least, enough data for a complete telegram ?
				if(dataLength >= EnoceanESP2Telegram.TELEGRAM_SIZE)
				{
					// At least, one telegram arrived. Its data are put in rawTelegram to be decoded.
					rawTelegram = Arrays.copyOfRange(rawData, sync0position, EnoceanESP2Telegram.TELEGRAM_SIZE + sync0position);
					
					// telegram reception is marked as done
					telegramReceptionOnGoing = false;
					firstTelegramInThisEvent = false;
					
					// sync0position is set to point onto data that come after
					// the processed telegram
					sync0position += EnoceanESP2Telegram.TELEGRAM_SIZE;
				}
				else {
					// Not enough data for a whole telegram. They are buffered
					// and will be processed later
					buffer.put(rawData, sync0position, dataLength);
					return;
				}
			}
			
			// Is there any telegram data to decode ?
			if(rawTelegram != null)
			{
				// Yes, they are.
				long startDecodingTime = System.nanoTime();
				
				try 
				{	
					EnoceanESP2Telegram telegram = new EnoceanESP2Telegram(rawTelegram);
					diagnosticManager.logTelegram(rawTelegram, telegram);
					if(telegram.isAckTelegram()) 
					{
						if(!ackTelegrams.offer(telegram, 1000, TimeUnit.MILLISECONDS))
						{
							Logger.error(LC.gi(), this, "Could not place a response telegram into the response blocking queue.");
						}
					}
					else 
					{
						receivedTelegrams.put(telegram);
						diagnosticManager.telegramReceptionSuccess();
					}
				}
				catch(IllegalArgumentException e)
				{
					diagnosticManager.telegramReceptionFailure();
					diagnosticManager.increaseDiscardedBytesCounter(EnoceanESP2Telegram.TELEGRAM_SIZE);
					diagnosticManager.logRawData(rawTelegram, 0);
					Logger.warn(LC.gi(), this, "Error while decoding a raw telegram", e);
				} 
				catch (InterruptedException e) 
				{
					Logger.warn(LC.gi(), this, "A received telegram could NOT have been put in queue to be processed", e);
				}
				
				long decodingTime = System.nanoTime() - startDecodingTime;
				diagnosticManager.timeToDecodeTelegram(decodingTime);
			}
			
			// Any more data to process ? Will loop if any. (see looping condition)
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
	public synchronized EnoceanESP2Telegram emitRawTelegram(byte[] rawTelegram)
	{
		if(serialPort != null && rawTelegram != null) 
		{
			try 
			{
				int emissionFailureCount = 0;
				EnoceanESP2Telegram ack = null;
				while(emissionFailureCount < 5)
				{
					diagnosticManager.logEmittedRawTelegram(rawTelegram);
					serialPort.write(rawTelegram);
					
					ack = ackTelegrams.poll(1000, TimeUnit.MILLISECONDS);
					if(ack == null) 
					{
						diagnosticManager.telegramEmissionFailure();
						diagnosticManager.logNoEmissionAck();
						emissionFailureCount++;
					}
					else if(ack.isErrorTelegram()) 
					{
						diagnosticManager.telegramEmissionFailure();
						emissionFailureCount++;
					}
					else
					{
						diagnosticManager.telegramEmissionSuccess();
						diagnosticManager.increaseEmittedByteCounter(rawTelegram.length);
						return ack;
					}
				}
				
				return ack;
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
		return createRawTransmitRadioTelegram(rorg, senderId, data, (byte) 0x0);
	}
	
	@Override
	public byte[] createRawTransmitRadioTelegram(EnoceanTelegram.RORG rorg, int senderId, byte[] data, byte status)
	{
		byte type;
		switch (rorg)
		{
			case RORG_RPS: 
				type = EnoceanESP2Telegram.ORG_RPS; break;
			case RORG_1BS: 
				type = EnoceanESP2Telegram.ORG_1BS; break;
			case RORG_4BS: 
				type = EnoceanESP2Telegram.ORG_4BS; break;
			case RORG_HRC: 
				type = EnoceanESP2Telegram.ORG_HRC; break;
			case RORG_UTE: 
			case RORG_VLD: 
				Logger.info(LC.gi(), this, "This RORG is not supported with ESP2.");
				return null;
			default:
				Logger.error(LC.gi(), this, "Unsupported RORG. This should never happen. This is a bug to fix.");
				return null;
		}
		return EnoceanESP2Telegram.createRawTransmitRadioTelegram(type, senderId, data, status);
	}
	
	@Override
	public int readEnoceanTransceiverIDFromTCM() 
	{
		EnoceanESP2Telegram ack = emitRawTelegram(EnoceanESP2Telegram.createRawTransmitCommandTelegram(EnoceanESP2Telegram.RD_IDBASE, null));
		if(ack != null && !ack.isErrorTelegram())
		{
			byte[] data = ack.getData().getBytes();
			enoceanTranceiverID = (data[3] & 0xff) << 24 | (data[2] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[0] & 0xff);
			diagnosticManager.setTcmUID(Integer.toHexString(enoceanTranceiverID));
			return enoceanTranceiverID;
		}
		
		return 0;
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
	
	/**
	 * Return the sensitivity of the connected Enocean transceiver.
	 * 
	 * @return a 0 if the transceiver sensitivity is low, 1 if it is high.
	 */
	public int getEnoceanTransceiverSensitivity()
	{
		try 
		{
			connectionLatch.await(2, TimeUnit.SECONDS);
		} 
		catch (InterruptedException e) 
		{
			Logger.warn(LC.gi(), this, "getEnoceanTranceiverSensibility(): serial port connection awaiting was interrupted");
			return 0;
		}
		
		return enoceanTranceiverSensitivity;
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
			
			EnoceanESP2Telegram ack = emitRawTelegram(EnoceanESP2Telegram.createRawTransmitCommandTelegram(EnoceanESP2Telegram.SET_IDBASE, newBaseID));
			if(ack != null)
			{
				if(ack.isErrorTelegram())
				{
					switch(ack.getErrorID())
					{
						case EnoceanESP2Telegram.ERR:
							return TCMSettingResponse.ERR_MORE_THAN_10_TIMES;
							
						case EnoceanESP2Telegram.ERR_ID_RANGE:
							return TCMSettingResponse.ERR_RANGE;
							
						default:
							return TCMSettingResponse.WRITING_FAILED; // Should never happen. If so, it is a bug.
					}
				}
				else
				{
					int writtenID = readEnoceanTransceiverIDFromTCM();
					if(writtenID != newBaseID)
					{
						return TCMSettingResponse.WRITING_FAILED;
					}
					
					return TCMSettingResponse.OK;
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

	@Override
	public boolean isConnected()
	{
		return connected;
	}
}
