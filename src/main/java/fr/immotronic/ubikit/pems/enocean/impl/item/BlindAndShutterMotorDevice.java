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

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;

import fr.immotronic.ubikit.pems.enocean.ActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.ControllerProfile;
import fr.immotronic.ubikit.pems.enocean.event.in.MoveDownBlindOrShutterEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.MoveUpBlindOrShutterEvent;
import fr.immotronic.ubikit.pems.enocean.event.in.StopBlindOrShutterEvent;
import fr.immotronic.ubikit.pems.enocean.impl.DatabaseManager;
import fr.immotronic.ubikit.pems.enocean.impl.DeviceManager;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanActuatorDeviceImpl;
import fr.immotronic.ubikit.pems.enocean.impl.LC;
import fr.immotronic.ubikit.pems.enocean.impl.NoControllerChannelLeftException;

public final class BlindAndShutterMotorDevice extends EnoceanActuatorDeviceImpl 
				implements 	MoveUpBlindOrShutterEvent.Listener, 
							MoveDownBlindOrShutterEvent.Listener, 
							StopBlindOrShutterEvent.Listener
{
	private static int shortDelayBetweenCommandAndRelease = 250; // milliseconds.
	private static int standardDelayBetweenCommandAndRelease = 400; // milliseconds.
	private static int longDelayBetweenCommandAndRelease = 800; // milliseconds.
	
	private enum Movement {
		MovingUp,
		Stopped,
		MovingDown
	}
	
	private int controllerChannel;
	private FourChannelPTMController controller;
	private Movement currentMovement;
	private boolean inverted;
	private boolean commandOnShortPressing;
	
	public BlindAndShutterMotorDevice(DatabaseManager.Record record, DeviceManager deviceManager)
	{
		super(record.getUID(), ActuatorProfile.BLIND_AND_SHUTTER_MOTOR_DEVICE, deviceManager);
		this.setPropertiesFromJSONObject(record.getPropertiesAsJSON());
		String[] data = record.getData().split(" ");
		controllerChannel = Integer.parseInt(data[0]);
		controller = (FourChannelPTMController) deviceManager.getController(Integer.parseInt(data[1]));
		controller.markChannelAsUsed(controllerChannel);
		currentMovement = Movement.Stopped;
		inverted = Boolean.parseBoolean(data[2]);
		if(data.length > 3)
		{
			commandOnShortPressing = Boolean.parseBoolean(data[3]);
		}
		else
		{
			commandOnShortPressing = false;
		}
	}
	
	public BlindAndShutterMotorDevice(String actuatorUID, DeviceManager deviceManager) throws NoControllerChannelLeftException 
	{
		super(actuatorUID, ActuatorProfile.BLIND_AND_SHUTTER_MOTOR_DEVICE, deviceManager);
		
		controller = (FourChannelPTMController) deviceManager.getController(ControllerProfile.FOUR_CHANNEL_PTM_CONTROLLER, 1);
		if(controller == null)
		{
			throw new NoControllerChannelLeftException();
		}
		controllerChannel = controller.getFreeChannel();
		currentMovement = Movement.Stopped;
		inverted = false;
		commandOnShortPressing = false;
	}
	
	public void moveUp()
	{
		move(Movement.MovingUp);
	}
	
	public void stop()
	{
		move(Movement.Stopped);
	}
	
	public void moveDown()
	{
		move(Movement.MovingDown);
	}
	
	public void setInverted(boolean inverted)
	{
		this.inverted = inverted;
		propertiesHaveBeenUpdated(null); // no need to detail which property has been updated.
		
		if(LC.debug) {
			if(this.inverted) {
				Logger.debug(LC.gi(), this, "Motor is inverted");
			}
			else {
				Logger.debug(LC.gi(), this, "Motor is NO MORE inverted");
			}
		}
	}
	
	public void setCommandOnShortPressing(boolean shortPressing)
	{
		commandOnShortPressing = shortPressing;
		propertiesHaveBeenUpdated(null); // no need to detail which property has been updated.
		
		if(LC.debug) {
			if(commandOnShortPressing) {
				Logger.debug(LC.gi(), this, "Commands need SHORT pressing");
			}
			else {
				Logger.debug(LC.gi(), this, "Commands need LONG pressing");
			}
		}
	}
	
	public void send1()
	{
		if(controller != null)
		{
			controller.setChannelOn(controllerChannel);
			try { Thread.sleep(standardDelayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
			controller.setChannelReleased(controllerChannel);
		}
	}
	
	public void send0()
	{
		if(controller != null)
		{
			controller.setChannelOff(controllerChannel);
			try { Thread.sleep(standardDelayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
			controller.setChannelReleased(controllerChannel);
		}
	}
	
	@Override
	public FourChannelPTMController releaseController()
	{
		controller.releaseChannel(controllerChannel);
		FourChannelPTMController c = controller;
		controller = null;
		return c;
	}

	@Override
	public void onEvent(MoveUpBlindOrShutterEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()))
		{
			moveUp();
		}
	}
	
	@Override
	public void onEvent(StopBlindOrShutterEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()))
		{
			stop();
		}
	}

	@Override
	public void onEvent(MoveDownBlindOrShutterEvent event) 
	{
		if(event.getSourceItemUID().equals(getUID()))
		{
			moveDown();
		}
	}
	
	@Override
	public DatabaseManager.Record getDeviceRecord()
	{
		DatabaseManager.Record rec = this.getPreFilledRecordWithoutData();
		rec.setData(Integer.toString(controllerChannel)+" "+Integer.toString(controller.getControllerIndex())+" "+inverted+" "+commandOnShortPressing);
		
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
		JSONObject o = new JSONObject();
		
		try {
			o.put("inverted", inverted);
			o.put("commandOnShortPressing", commandOnShortPressing);
		} 
		catch (JSONException e) {
			Logger.error(LC.gi(), this, "While building JSON value object.", e);
			return null;
		}
		
		return o;
	}
	
	@Override
	public void sendPairingSignals(int nbOfSignals)
	{
		if(controller != null)
		{
			for(int i = 0; i < nbOfSignals; i++)
			{
				controller.setChannelOn(controllerChannel);
				try { Thread.sleep(standardDelayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
				controller.setChannelReleased(controllerChannel);
				try { Thread.sleep(standardDelayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
				controller.setChannelReleased(controllerChannel);
			}
		}
	}

	@Override
	protected void terminate() 
	{
		// Release channel should be done here, not in deviceManager.
	}
	
	private void move(Movement movement) 
	{
		if(controller != null)
		{
			switch(movement)
			{
				case MovingUp:
					if(!inverted) {
						controller.setChannelOn(controllerChannel);
					}
					else {
						controller.setChannelOff(controllerChannel);
					}
					
					if(commandOnShortPressing)
					{
						try { Thread.sleep(longDelayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
						controller.setChannelReleased(controllerChannel);
					}
					
					currentMovement = Movement.MovingUp;
					break;
					
				case Stopped:
					if(currentMovement != Movement.Stopped) 
					{
						controller.setChannelOn(controllerChannel);
						try { Thread.sleep(shortDelayBetweenCommandAndRelease); } catch(InterruptedException e) {}
						controller.setChannelReleased(controllerChannel);
						currentMovement = Movement.Stopped;
					}
					break;
					
				case MovingDown:
					if(!inverted) {
						controller.setChannelOff(controllerChannel);
					}
					else {
						controller.setChannelOn(controllerChannel);
					}
					
					if(commandOnShortPressing)
					{
						try { Thread.sleep(longDelayBetweenCommandAndRelease); } catch(InterruptedException e) { return; };
						controller.setChannelReleased(controllerChannel);
					}
					
					currentMovement = Movement.MovingDown;
					break;
			}
		}
	}
}
