package projects.clocksync.nodes.clock;

import sinalgo.runtime.Global;

public class JumpClock extends TempClock {
	
	private final double jumpAtTime;
	private final double jumpInterval;
	private final float tempBeforeJump;
	private final float tempJumpsBy;
	
	
	public JumpClock(double hardwareClockDrift, long hardwareTimeOffset) {
		super(hardwareClockDrift, hardwareTimeOffset);
		jumpAtTime = tempBeforeJump = tempJumpsBy = 0;
		jumpInterval = 1;
	}

	public JumpClock(double hardwareClockDrift, long hardwareTimeOffset,
			long logicalOffset) {
		super(hardwareClockDrift, hardwareTimeOffset, logicalOffset);
		jumpAtTime = tempBeforeJump = tempJumpsBy = 0;
		jumpInterval = 1;
	}
	
	public JumpClock(double hardwareClockDrift, long hardwareTimeOffset, double jumpAtTime,
			double jumpInterval, float tempBeforeJump, float tempJumpsBy) {
		super(hardwareClockDrift, hardwareTimeOffset);
		this.jumpAtTime = jumpAtTime;
		this.jumpInterval = jumpInterval;
		this.tempBeforeJump = tempBeforeJump;
		this.tempJumpsBy = tempJumpsBy;
	}

	public JumpClock(double hardwareClockDrift, long hardwareTimeOffset, long logicalOffset,
			double jumpAtTime, double jumpInterval, float tempBeforeJump, float tempJumpsBy) {
		super(hardwareClockDrift, hardwareTimeOffset, logicalOffset);
		this.jumpAtTime = jumpAtTime;
		this.jumpInterval = jumpInterval;
		this.tempBeforeJump = tempBeforeJump;
		this.tempJumpsBy = tempJumpsBy;
	}
	
	@Override
	public float getTemperature(long second) {
		if (Global.currentTime <= jumpAtTime) {
			return tempBeforeJump;
		} else if (Global.currentTime>= jumpAtTime + jumpInterval){
			return tempBeforeJump + tempJumpsBy;
		} else {
			return (float) (tempBeforeJump + (Global.currentTime - jumpAtTime) / jumpInterval
				* tempJumpsBy);
		}
		
	}

}
