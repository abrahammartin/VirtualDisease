package cam.virtualdisease.datatypes;

import java.util.ArrayList;
import android.location.Location;

public class Measurement
{
	private final ArrayList<String> addresses;
	private final Location location;
	private final long timestamp;
	
	private final boolean noGPSData;
	private final boolean noBluetoothData;
	
	public Measurement(ArrayList<String> addresses, Location location, long timestamp)
	{
		this.addresses = addresses;
		this.location = location;
		this.timestamp = timestamp;

        noGPSData = (this.location == null);

        noBluetoothData = (this.addresses == null || this.addresses.isEmpty());
	}
	
	public String toXML()
	{
		if (noGPSData && noBluetoothData)
		{
			return null;
		}
		
		StringBuilder buffer = new StringBuilder();

		buffer.append("<measurement timestamp=\"").append(timestamp).append("\"");

		if (noGPSData)
		{
			buffer.append(">");
		}
		else
		{
			buffer.append(" longitude=\"").append(location.getLongitude()).append("\" latitude=\"").append(location.getLatitude()).append("\">");
		}

		if (!noBluetoothData)
		{	
			for (String address : addresses)
			{
				buffer.append("<mac>").append(address).append("</mac>");
			}
		}

		buffer.append("</measurement>");

		return buffer.toString();
	}
}