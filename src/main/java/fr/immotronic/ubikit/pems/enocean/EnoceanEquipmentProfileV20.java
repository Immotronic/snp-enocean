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

package fr.immotronic.ubikit.pems.enocean;

public enum EnoceanEquipmentProfileV20 
{
	NONE(0,0,0, ""),
	EEP_05_02_01 (0x5, 0x2, 0x1, "Rocker Switch, 2 Rockers"),
	EEP_05_02_02 (0x5, 0x2, 0x2, "Rocker Switch, 2 Rockers"),
	EEP_05_03_01 (0x5, 0x3, 0x1, "Rocker Switch, 4 Rockers"),
	EEP_05_03_02 (0x5, 0x3, 0x2, "Rocker Switch, 4 Rockers"),
	EEP_05_04_01 (0x5, 0x4, 0x1, "Key Card Activated Switch"),
	EEP_05_10_00 (0x5, 0x10, 0x0, "Window Handle"),
	EEP_05_xx_xx (0x5, 0,0,"PTM based device"),
	ELTAKO_FRW_WS_SMOKE_ALARM (0x5, 0, 0, "Smoke Alarm Device (Eltako FRW-ws)"),
	EEP_06_00_01 (0x6, 0x0, 0x1, "Single Input Contact"),
	EEP_07_02_01 (0x7, 0x2, 0x1, "Temperature Sensor: -40&deg;C..0&deg;C"),
	EEP_07_02_02 (0x7, 0x2, 0x2, "Temperature Sensor: -30&deg;C..10&deg;C"),
	EEP_07_02_03 (0x7, 0x2, 0x3, "Temperature Sensor: -20&deg;C..20&deg;C"),
	EEP_07_02_04 (0x7, 0x2, 0x4, "Temperature Sensor: -10&deg;C..30&deg;C"),
	EEP_07_02_05 (0x7, 0x2, 0x5, "Temperature Sensor: 0&deg;C..40&deg;C"),
	EEP_07_02_06 (0x7, 0x2, 0x6, "Temperature Sensor: 10&deg;C..50&deg;C"),
	EEP_07_02_07 (0x7, 0x2, 0x7, "Temperature Sensor: 20&deg;C..60&deg;C"),
	EEP_07_02_08 (0x7, 0x2, 0x8, "Temperature Sensor: 30&deg;C..70&deg;C"),
	EEP_07_02_09 (0x7, 0x2, 0x9, "Temperature Sensor: 40&deg;C..80&deg;C"),
	EEP_07_02_0A (0x7, 0x2, 0xa, "Temperature Sensor: 50&deg;C..90&deg;C"),
	EEP_07_02_0B (0x7, 0x2, 0xb, "Temperature Sensor: 60&deg;C..100&deg;C"),
	EEP_07_02_10 (0x7, 0x2, 0x10, "Temperature Sensor: -60&deg;C..20&deg;C"),
	EEP_07_02_11 (0x7, 0x2, 0x11, "Temperature Sensor: -50&deg;C..30&deg;C"),
	EEP_07_02_12 (0x7, 0x2, 0x12, "Temperature Sensor: -40&deg;C..40&deg;C"),
	EEP_07_02_13 (0x7, 0x2, 0x13, "Temperature Sensor: -30&deg;C..50&deg;C"),
	EEP_07_02_14 (0x7, 0x2, 0x14, "Temperature Sensor: -20&deg;C..60&deg;C"),
	EEP_07_02_15 (0x7, 0x2, 0x15, "Temperature Sensor: -10&deg;C..70&deg;C"),
	EEP_07_02_16 (0x7, 0x2, 0x16, "Temperature Sensor: 0&deg;C..80&deg;C"),
	EEP_07_02_17 (0x7, 0x2, 0x17, "Temperature Sensor: 10&deg;C..90&deg;C"),
	EEP_07_02_18 (0x7, 0x2, 0x18, "Temperature Sensor: 20&deg;C..100&deg;C"),
	EEP_07_02_19 (0x7, 0x2, 0x19, "Temperature Sensor: 30&deg;C..110&deg;C"),
	EEP_07_02_1A (0x7, 0x2, 0x1a, "Temperature Sensor: 40&deg;C..120&deg;C"),
	EEP_07_02_1B (0x7, 0x2, 0x1b, "Temperature Sensor: 50&deg;C..130&deg;C"),
	EEP_07_04_01 (0x7, 0x4, 0x1, "Temperature Sensor: 0&deg;C..40&deg;C & Humidity Sensor"),
	EEP_07_04_02 (0x7, 0x4, 0x2, "Temperature Sensor: -20&deg;C..+60&deg;C & Humidity Sensor"),
	EEP_07_06_01 (0x7, 0x6, 0x1, "Light Sensor: Range 300..60000 lx"),
	EEP_07_06_02 (0x7, 0x6, 0x2, "Light Sensor: Range 0..1024 lx"),
	EEP_07_07_01 (0x7, 0x7, 0x1, "Occupancy Sensor"),
	EEP_07_08_01 (0x7, 0x8, 0x1, "Light, Temperature & Occupancy Sensor: 0..510 lx, 0&deg;C..51&deg;C"),
	EEP_07_08_02 (0x7, 0x8, 0x2, ""),
	EEP_07_08_03 (0x7, 0x8, 0x3, ""),
	EEP_07_09_01 (0x7, 0x9, 0x1, ""),
	EEP_07_09_04 (0x7, 0x9, 0x4, "Temperature, Humidity & CO2 Sensor"),
	EEP_07_09_05 (0x7, 0x9, 0x5, "Gas Sensor"),
	EEP_07_09_08 (0x7, 0x9, 0x8, ""),
	EEP_07_09_0C (0x7, 0x9, 0xc, ""),
	EEP_07_10_01 (0x7, 0x10, 0x1, "Temperature Sensor 0&deg;C..40&deg;C, Set Point, Fan Speed & Occupancy Controls"),
	EEP_07_10_02 (0x7, 0x10, 0x2, "Temperature Sensor 0&deg;C..40&deg;C, Set Point, Fan Speed & Day/Night Controls"),
	EEP_07_10_03 (0x7, 0x10, 0x3, "Temperature Sensor 0&deg;C..40&deg;C, Set Point Control"),
	EEP_07_10_04 (0x7, 0x10, 0x4, "Temperature Sensor 0&deg;C..40&deg;C, Set Point & Fan Speed Controls"),
	EEP_07_10_05 (0x7, 0x10, 0x5, "Temperature Sensor 0&deg;C..40&deg;C, Set Point & Occupancy Controls"),
	EEP_07_10_06 (0x7, 0x10, 0x6, "Temperature Sensor 0&deg;C..40&deg;C, Set Point & Day/Night Controls"),
	EEP_07_10_07 (0x7, 0x10, 0x7, "Temperature Sensor 0&deg;C..40&deg;C & Fan Speed Control"),
	EEP_07_10_08 (0x7, 0x10, 0x8, "Temperature Sensor 0&deg;C..40&deg;C, Fan Speed & Occupancy Controls"),
	EEP_07_10_09 (0x7, 0x10, 0x9, "Temperature Sensor 0&deg;C..40&deg;C, Fan Speed & Day/Night Controls"),
	EEP_07_10_0A (0x7, 0x10, 0xa, ""),
	EEP_07_10_0B (0x7, 0x10, 0xb, ""),
	EEP_07_10_0C (0x7, 0x10, 0xc, ""),
	EEP_07_10_0D (0x7, 0x10, 0xd, ""),
	EEP_07_10_10 (0x7, 0x10, 0x10, "Temperature & Humidity Sensor, Set Point & Occupancy Controls"),
	EEP_07_10_11 (0x7, 0x10, 0x11, "Temperature & Humidity Sensor, Set Point & Day/Night Controls"),
	EEP_07_10_12 (0x7, 0x10, 0x12, "Temperature & Humidity Sensor, Set Point Control"),
	EEP_07_10_13 (0x7, 0x10, 0x13, "Temperature & Humidity Sensor, Occupancy Control"),
	EEP_07_10_14 (0x7, 0x10, 0x14, "Temperature & Humidity Sensor, Day/Night Control"),
	EEP_07_11_01 (0x7, 0x11, 0x1, ""),
	EEP_07_20_10 (0x7, 0x20, 0x10, "Generic HVAC interface"),
	EEP_07_20_11 (0x7, 0x20, 0x11, "Generic HVAC interface - Error control"),
	EEP_07_30_01 (0x7, 0x30, 0x1, ""),
	EEP_07_30_02 (0x7, 0x30, 0x2, ""),
	EEP_07_38_08 (0x7, 0x38, 0x8, ""),
	EEP_07_3F_7F (0x7, 0x3F, 0x7F, ""),
	EEP_A5_12_00 (0x7, 0x12, 0x00, "Automated meter reading, Multi-Channels Counter"),
	EEP_A5_12_01 (0x7, 0x12, 0x01, "Automated meter reading, Electricity"),
	EEP_A5_12_02 (0x7, 0x12, 0x02, "Automated meter reading, Gas"),
	EEP_A5_12_03 (0x7, 0x12, 0x03, "Automated meter reading, Water"),
	EEP_D2_01_00 (0xd2, 0x01, 0x00, "Electronic Switch with Energy Measurement and Local Control, Type 0"),
	EEP_D2_01_02 (0xd2, 0x01, 0x02, "Electronic Switch with Energy Measurement and Local Control, Type 2"),
	EEP_D2_01_06 (0xd2, 0x01, 0x06, "Electronic Switch with Energy Measurement and Local Control, Type 6"),
	EEP_D2_01_11 (0xd2, 0x01, 0x11, "Electronic Switch with Energy Measurement and Local Control, Type 11");
	
	private int org;
	private int function;
	private int type;
	private String description;
	
	public static EnoceanEquipmentProfileV20 selectEEP(int org, int function, int type)
	{
		EnoceanEquipmentProfileV20[] values = EnoceanEquipmentProfileV20.values();
		for(EnoceanEquipmentProfileV20 value : values)
		{
			if(value.isMatching(org, function, type)) {
				return value;
			}
		}
		
		return null;
	}
	
	private EnoceanEquipmentProfileV20(int org, int function, int type, String description)
	{
		this.org = org;
		this.function = function;
		this.type = type;
		this.description = description;
	}
	
	public int getFunction()
	{
		return function;
	}
	
	public int getType()
	{
		return type;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	private boolean isMatching(int org, int function, int type)
	{
		return (this.org == org) && (this.function == function) && (this.type == type);
	}
}
