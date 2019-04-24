package qasino.simulation.qasino;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * For the simulation context only!!!!
 * This implementation is only usable when the same Q is executed in the network.
 * Consider to put this for each triple pattern of a query if you want to implement a real prototype.
 */
public class StateBasedCrdtCounter {
    public Map<Integer, Integer> crdt = new LinkedHashMap<>();
    public int id;

    public StateBasedCrdtCounter(int ourid) {
        crdt.put(ourid, 0);
        this.id = ourid;
    }

    public int state() {
        return this.crdt.get(id);
    }

    public void increment() {
        crdt.put(id, this.crdt.get(this.id) + 1);
    }

    public void update(StateBasedCrdtCounter remoteCrdt) {
        for (Integer peer : remoteCrdt.crdt.keySet()) {
            if (!this.crdt.containsKey(peer)) {
                this.crdt.put(peer, remoteCrdt.crdt.get(peer));
            } else {
                this.crdt.put(peer, Math.max(remoteCrdt.crdt.get(peer), crdt.get(peer)));
            }
        }
    }

    public int sum() {
        int sum = 0;
        for (Integer peer : this.crdt.keySet()) {
            sum += this.crdt.get(peer);
        }
        return sum;
    }
}
