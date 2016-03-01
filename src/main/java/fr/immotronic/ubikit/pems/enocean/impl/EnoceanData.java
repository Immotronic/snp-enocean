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

/**
 * A class that encapsulate data from an Enocean device. These
 * data could be read as a 32 bits integer, two 16 bits integer
 * or as an array of 4 bytes.
 */
public final class EnoceanData 
{
	private final byte[] data;
	private final Date date;
	
	
	/** 
	 * Construct an Enocean data object.
	 * 
	 * @param byte_3 the fourth and most significant byte of data
	 * @param byte_2 the third byte of data
	 * @param byte_1 the second byte of data
	 * @param byte_0 the first and less significant byte of data
	 */
	@Deprecated
	public EnoceanData(byte byte_3, byte byte_2, byte byte_1, byte byte_0, Date date)
	{
		data = new byte[4];
		data[3] = byte_3;
		data[2] = byte_2;
		data[1] = byte_1;
		data[0] = byte_0;
		this.date = date;
	}
	
	/** 
	 * Construct an Enocean data object.
	 * 
	 * data[0] is considered as the first and less significant byte of data.
	 */
	public EnoceanData(byte[] data, Date date)
	{
		this.data = data;
		this.date = date;
	}
	
	/**
	 * Get data value as a 32 bits integer
	 * 
	 * @return data as a 32 bits integer
	 */
	@Deprecated
	public int get32bytesValue()
	{
		return (data[3] & 0xff) << 24 | (data[2] & 0xff) << 16 | (data[1] & 0xff) << 8 | (data[0] & 0xff);
	}
	
	/**
	 * Get the most significant 16 bits integer of data
	 * 
	 * @return The most significant 16 bits as an integer value. 
	 */
	@Deprecated
	public short getHi16bytesValue()
	{
		return (short)((data[3] & 0xff) << 8 | (data[2] & 0xff));
	}
	
	/**
	 * Get the less significant 16 bits integer of data
	 * 
	 * @return The less significant 16 bits as an integer value. 
	 */
	@Deprecated
	public short getLo16bytesValue()
	{
		return (short)((data[1] & 0xff) << 8 | (data[0] & 0xff));
	}
	
	/**
	 * Get data as an array of 4 bytes, the most significant byte is
	 * at index 3, the less significant byte is at index 0. 
	 * 
	 * @return data as an array of 4 bytes. 
	 */
	public byte[] getBytes()
	{
		return data;
	}
	
	public Date getDate()
	{
		return date;
	}
	
	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		sb.append("MSB [ ");
		for (int i = data.length-1; i >= 0; i--)
		{
			sb.append(" "+Integer.toHexString(data[i]));
		}
		sb.append(" ] LSB");
		return sb.toString();
	}
}
