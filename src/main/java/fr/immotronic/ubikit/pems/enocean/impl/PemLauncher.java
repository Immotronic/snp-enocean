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

package fr.immotronic.ubikit.pems.enocean.impl;	// PEM_NAME_ID must be replaced.

import java.io.IOException;
import java.sql.SQLException;
import java.util.concurrent.BlockingQueue;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.ubikit.AbstractRootPhysicalEnvironmentModel;
import org.ubikit.Logger;
import org.ubikit.PhysicalEnvironmentItem;
import org.ubikit.PhysicalEnvironmentModelInformations;
import org.ubikit.PhysicalEnvironmentModelObserver;

//import fr.immotronic.license.LicenseName;
//import fr.immotronic.license.impl.LicenseManagerImpl;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorAndActuatorDevice;
import fr.immotronic.ubikit.pems.enocean.EnoceanSensorDevice;
import fr.immotronic.ubikit.pems.enocean.impl.EnoceanSerialAdapter.ESP;
import fr.immotronic.ubikit.pems.enocean.PEMEnocean;

public final class PemLauncher extends AbstractRootPhysicalEnvironmentModel implements EnoceanTCMManager, PEMEnocean
{	
	//-------SPECIFIC APP PRIVATE MEMBERS--------------------------
	
	private final DiagnosticManager diagnosticManager;
	//private final LicenseManagerImpl licenseManager;
	
	private EnoceanSerialAdapter enoceanSerialAdapter;
	
	private PairingManagerImpl pairingManager;
	private DatabaseManager databaseManager;
	private DeviceManager deviceManager;
	private TelegramConsumer telegramConsumer;
	private APIServlet apiServlet;
	private boolean tcmOnUSB;
	private boolean esp3;
	private Status pemStatus;
	
	private class TelegramConsumer implements Runnable
	{
		private BlockingQueue<EnoceanTelegram> receptionQueue;
		private boolean running;
		
		public TelegramConsumer(BlockingQueue<EnoceanTelegram> receptionQueue)
		{
			this.receptionQueue = receptionQueue;
			running = true;
		}
		
		public void run()
		{
			while(!Thread.currentThread().isInterrupted() && running)
			{
				EnoceanTelegram telegram = null;
				// Awaiting for a telegram to be received
				
				try 
				{
					telegram = receptionQueue.take();
				} 
				catch (InterruptedException e) 
				{ 
					running = false;
				}
				
				long startProcessingTime = System.nanoTime();
				
				if(telegram != null && deviceManager != null && pairingManager != null) 
				{
					// Is this telegram from a known sensor device ?
					EnoceanSensorDevice device = deviceManager.getSensorDevice(telegram.getTransmitterId());
					if(device != null)
					{
						// Yes, telegram is from a known sensor device, we deliver new incoming data to
						// the device object.
						if(LC.debug) {
							Logger.debug(LC.gi(), this, "TelegramConsumer: New data from sensor device " + Integer.toHexString(device.getEnoceanUID()));
						}
						device.getDataHandler().processNewIncomingData(telegram.getTransmitterId(), telegram.getData());
						
						// Device last data are saved in database.
						deviceManager.updateDeviceLastKnownData(device.getUID());
						
						// Telegram's RSSI is saved in the device object if received.
						if (enoceanSerialAdapter.getSupportedESP() == ESP.ESP3)
						{
							((EnoceanSensorDeviceImpl) device).setDeviceLastRSSI(((EnoceanESP3Telegram) telegram).getRSSI());
						}
					}
					else
					{
						// Is this telegram from a known sensor and actuator device ?
						EnoceanSensorAndActuatorDevice saDevice = deviceManager.getSensorActuatorDevice(telegram.getTransmitterId());
						if(saDevice != null)
						{
							// Yes, telegram is from a known sensor and actuator device, we deliver new incoming data to
							// the device object.
							if(LC.debug) {
								Logger.debug(LC.gi(), this, "TelegramConsumer: New data from sensor and actuator device " + Integer.toHexString(saDevice.getEnoceanUID()));
							}
							saDevice.getDataHandler().processNewIncomingData(telegram.getTransmitterId(), telegram.getData());
							
							// Device last data are saved in database.
							deviceManager.updateDeviceLastKnownData(saDevice.getUID());
							
							// Telegram's RSSI is saved in the device object if received.
							if (enoceanSerialAdapter.getSupportedESP() == ESP.ESP3)
							{
								int rssi = ((EnoceanESP3Telegram) telegram).getRSSI();
								((EnoceanSensorAndActuatorDeviceImpl) saDevice).setDeviceLastRSSI(rssi);
							}
						}
						else
						{
							// No, the device is unknown. 
							
							// Is the user trying to pair a new device ?
							if(pairingManager.getPairingMode())
							{
								// Yes, telegram is pushed in the pairing manager
								pairingManager.pairNewDevice(telegram);
							}
						}
					}
				}
				else if(running && telegram != null){
					Logger.warn(LC.gi(), this, "TelegramConsumer: An Enocean telegram was lost, because of missing deviceManager or pairingManager: "+telegram);
				}
				
				long processingTime = System.nanoTime() - startProcessingTime;
				diagnosticManager.timeToProcessTelegram(processingTime);
			}
			
			if(LC.debug) {
				Logger.debug(LC.gi(), this, "TelegramConsumer exited its processing loop");
			}
		}
	}
	
	//-------END OF SPECIFIC APP PRIVATE MEMBERS-------------------
	
