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

import java.text.NumberFormat;

import fr.immotronic.ubikit.pems.enocean.EnoceanDiagnosticInformations;

public class EnoceanDiagnosticInformationsImpl implements EnoceanDiagnosticInformations 
{
	private final static NumberFormat nf = NumberFormat.getInstance();
	
	static {
		nf.setMaximumFractionDigits(3);
	}
	
	private final long period;
	private final String tcmUID;
	private final int tcmSensitivity;
	private final long receivedBytes;
	private final long discardedBytes;
	private final double discardedBytesRatio;
	private final long emittedBytes;
	private final long receivedTelegrams;
	private final long illegalTelegrams;
	private final double illegalTelegramsRatio;
	private final double averageReceptionSpeed;
	private final long emittedTelegrams;
	private final long nonEmittedTelegrams;
	private final double nonEmittedRatio;
	private final double averageEmissionSpeed;
	private final double averageDecodingTime;
	private final double averageProcessingTime;
	
	
	public EnoceanDiagnosticInformationsImpl(
				long period,
				String tcmUID,
				int tcmSensitivity,
				long receivedBytes,
				long discarderBytes,
				long emittedBytes,
				long receivedTelegrams,
				long illegalTelegrams,
				long emittedTelegrams,
				long nonEmittedTelegrams,
				double averageDecodingTime,
				double averageProcessingTime)
	{
		this.period = period;
		this.tcmUID = tcmUID;
		this.tcmSensitivity = tcmSensitivity;
		this.receivedBytes = receivedBytes;
		this.discardedBytes = discarderBytes;
		this.emittedBytes = emittedBytes;
		this.receivedTelegrams = receivedTelegrams;
		this.illegalTelegrams = illegalTelegrams;
		this.emittedTelegrams = emittedTelegrams;
		this.nonEmittedTelegrams = nonEmittedTelegrams;
		this.averageDecodingTime = averageDecodingTime;
		this.averageProcessingTime = averageProcessingTime;
		
		discardedBytesRatio = ((receivedBytes != 0)?((double)(discarderBytes * 100) / (double)receivedBytes):0);
		illegalTelegramsRatio = (((illegalTelegrams + receivedTelegrams) != 0)?((double)(illegalTelegrams * 100) / (double)(illegalTelegrams + receivedTelegrams)):0);
		averageReceptionSpeed = ((period != 0)?((double)(illegalTelegrams + receivedTelegrams) * 1000 / (double)period):0);
		nonEmittedRatio = (((nonEmittedTelegrams + emittedTelegrams) != 0)?((double)(nonEmittedTelegrams * 100) / (double)(nonEmittedTelegrams + emittedTelegrams)):0);
		averageEmissionSpeed = ((period != 0)?((double)(nonEmittedTelegrams + emittedTelegrams) * 1000 / (double)period):0);
	}
	
	@Override
	public long receivedBytes() 
	{
		return receivedBytes;
	}
	
	@Override
	public long discardedBytes() 
	{
		return discardedBytes;
	}
	
	@Override
	public double discardedBytesRatio() 
	{
		return discardedBytesRatio;
	}

	@Override
	public long emittedBytes() 
	{
		return emittedBytes;
	}
	
	@Override 
	public String getTcmUID()
	{
		return tcmUID;
	}

	@Override 
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("-----------------------------------\n");
		sb.append("- Enocean diagnostic informations -\n");
		sb.append("-----------------------------------\n");
		sb.append("Running for ");
		sb.append(nf.format((double)(period) / 1000d));
		sb.append(" sec\n");
		sb.append("-----------------------------------\n");
		sb.append("\n");
		sb.append("Serial level\n");
		sb.append("\n");
		sb.append(" * Received bytes: ");
		sb.append(receivedBytes);
		sb.append("\n");
		sb.append(" * Discarded bytes: ");
		sb.append(discardedBytes);
		sb.append("\n");
		sb.append(" * Discard ratio: ");
		sb.append(nf.format(discardedBytesRatio));
		sb.append(" %\n");
		sb.append(" * Emitted bytes: ");
		sb.append(emittedBytes);
		sb.append("\n");
		sb.append("\n-----------------------------------\n");
		sb.append("\n");
		sb.append("Enocean level\n");
		sb.append("\n");
		sb.append(" * TCM UID: ");
		sb.append(tcmUID);
		sb.append("\n");
		sb.append(" * TCM Sensitivity: ");
		sb.append(tcmSensitivity);
		sb.append("\n");
		sb.append("\n");
		sb.append(" * Receptions\n");
		sb.append("  - Successful telegrams: ");
		sb.append(receivedTelegrams);
		sb.append("\n");
		sb.append("  - Illegal telegrams: ");
		sb.append(illegalTelegrams);
		sb.append("\n");
		sb.append("  - Failure ratio: ");
		sb.append(nf.format(illegalTelegramsRatio));
		sb.append(" %\n");
		sb.append("  - Average throughput: ");
		sb.append(nf.format(averageReceptionSpeed));
		sb.append(" t/sec\n");
		sb.append("  - Average decoding time: ");
		sb.append(nf.format(averageDecodingTime));
		sb.append(" ms/t\n");
		sb.append("  - Average processing time: ");
		sb.append(nf.format(averageProcessingTime));
		sb.append(" ms/t\n");
		sb.append("\n * Emissions\n");
		sb.append("  - Successfull: ");
		sb.append(emittedTelegrams);
		sb.append("\n");
		sb.append("  - Failures: ");
		sb.append(nonEmittedTelegrams);
		sb.append("\n");
		sb.append("  - Failure ratio: ");
		sb.append(nf.format(nonEmittedRatio));
		sb.append(" %\n");
		sb.append("  - Average throughput: ");
		sb.append(nf.format(averageEmissionSpeed));
		sb.append(" t/sec\n");
		
		return sb.toString();
	}
}
