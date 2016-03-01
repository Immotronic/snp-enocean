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

package fr.immotronic.ubikit.pems.enocean.data;

import fr.immotronic.ubikit.pems.enocean.EnoceanSensorData;

public interface EEP07201011Data extends EnoceanSensorData 
{
	public enum Mode 
	{
		AUTO((byte) 0x00),
		HEAT((byte) 0x01),
		MORNING_WARMUP((byte) 0x02),
		COOL((byte) 0x03),
		NIGHT_PURGE((byte) 0x04),
		PRECOOL((byte) 0x05),
		OFF((byte) 0x06),
		TEST((byte) 0x07),
		EMERGENCY_HEAT((byte) 0x08),
		FAN_ONLY((byte) 0x09),
		FREECOOL((byte) 0x0a),
		ICE((byte) 0x0b),
		MAX_HEAT((byte) 0x0c),
		ECONOMIC((byte) 0x0d),
		DRY((byte) 0X0e),
		CALIBRATION((byte) 0x0f),
		EMERGENCY_COOL((byte) 0x10),
		EMERGENCY_STEAM((byte) 0x11),
		MAX_COOL((byte) 0x12),
		HVC_LOAD((byte) 0x13),
		NO_LOAD((byte) 0x14),
		/*
		 * 0x15 ... 0x1e : RESERVED FOR FUTURE USE
		 */
		AUTO_LOAD((byte) 0x1f),
		AUTO_COOL((byte) 0x20),
		NO_ACTION((byte) 0xff);
		
		private byte value = 0;
		
		private Mode(byte mode)	{
			this.value = mode;
		}
		
		public static Mode getValueOf(byte value)
		{
			for (Mode mode : Mode.values())
			{
				if (mode.getValue() == value)
					return mode;
			}
			
			return null;
		}
		
		public byte getValue(){
			return value;
		}
	}
	
	public enum VanePosition
	{
		AUTO((byte) 0x00),
		HORIZONTAL((byte) 0x01),
		POSITION_2((byte) 0x02),
		POSITION_3((byte) 0x03),
		POSITION_4((byte) 0x04),
		VERTICAL((byte) 0x05),
		SWING((byte) 0x06),
		/*
		 * 0x07 ... 0x0a : RESERVED FOR FUTURE USE
		 */
		VERTICAL_SWING((byte) 0x0b),
		HORIZONTAL_SWING((byte) 0x0c),
		HORIZONTAL_AND_VERTICAL_SWING((byte) 0x0d),
		STOP_SWING((byte) 0x0e),
		NO_ACTION((byte) 0x0f);
		
		private byte value = 0;
		
		private VanePosition(byte position)	{
			this.value = position;
		}
		
		public static VanePosition getValueOf(byte value)
		{
			for (VanePosition position : VanePosition.values())
			{
				if (position.getValue() == value)
					return position;
			}
			return null;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	public enum FanSpeed
	{
		AUTO((byte) 0x00),
		SPEED_1((byte) 0x01),
		SPEED_2((byte) 0x02),
		SPEED_3((byte) 0x03),
		SPEED_4((byte) 0x04),
		SPEED_5((byte) 0x05),
		SPEED_6((byte) 0x06),
		SPEED_7((byte) 0x07),
		SPEED_8((byte) 0x08),
		SPEED_9((byte) 0x09),
		SPEED_10((byte) 0x0a),
		SPEED_11((byte) 0x0b),
		SPEED_12((byte) 0x0c),
		SPEED_13((byte) 0x0d),
		SPEED_14((byte) 0x0e),
		SPEED_MAX((byte) 0x04),
		NO_ACTION((byte) 0x0f);
		
		private byte value = 0;
		
		private FanSpeed(byte speed) {
			this.value = speed;
		}
		
		public static FanSpeed getValueOf(byte value)
		{
			for (FanSpeed speed : FanSpeed.values())
			{
				if (speed.getValue() == value)
					return speed;
			}
			return null;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	public enum RoomOccupancy
	{
		OCCUPIED((byte) 0x00),
		STANDBY((byte) 0x01),
		UNOCCUPIED((byte) 0x02),
		OFF((byte) 0x03);
		
		private byte value = 0;
		
		private RoomOccupancy(byte occupancy) {
			value = occupancy;
		}
		
		public static RoomOccupancy getValueOf(byte value)
		{
			for (RoomOccupancy roomOccupancy : RoomOccupancy.values())
			{
				if (roomOccupancy.getValue() == value)
					return roomOccupancy;
			}
			return null;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	public enum OnOffStatus
	{
		ON((byte) 0x01),
		OFF((byte) 0x00);
		
		private byte value = 0;
		
		private OnOffStatus(byte onoff) {
			value = onoff;
		}
		
		public static OnOffStatus getValueOf(byte value)
		{
			for (OnOffStatus status : OnOffStatus.values())
			{
				if (status.getValue() == value)
					return status;
			}
			return null;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	public enum Disablement
	{
		NOT_DISABLED((byte) 0x00),
		DISABLED((byte) 0x01);
		
		private byte value = 0;
		
		private Disablement(byte disablement) {
			value = disablement;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	public enum WindowsStatus
	{
		WINDOWS_OPENED((byte) 0x00),
		WINDOWS_CLOSED((byte) 0x01);
		
		private byte value = 0;
		
		private WindowsStatus(byte status) {
			value = status;
		}
		
		public byte getValue() {
			return value;
		}
	}
	
	public enum AlarmState
	{
		OK,
		ERROR
	}	
	
	public Mode getMode();
	public VanePosition getVanePosition();
	public FanSpeed getFanSpeed();
	public RoomOccupancy getRoomOccupancy();
	public OnOffStatus getOnOffStatus();
	
	public int getErrorCode();
	public Disablement getWindowContactDisablement();
	public Disablement getKeyCardDisablement();
	public Disablement getExternalDisablement();
	public Disablement getRemoteControllerDisablement();
	public WindowsStatus getWindowContact();
	public AlarmState getAlarmState();
}


