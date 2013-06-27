package cam.virtualdisease;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import org.haggle.DataObject;
import org.haggle.DataObject.DataObjectException;

import cam.virtualdisease.datatypes.Disease;
import cam.virtualdisease.datatypes.Measurement;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;
import android.os.Message;

public class DataCollector extends BroadcastReceiver implements LocationListener, Runnable
{
	private VirtualDisease parent;
	private ArrayList<Location> locations;
	private ArrayList<String> addresses;
	
	private boolean isRunning;
	private long updateStart;
	
	public DataCollector(VirtualDisease vd)
	{
		parent = vd;
		isRunning = true;
		addresses = new ArrayList<String>();
		locations = new ArrayList<Location>();
		
		parent.getLocationManager().requestLocationUpdates(LocationManager.GPS_PROVIDER, Constants.SCAN_INTERVAL, 0, this);
		IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
		parent.registerReceiver(this, filter);
	}

	@Override public void run()
	{
		Looper.prepare();
		Message msg;
		
		while (isRunning)
		{
			System.out.println(new Date().toString());
			updateStart = System.currentTimeMillis();
			
			// If bluetooth has been turned off, turn it back on again.
			if (parent.getBluetoothAdapter() != null)
			{
				if (!parent.getBluetoothAdapter().isEnabled())
				{
					parent.getBluetoothAdapter().enable();
				}
				
				// If the current local bluetooth address is not set, attempt to set it.
				if (parent.getLocalBluetoothAddress() == null && parent.getBluetoothAdapter().getAddress() != null)
				{
					parent.setLocalBluetoothAddress(parent.getBluetoothAdapter().getAddress().replaceAll(":", ""));
					msg = new Message();
					msg.what = Constants.UPDATE_BLUETOOTH_ADDRESS;
					parent.getGUIHandler().sendMessage(msg);
				}
			}
			
			msg = new Message();
			msg.what = Constants.UPDATE_BLUETOOTH_STATUS;
			parent.getGUIHandler().sendMessage(msg);
			
			// Delete all published diseases.
			for (DataObject d : parent.getPublished())
			{
				parent.getHaggleHandle().deleteDataObject(d);
				d.dispose();
			}
			parent.getPublished().clear();

			// Publish infectious diseases to the haggle network.
			for (Disease d : parent.getDiseases())
			{
				if (d.isInfectious())
				{
					String disease = d.getName() + "," + parent.getLocalBluetoothAddress() + "," + d.getExposedDuration() + "," + d.getInfectiousDuration() + "," + d.getInfectionProbability() + "," + parent.getRandom().nextLong();

					try
					{
						DataObject dataObject = new DataObject(disease.getBytes());
						dataObject.addAttribute(parent.getAttribute().getName(), parent.getAttribute().getValue(), parent.getAttribute().getWeight());
						dataObject.addHash();
						
						parent.getPublished().add(dataObject);
						parent.getHaggleHandle().publishDataObject(dataObject);
						
						System.out.println("Published: " + d.getName() + " as " + dataObject.getFileName());
					}
					catch (DataObjectException e)
					{
						System.out.println("Something went wrong with the DataObject");
						parent.quit(1);
					}
				}
			}
			
			msg = new Message();
			msg.what = Constants.UPDATE_HEALTH_STATUS;
			parent.getGUIHandler().sendMessage(msg);
			
			if (parent.getLocationManager() != null && parent.getLocationManager().isProviderEnabled(Constants.GPS))
			{
				parent.setIsGPSEnabled(true);
				parent.setIsGPSScanning(true);
				
				// Wait until either a location is found, or the GPS times out.
				while (locations.isEmpty() && (updateStart + Constants.GPS_SCAN_TIMEOUT) > System.currentTimeMillis())
				{
					try
					{
						Thread.sleep(1000);
					}
					catch (InterruptedException e){}
				}
			}
			else
			{
				parent.setIsGPSEnabled(false);
				parent.setIsGPSScanning(false);
			}
			
			msg = new Message();
			msg.what = Constants.UPDATE_GPS_STATUS;
			parent.getGUIHandler().sendMessage(msg);
			
			if (parent.getBluetoothAdapter() != null && parent.getBluetoothAdapter().isEnabled())
			{
				parent.setIsBluetoothScanning(true);
				msg = new Message();
				msg.what = Constants.UPDATE_BLUETOOTH_STATUS;
				parent.getGUIHandler().sendMessage(msg);
				
				// Try to get Bluetooth.
				parent.getBluetoothAdapter().startDiscovery();
				
				try
				{
					Thread.sleep(Constants.BLUETOOTH_SCAN_DURATION);
				}
				catch (InterruptedException e){}
			
				// Cancel Bluetooth scan.
				parent.getBluetoothAdapter().cancelDiscovery();
			}
			
			parent.setIsBluetoothScanning(false);
			msg = new Message();
			msg.what = Constants.UPDATE_BLUETOOTH_STATUS;
			parent.getGUIHandler().sendMessage(msg);
			
			Location chosenLocation = null;
			
			parent.setIsGPSScanning(false);
			msg = new Message();
			msg.what = Constants.UPDATE_GPS_STATUS;
			parent.getGUIHandler().sendMessage(msg);
			
			if (!locations.isEmpty())
			{
				float gpsAccuracy = -1;
				
				for (Location l : locations)
				{
					if (l.hasAccuracy())
					{	
						if (gpsAccuracy == -1)
						{
							gpsAccuracy = l.getAccuracy();
							chosenLocation = l;
						}
						else if (l.getAccuracy() < gpsAccuracy)
						{
							gpsAccuracy = l.getAccuracy();
							chosenLocation = l;
						}
					}
				}
			}
			
			locations = new ArrayList<Location>();
			
			ArrayList<String> chosenAddresses = addresses;
			addresses = new ArrayList<String>();
			
			if (!chosenAddresses.isEmpty() || chosenLocation != null)
			{
				Measurement m = new Measurement(chosenAddresses, chosenLocation, System.currentTimeMillis());
				
				try
				{
					BufferedWriter bw = new BufferedWriter(new FileWriter(parent.getLogFile(), true));
					bw.write(m.toXML() + "\n");
					bw.flush();
					bw.close();
				}
				catch (IOException ex)
				{
					System.out.println("Cannot append to log file.");
					parent.quit(1);
				}
			}
			
			// Calculate how long is left to wait.
			long waitTime = Constants.SCAN_INTERVAL - (System.currentTimeMillis() - updateStart);
			
			try
			{
				if (waitTime > 0)
				{
					Thread.sleep(waitTime);
				}
			}
			catch (InterruptedException e){}
		}
	}

