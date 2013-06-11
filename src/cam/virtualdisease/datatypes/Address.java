package cam.virtualdisease.datatypes;

public class Address
{
	private String address;
	private long timestamp;
	
	public Address(String address, long timestamp)
	{
		this.address = address;
		this.timestamp = timestamp;
	}
	
	public String getAddress()
	{
		return address;
	}
	
	public long getTimestamp()
	{
		return timestamp;
	}
}
