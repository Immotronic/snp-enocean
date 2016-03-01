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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.ubikit.DatabaseProxy;
import org.ubikit.Logger;

import fr.immotronic.ubikit.pems.enocean.ActuatorProfile;
import fr.immotronic.ubikit.pems.enocean.ControllerProfile;
import fr.immotronic.ubikit.pems.enocean.EnoceanEquipmentProfileV20;
import fr.immotronic.ubikit.pems.enocean.SensorActuatorProfile;

public final class DatabaseManagerImpl implements DatabaseManager
{
	private static final String TABLE_STRUCTURE = "CREATE TABLE IF NOT EXISTS items (" +
			"UID VARCHAR(50) NOT NULL, " +
			"manufacturerUID INT, " +
			"properties VARCHAR(4096), " +
			"enoceanUID INT, " +
			"profile VARCHAR(80) NOT NULL, " +
			"data VARCHAR(300)," +
			"lastKnownData VARCHAR(500), " +
			"configuration VARCHAR(500), " +
			"PRIMARY KEY (UID));";
	
	private static final String INSERT_QUERY = "INSERT INTO items " +
			"(UID, manufacturerUID, properties, enoceanUID, profile, data, lastKnownData, configuration) " +
			"VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
	
	private static final String UPDATE_QUERY = "UPDATE items SET " +
			"UID = ?, " +
			"manufacturerUID = ?, " +
			"properties = ?, " +
			"enoceanUID = ?, " +
			"profile = ?, " +
			"data = ?, " +
			"lastKnownData = ?, " +
			"configuration = ? " +
			"WHERE UID = ?;";
	
	private static final String UPDATE_PROPERTIES_QUERY = "UPDATE items SET " +
			"properties = ? " +
			"WHERE UID = ?;";
	
	private static final String UPDATE_LAST_KNOWN_DATA_QUERY = "UPDATE items SET " +
			"lastKnownData = ? " +
			"WHERE UID = ?;";
	
	private static final String REMOVE_QUERY = "DELETE FROM items WHERE UID = ?;";
	
	private static final String QUERY_ALL_ITEMS = "SELECT * FROM items;";
	
	private DatabaseProxy dbProxy;
	private PreparedStatement getItemList;
	private PreparedStatement insertNewItem;
	private PreparedStatement updateItem;
	private PreparedStatement updateItemProperties;
	private PreparedStatement updateItemLastKnownData;
	private PreparedStatement removeItem;
	
	
	public DatabaseManagerImpl(DatabaseProxy dbProxy)
	{
		this.dbProxy = dbProxy;
		dbProxy.executeUpdate(TABLE_STRUCTURE);
		getItemList = dbProxy.getPreparedStatement(QUERY_ALL_ITEMS);
		insertNewItem = dbProxy.getPreparedStatement(INSERT_QUERY);
		updateItem = dbProxy.getPreparedStatement(UPDATE_QUERY);
		updateItemProperties = dbProxy.getPreparedStatement(UPDATE_PROPERTIES_QUERY);
		updateItemLastKnownData = dbProxy.getPreparedStatement(UPDATE_LAST_KNOWN_DATA_QUERY);
		removeItem = dbProxy.getPreparedStatement(REMOVE_QUERY);
	}

	@Override
	public ArrayList<DatabaseManager.Record> getAllItems() 
	{
		ResultSet rs = dbProxy.executePreparedQuery(getItemList);
		if(rs != null)
		{
			ArrayList<Record> res = new ArrayList<Record>(); 
			try 
			{
				if(LC.debug) {
					Logger.debug(LC.gi(), this, "Pem-enocean::items table");
					Logger.debug(LC.gi(), this, "------------------------");
				}
				
				while(rs.next())
				{
					if(LC.debug){
						StringBuilder trace = new StringBuilder();
						
						trace.append(rs.getString(1)).append("\t").append(rs.getString(2))
							.append("\t").append(rs.getString(3)).append("\t").append(rs.getString(4))
							.append("\t").append(rs.getString(5)).append("\t").append(rs.getString(6))
							.append("\t").append(rs.getString(7)).append("\t").append(rs.getString(8));
						Logger.debug(LC.gi(), this, trace.toString());
					}
					
					Record record = null;
					String objectDescription = rs.getString(5);
					
					try {
						EnoceanEquipmentProfileV20 eep = EnoceanEquipmentProfileV20.valueOf(objectDescription);
						record =  new Record(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getInt(4), eep, rs.getString(6), rs.getString(7));
					}
					catch(IllegalArgumentException e) {}
					
					try {
						ActuatorProfile ap = ActuatorProfile.valueOf(objectDescription);
						record =  new Record(rs.getString(1), rs.getString(3), rs.getInt(4), ap, rs.getString(6));
					}
					catch(IllegalArgumentException e) {}
					
					try {
						SensorActuatorProfile sap = SensorActuatorProfile.valueOf(objectDescription);
						record =  new Record(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getInt(4), sap, rs.getString(6), rs.getString(7), rs.getString(8));
					}
					catch(IllegalArgumentException e) {}
					
					/*try {
						String[] profiles = objectDescription.split(" ");
						if (profiles.length == 2)
						{
							EnoceanEquipmentProfileV20 eep = EnoceanEquipmentProfileV20.valueOf(profiles[0]);
							ActuatorProfile ap = ActuatorProfile.valueOf(profiles[1]);
							record =  new Record(rs.getString(1), rs.getInt(2), rs.getString(3), rs.getInt(4), eep, ap, rs.getString(6));
						}
					}
					catch(IllegalArgumentException e) {}
					*/
					try {
						ControllerProfile cp = ControllerProfile.valueOf(objectDescription);
						record =  new Record(rs.getString(1), rs.getString(3), rs.getInt(4), cp, rs.getString(6));
					}
					catch(IllegalArgumentException e) {}
					
					res.add(record);
				}
				
				return res;
			} 
			catch (SQLException e) 
			{
				Logger.error(LC.gi(), this, "While getting item list from the Enocean PEM database", e);
			}
		}
		
		return null;
	}

