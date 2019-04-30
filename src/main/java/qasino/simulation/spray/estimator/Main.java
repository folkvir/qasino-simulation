package qasino.simulation.spray.estimator;

import java.util.Random;

public class Main{
	
    public static void testInstance(int n) {
    	int viewsize = (int)Math.ceil(Math.log(n));
    	System.out.println(viewsize);
    	Random rand = new Random();
    	Instance est[] = new Instance[n];
    	for(int i = 0; i<n; i++) {
    		est[i] = null;
    	}
		est[0] = new Instance(0, 0, 100);
    	
        for (int i = 0; i < 100; i++) {
        	System.out.println(i);
        	for(int j = 0; j<n; j++) {
        		int x = rand.nextInt(n);
        		est[j] = Instance.merge(est[j], est[x]);
        		est[x] = est[j];
            	if(j<5)
                System.out.println(est[j]);
        	}

       }
        
    }
    public static void testSizeEstimator(int n) {
    	int viewsize = (int)Math.ceil(Math.log(n));
    	System.out.println(viewsize);
    	Random rand = new Random();
    	SizeEstimator est[] = new SizeEstimator[n];
    	for(int i = 0; i<n; i++) {
    		est[i] = new SizeEstimator(i, rand);
    	}

        for (int i = 0; i < 1000; i++) {
        	System.out.println();
        	for(int j = 0; j<n; j++) {
        		// !!!!!!do not pick yourself!!!!!
				int pick = j;
				while(pick == j) {
					pick = rand.nextInt(n);
				}
        		SizeEstimator s2 = est[pick];
            	if(j==0)
                    System.out.println(est[j] + "  +  " + s2);
        		est[j].compute(viewsize, s2);
            	if(j==0)
            		System.out.println(est[j] + "  -  " + s2);
        	}
       }

    //	for(int j = 0; j<n; j++) {
    //		System.out.println(est[j]);
    //	}
        
    }

    public static void main(String[] args) {
    	testSizeEstimator(197);
    }
}