package cam.virtualdisease.datatypes;

public class Disease
{
	private String name;
	private String sender;
	private long timeContracted;
	private long exposedDuration;
	private long infectiousDuration;
	private double infectionProbability;

	public Disease(String name, String sender, long timeContracted, long exposedDuration, long infectiousDuration, double infectionProbability)
	{
		this.name = name;
		this.sender = sender;
		this.timeContracted = timeContracted;
		this.exposedDuration = exposedDuration;
		this.infectiousDuration = infectiousDuration;
		this.infectionProbability = infectionProbability;
	}

	public String getName()
	{
		return name;
	}
	
	public String getSender()
	{
		return sender;
	}
	
	public long getTimeContracted()
	{
		return timeContracted;
	}

	public long getExposedDuration()
	{
		return exposedDuration;
	}

	public long getInfectiousDuration()
	{
		return infectiousDuration;
	}

	public double getInfectionProbability()
	{
		return infectionProbability;
	}
	
	public boolean isInfectious()
	{
        return System.currentTimeMillis() >= (timeContracted + exposedDuration) && System.currentTimeMillis() <= (timeContracted + exposedDuration + infectiousDuration);
    }
	
	public boolean isCured()
	{
        return System.currentTimeMillis() > (timeContracted + exposedDuration + infectiousDuration);
    }
}