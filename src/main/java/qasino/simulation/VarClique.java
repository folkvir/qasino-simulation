package qasino.simulation;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VarClique implements Runnable {

    private int n;
    private int q;
    private int sample;

    public VarClique(int n, int q, int sample) {
        this.n = n;
        this.q = q;
        this.sample = sample;
    }

    public static void main(String[] argv) {
        ExecutorService executorService = Executors.newFixedThreadPool(10);
        int sample = 1000;
        int n = 1000;
        for (int q = 1; q <= n; q++) {
            executorService.submit(new VarClique(n, q, sample));
        }
    }

    public void run() {
        Random r = new Random();

        int t = 0;

        for (int samp = 0; samp < sample; samp++) {

            boolean[][] seen = new boolean[q][n];
            int[] clique = new int[q];
            int[] nbseen = new int[q];

            for (int i = 0; i < q; i++) {
                clique[i] = i;
                nbseen[i] = 1;
                for (int j = 0; j < n; j++) {
                    seen[i][j] = false;
                }
                seen[i][i] = true;
            }
            while (nbseen[clique[0]] < n) {
                t++;
                for (int i = 0; i < q; i++) {
                    int j = r.nextInt(n);
                    if (!seen[clique[i]][j]) {
                        seen[clique[i]][j] = true;
                        nbseen[clique[i]]++;
                    }
                    if (j < q && clique[i] != clique[j]) {
                        for (int k = 0; k < n; k++) {
                            if (seen[clique[i]][k] && !seen[clique[j]][k]) {
                                seen[clique[j]][k] = true;
                                nbseen[clique[j]]++;
                            }
                        }

                        for (int k = 0; k < q; k++) {
                            if (clique[k] == clique[i]) {
                                clique[k] = clique[j];
                            }
                        }
                    }
                }
            }
        }

        System.out.println(n + "\t" + q + "\t" + 1.0 * t / sample);
    }

}