	@Override public void onReceive(Context context, Intent intent)
	{
		String action = intent.getAction();

		if (BluetoothDevice.ACTION_FOUND.equals(action))
		{	
			BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			String address = device.getAddress().replaceAll(":", "");
			
			if (!addresses.contains(address))
			{
				addresses.add(address);
			}
		}
		
		Message msg = new Message();
		msg.what = Constants.UPDATE_BLUETOOTH_STATUS;
		parent.getGUIHandler().sendMessage(msg);
	}
	
	@Override public void onLocationChanged(Location location)
	{
		if (location != null)
		{
			locations.add(location);
        }
		
		parent.setIsGPSEnabled(true);
		Message msg = new Message();
		msg.what = Constants.UPDATE_GPS_STATUS;
		parent.getGUIHandler().sendMessage(msg);
	}
	
	@Override public void onProviderDisabled(String provider)
	{
		parent.setIsGPSEnabled(false);
		Message msg = new Message();
		msg.what = Constants.UPDATE_GPS_STATUS;
		parent.getGUIHandler().sendMessage(msg);
	}
	
	@Override public void onProviderEnabled(String provider)
	{
		parent.setIsGPSEnabled(true);
		Message msg = new Message();
		msg.what = Constants.UPDATE_GPS_STATUS;
		parent.getGUIHandler().sendMessage(msg);
	}
	
	@Override public void onStatusChanged(String provider, int status, Bundle extras)
	{
		if (provider.equalsIgnoreCase("gps"))
		{
			parent.setIsGPSEnabled(true);
			parent.setGPSInfo(status, extras.getInt("satellites"));
			
			Message msg = new Message();
			msg.what = Constants.UPDATE_GPS_STATUS;
			parent.getGUIHandler().sendMessage(msg);
		}
	}
	
	public void setRunning(boolean run)
	{
		isRunning = run;
	}
}
