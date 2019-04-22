package qasino.simulation.snob2.data.Strata;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class StrataEstimator {
    public int L = 32;// Is U the hash range? the ith partition covers
    // 1/2^(i+1) of U
    public IBF[] ibfs = new IBF[L];

    public StrataEstimator(int l) {
        L = l;
        for (int i = 0; i < ibfs.length; i++)
            ibfs[i] = new IBF(100);// ?? how to determine the
        // approximate size of
        // the ibfs[i]?
    }

    public IBF[] encode(int[] s) {
        for (int element : s) {
            int i = trailingZeros(element);
            ibfs[i].add(element);
        }
        return ibfs;
    }

    public int decode(StrataEstimator se2) {
        IBF[] ibfs2 = se2.ibfs;
        int count = 0;
        for (int i = ibfs.length - 1; i >= -1; i--) {
            if (i < 0)
                return count * (int) Math.pow(2, i + 1);
            Cell[] subResult = ibfs[i].subtract(ibfs2[i].getCells());
            List[] decResult = ibfs[i].decode(subResult);
            if (decResult == null)
                return count * (int) Math.pow(2, i + 1);
            count += decResult[0].size() + decResult[1].size();
        }
        return count;
    }

    public int trailingZeros(int num) {
        int res = 0;
        res = Integer.numberOfTrailingZeros(IBF.genHash(String.valueOf(num).getBytes(StandardCharsets.UTF_8)));
        return res;
    }

    public IBF[] getIbfs() {
        return ibfs;
    }
}
