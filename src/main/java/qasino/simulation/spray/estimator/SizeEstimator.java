package qasino.simulation.spray.estimator;

import peersim.core.Network;
import sun.nio.ch.Net;

import java.util.*;

import static java.lang.System.exit;
import static java.lang.System.in;

public class SizeEstimator {

	private Random rand;
	private int alpha = 10;
	private long current_pv_size = 0;
	private final int pid;
	private int created = 0;
	private int clock = 0;
	private Deque<Instance> instances = new ArrayDeque<>();

	public SizeEstimator(int pid, Random rand) {
		this.pid = pid;
		this.rand = rand;
	}

	public int getInstancesSize() {
		return this.instances.size();
	}


	public void merge(SizeEstimator ia) {
		ArrayList<Instance> us = new ArrayList<>(instances);
		ArrayList<Instance> remote = new ArrayList<>(ia.instances);
		ArrayList<Instance> arr = new ArrayList<>();
		clock = Math.max(clock, ia.clock);
		ia.clock = clock;

		us.forEach(elem -> {
			if(remote.contains(elem)) {
				int index = remote.indexOf(elem);
				arr.add(Instance.merge(elem, remote.get(index)));
				remote.remove(index);
			} else {
				// if not contained in remote add
				arr.add(Instance.merge(null, elem));
			}
		});

		for (Instance instance : remote) {
			arr.add(Instance.merge(null, instance));
		}

		Collections.sort(arr, Comparator.comparing((Instance i) -> i.getBirth()).thenComparing(i ->  i.getPid()));
		ArrayDeque<Instance> union = new ArrayDeque<>(arr);


		if(!union.isEmpty()) {
			Instance major;
			do {
				major = union.pollFirst();
				//System.err.println("[" + pid + "]dowhile: " + clock + " : " + major);
			} while(!union.isEmpty() && union.getFirst().isMajor(clock) );
			// it remains minor at this points add all in the union list
			//System.err.println("[" + pid + "]keep: " + clock + " : " + major);
			union.addFirst(major);
		}

		instances = union;
		ia.instances = union.clone();
	}	

	private void createNewInstance() {
		long size = this.size();
		int ttl = (int) Math.round(alpha * Math.log(size) + alpha);
		if(ttl < 1) ttl = 1;
		int proba = Math.round(size);
		if(proba <= 0 || rand.nextInt(proba) == 0) {
			created++;
			Instance i = new  Instance(pid, clock, clock + ttl);
			System.err.println("Creating a new estimator instance:" + i);
			instances.addLast(i);
		}
	}

	/**
	 * Compute aggregation using a remote SizeEstimatorOld
	 * @param pvSize
	 * @param remote
	 */
	public void compute(int pvSize, SizeEstimator remote) {
		current_pv_size = pvSize;
		// increment the global clock
		clock++;
		// probabilistic creation of a new instance
		createNewInstance();
		// merge global clock using max
		merge(remote);
	}

	/**
	 * Estimate the size of the network using the oldest instances created in the network.
	 *
	 * @return
	 */
	public long size() {
		if (instances.isEmpty()) {
			return Math.round(Math.exp(current_pv_size));
		} else {
			return instances.getFirst().size();
		}
	}

	@Override
	public String toString() {
		String s = pid + ": " + clock + " { ";
		for(Instance i : instances) {
			s += i.toString() + " ";
		}
		s += "}";
		return s;
	}

}

