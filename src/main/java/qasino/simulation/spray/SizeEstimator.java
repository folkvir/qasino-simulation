package qasino.simulation.spray;

import peersim.core.Network;
import qasino.simulation.spray.Spray;

import java.util.*;

public class SizeEstimator {
    double threshold = 0.1;
    Random rand;
    private boolean started;

    private class Instance {
        public Date created = new Date();
        public String id = UUID.randomUUID().toString();
        public double estimation = 1;

        public void update(Spray remote) {
            // get the remote estimation
            double r_est = remote.estimator.instances.get(id).estimation;
            double newEstimation = (this.estimation + r_est) / 2;
            remote.estimator.instances.get(id).estimation = new Double(newEstimation);
            this.estimation = new Double(newEstimation);
        }

        public Instance create(Date d, String id, double estimation) {
            Instance inst = new Instance();
            inst.created = d;
            inst.id = id;
            inst.estimation = estimation;
            return inst;
        }

        public Instance copy() {
            return create(this.created, this.id, this.estimation);
        }
    }

    public int getNumberOfInstances() {
        return instances.size();
    }

    protected HashMap<String, Instance> instances = new LinkedHashMap<>();

    public SizeEstimator(Random random) {
        rand = random;
    }

    private void createNewInstance() {
        Instance i = new Instance();
        instances.put(i.id, i);
    }


    public void compute(Spray us, Spray remote) {
        if(!started) {
            int pick = rand.nextInt(us.partialView.size());
            if(pick < 1){
                System.err.println("Creating a new estimator instance: " + Network.size() + " " + threshold + " " + pick + " pv: " + us.partialView.size());
                createNewInstance();
            }
            started = true;
        }

        instances.forEach((id, inst) -> {
            // if new, make a copy
            if(!remote.estimator.instances.containsKey(id)) {
                Instance copy = inst.copy();
                copy.estimation = 0;
                remote.estimator.instances.put(id, copy);
            }
            inst.update(remote);
        });
    }
    public int size() {
        if(instances.size() == 0) return -1;
        return (int) Math.round(1 / this.instances.values().parallelStream().min(Comparator.comparing(a -> a.created)).get().estimation);
    }
}
