package cam.virtualdisease;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import org.haggle.Attribute;
import org.haggle.DataObject;
import org.haggle.EventHandler;
import org.haggle.Handle;
import org.haggle.LaunchCallback;
import org.haggle.Node;
import cam.virtualdisease.datatypes.Disease;
import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class VirtualDisease extends Activity implements EventHandler
{
	private Handle haggleHandle;
	private BluetoothAdapter bluetoothAdapter;
	private LocationManager locationManager;
	private Handler guiHandler;
	private DataCollector dataCollector;
	private Thread dataCollectorThread;
	private Random rand;
	
	// Listeners.
	private QuitMenuListener quitListener;
	
	// Files.
	private File diseaseFile;
	private File logFile;
	private File extraDiseaseFile;
	
	// Layout objects.
	private StringBuilder stringBuilder;
	private TextView bluetoothAddress;
	private TextView healthStatus;
    private ImageView imagehealthStatus;
	private TextView dataCollectionBluetoothStatus;
	private TextView dataCollectionGPSStatus;
	
	// Disease data.
	private Attribute attribute;
	private LinkedBlockingQueue<Disease> diseases;
	//private LinkedBlockingQueue<Disease> publishQueue;
	private LinkedBlockingQueue<DataObject> published;
	
	// Flags.
	private boolean isGPSEnabled, isGPSScanning, isBluetoothScanning;
	
	// Data Collection Info.
	private int gpsStatus, numOfSatellites;
	private String localBluetoothAddress;
	
    @Override public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(1); // Stop screen from rotating!
        setContentView(R.layout.main);
        
        if (savedInstanceState == null)
        {
        	// Get bluetooth adapter and turn it on.
    		// BluetoothAdapter.getDefaultAdapter().enable();
        	
        	bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (null == bluetoothAdapter)
            {
                Log.e("LOG_TAG","Bluetooth adapter is NULL!!!");
            } else {
            	bluetoothAdapter.enable();
            }
        	
        	// Load diseases.
        	diseases = new LinkedBlockingQueue<Disease>();
        	published = new LinkedBlockingQueue<DataObject>();
        	
        	// Set up SD Card.
			String state = Environment.getExternalStorageState();
			if (Environment.MEDIA_MOUNTED.equals(state))
			{
				File root = Environment.getExternalStorageDirectory();
				File dir = new File(root, "VirtualDisease");
				
				if (dir.exists())
				{
					diseaseFile = new File(dir, Constants.DISEASE_FILE);
					try
					{
						if (!diseaseFile.exists() && !diseaseFile.createNewFile())
						{
							System.out.println("Cannot create disease database on SD Card.");
							quit(1);
						}
					}
					catch (IOException ex)
					{
						System.out.println("Cannot create disease database on SD Card.");
						quit(1);
					}
					
					logFile = new File(dir, Constants.MEASUREMENTS_FILE);
					try
					{
						if (!logFile.exists() && !logFile.createNewFile())
						{
							System.out.println("Cannot create disease log on SD Card.");
							quit(1);
						}
					}
					catch (IOException ex)
					{
						System.out.println("Cannot create disease log on SD Card.");
						quit(1);
					}
					
					extraDiseaseFile = new File(dir, Constants.EXTRA_DISEASE_FILE);
					try
					{
						if (!extraDiseaseFile.exists() && !extraDiseaseFile.createNewFile())
						{
							System.out.println("Cannot create extra disease database on SD Card.");
							quit(1);
						}
					}
					catch (IOException e)
					{
						System.out.println("Cannot create extra disease database on SD Card.");
						quit(1);
					}
				}
				else
				{
					if (dir.mkdir())
					{
						diseaseFile = new File(dir, Constants.DISEASE_FILE);
						try
						{
							if (!diseaseFile.createNewFile())
							{
								System.out.println("Cannot create disease database on SD Card.");
								quit(1);
							}
						}
						catch (IOException ex)
						{
							System.out.println("Cannot create disease database on SD Card.");
							quit(1);
						}
						
						logFile = new File(dir, Constants.MEASUREMENTS_FILE);
						try
						{
							if (!logFile.createNewFile())
							{
								System.out.println("Cannot create disease log on SD Card.");
								quit(1);
							}
						}
						catch (IOException ex)
						{
							System.out.println("Cannot create disease log on SD Card.");
							quit(1);
						}
						
						extraDiseaseFile = new File(dir, Constants.EXTRA_DISEASE_FILE);
						try
						{
							if (!extraDiseaseFile.createNewFile())
							{
								System.out.println("Cannot create disease log on SD Card.");
								quit(1);
							}
						}
						catch (IOException ex)
						{
							System.out.println("Cannot create disease log on SD Card.");
							quit(1);
						}
					}
					else
					{
						System.out.println("Cannot create storage directory on SD Card.");
						quit(1);
					}
				}
			}
			else
			{
				System.out.println("Cannot read/write on SD Card.");
				quit(1);
			}
			
			// Read diseases from database.
			try
			{
				BufferedReader br = new BufferedReader(new FileReader(diseaseFile));
				while (br.ready())
				{
					String line = br.readLine().trim();
					if (!line.equals(""))
					{
						String[] d = line.split(",");
						Disease disease = new Disease(d[0], d[1], Long.parseLong(d[2]), Long.parseLong(d[3]), Long.parseLong(d[4]), Double.parseDouble(d[5]));
						diseases.add(disease);
						System.out.println("Added " + d[0] + ".");
					}
				}
			}
			catch (FileNotFoundException ex)
			{
				System.out.println("Cannot find disease database.");
				quit(1);
			}
			catch (IOException ex)
			{
				System.out.println("Cannot read disease database.");
				quit(1);
			}
			
			// Make universal random object.
			rand = new Random();
    		
    		// Try to get the Location Manager (GPS).
    		isGPSEnabled = false;
			locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
			
			if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER))
			{
				Intent enableGPSIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
				Toast.makeText(this, "Please enable GPS and return to the application.", Toast.LENGTH_LONG).show();
				startActivityForResult(enableGPSIntent, Constants.GPS_REQUEST_CODE);
			}
			else
			{
				isGPSEnabled = true;
			}
    		
    		// We will only consider haggle packets that contain this attribute.
    		attribute = new Attribute("Disease", "Disease", 1);

    		// Initiate listeners.
    		quitListener = new QuitMenuListener(this);
    		
    		// Create instances of changeable layout.
    		bluetoothAddress = (TextView) findViewById(R.id.deviceBluetoothAddress);
    		healthStatus = (TextView) findViewById(R.id.healthStatus);
            imagehealthStatus = (ImageView) findViewById(R.id.imagehealthStatus);
			dataCollectionBluetoothStatus = (TextView) findViewById(R.id.dataCollectionBluetoothStatus);
			dataCollectionGPSStatus = (TextView) findViewById(R.id.dataCollectionGPSStatus);
    		
    		// Set up haggle.
    		if (initHaggle() == Constants.HAGGLE_STATUS_OK && haggleHandle.registerInterest(attribute) == 0)
			{
				System.out.println("Attribute Registered");
			}
			else
			{
				System.out.println("Haggle shutting down.");
				quit(1);
			}
    		
    		// Create handler for GUI updates.
			guiHandler = new Handler()
			{
				public void handleMessage(Message msg)
				{
					super.handleMessage(msg);
					
					// Update all GUI elements.
					switch (msg.what)
					{
						case Constants.UPDATE_BLUETOOTH_ADDRESS:
							
							if (localBluetoothAddress == null)
							{
								bluetoothAddress.setText("Device: <unknown>\n");
							}
							else
							{
								bluetoothAddress.setText("Device: " + localBluetoothAddress + "\n");
							}
							
							break;
					
						case Constants.UPDATE_HEALTH_STATUS:
							
							stringBuilder = new StringBuilder();
							
							if (diseases.isEmpty())
							{
								stringBuilder.append("You are healthy.");
                                imagehealthStatus.setImageResource(R.drawable.recovered);
							}
							else
							{
								int infections = 0;
								for (Disease d : diseases)
								{
									if (d.isInfectious())
									{
										infections++;
										stringBuilder.append(d.getName() + " from " + d.getSender() + " (Infectious)\n");
									}
									else if (!d.isCured())
									{
										infections++;
										stringBuilder.append(d.getName() + " from " + d.getSender() + " (Exposed)\n");
									}
								}
								
								if (infections == 1)
								{
									stringBuilder.insert(0, "You are infected with the following disease:\n\n");
								}
								else
								{
									stringBuilder.insert(0, "You are infected with the following diseases:\n\n");
								}
								
								// Recoveries.
								if (diseases.size() - infections > 0)
								{
									stringBuilder.append("\nYou have recovered from:\n\n");
									
									for (Disease d : diseases)
									{
										if (d.isCured())
										{
											stringBuilder.append(d.getName() + " from " + d.getSender() + "\n");
										}
									}
								}

                                if (infections == 0)
                                {
                                    //Recovered from all the infections
                                    imagehealthStatus.setImageResource(R.drawable.recovered);
                                } else {
                                    imagehealthStatus.setImageResource(R.drawable.bad);
                                }
							}
							
							healthStatus.setText(stringBuilder.toString());
							
							break;
					
						case Constants.UPDATE_BLUETOOTH_STATUS:
							
							stringBuilder = new StringBuilder();
							stringBuilder.append("Bluetooth: ");
							
							if (!bluetoothAdapter.isEnabled())
							{
								stringBuilder.append("Disabled.");
							}
							else
							{
								if (isBluetoothScanning)
								{
									stringBuilder.append("Scanning.");
								}
								else
								{
									stringBuilder.append("Waiting.");
								}
							}
							
							dataCollectionBluetoothStatus.setText(stringBuilder.toString());
							
							break;
							
						case Constants.UPDATE_GPS_STATUS:
							
							stringBuilder = new StringBuilder();
							stringBuilder.append("GPS: ");
							
							if (!isGPSEnabled)
							{
								stringBuilder.append("Disabled.");
							}
							else
							{
								if (isGPSScanning)
								{
									switch (gpsStatus)
									{
										case LocationProvider.OUT_OF_SERVICE:
											stringBuilder.append("Lost Signal.");
											break;
										case LocationProvider.TEMPORARILY_UNAVAILABLE:
											stringBuilder.append("Temporarily Unavailable.");
											break;
										case LocationProvider.AVAILABLE:
											stringBuilder.append("Scanning.");
												
											if (numOfSatellites == 1)
											{
												stringBuilder.append(" (1 Satellite)");
											}
											else
											{
												stringBuilder.append(" (" + numOfSatellites + " Satellites)");
											}
											break;
									}
								}
								else
								{
									stringBuilder.append("Waiting.");
								}
							}
							
							dataCollectionGPSStatus.setText(stringBuilder.toString());
							
							break;
					}
				}
			};
			
			// Update all GUI elements.
			Message msg = new Message();
			msg.what = Constants.UPDATE_HEALTH_STATUS;
			guiHandler.sendMessage(msg);
			
			msg = new Message();
			msg.what = Constants.UPDATE_GPS_STATUS;
			guiHandler.sendMessage(msg);
			
			msg = new Message();
			msg.what = Constants.UPDATE_BLUETOOTH_STATUS;
			guiHandler.sendMessage(msg);
			
			if (bluetoothAdapter != null)
			{
				// If the current local bluetooth address is not set, attempt to set it.
				if (localBluetoothAddress == null && bluetoothAdapter.getAddress() != null)
				{
					localBluetoothAddress = bluetoothAdapter.getAddress().replaceAll(":", "");
					msg = new Message();
					msg.what = Constants.UPDATE_BLUETOOTH_ADDRESS;
					guiHandler.sendMessage(msg);
				}
			}
			
			// Start Data Collection Thread.
			dataCollector = new DataCollector(this);
			dataCollectorThread = new Thread(dataCollector);
			dataCollectorThread.start();
        }
    }

	@Override public void onInterestListUpdate(Attribute[] arg0){}

	@Override public void onNeighborUpdate(Node[] arg0){}

	@Override public void onNewDataObject(DataObject dataObject)
	{
		if (dataObject.getAttribute(attribute.getName(), attribute.getValue()) != null)
		{
			String filepath = dataObject.getFilePath();
			
			try
			{
				FileReader fr = new FileReader(new File(filepath));
				BufferedReader br = new BufferedReader(fr);
				
				String contents = "";
				while (br.ready())
				{
					contents = br.readLine();
				}
				fr.close();
				br.close();
				
				String[] diseaseInfo = contents.split(",");
				
				if (diseaseInfo.length == 6)
				{
					String name = diseaseInfo[0];
					String sender = diseaseInfo[1];
					long exposedDuration = Long.parseLong(diseaseInfo[2]);
					long infectiousDuration = Long.parseLong(diseaseInfo[3]);
					double infectionProbability = Double.parseDouble(diseaseInfo[4]);
					
					long time = System.currentTimeMillis();
					
					// Check whether we are already infected.
					boolean hasDisease = false;
					boolean isSender = false;
					for (Disease d : diseases)
					{
						if (d.getName().equals(name))
						{
							hasDisease = true;
						}
						
						if (sender.equals(localBluetoothAddress)) // If we received the disease from ourselves...
						{
							isSender = true;
						}
						
						if (hasDisease)
						{
							break;
						}
					}
					
					if (hasDisease)
					{
						if (!isSender) // If we have the disease but we aren't the sender, we can delete the file.
						{
							// Log disease to extra.
							BufferedWriter extraWriter = new BufferedWriter(new FileWriter(extraDiseaseFile, true));
							extraWriter.write(name + "," + sender + "," + time + ",2\n"); // 0 for not infected, 1 for new infection, 2 for already infected.
							extraWriter.flush();
							extraWriter.close();
							
							haggleHandle.deleteDataObject(dataObject);
							System.out.println("Already infected with " + name + ". Deleted " + dataObject.getFileName());
							dataObject.dispose();
						}
					}
					else
					{
						// If we don't have the disease we need to see if we can get infected by it.
						if (infectionProbability > rand.nextDouble())
						{
							System.out.println("Contracted disease: " + name + " from " + sender);
							Disease disease = new Disease(name, sender, time, exposedDuration, infectiousDuration, infectionProbability);
							diseases.add(disease);
							//publishQueue.add(disease);
							
							BufferedWriter extraWriter = new BufferedWriter(new FileWriter(extraDiseaseFile, true));
							extraWriter.write(name + "," + sender + "," + time + ",1\n"); // 0 for not infected, 1 for new infection, 2 for already infected.
							extraWriter.flush();
							extraWriter.close();
							
							Message msg = new Message();
							msg.what = Constants.UPDATE_HEALTH_STATUS;
							guiHandler.sendMessage(msg);
							
							// Write the new disease to the disease log.
							try
							{
								BufferedWriter bw = new BufferedWriter(new FileWriter(diseaseFile, false));
								
								for (Disease d : diseases)
								{
									bw.write(d.getName() + "," + d.getSender() + "," + d.getTimeContracted() + "," + d.getExposedDuration() + "," + d.getInfectiousDuration() + "," + d.getInfectionProbability() + "\n");
									bw.flush();
								}
								
								bw.close();
							}
							catch (IOException ex)
							{
								System.out.println("Cannot write to log file.");
								quit(1);
							}
						}
						else
						{
							BufferedWriter extraWriter = new BufferedWriter(new FileWriter(extraDiseaseFile, true));
							extraWriter.write(name + "," + sender + "," + time + ",0\n"); // 0 for not infected, 1 for new infection, 2 for already infected.
							extraWriter.flush();
							extraWriter.close();
							System.out.println("Received " + name + " but wasn't infected.");
						}
						
						// Once we've dealt with the disease, delete the data object.
						haggleHandle.deleteDataObject(dataObject);
						System.out.println("Deleted " + name + " " + dataObject.getFileName());
						dataObject.dispose();
					}
				}
				else // Received a bad file (contents are not valid).
				{
					haggleHandle.deleteDataObject(dataObject);
					System.out.println("Deleted " + dataObject.getFileName());
					dataObject.dispose();
				}
			}
			catch (FileNotFoundException e)
			{
				System.out.println("File not found.");
			}
			catch (IOException e)
			{
				System.out.println("Could not read from file.");
			}
		}
		
		dataObject.dispose();
	}

	@Override public void onShutdown(int arg0)
	{
		if (haggleHandle != null)
		{
			haggleHandle.dispose();
			haggleHandle = null;
		}
	}
	
	// Called when user presses the "menu" button.
	@Override public boolean onCreateOptionsMenu(Menu menu)
	{
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	@Override protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
	    super.onActivityResult(requestCode, resultCode, data);
	    
	    switch (requestCode)
	    {
	    	case Constants.GPS_REQUEST_CODE:
	    		if (resultCode == 0)
	    		{
	    			isGPSEnabled = (Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED)) != null ? (Settings.Secure.getString(getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED)).contains("gps") : false;

	    			Message msg = new Message();
	    			msg.what = Constants.UPDATE_GPS_STATUS;
	    			guiHandler.sendMessage(msg);
	    		}
	    		break;
	    }
	}
	
	// Stops screen from rotating.
	@Override public void onConfigurationChanged(Configuration newConfig)
	{
		setRequestedOrientation(1);
	}
	
	// Called when user selects an item from the menu.
	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.quit:
				AlertDialog dialog = new AlertDialog.Builder(this).create();
				dialog.setMessage("This application can run in the background, " +
						"collecting and sending data whilst you use other " +
						"applications.\n\nIt is recommended that you run " +
						"this application in the background rather than quitting " +
						"it completely.");
				dialog.setButton(AlertDialog.BUTTON_POSITIVE, "Run in background.", quitListener);
				dialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Quit completely.", quitListener);
				dialog.show();
				break;
		}
		return true;
	}
	
	// Initiates haggle.
	public int initHaggle()
	{
		if (haggleHandle != null) return Constants.HAGGLE_STATUS_OK; // Do nothing if haggle is already running.

		int status = Handle.getDaemonStatus();

		if (status == Handle.HAGGLE_DAEMON_NOT_RUNNING || status == Handle.HAGGLE_DAEMON_CRASHED)
		{
			System.out.println("Trying to spawn haggle.");
			if (!Handle.spawnDaemon(new LaunchCallback(){public int callback(long milliseconds){return 0;}}))
			{
				System.out.println("Haggle failed to spawn.");
				Toast.makeText(this, "Haggle failed to spawn. Please contact ah647@cam.ac.uk.", Toast.LENGTH_LONG).show();

				return Constants.HAGGLE_STATUS_SPAWN_DAEMON_FAILED;
			}
		}
		
		long pid = Handle.getDaemonPid();
		System.out.println("Haggle daemon pid is " + pid);

		int tries = 1;

		while (tries > 0)
		{
			try
			{
				haggleHandle = new Handle("Virtual Disease Experiment");
			}
			catch (Handle.RegistrationFailedException e)
			{
				System.out.println("Registration failed : " + e.getMessage());

				if (e.getError() == Handle.HAGGLE_BUSY_ERROR)
				{
					Handle.unregister("Virtual Disease Experiment");
					continue;
				}
				else if (--tries > 0)
				{
					continue;
				}
				
				System.out.println("Registration failed, giving up");
				Toast.makeText(this, "Haggle failed to spawn. Please contact ah647@cam.ac.uk.", Toast.LENGTH_LONG).show();
				return Constants.HAGGLE_STATUS_REGISTRATION_FAILED;
			}
			break;
		}

		haggleHandle.registerEventInterest(EVENT_NEIGHBOR_UPDATE, this);
		haggleHandle.registerEventInterest(EVENT_NEW_DATAOBJECT, this);
		haggleHandle.registerEventInterest(EVENT_INTEREST_LIST_UPDATE, this);
		haggleHandle.registerEventInterest(EVENT_HAGGLE_SHUTDOWN, this);
		
		haggleHandle.eventLoopRunAsync();
		haggleHandle.getApplicationInterestsAsync();
		haggleHandle.getDataObjectsAsync();
		
		System.out.println("Haggle event loop started");   

		return Constants.HAGGLE_STATUS_OK;
	}
	
	// Shuts down haggle.
	public void shutdownHaggle()
	{
		if (haggleHandle != null)
		{
			haggleHandle.unregisterInterest(attribute);
			haggleHandle.eventLoopStop();
			haggleHandle.shutdown();
		}
	}
	
	// Quits the program. This method should stop all threads, do some cleanup, etc.
	public void quit(int quitcode)
	{
		if (locationManager != null && dataCollector != null)
		{
			locationManager.removeUpdates(dataCollector);
		}
		
		if (bluetoothAdapter != null && dataCollector != null)
		{
			this.unregisterReceiver(dataCollector);
		}
		
		if (dataCollector != null)
		{
			dataCollector.setRunning(false);
			dataCollector = null;
		}
		
		if (dataCollectorThread != null)
		{
			dataCollectorThread.interrupt();
			dataCollectorThread = null;
		}
		
		if (haggleHandle != null && published != null)
		{
			for (DataObject d : published)
			{
				haggleHandle.deleteDataObject(d);
				d.dispose();
				d = null;
			}
			published.clear();
			published = null;
		}
		
		shutdownHaggle();
		haggleHandle = null;
		
		super.onDestroy();
		
		System.exit(quitcode);
	}
	
	/*
	 * Accessor Methods.
	 */
	
	public Handle getHaggleHandle()
	{
		return haggleHandle;
	}
	
	public BluetoothAdapter getBluetoothAdapter()
	{
		return bluetoothAdapter;
	}
	
	public LocationManager getLocationManager()
	{
		return locationManager;
	}
	
	public Handler getGUIHandler()
	{
		return guiHandler;
	}
	
	public LinkedBlockingQueue<DataObject> getPublished()
	{
		return published;
	}
	
	public File getDiseaseFile()
	{
		return diseaseFile;
	}
	
	public File getLogFile()
	{
		return logFile;
	}
	
	public Attribute getAttribute()
	{
		return attribute;
	}
	
	public String getLocalBluetoothAddress()
	{
		return localBluetoothAddress;
	}
	
	public LinkedBlockingQueue<Disease> getDiseases()
	{
		return diseases;
	}
	
	public Random getRandom()
	{
		return rand;
	}
	
	/*
	 * Mutator Methods.
	 */
	
	public void setGPSInfo(int status, int satellites)
	{
		gpsStatus = status;
		numOfSatellites = satellites;
	}
	
	public void setIsGPSEnabled(boolean bool)
	{
		isGPSEnabled = bool;
	}
	
	public void setIsGPSScanning(boolean bool)
	{
		isGPSScanning = bool;
	}
	
	public void setIsBluetoothScanning(boolean bool)
	{
		isBluetoothScanning = bool;
	}
	
	public void setLocalBluetoothAddress(String address)
	{
		localBluetoothAddress = address;
	}
}

class QuitMenuListener implements android.content.DialogInterface.OnClickListener
{
	private VirtualDisease parent;
	
	public QuitMenuListener(VirtualDisease vd)
	{
		parent = vd;
	}
	
	@Override public void onClick(DialogInterface dialog, int which)
	{
		switch (which)
		{
			case AlertDialog.BUTTON_POSITIVE:
				parent.moveTaskToBack(true);
				break;
			case AlertDialog.BUTTON_NEGATIVE:
				parent.quit(0);
				break;
		}
	}
}