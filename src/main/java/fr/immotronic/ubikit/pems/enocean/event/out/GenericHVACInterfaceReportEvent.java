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

package fr.immotronic.ubikit.pems.enocean.event.out;

import java.util.Date;

import org.ubikit.AbstractPhysicalEnvironmentModelEvent;
import org.ubikit.event.EventListener;

import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.FanSpeed;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.Mode;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.OnOffStatus;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.RoomOccupancy;
import fr.immotronic.ubikit.pems.enocean.data.EEP07201011Data.VanePosition;

public class GenericHVACInterfaceReportEvent extends AbstractPhysicalEnvironmentModelEvent 
{
	private final Mode mode;
	private final VanePosition vanePosition;
	private final FanSpeed fanSpeed;
	private final RoomOccupancy roomOccupancy;
	private final OnOffStatus onOffStatus;
	
	public interface Listener extends EventListener 
	{
		public void onEvent(GenericHVACInterfaceReportEvent event);
	}
	
	public GenericHVACInterfaceReportEvent(String sourceItemUID, Mode mode, VanePosition vanePosition, FanSpeed fanSpeed, RoomOccupancy roomOccupancy, OnOffStatus onOffStatus, Date date)
	{
		super(sourceItemUID, date);
		this.mode = mode;
		this.vanePosition = vanePosition;
		this.fanSpeed = fanSpeed;
		this.roomOccupancy = roomOccupancy;
		this.onOffStatus = onOffStatus;
	}
	
	/**
	 * Returns HVAC's mode.
	 * @return HVAC's mode.
	 */
	public Mode getMode()
	{
		return mode;
	}
	
	/**
	 * Returns HVAC's vane position.
	 * @return HVAC's vane position.
	 */
	public VanePosition getVanePosition()
	{
		return vanePosition;
	}
	
	/**
	 * Returns HVAC's fan speed.
	 * @return HVAC's fan speed.
	 */
	public FanSpeed getFanSpeed()
	{
		return fanSpeed;
	}
	
	/**
	 * Returns HVAC's value of room occupancy.
	 * @return HVAC's value of room occupancy.
	 */
	public RoomOccupancy getRoomOccupancy()
	{
		return roomOccupancy;
	}
	
	/**
	 * Returns HVAC's ON/OFF status.
	 * @return HVAC's ON/OFF status. 
	 */
	public OnOffStatus getOnOrOff()
	{
		return onOffStatus;
	}

	@Override
	public void deliverTo(EventListener listener) 
	{
		((GenericHVACInterfaceReportEvent.Listener)listener).onEvent(this);
	}
}
