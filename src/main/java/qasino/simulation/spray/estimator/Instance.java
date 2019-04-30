package qasino.simulation.spray.estimator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;

/**
 * A instance of an aggregator
 */
public class Instance{

	private final int pid;

	public int getPid() {
		return pid;
	}

	public long getDeath() {
		return death;
	}

	public long getBirth() {
		return birth;
	}

	public BigDecimal getEstimation() {
		return estimation;
	}

	private final long death;
	private final long birth;
	private final BigDecimal estimation;

	private Instance(int pid, long birth, long death, BigDecimal estimation) {
		this.pid = pid;
		this.death = death;
		this.birth = birth;
		this.estimation = estimation;
	}

	public Instance(int pid, long birth, long death) {
		this(pid, birth, death, BigDecimal.ONE);
	}

	public static Instance merge(Instance i1, Instance i2) {
		if(i1 == null && i2 == null) return null;
		else if(i1 == null) {
			return new Instance(i2.pid, i2.birth, i2.death, i2.estimation.divide(BigDecimal.valueOf(2)));
		} else if(i2 == null) {
			return new Instance(i1.pid, i1.birth, i1.death, i1.estimation.divide(BigDecimal.valueOf(2)));
		} else if (i1.pid == i2.pid && i1.birth == i2.birth && i1.death == i2.death) {
			return new Instance(i1.pid, i1.birth, i1.death, i1.estimation.add(i2.estimation).divide(BigDecimal.valueOf(2)));
		} else {
			throw new Error("Instances cannot be different;");
		}
	}
	
	public long size() {
		return Math.round(BigDecimal.ONE.divide(estimation, 1, RoundingMode.HALF_UP).doubleValue());
	}

	/**
	 * Return true if it is a major instance, aka if the global clock is older than the death age
	 * @param global_clock
	 * @return
	 */
	public boolean isMajor(int global_clock) {
		return global_clock > this.death;
	}

	/**
	 * Return true if it is a minor instance, aka if instance.death is older than the global clock
	 * @param global_clock
	 * @return
	 */
	public boolean isMinor(int global_clock) {
		return !this.isMajor(global_clock);
	}

	@Override
	public String toString() {
//		return "<"+ pid +","+birth+","+death+" : "+size()+"("+estimation+")>";
		return "<pid="+ pid + ",birth=" + birth + ",death=" + death + ",size="+size()+">";
	}

	@Override
	public boolean equals(Object obj) {
		return this.getPid() == ((Instance) obj).getPid() && this.getBirth() == ((Instance) obj).getBirth() && this.getDeath() == ((Instance) obj).getDeath();
	}

	public static int compare(Instance i1, Instance i2) {
		if(i1 == null) return -1;
		if(i2 == null) return 1;
		else if (i1.birth == i2.birth && i1.pid==i2.pid && i1.death == i2.death) return 0;
		return Comparator.comparing((Instance i) -> i.birth).thenComparing(i -> i.pid).compare(i1, i2);
	}
}
