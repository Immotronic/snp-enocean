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

public interface EnoceanTCMManager 
{
	public enum TCMSettingResponse {
		OK,
		ERR_MORE_THAN_10_TIMES,
		ERR_RANGE,
		INVALID_VALUE,
		NOT_READY,
		WRITING_FAILED
	}
	
	/**
	 * Return an hexadecimal string that represent the current TCM base ID. This ID is a 32 bits integer.
	 * @return an hexadecimal string that represent the current TCM base ID.
	 */
	String getTransceiverBaseID();
	
	/**
	 * Write a new TCM base ID in the TCM flash memory. This ID MUST be a 32 bits integer represented as an hexadecimal string. The allowed range for this integer is 0xff800000 to 0xfffffffe.
	 * @param baseID a hexadecimal string that represent a 32 bits integer in the range 0xff800000 to 0xfffffffe.
	 * @return a TCMSettingResponse value : OK if the new TCM base ID has been successfully written in the TCM flash memory. 
	 * 			Otherwise, return ERR if the maximum number of 10 times is exceeded, ERR_RANGE if the new ID is not in the range 0xff800000 to 0xfffffffe, 
	 * 			and  INVALID_VALUE if the given string is not parsable as an integer.
	 * 			This method return NOT_READY if the communication cannot be established with the TCM.
	 */
	TCMSettingResponse setTransceiverBaseID(String baseID);
}
