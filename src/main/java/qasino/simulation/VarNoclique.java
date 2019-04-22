package qasino.simulation;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VarNoclique implements Runnable {

    private int n;
    private int q;
    private int sample;

    public VarNoclique(int n, int q, int sample) {
        this.n = n;
        this.q = q;
        this.sample = sample;
    }

    public static void main(String[] argv) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        int sample = 1000;
        int n = 1000;
        for (int q = 1; q <= n; q++) {
            executorService.submit(new VarNoclique(n, q, sample));
        }
    }

    public void run() {
        Random r = new Random();
        int t = 0;
        for (int samp = 0; samp < sample; samp++) {
            boolean[][] seen = new boolean[q][n];
            int[] nbseen = new int[q];
            int nbdone = 0;
            int nbround = 0;
            for (int i = 0; i < q; i++) {
                nbseen[i] = 1;
                for (int j = 0; j < n; j++) {
                    seen[i][j] = false;
                }
                seen[i][i] = true;
            }
            while (nbdone < q) {
                nbround++;
                for (int i = 0; i < q; i++) {
                    int j = r.nextInt(n);
                    if (!seen[i][j]) {
                        seen[i][j] = true;
                        nbseen[i]++;
                        if (nbseen[i] == n) {
                            nbdone++;
                            t += nbround;
                        }
                    }
                    if (j < q) {
                        for (int k = 0; k < n; k++) {
                            if (!seen[j][k] && seen[i][k]) {
                                seen[j][k] = true;
                                nbseen[j]++;
                                if (nbseen[j] == n) {
                                    nbdone++;
                                    t += nbround;
                                }
                            } else if (!seen[i][k] && seen[j][k]) {
                                seen[i][k] = true;
                                nbseen[i]++;
                                if (nbseen[i] == n) {
                                    nbdone++;
                                    t += nbround;
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println(n + "\t" + q + "\t" + 1.0 * t / sample / q);
    }

}

