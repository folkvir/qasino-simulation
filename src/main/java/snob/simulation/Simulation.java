package snob.simulation;

import java.util.*;

public class Simulation {
    public static void main(String[] args) {
        int size = 0;
        int sample = 0; //10 * 1000;
        int max = (int) Math.floor(Math.log(size)) + 1;
        int kson = 0; // (int) Math.floor(Math.log(20));

        if (args.length > 0 && args.length == 4) {
            size = Integer.valueOf(args[0]);
            sample = Integer.valueOf(args[1]); //10 * 1000;
            max = Integer.valueOf(args[2]);
            kson = Integer.valueOf(args[3]);
        } else {
            size = 1000;
            sample = 100;
            max = 1; // (int) Math.floor(Math.log(size)) + 1; // log(size)
            kson = 0; // only rps;
        }
        int nqs = 100; // number of q points
        int[] qs = new int[nqs];
        // set the different q points for a given size of the network
        for (int i = 0; i < nqs; ++i) {
            qs[i] = (int) Math.floor(size / (i+1));
        }
//        int nqs = 1;
//        int[] qs = new int[1];
//        qs[0] = 25;

        int krps = max - kson; //2* (int) Math.floor(Math.log(size));

        System.err.println("=> Simulating...");
        System.err.println("** K(rps + son)= " + max);
        System.err.println("** K(rps)= " + krps);
        System.err.println("** K(son)= " + kson);
        System.err.println("** Sample= " + sample);
        System.err.println("** Size= " + size);
        System.err.println("** Qs point= " + nqs);

        int r = 0;
        for (int q : qs) {
            r++;
            Run run = new Run(sample, size, krps, kson, q, r);
            Thread t = new Thread(run);
            t.start();
        }
    }

    public static class Run implements Runnable {
        private final int sample;
        private final int size;
        private final int krps;
        private final int kson;
        private final int q;
        private final int r;

        Run(int sample, int size, int krps, int kson, int q, int r) {
            this.sample = sample;
            this.size = size;
            this.krps = krps;
            this.kson = kson;
            this.q = q;
            this.r = r;
        }
        @Override
        public void run() {
            double meanQ = 0;
            double meanQall = 0;
            for (int i = 0; i < sample; i++) {
                Sim s = new Sim(size, krps, kson, -1, q, true);
                s.start();
                meanQ += s.getMeanQ();
                meanQall += s.getMeanQAll();
                // System.err.printf("s=%d q=%d, mean=%f %n", i, q, meanQ / (i + 1));
            }
            double H = harmonic(size);

            double approximationHarmonic = ((size * H) / (q * (krps + kson)));
            // double approximationDev = ((size * Math.log((size)) + 0.5772156649 * size + 1 / 2) / (q * (krps + kson))) + Math.log(size);
            meanQ = meanQ / sample;
            meanQall = meanQall / sample;
            double ratioQ = meanQ / approximationHarmonic;
            double ratioQAll = meanQall / approximationHarmonic;
            String res = String.format(Locale.US, "%d, %d, %d, %d, %d, %.2f, %.2f, %.2f, %.2f, %.2f %n", r, size, krps, kson, q, meanQ, meanQall, approximationHarmonic, ratioQ, ratioQAll);
            System.out.printf(res);
        }
    }

    private static double harmonic(int n) {
        double res = 0;
        for (int i = 1; i <= n; i++) {
            res += 1.0 / i;
        }
        return res;
    }


    private static class Sim {
        // store the neighborhood of each peer in the rps
        public Map<Integer, LinkedHashSet<Integer>> rps;
        // store the neighborhood of each peer in the son
        public Map<Integer, LinkedHashSet<Integer>> son;
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
        // all q peers seen for each peer.
        private Map<Integer, LinkedHashSet<Integer>> seen;
        // all q peers seen for each peer.
        private Map<Integer, LinkedHashSet<Integer>> seenall;
        // store the round where peer q saw every q in the network
        private Map<Integer, Integer> seenfinished;
        // store the round where peer q saw every peer in the network
        private Map<Integer, Integer> seenallfinished;
        // store each q for each peer, q in {q; 1}, 2 values
        private int[] peers;

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
            this.seenall = new LinkedHashMap<>();

            this.seenfinished = new LinkedHashMap<>();
            this.seenallfinished = new LinkedHashMap<>();

            if (rounds == -1) {
                this.rounds = Integer.MAX_VALUE;
            }

            for (int i = 0; i < size; i++) {
                rps.put(i, new LinkedHashSet<>());
                while (rps.get(i).size() != 1) {
                    int rn = (int) Math.floor(Math.random() * size);
                    if (rn != i && !rps.get(i).contains(rn)) rps.get(i).add(rn);
                }
                son.put(i, new LinkedHashSet<>());
                seen.put(i, new LinkedHashSet<>());
                seen.get(i).add(i);
                seenall.put(i, new LinkedHashSet<>());
                seenall.get(i).add(i);
            }

