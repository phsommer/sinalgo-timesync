package projects.clocksync.statistics;

public class Probe {
	
	public final int id;
	public final double time;
	
	public final long hardware;
	public final long logical;
	
	public final double rate;
	public final double temperature;

	public Probe(int id, double time, long hardware, long logical, double rate,
			double temperature) {
		this.id = id;
		this.time = time;
		this.hardware = hardware;
		this.logical = logical;
		this.rate = rate;
		this.temperature = temperature;
	}
}
