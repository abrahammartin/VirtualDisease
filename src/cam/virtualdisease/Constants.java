package cam.virtualdisease;

public interface Constants
{
	public final int HAGGLE_STATUS_OK = 0;
	public final int HAGGLE_STATUS_ERROR = -1;
	public final int HAGGLE_STATUS_REGISTRATION_FAILED = -2;
	public final int HAGGLE_STATUS_SPAWN_DAEMON_FAILED = -3;
	
	public final String DISEASE_FILE = "diseases.db";
	public final String MEASUREMENTS_FILE = "measurements.log";
	public final String EXTRA_DISEASE_FILE = "extra.db";
	public final String GPS = "gps";
	
	public final int GPS_REQUEST_CODE = 1;
	public final int SCAN_INTERVAL = 90000; // 90 seconds.
	public final int GPS_SCAN_TIMEOUT = 30000; // 30 seconds.
	public final int BLUETOOTH_SCAN_DURATION = 15000; // 15 seconds.
	
	public final int UPDATE_HEALTH_STATUS = 0;
	public final int UPDATE_GPS_STATUS = 1;
	public final int UPDATE_BLUETOOTH_STATUS = 2;
	public final int UPDATE_BLUETOOTH_ADDRESS = 3;
}