            int choosen = 0;
            if (q == 1) {
                pq = size;
                for (int i = 0; i < size; i++) {
                    peers[i] = 1;
                }
            } else {
                pq = q;
                for (int i = 0; i < size; i++) {
                    if (i < q) {
                        peers[i] = q;
                    } else {
                        peers[i] = 1;
                    }
                }
            }
        }

        public int[] getPeers() {
            return peers;
        }

        public void start() {
            // System.out.printf("Starting with N=%d q=%d |rps|=%d |son|=%d rounds=%d %n", size, q, krps, kson, rounds);
            boolean stop = false;
            int i = 0;
            while (!stop && i < rounds) {
                currentRound = i;
                shuffle();
                computeSeen();
                if (seenallfinished.size() == pq && seenfinished.size() == pq) { // finished when all q have seen all peers in the network
                    stop = true;
                    // System.err.printf("** qq=%.2f qn=%.2f %n", getMeanQ(), getMeanQAll());
                }
                ++i;
            }
        }

        public double getMeanQ() {
            int sum = 0;
            for (int i = 0; i < size; i++) {
                if (peers[i] == q) {
                    sum += seenfinished.get(i);
                }
            }
            return sum / pq;
        }
        public double getMeanQAll() {
            int sum = 0;
            for (int i = 0; i < size; i++) {
                if (peers[i] == q) {
                    sum += seenallfinished.get(i);
                }
            }
            return sum / pq;
        }

        private void computeSeen() {
            // System.err.printf("Merging the set of %d with neighbours. %n", k);
            for (int i = 0; i < size; i++) {
                if (!seenfinished.containsKey(i) || !seenallfinished.containsKey(i)) {
                    if (pull) {
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
                        // put here the behavior for push
                    }
                    if (!seenfinished.containsKey(i) && peers[i] == pq && seen.get(i).size() == pq) {
//                        System.out.printf("[query-%d] meet all q q=%d |rps|=%d |son|=%d round=%d %n",
//                                i, peers[i],
//                                krps, kson,
//                                currentRound);
                        seenfinished.put(i, currentRound);
                    }
                    if (!seenallfinished.containsKey(i) && peers[i] == pq && seenall.get(i).size() == size) {
//                        System.out.printf("[query-%d] meet all peers. q=%d |rps|=%d |son|=%d round=%d %n",
//                                i, peers[i],
//                                krps, kson,
//                                currentRound);
                        seenallfinished.put(i, currentRound);
                    }
                }
            }
        }

        private void update(int toUpdate, int newPeer, int q) {
            if (q == peers[newPeer] && q != 1) {
                // System.err.println(pq + "_" + currentRound + "_" + seenfinished.containsKey(toUpdate) + "__" + seenallfinished.containsKey(toUpdate));
                for (Integer peer : seen.get(newPeer)) {
                    if(peers[peer] == q) seen.get(toUpdate).add(peer);
                }
                seenall.get(toUpdate).addAll(seenall.get(newPeer));
                seen.get(toUpdate).add(newPeer);
            }
            seenall.get(toUpdate).add(newPeer);
        }

        private void shuffle() {
            // System.err.println("shuffle" + krps + "_" + kson + "_");
            for (int i = 0; i < size; i++) {
                // LinkedHashSet<Integer> newRpsPeers
                rps.get(i).clear();
                Random rnd = new Random();
                // fill the rps view of i
                while (rps.get(i).size() != krps) {
                    int rn = rnd.nextInt(size);
                    if (!rps.get(i).contains(rn) && rn != i) {
                        rps.get(i).add(rn);
                    }
                }
                if (kson > 0) {
                    // first check if we have different q in our son's view, remove them
                    List<Integer> toRemove = new ArrayList<>();
                    for (Integer peer : son.get(i)) {
                        if (peers[i] != peers[peer]) toRemove.add(peer);
                    }
                    if (toRemove.size() > 0) {
                        for (Integer peer : toRemove) {
                            son.get(i).remove(peer);
                        }
                    }
                    //  When the rps meet someone with q == peers[i], keep him in the view.
                    if (son.get(i).size() < kson) {
                        // first fill it with common entries matching q == peers[i]
                        for (Integer peer : rps.get(i)) {
                            if (!son.get(i).contains(peer) && peers[i] == peers[peer] && son.get(i).size() != kson) {
                                // System.err.println("adding a compon entry");
                                son.get(i).add(peer);
                            }
                        }
                        if (son.get(i).size() < kson) {
                            while (son.get(i).size() != kson) {
                                int rn = rnd.nextInt(size);
                                if (!son.get(i).contains(rn) && rn != i) {
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
