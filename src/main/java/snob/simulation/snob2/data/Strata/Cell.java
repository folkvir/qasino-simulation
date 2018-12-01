package snob.simulation.snob2.data.Strata;

import java.nio.charset.StandardCharsets;

public class Cell {
    private int count;
    private int idSum;
    private int hashSum;

    public void add(int id, int idHashValue) {
        idSum ^= id;
        hashSum ^= idHashValue;
        count++;
    }

    public void delete(int id, int idHashValue) {
        idSum ^= id;
        hashSum ^= idHashValue;
        count--;
    }

    public boolean isPure() {
        return (count == -1 || count == 1)
                && (IBF.genIdHash(String.valueOf(idSum).getBytes(StandardCharsets.UTF_8)) == hashSum);
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getIdSum() {
        return idSum;
    }

    public void setIdSum(int idSum) {
        this.idSum = idSum;
    }

    public int getHashSum() {
        return hashSum;
    }

    public void setHashSum(int hashSum) {
        this.hashSum = hashSum;
    }
}
