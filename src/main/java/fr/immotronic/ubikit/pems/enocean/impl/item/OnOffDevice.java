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

package fr.immotronic.ubikit.pems.enocean.impl.item;

import org.json.JSONObject;

import fr.immotronic.ubikit.pems.enocean.ActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.ControllerProfile;
import fr.immotronic.ubikit.pems.enocean.event.in.TurnOffActuatorEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.TurnOnActuatorEvent;
import fr.immotronic.ubikit.pems.enocean.impl.DatabaseManager;
import fr.immotronic.ubikit.pems.enocean.impl.DeviceManager;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanActuatorDeviceImpl;
import fr.immotronic.ubikit.pems.enocean.impl.NoControllerChannelLeftException;

public final class OnOffDevice extends EnoceanActuatorDeviceImpl implements TurnOnActuatorEvent.Listener, TurnOffActuatorEvent.Listener
{
	private static int delayBetweenCommandAndRelease = 250; // milliseconds.
	
	private int controllerChannel;
	private FourChannelPTMController controller;
	
	public OnOffDevice(DatabaseManager.Record record, DeviceManager deviceManager)
	{
		super(record.getUID(), ActuatorProfile.ONOFF_DEVICE, deviceManager);
		this.setPropertiesFromJSONObject(record.getPropertiesAsJSON());
		String[] data = record.getData().split(" ");
		controllerChannel = Integer.parseInt(data[0]);
		controller = (FourChannelPTMController) deviceManager.getController(Integer.parseInt(data[1]));
		controller.markChannelAsUsed(controllerChannel);
	}
	
	public OnOffDevice(String actuatorUID, DeviceManager deviceManager) throws NoControllerChannelLeftException 
	{
		super(actuatorUID, ActuatorProfile.ONOFF_DEVICE, deviceManager);
		
		controller = (FourChannelPTMController) deviceManager.getController(ControllerProfile.FOUR_CHANNEL_PTM_CONTROLLER, 1);
		if(controller == null)
		{
			throw new NoControllerChannelLeftException();
		}
		controllerChannel = controller.getFreeChannel();
	}
	
	public void setOn()
	{
		if(controller != null)
		{
			controller.setChannelOn(controllerChannel);
			try { Thread.sleep(delayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
			controller.setChannelReleased(controllerChannel);
		}
	}
	
	public void setOff()
	{
		if(controller != null)
		{
			controller.setChannelOff(controllerChannel);
			try { Thread.sleep(delayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
			controller.setChannelReleased(controllerChannel);
		}
	}
	
	@Override
	public FourChannelPTMController releaseController()
	{
		if(controller != null)
		{
			controller.releaseChannel(controllerChannel);
			FourChannelPTMController c = controller;
			controller = null;
			return c;
		}
		
		return null;
	}

	@Override
	public void onEvent(TurnOnActuatorEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()) && controller != null)
		{
			setOn();
		}
	}

	@Override
	public void onEvent(TurnOffActuatorEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()) && controller != null)
		{
			setOff();
		}
	}
	
	@Override
	public DatabaseManager.Record getDeviceRecord()
	{
		DatabaseManager.Record rec = this.getPreFilledRecordWithoutData();
		rec.setData(Integer.toString(controllerChannel)+" "+Integer.toString(controller.getControllerIndex()));
		
		return rec;
	}

	@Override
	public Object getValue() 
	{
		return null;
	}
	
	@Override
	public JSONObject getValueAsJSON()
	{
		return null;
	}
	
	@Override
	public void sendPairingSignals(int nbOfSignals)
	{
		if(controller != null)
		{
			for(int i = 0; i < nbOfSignals; i++)
			{
				controller.setChannelOn(controllerChannel);
				try { Thread.sleep(delayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
				controller.setChannelReleased(controllerChannel);
				try { Thread.sleep(delayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
				controller.setChannelReleased(controllerChannel);
			}
		}
	}
	
	@Override
	protected void terminate() 
	{
		// Release channel should be done here, not in deviceManager.
	}
}
