package snob.simulation;

import java.util.*;

public class Simulation {

    public static void main(String[] args) {
        int nqs = 100; // number of q points
        int[] qs = new int[nqs];

        int size = 0;
        int sample = 0; //10 * 1000;
        int max = (int) Math.floor(Math.log(size)) + 1;
        int kson = 0; // (int) Math.floor(Math.log(20));

        if(args.length > 0 && args.length == 4) {
            size = Integer.valueOf(args[0]);
            sample = Integer.valueOf(args[1]); //10 * 1000;
            max = Integer.valueOf(args[2]);
            kson = Integer.valueOf(args[3]);
        } else {
            size = 1000;
            sample = 10 * 1000;
            max = (int) Math.floor(Math.log(size)) + 1; // log(size)
            kson = 0; // only rps;
        }
        // set the different q points for a given size of the network
        for(int i = 0; i < nqs; ++i) {
            qs[i] = (int) Math.floor(size/+1);
        }

        int krps = max - kson; //2* (int) Math.floor(Math.log(size));

        System.err.println("Simulating...");
        System.err.println("K(rps)= "+krps);
        System.err.println("K(son)= "+kson);
        int r = 0;
        for (int q : qs) {
            r++;
            double meanQ = 0;
            for (int i = 0; i < sample; i++) {
                Sim s = new Sim(size, krps, kson, -1, q, true);
                s.start();
                meanQ += s.getMeanQ();
                System.err.printf("s=%d q=%d, mean=%f %n", i, q, meanQ/(i+1));
            }
            double approximationHarmonic = ((size * harmonic(size)) / (q * (krps+kson))) + Math.log(size) ;
            double approximationDev = ((size * Math.log((size)) + 0.5772156649 * size + 1/2) / (q * (krps+kson))) + Math.log(size);
            meanQ = meanQ / sample;
            String res = String.format(Locale.US, "%d, %d, %d, %d, %d, %.2f, %.2f, %.2f %n", r, size, krps, kson, q, meanQ, approximationHarmonic, approximationDev);
            System.out.printf(res);
        }
    }

    private static double harmonic(int n) {

        double res = 0;
        for(int i = 1; i <= n; i++) {
            res += 1.0/i;
        }
        System.err.printf("Harmonic(%d) = %f%n", n, res);
        return res;
    }



    private static class Sim {
        // size of the network
        private int size;
        // size of the rps view
        private int krps;
        // son view size
        private int kson;
        // number of rounds before finish the experiment
        private int rounds = 100;
        // current round
        private int currentRound = 0;
        // number of replicated query
        private int q = 1;
        // number of replicated query, if q == -1, pq = size
        private int pq = 0;
        // pull or push method on the overlay
        private boolean pull = true;

        // all peers seen for each peers.
        private Map<Integer, LinkedHashSet<Integer>> seen;
        // store all rounds corresponding to a q when a q has seen every peer
        private Map<Integer, Integer> seenqfinished;
        // store the round where peer i saw every peer in the network
        private Map<Integer, Integer> seenfinished;
        // store the neighborhood of each peer in the rps
        public Map<Integer, LinkedHashSet<Integer>> rps;
        // store the neighborhood of each peer in the son
        public Map<Integer, LinkedHashSet<Integer>> son;
        // store each q for each peer, q in {q; 1}, 2 values
        private int[] peers;
        public int[] getPeers() {
            return peers;
        }



