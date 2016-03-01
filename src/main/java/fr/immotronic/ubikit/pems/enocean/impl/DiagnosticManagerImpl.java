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
import org.ubikit.PhysicalEnvironmentModelInformations;
import org.ubikit.PhysicalEnvironmentModelObserver;

public class DiagnosticManagerImpl implements DiagnosticManager
{
	private static final int MAX_TIMES_CARDINALITY = 50; 
	private long receivedBytes = 0;
	private long emittedBytes = 0;
	private long discarderBytes = 0;
	private long receivedTelegrams = 0;
	private long illegalTelegrams = 0;
	private long emittedTelegrams = 0;
	private long nonEmittedTelegrams = 0;
	private long begining = 0;
	private String tcmUID = null;
	private int tcmSensitivity;
	private boolean decodingBufferfull = false;
	private int decodingCounter = 0;
	private final long[] decodingTimes = new long[MAX_TIMES_CARDINALITY];
	private boolean processingBufferfull = false;
	private int processingCounter = 0;
	private final long[] processingTimes = new long[MAX_TIMES_CARDINALITY];
	
	private PhysicalEnvironmentModelObserver observer = null;
	
	@Override
	public void setObserver(PhysicalEnvironmentModelObserver observer)
	{
		this.observer = observer;
	}
	
	@Override
	public void increaseReceivedByteCounter(int numberOfBytes) 
	{
		if(begining == 0) {
			begining = new Date().getTime();
		}
		receivedBytes += numberOfBytes;
	}

	@Override
	public void increaseEmittedByteCounter(int numberOfBytes) 
	{
		if(begining == 0) {
			begining = new Date().getTime();
		}
		emittedBytes += numberOfBytes;
	}
	
	@Override
	public void increaseDiscardedBytesCounter(int numberOfBytes)
	{
		discarderBytes += numberOfBytes;
	}

	@Override
	public void telegramEmissionSuccess() 
	{
		emittedTelegrams++;
	}
	
	@Override
	public void telegramEmissionFailure() 
	{
		nonEmittedTelegrams++;
	}

	@Override
	public void telegramReceptionSuccess() 
	{
		receivedTelegrams++;
	}

	@Override
	public void telegramReceptionFailure() 
	{
		illegalTelegrams++;
	}

	@Override
	public void timeToDecodeTelegram(long nanosecs) 
	{
		decodingTimes[decodingCounter] = nanosecs;
		decodingCounter++;
		if(decodingCounter >= MAX_TIMES_CARDINALITY) {
			decodingBufferfull = true;
			decodingCounter = 0;
		}
	}

	@Override
	public void timeToProcessTelegram(long nanosecs) 
	{
		processingTimes[processingCounter] = nanosecs;
		processingCounter++;
		if(processingCounter >= MAX_TIMES_CARDINALITY) {
			processingBufferfull = true;
			processingCounter = 0;
		}
	}
	
	@Override
	public void setTcmUID(String tcmUID) 
	{
		this.tcmUID = tcmUID;
	}
	
	@Override
	public void setTcmSensitivity(int tcmSensitivity) 
	{
		this.tcmSensitivity = tcmSensitivity;
	}
	
	@Override
	public PhysicalEnvironmentModelInformations getInformations()
	{
		double averageDecodingTime = 0;
		double averageProcessingTime = 0;
		int dtCount = (decodingBufferfull?MAX_TIMES_CARDINALITY:processingCounter);
		int ptCount = (processingBufferfull?MAX_TIMES_CARDINALITY:processingCounter);
		
		for(int i = dtCount - 1 ;i >= 0; i--) {
			averageDecodingTime += ((double)decodingTimes[i] / 1000000d); // nanosec to ms
		}
		averageDecodingTime = ((dtCount != 0)?(averageDecodingTime / dtCount):0);
		
		for(int i = ptCount - 1; i >= 0; i--) {
			averageProcessingTime += ((double)processingTimes[i] / 1000000d); // nanosec to ms
		}
		averageProcessingTime = ((ptCount != 0)?(averageProcessingTime / ptCount):0);
		
		return new EnoceanDiagnosticInformationsImpl(
				((begining != 0)?(new Date().getTime() - begining):0),
				tcmUID,
				tcmSensitivity,
				receivedBytes,
				discarderBytes,
				emittedBytes,
				receivedTelegrams,
				illegalTelegrams,
				emittedTelegrams,
				nonEmittedTelegrams,
				averageDecodingTime,
				averageProcessingTime
				);
	}
	
	@Override
	public void logEmittedRawTelegram(byte[] telegram)
	{
		StringBuilder sb = new StringBuilder("EMIT < ");
		for(byte b : telegram) {
			sb.append(Integer.toHexString(b & 0xff));
			sb.append(" ");
		}
		sb.append(">");
		
		if(observer != null) {
			observer.log(sb.toString());
		}
	}
	
	@Override
	public void logNoEmissionAck()
	{
		Logger.error(LC.gi(), this, "EnOcean telegram has been sent for emission, but no ACK was received from the TCM.");
		if(observer != null) {
			observer.log("ACK < Acknowledgement expected but NOT received >");
		}
	}
	
	@Override
	public void logTelegram(byte[] rawTelegram, EnoceanESP2Telegram telegram)
	{
		StringBuilder sb = new StringBuilder();
		
		if(telegram.isAckTelegram()) {
			sb.append("ACK < ");
		}
		else {
			sb.append("RECV < ");
		}
		
		for(byte b : rawTelegram) {
			sb.append(Integer.toHexString(b & 0xff));
			sb.append(" ");
		}
		sb.append("> ");
		
		sb.append(telegram.toString());
		
		if(observer != null) {
			observer.log(sb.toString());
		}
	}
	
	@Override
	public void logTelegram(EnoceanESP3Telegram telegram)
	{
		StringBuilder sb = new StringBuilder();
		
		switch(telegram.getPacketType())
		{
			case RADIO :
			case EVENT :
			case REMOTE_MAN_COMMAND:
				sb.append("RECV ");
				break;
			case RESPONSE :
				sb.append("ACK ");
				break;
			default:
				sb.append("??? ");
				break;
		}
	
		sb.append("< "+telegram.toReadableBytes()+" > ");
		
		sb.append(telegram.toString());
				
		if(observer != null) {
			observer.log(sb.toString());
		}
	}
	
	@Override
	public void logRawData(byte[] data, int length)
	{
		StringBuilder sb = new StringBuilder("DISCARD < ");
		for(int i = 0; i < length; i++) {
			sb.append(Integer.toHexString(data[i] & 0xff));
			sb.append(" ");
		}
		sb.append(">");
		
		if(observer != null) {
			observer.log(sb.toString());
		}
	}
}