	@Override
	public void updateRecord(DatabaseManager.Record record) 
	{
		Logger.debug(LC.gi(), this, "updateRecord: "+record.getUID()+" "+record.getProfile());
		try 
		{
			insertNewItem.setString(1, record.getUID());
			insertNewItem.setInt(2, record.getManufacturerUID());
			insertNewItem.setString(3, record.getPropertiesAsJSON().toString());
			insertNewItem.setInt(4, record.getEnoceanUID());
			insertNewItem.setString(5, record.getProfile());
			insertNewItem.setString(6, record.getData());
			insertNewItem.setString(7, record.getLastKnownDataAsString());
			insertNewItem.setString(8, record.getConfigurationAsString());
			
			if(dbProxy.executePreparedUpdate(insertNewItem) < 0)
			{
				updateItem.setString(1, record.getUID());
				updateItem.setInt(2, record.getManufacturerUID());
				updateItem.setString(3, record.getPropertiesAsJSON().toString());
				updateItem.setInt(4, record.getEnoceanUID());
				updateItem.setString(5, record.getProfile());
				updateItem.setString(6, record.getData());
				updateItem.setString(7, record.getLastKnownDataAsString());
				updateItem.setString(8, record.getConfigurationAsString());
				updateItem.setString(9, record.getUID());
				if(dbProxy.executePreparedUpdate(updateItem) < 0)
				{
					Logger.error(LC.gi(), this, "While updating an item in Enocean PEM database");
				}
			}
		} 
		catch (SQLException e) 
		{
			Logger.error(LC.gi(), this, "While (inserting a new / updating an) item in the Enocean PEM database", e);
		}
	}

	@Override
	public void removeRecord(String UID) 
	{
		try 
		{
			Logger.debug(LC.gi(), this, "Removing item "+UID+" from DB...");
			removeItem.setString(1, UID);
			if(dbProxy.executePreparedUpdate(removeItem) < 0)
			{
				Logger.error(LC.gi(), this, "While removing "+UID+" item in Enocean PEM database: executePreparedUpdate() < 0");
			}
		} 
		catch (SQLException e) 
		{
			Logger.error(LC.gi(), this, "While removing "+UID+" item from the Enocean PEM database", e);
		}
	}
	
	@Override
	public void updateDeviceProperties(String UID, String propertiesAsJSONString)
	{
		Logger.debug(LC.gi(), this, "update properties of item "+UID+ " with "+propertiesAsJSONString);
		try 
		{
			updateItemProperties.setString(1, propertiesAsJSONString);
			updateItemProperties.setString(2, UID);
			if(dbProxy.executePreparedUpdate(updateItemProperties) < 0)
			{
				Logger.error(LC.gi(), this, "While updating item properties in Enocean PEM database (query did not modify anything in DB)");
			}
		}
		catch (SQLException e) 
		{
			Logger.error(LC.gi(), this, "While updating item properties in the Enocean PEM database", e);
		}
	}
	
	@Override
	public void updateDeviceLastKnownData(DatabaseManager.Record record)
	{
		Logger.debug(LC.gi(), this, "update properties of item "+record.getUID()+ " with "+record.getLastKnownDataAsString());
		try 
		{
			updateItemLastKnownData.setString(1, record.getLastKnownDataAsString());
			updateItemLastKnownData.setString(2, record.getUID());
			if(dbProxy.executePreparedUpdate(updateItemLastKnownData) < 0)
			{
				Logger.error(LC.gi(), this, "While updating last known data in Enocean PEM database (query did not modify anything in DB)");
			}
		}
		catch (SQLException e) 
		{
			Logger.error(LC.gi(), this, "While updating last known data in the Enocean PEM database", e);
		}
	}
	
}