	public PemLauncher(BundleContext bc)
	{
		super(3, bc);
		diagnosticManager = new DiagnosticManagerImpl();
		enoceanSerialAdapter = null; // need serialPortService to be instantiated.
		databaseManager = null; // need a database connection that will be available at component start time.
		deviceManager = null; // need a databaseManager object to instantiate this one.
		pairingManager = null; // need a deviceManager to be instantiated
		telegramConsumer = null; // need a EnoceanSerialAdapter to be instantiated
		pemStatus = Status.STOPPED;
		
		tcmOnUSB = false;
		String tcm = bc.getProperty("fr.immotronic.enocean.tcm");
		if(tcm != null && tcm.equalsIgnoreCase("usb")) {
			tcmOnUSB = true;
		}
		
		esp3 = false;
		String esp = bc.getProperty("fr.immotronic.enocean.esp");
		if(esp != null && esp.equalsIgnoreCase("esp3")) {
			esp3 = true;
		}
		
		/*LicenseManagerImpl lm = null;
		try 
		{
			lm = new LicenseManagerImpl(bc, System.getProperty("user.dir")+"/"+bc.getProperty("fr.immotronic.placetouch.downloadFolder"));
		} 
		catch (IOException e) 
		{
			Logger.info(LC.gi(), this, "##### LICENSE MANAGER CANNOT BE CREATED #######");
			try { bc.getBundle(0).stop(); } catch (BundleException be) { System.exit(0); }
		}
		finally
		{
			licenseManager = lm;
			if(licenseManager != null) 
			{
				if(licenseManager.checkLicenceFile() == LicenseName.INVALID) {
					pemStatus = Status.INVALID_LICENSE;
				}
			}
		}*/
	}
	
	@Override
	protected void start() 
	{	
		// Instantiate a serial adapter. This object will be the gate to the 
		// Enocean transceiver.
		if (!esp3) {
			enoceanSerialAdapter = new EnoceanESP2SerialAdapterImpl(tcmOnUSB, diagnosticManager, getExecutorService(), getHigherAbstractionLevelEventGate());
		}
		else {
			enoceanSerialAdapter = new EnoceanESP3SerialAdapterImpl(tcmOnUSB, diagnosticManager, getExecutorService(), getHigherAbstractionLevelEventGate());
		}
		
		try 
		{
			// Open a connection to the local database that keep Enocean device list 
			// and Enocean device configuration persistent.
			databaseManager = new DatabaseManagerImpl(getDatabaseConnection());
		} 
		catch (SQLException e) 
		{
			databaseManager = null;
			Logger.error(LC.gi(), this, "Cannot get a connection to the Enocean PEM database.", e);
		}
		
		if(databaseManager != null) // If a database connection has been successfully established
		{
			// Instantiate the device manager and the pairing manager.
			deviceManager = new DeviceManagerImpl(this, databaseManager, enoceanSerialAdapter/*, licenseManager*/);
			pairingManager = new PairingManagerImpl(deviceManager, /*licenseManager,*/ getHigherAbstractionLevelEventGate(), getExecutorService(), getUID());
			apiServlet = new APIServlet(deviceManager, this);
			
			registerServlet("", apiServlet);
		}
		
		// Setting up the telegram consumer
		telegramConsumer = new TelegramConsumer(enoceanSerialAdapter.getTelegramReceptionQueue());
		getExecutorService().execute(telegramConsumer);
		
		Logger.info(LC.gi(), this, "PEM did started");
	}


	@Override
	protected void stop() 
	{	
		if(enoceanSerialAdapter != null) {
			enoceanSerialAdapter.disconnect();
		}
		
		enoceanSerialAdapter = null; 
		databaseManager = null;
		deviceManager = null; 
		pairingManager = null;
		telegramConsumer = null;
		
		pemStatus = Status.STOPPED;
		Logger.info(LC.gi(), this, "PEM did stopped");
	}
	
	@Override
	public PhysicalEnvironmentItem removeItem(String UID)
	{
		if(deviceManager != null) {
			deviceManager.removeDevice(UID);
			return super.removeItem(UID);
		}
		
		return null;
	}
	
	@Override
	public PhysicalEnvironmentModelInformations getInformations()
	{
		return diagnosticManager.getInformations();
	}
	
	@Override
	public void setObserver(PhysicalEnvironmentModelObserver observer)
	{
		diagnosticManager.setObserver(observer);
	}
	
	@Override
	public HardwareLinkStatus getHardwareLinkStatus()
	{
		if(enoceanSerialAdapter != null && enoceanSerialAdapter.isConnected())
		{
			return HardwareLinkStatus.CONNECTED;
		}
		
		return HardwareLinkStatus.DISCONNECTED;
	}

	@Override
	public String getTransceiverBaseID() 
	{
		if(enoceanSerialAdapter != null)
		{
			return Integer.toHexString(enoceanSerialAdapter.getEnoceanTransceiverID());
		}
		
		return null;
	}

	@Override
	public TCMSettingResponse setTransceiverBaseID(String baseID) 
	{
		if(enoceanSerialAdapter != null)
		{
			return enoceanSerialAdapter.setTransceiverBaseID(baseID);
		}
		
		return TCMSettingResponse.NOT_READY;
	}

	@Override
	public void checkLicense() 
	{
		/*if(licenseManager != null)
		{
			if(licenseManager.checkLicenceFile() == LicenseName.INVALID) {
				pemStatus = Status.INVALID_LICENSE;
			}
		}*/
	}

	@Override
	public Status getStatus() 
	{
		return pemStatus;
	}
}