        Sim(int n, int krps, int kson, int rounds, int q, boolean pull) {
            this.pull = pull;
            this.size = n;
            this.q = q;
            this.krps = krps;
            this.kson = kson;
            this.rounds = rounds;
            this.peers = new int[n];
            this.rps = new LinkedHashMap<>();
            this.son = new LinkedHashMap<>();
            this.seen = new LinkedHashMap<>();

            this.seenfinished = new LinkedHashMap<>();
            this.seenqfinished = new LinkedHashMap<>();

            if(rounds == -1) {
                this.rounds = Integer.MAX_VALUE;
            }

            for (int i = 0; i < size; i++) {
                rps.put(i, new LinkedHashSet<>());
                while(rps.get(i).size() != 1) {
                    int rn = (int) Math.floor(Math.random() * size);
                    if(rn != i && !rps.get(i).contains(rn)) rps.get(i).add(rn);
                }
                son.put(i, new LinkedHashSet<>());
                seen.put(i, new LinkedHashSet<>());
                seen.get(i).add(i);
            }

            int choosen = 0;
            if(q == 1) {
                pq = size;
                for(int i = 0; i < size; i++) {
                    peers[i] = 1;
                }
            } else {
                pq = q;
                for(int i = 0; i < size; i++) {
                    if(i < q) {
                        peers[i] = q;
                    } else {
                        peers[i] = 1;
                    }
                }
            }
        }
        public void start() {
            // System.out.printf("Starting with N=%d q=%d |rps|=%d |son|=%d rounds=%d %n", size, q, krps, kson, rounds);
            boolean stop = false;
            int i = 0;
            while(!stop && i < rounds) {
                currentRound = i;
                shuffle();
                computeSeen();
                if(seenfinished.size() == pq) {
                    stop = true;
                }
                ++i;
            }
        }
        public double getMeanQ() {
            int sum = 0;
            for (int i = 0; i < size; i++) {
                if(peers[i] == q){
                    sum += seenfinished.get(i);
                }
            }
            return sum/pq;
        }

        private void computeSeen() {
            // System.err.printf("Merging the set of %d with neighbours. %n", k);
            for (int i = 0; i < size; i++) {
                if(!seenfinished.containsKey(i)) {

                    if(pull) {
                        Object[] arr = rps.get(i).toArray();
                        Object[] arr_son = son.get(i).toArray();
                        int q = (int) peers[i];
                        for (int i1 = 0; i1 < arr.length; i1++) {
                            int id = (int) arr[i1];
                            update(i, id, q);
                        }
                        if (kson > 0) {
                            for (int i1 = 0; i1 < arr_son.length; i1++) {
                                int id = (int) arr_son[i1];
                                update(i, id, q);
                            }
                        }
                    } else {

                    }
                    // System.err.println(seen.get(i));
                    if(peers[i] == pq && seen.get(i).size() == pq) {
//                        System.out.printf("[query-%d]finished. q=%d |rps|=%d |son|=%d round=%d %n",
//                                i, peers[i],
//                                krps, kson,
//                                currentRound);
                        seenfinished.put(i, currentRound);
                    }
                }
            }
        }

        private LinkedHashSet<Integer> update(int toUpdate, int newPeer, int q) {
            LinkedHashSet<Integer> updates = new LinkedHashSet<>();
            if (q == peers[newPeer] && q != 1) {
                seen.get(newPeer).forEach(p -> {
                    if(!seen.get(toUpdate).contains(p)) updates.add(p);
                    seen.get(toUpdate).add(p);
                });
                if(!seen.get(toUpdate).contains(newPeer)) updates.add(newPeer);
                seen.get(toUpdate).add(newPeer);
            }
            return updates;
        }

        private void shuffle() {
            // System.err.println("shuffle" + krps + "_" + kson + "_");
            for (int i = 0; i < size; i++) {
                // LinkedHashSet<Integer> newRpsPeers
                rps.get(i).clear();
                Random rnd = new Random();
                // fill the rps view of i
                while(rps.get(i).size() != krps){
                    int rn = rnd.nextInt(size);
                    if(!rps.get(i).contains(rn) && rn != i) {
                        rps.get(i).add(rn);
                    }
                }
                if(kson > 0) {
                    // first check if we have different q in our son's view, remove them
                    List<Integer> toRemove = new ArrayList<>();
                    for (Integer peer : son.get(i)) {
                        if(peers[i] != peers[peer]) toRemove.add(peer);
                    }
                    if(toRemove.size() > 0) {
                        for (Integer peer : toRemove) {
                            son.get(i).remove(peer);
                        }
                    }
                    //  When the rps meet someone with q == peers[i], keep him in the view.
                    if(son.get(i).size() < kson) {
                        // first fill it with common entries matching q == peers[i]
                        for (Integer peer : rps.get(i)) {
                            if(!son.get(i).contains(peer) && peers[i] == peers[peer] && son.get(i).size() != kson) {
                                // System.err.println("adding a compon entry");
                                son.get(i).add(peer);
                            }
                        }
                        if(son.get(i).size() < kson) {
                            while(son.get(i).size() != kson) {
                                int rn = rnd.nextInt(size);
                                if(!son.get(i).contains(rn) && rn != i) {
                                    son.get(i).add(rn);
                                }
                            }
                        }
                    }
                }
            }
            // System.err.printf("*end%n");
        }
    }
}
