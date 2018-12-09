package snob.simulation;

import java.util.*;
import java.util.stream.Collectors;

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
            sample = 10;
            max = 2; // (int) Math.floor(Math.log(size)) + 1; // log(size)
            kson = 0; // only rps;
        }
//        int nqs = 100; // number of q points
//        int[] qs = new int[nqs];
//        // set the different q points for a given size of the network
//        for (int i = 0; i < nqs; ++i) {
//            qs[i] = (int) Math.floor(size / (i+1));
//        }
        int start = 1;
        int nqs = 1;
        int[] qs = new int[1];
        qs[0] = 10;

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
            Run run = new Run(sample, size, krps, kson, q, r, start);
            Thread t = new Thread(run);
            t.start();
        }
    }

    private static double harmonic(int n) {
        double res = 0;
        for (int i = 1; i <= n; i++) {
            res += 1.0 / i;
        }
        return res;
    }

    public static class Run implements Runnable {
        private final int sample;
        private final int size;
        private final int krps;
        private final int kson;
        private final int q;
        private final int r;
        private final int start;

        Run(int sample, int size, int krps, int kson, int q, int r, int start) {
            this.start = start;
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
            for (int i = 0; i < sample; i++) {
                Sim s = new Sim(size, krps, kson, -1, q, true, start);
                s.start();
                meanQ += s.finish;
                // System.err.printf("s=%d q=%d, mean=%f %n", i, q, meanQ / (i + 1));
            }
            double H = harmonic(size);

            double approximationHarmonic = ((size * H) / (q * krps));
            // double approximationDev = ((size * Math.log((size)) + 0.5772156649 * size + 1 / 2) / (q * (krps + kson))) + Math.log(size);
            meanQ = meanQ / sample;
            double ratioQ = meanQ / approximationHarmonic;
            double maxApproximation = approximationHarmonic * 2.5;
            double other = approximationHarmonic / ((2.0 * krps) / (size - 1.0));
            String res = String.format(Locale.US, "%d, %d, %d, %d, %d, %.2f, %.2f, %.2f, %.2f, %.2f %n", r, size, krps, kson, q, meanQ, approximationHarmonic, ratioQ, maxApproximation, other);
            System.out.printf(res);
        }
    }

    private static class Sim {
        // store the neighborhood of each peer in the rps
        public Map<Integer, LinkedHashSet<Integer>> rps;
        // store the neighborhood of each peer in the son
        public Map<Integer, LinkedHashSet<Integer>> son;
        // finish is equal to round when a q has discover all peers in the network.
        public int finish = -1;
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
        // pull or push method on the overlay
        private boolean pull = true;
        // all q peers seen for each peer.
        private Map<Integer, LinkedHashSet<Integer>> seen;
        // store each q for each peer, q in {q; 1}, 2 values
        private int[] peers;
        // start after 'start' rounds
        private int start;
        private boolean started = false;
        private List<Integer> qs = new LinkedList<>();
        private int qsmax = 0;
        // number of pick per round
        private int pick = 1;

        Sim(int n, int krps, int kson, int rounds, int q, boolean pull, int start) {
            this.start = start;
            this.pull = pull;
            this.size = n;
            this.q = q;
            this.krps = krps;
            this.kson = kson;
            this.rounds = rounds;
            this.peers = new int[n];
            this.start = start;
            this.rps = new LinkedHashMap<>();
            this.son = new LinkedHashMap<>();
            this.seen = new LinkedHashMap<>();

            if (rounds == -1) {
                this.rounds = Integer.MAX_VALUE;
            }

            LinkedHashSet<Integer> connected = new LinkedHashSet<>();
            rps.put(0, new LinkedHashSet<>());
            son.put(0, new LinkedHashSet<>());
            seen.put(0, new LinkedHashSet<>());
            seen.get(0).add(0);

            rps.put(1, new LinkedHashSet<>());
            son.put(1, new LinkedHashSet<>());
            seen.put(1, new LinkedHashSet<>());
            seen.get(1).add(1);

            rps.get(0).add(1);
            rps.get(1).add(0);
            connected.add(0);
            connected.add(1);

            for (int i = 2; i < size; i++) {
                rps.put(i, new LinkedHashSet<>());
                son.put(i, new LinkedHashSet<>());
                seen.put(i, new LinkedHashSet<>());
                seen.get(i).add(i);
                boolean stop = false;
                while (!stop && rps.get(i).size() != krps) {
                    int rn = (int) Math.floor(Math.random() * connected.size());
                    rps.get(i).add(rn);
                    if (rps.get(i).size() >= connected.size()) stop = true;
                }
                connected.add(i);
                System.err.println("Local Clustering coefficient: " + localClusteringCoefficient(i));
            }
            // fill 0 and 1
            while (rps.get(0).size() < krps) {
                int rn = (int) Math.floor(Math.random() * size);
                if (rn != 0 && rn != 1) {
                    rps.get(0).add(rn);
                }
                if (rn != 0 && rn != 1) {
                    rps.get(1).add(rn);
                }
            }
            // pick = (int) Math.floor(size / idealClusteringCoefficient());
            System.err.println("Global Clustering coefficient: " + globalClusteringCoefficient() + " (ideal: " + idealClusteringCoefficient() + ")");
            System.err.printf("** [*] Warming up the network (%d rounds)...%n", start);
            System.err.println(rps);
            shuffle();
            // exit(1);
        }

        static <E> E getRandomSetElement(Set<E> set) {
            return set.stream().skip(new Random().nextInt(set.size())).findFirst().orElse(null);
        }

        public int[] getPeers() {
            return peers;
        }

        public void start() {
            // System.out.printf("Starting with N=%d q=%d |rps|=%d |son|=%d rounds=%d %n", size, q, krps, kson, rounds);
            boolean stop = false;
            int i = 0;
            int step = 10;
            while (!stop && i < rounds) {
                currentRound = i;
                ++i;
                shuffle();
                if (currentRound == start) {
                    System.err.printf("** [*] Put %d queries randomly in the network... %n", q);
                    while (qs.size() != q) {
                        int rn = (int) Math.floor(Math.random() * size);
                        if (!qs.contains(rn)) {
                            qs.add(rn);
                            peers[rn] = q;
                        }
                    }
                    System.err.println("** [*] Starting the measure...");
                    started = true;
                }
                if (started) {
                    if (i % pick == 0) {
                        computeSeen();
                        if (finish != -1) { // finished when all q have seen all peers in the network
                            System.err.printf("** [*] Finished (%d) on a total of %d %n", finish, currentRound);
                            stop = true;
                        }
                    }
                }
            }
        }

        private double idealClusteringCoefficient() {
            return 2.0 * krps / (size - 1.0);
        }

        private double globalClusteringCoefficient() {
            double mean = 0;
            for (Map.Entry<Integer, LinkedHashSet<Integer>> entry : rps.entrySet()) {
                mean += localClusteringCoefficient(entry.getKey());
            }
            return mean / rps.size();
        }

        private double localClusteringCoefficient(int id) {
            List<Integer> neighbors = this.rps.get(id).parallelStream().collect(Collectors.toList());
            if (neighbors.size() == 0) return 0;
            double possible = neighbors.size() * (neighbors.size() - 1);
            if (possible == 0) return 0;
            double actual = 0;
            for (int a : neighbors) {
                for (int b : neighbors) {
                    if (a != b) {
                        if (hasDirectedConnection(a, b)) {
                            actual += 1;
                        }
                    }
                }
            }
            return actual / possible;
        }

        private boolean hasDirectedConnection(int a, int b) {
            return this.rps.get(a).parallelStream().collect(Collectors.toList()).contains(b);
        }

        private void computeSeen() {
            for (Integer qpeer : qs) {
                if (pull) {
//                    int k = (int) Math.floor(Math.random() * rps.get(qpeer).size());
//                    List<Integer> qpeerlist = rps.get(qpeer).parallelStream().collect(Collectors.toList());
//                    seen.get(qpeer).add(qpeerlist.get(k));
                    for (Integer neigh : rps.get(qpeer)) {
                        if (qs.contains(neigh)) {
                            // merge view
                            seen.get(qpeer).addAll(seen.get(neigh));
                        }
                        // otherwise always add only neigh
                        seen.get(qpeer).add(neigh);
                        if (seen.get(qpeer).size() >= qsmax) {
                            qsmax = seen.get(qpeer).size();
                        }
                    }
                }
                if (finish == -1 && seen.get(qpeer).size() >= size) {
                    System.err.printf("[query-%d] meet all peers q=%d |rps|=%d |son|=%d round=%d %n",
                            qpeer, peers[qpeer],
                            krps, kson,
                            (currentRound - start) / pick);
                    finish = (currentRound - start) / pick;
                }
            }
        }

        private void shuffle() {
            System.err.printf("[%d]Shuffling %f...%n", currentRound, globalClusteringCoefficient());
//            for (Integer p : qs) {
//                System.err.println(seen.get(p).size());
//            }
            rps.forEach((id, view) -> {
                exchangeRps(id);
            });
            // System.err.println("Global Clustering coefficient: " + globalClusteringCoefficient() + " (ideal: " + idealClusteringCoefficient() + ")");
        }

        private void exchangeRps(int i) {
            if (rps.get(i).size() > 0) {
                int ex = krps;

                Integer neighbour = getRandomSetElement(rps.get(i));

                LinkedList<Integer> union = new LinkedList<>();
                union.addAll(rps.get(i));
                union.addAll(rps.get(neighbour));

                Collections.shuffle(union);

                List<Integer> rpsi = new LinkedList<>();
                System.err.println(i + "_" + neighbour + "_" + union);
                while (rpsi.size() <= krps) {
                    Integer elem = union.getFirst();
                    if (!rpsi.contains(elem)) {
                        if (elem == i) {
                            rpsi.add(neighbour);
                        } else {
                            rpsi.add(elem);
                        }
                    }
                }
                rps.get(i).clear();
                rps.get(i).addAll(rpsi);

                for (Integer integer : union) {
                    if (integer == neighbour) {
                        union.remove(integer);
                        union.add(i);
                    }
                }
                rps.get(neighbour).clear();
                rps.get(neighbour).addAll(union);
            }
        }

        private void exchangeSon(int i) {
            List<Integer> rpslist = rps.get(i).parallelStream().collect(Collectors.toList());
            for (Integer rpspeer : rpslist) {
                if (peers[rpspeer] == peers[i] && peers[i] == this.q) {
                    // System.err.println(son.get(i).contains(i));
                    // it is a peer that process the same query than us. keep in the fullmesh son
                    for (Integer node : son.get(i)) {
                        for (Integer remote : son.get(rpspeer)) {
                            son.get(node).add(remote);
                            son.get(remote).add(node);

                            // remove our self from our view
                            son.get(node).remove(node);
                            son.get(remote).remove(remote);
                        }
                    }
                    // for all neighbours, connect neighbours to the remote one
                    for (Integer node : son.get(i)) {
                        son.get(node).add(rpspeer);
                        son.get(rpspeer).add(node);
                        // remove our self from our view
                        son.get(node).remove(node);
                        son.get(rpspeer).remove(rpspeer);
                    }
                    // for all neighbours of the remote peer, connect them to us
                    for (Integer remote : son.get(rpspeer)) {
                        son.get(remote).add(i);
                        son.get(i).add(remote);
                        // remove our self from our view
                        son.get(i).remove(i);
                        son.get(remote).remove(remote);
                    }
                    son.get(rpspeer).add(i);
                    son.get(i).add(rpspeer);
                    // System.err.println("found a same peer" + son.get(i).size() + "_" + son.get(rpspeer).size());
                }
            }
        }
    }
}
