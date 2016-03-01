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

import java.util.ArrayList;

import org.json.JSONException;
import org.json.JSONObject;
import org.ubikit.Logger;

import fr.immotronic.ubikit.pems.enocean.ActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.ControllerProfile;
import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;

public interface DatabaseManager 
{
	public class Record {
		private String UID;
		private int manufacturerUID;
		private JSONObject propertiesAsJSON;
		private int enoceanUID;
		private EnoceanEquipmentProfileV20 eep;
		private ActuatorProfile ap;
		private ControllerProfile cp;
		private SensorActuatorProfile sap;
		private String data;
		private JSONObject lastKnownData;
		private JSONObject configuration;
		
		public Record(String UID, int manufacturerUID, String propertiesAsJSON, int enoceanUID, EnoceanEquipmentProfileV20 eep, String data, String lastKnownData)
		{
			this(UID, manufacturerUID, propertiesAsJSON, enoceanUID, eep, null, null, null, data, null, null);
			setLastKnownData(lastKnownData);
		}
		
		public Record(String UID, String propertiesAsJSON, int enoceanUID, ActuatorProfile ap, String data)
		{
			this(UID, 0, propertiesAsJSON, enoceanUID, null, ap, null, null, data, null, null);
		}
		
		public Record(String UID, int manufacturerUID, String propertiesAsJSON, int enoceanUID, SensorActuatorProfile sap, String data, String lastKnownData, String configuration)
		{
			this(UID, manufacturerUID, propertiesAsJSON, enoceanUID, null, null, sap, null, data, null, null);
			setLastKnownData(lastKnownData);
			setConfiguration(configuration);
		}
		
		public Record(String UID, String propertiesAsJSON, int enoceanUID, ControllerProfile cp, String data)
		{
			this(UID, 0, propertiesAsJSON, enoceanUID, null, null, null, cp, data, null, null);
		}
		
		private Record(String UID, int manufacturerUID, String propertiesAsJSON, int enoceanUID, EnoceanEquipmentProfileV20 eep, ActuatorProfile ap, SensorActuatorProfile sap, ControllerProfile cp, String data, JSONObject lastKnownData, JSONObject configuration)
		{
			this.UID = UID;
			this.manufacturerUID = manufacturerUID;
			try 
			{
				this.propertiesAsJSON = new JSONObject(propertiesAsJSON);
			} 
			catch (JSONException e) 
			{
				Logger.error(LC.gi(), this, "device properties are not a valid JSON object.", e);
			}
			this.enoceanUID = enoceanUID;
			this.eep = eep;
			this.ap = ap;
			this.sap = sap;
			this.cp = cp;
			this.data = data;
			this.lastKnownData = lastKnownData;
			this.configuration = configuration;
		}

		public String getUID() {
			return UID;
		}
		
		public int getManufacturerUID() {
			return manufacturerUID;
		}
		
		public JSONObject getPropertiesAsJSON() {
			return propertiesAsJSON;
		}

		public int getEnoceanUID() {
			return enoceanUID;
		}

		public EnoceanEquipmentProfileV20 getEnoceanEquipmentProfile() {
			return eep;
		}
		
		public ActuatorProfile getActuatorProfile() {
			return ap;
		}
		
		public SensorActuatorProfile getSensorActuatorProfile() {
			return sap;
		}
		
		public ControllerProfile getControllerProfile() {
			return cp;
		}
		
		public String getProfile() {
			if(eep != null) return eep.toString();
			else if(ap != null) return ap.toString();
			else if(sap != null) return sap.toString();
			else return cp.toString();
		}
		
		public boolean isSensor()
		{
			return (eep != null) || (sap != null);
		}
		
		public boolean isActuator()
		{
			return (ap != null) || (sap != null);
		}
		
		public boolean isController()
		{
			return cp != null;
		}

		public String getData() {
			return data;
		}
		
		public void setData(String data) {
			this.data = data;
		}
		
		public JSONObject getLastKnownData() {
			return lastKnownData;
		}
		
		public String getLastKnownDataAsString() {
			if (lastKnownData == null)
				return null;
			
			return lastKnownData.toString();
		}
		
		public void setLastKnownData(String lastKnownData) 
		{
			if (lastKnownData == null)
				return;
			
			try 
			{ 
				this.lastKnownData = new JSONObject(lastKnownData); 
			} 
			catch (JSONException e) 
			{
				Logger.error(LC.gi(), this, "setLastKnownData() : Error when building a JSON Object. This SHOULD never happen. This is a bug to fix.");
			}
		}
		
		public void setLastKnownData(JSONObject lastKnownData) 
		{
			this.lastKnownData = lastKnownData;
		}
		
		public void setConfiguration(String configuration) 
		{
			if (configuration == null)
				return;
			
			try 
			{ 
				this.configuration = new JSONObject(configuration); 
			} 
			catch (JSONException e) 
			{
				Logger.error(LC.gi(), this, "setConfiguration() : Error when building a JSON Object. This SHOULD never happen. This is a bug to fix.");
			}
		}
		
		public void setConfiguration(JSONObject configuration) 
		{
			this.configuration = configuration;
		}
		
		public String getConfigurationAsString()
		{
			if(configuration == null) {
				return null;
			}
			
			return configuration.toString();
		}
		
		public JSONObject getConfigurationAsJSON()
		{
			return configuration;
		}
	}
	
	public ArrayList<DatabaseManager.Record> getAllItems();
	public void updateRecord(DatabaseManager.Record record);
	public void removeRecord(String UID);
	public void updateDeviceProperties(String UID, String propertiesAsJSONString);
	public void updateDeviceLastKnownData(DatabaseManager.Record record);
}
