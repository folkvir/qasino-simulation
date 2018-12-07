package snob.simulation;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class VariationNQ implements Runnable {
	private int n;
	private int q;
	private int x;
	private int sample;

	public VariationNQ(int n, int x, int sample) {
		this.n = n;
		this.x = x;
		this.q = n*x/10;
		this.sample = sample;
	}


	public void run() {
		Random r = new Random();
		int t=0;
		for(int samp = 0; samp<sample; samp++) {
			boolean seen[][] = new boolean[q][n];
			int nbseen[] = new int[q];
			int nbdone = 0;
			for(int i = 0; i<q; i++) {
				nbseen[i]=1;
				for(int j = 0; j<n; j++) {
					seen[i][j] = false;
				}
				seen[i][i] = true;
			}
			while(nbdone<q){
				for(int i = 0; i<q; i++) {
					int j = r.nextInt(n);
					if(!seen[i][j]) {
						seen[i][j] = true;
						nbseen[i]++;
						if(nbseen[i]==n) {
							nbdone++;
						}
					}
					if(j<q) {
						for(int k = 0; k<n; k++) {
							if(!seen[i][k] && seen[j][k]) {
								seen[i][k] = true;
								nbseen[i]++;
								if(nbseen[i]==n) {
									nbdone++;
								}
							}
						}
					}
				}
				t=t+1;
			}
		}

		double h = 0;
		for(int i = 1; i<=n; i++) h = h + 1./i;

		System.out.println(n + "\t" + q + "\t" + x + "\t" + 1.0*t/sample + "\t" + (n*Math.log(n) + n*0.5772)/q);
	}


	public static void main(String argv[]) {
		int nbThread = 200;
		int sample = 1000;
		ExecutorService executorService = Executors.newFixedThreadPool(nbThread);

		for(int n = 16; n <= 2000000; n*=2) {
			for(int x = 1; x < 10; x++) {
				executorService.submit(new VariationNQ(n, x, sample));
			}
		}

	}

}

