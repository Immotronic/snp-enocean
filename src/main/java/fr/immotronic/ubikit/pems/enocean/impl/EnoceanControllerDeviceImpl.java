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

import org.ubikit.PhysicalEnvironmentItem;

import fr.immotronic.ubikit.pems.enocean.ControllerProfile;
import fr.immotronic.ubikit.pems.enocean.EnoceanControllerDevice;

public abstract class EnoceanControllerDeviceImpl extends EnoceanDeviceImpl implements EnoceanControllerDevice 
{
	private ControllerProfile controllerProfile;
	private EnoceanSerialAdapter enoceanSerialAdapter;
	
	/*protected EnoceanControllerDeviceImpl(long enoceanUID, ControllerProfile controllerProfile, DeviceManager deviceManager) 
	{
		this((int)enoceanUID, controllerProfile, deviceManager);
	}*/
	
	public static int getControllerIndexFromEnoceanUID(int enoceanUID)
	{
		return (enoceanUID & 0x7F);
	}
	
	protected EnoceanControllerDeviceImpl(int controllerUID, ControllerProfile controllerProfile, EnoceanSerialAdapter enoceanSerialAdapter, DeviceManager deviceManager) 
	{
		super(enoceanSerialAdapter.getEnoceanTransceiverID()+controllerUID, PhysicalEnvironmentItem.Type.OTHER, deviceManager, null);
		this.controllerProfile = controllerProfile;
		this.enoceanSerialAdapter = enoceanSerialAdapter;
	}

	protected EnoceanControllerDeviceImpl(String UID, int enoceanUID, ControllerProfile controllerProfile, EnoceanSerialAdapter enoceanSerialAdapter, DeviceManager deviceManager) 
	{
		super(UID, enoceanUID, PhysicalEnvironmentItem.Type.OTHER, deviceManager, null);
		this.controllerProfile = controllerProfile;
		this.enoceanSerialAdapter = enoceanSerialAdapter;
	}
	
	protected DatabaseManager.Record getPreFilledRecordWithoutData()
	{
		return new DatabaseManager.Record(getUID(), getPropertiesAsJSONObject().toString(), getEnoceanUID(), controllerProfile, null);
	}
	
	protected EnoceanSerialAdapter getSerialAdapter()
	{
		return enoceanSerialAdapter;
	}

	@Override
	public ControllerProfile getControllerProfile() 
	{
		return controllerProfile;
	}

	@Override
	public int getControllerIndex()
	{
		return getControllerIndexFromEnoceanUID(getEnoceanUID());
	}
}
