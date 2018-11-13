package snob.simulation.snob;

import peersim.core.Node;
import snob.simulation.rps.AAgingPartialView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Aging partial view class used within Snob random peer sampling protocol
 */
public class SonPartialView extends AAgingPartialView {

	// #A bounds
	private static int l;
	private static int c;

	/**
	 * Constructor of the Snob's partial view
	 * 
	 * @param c
	 *            the maximum size of the partial view
	 * @param l
	 *            the maximum size of the samples
	 */
	public SonPartialView(int c, int l) {
		super();
		SonPartialView.l = l;
		SonPartialView.c = c;
	}

	public List<Node> getSample(Snob caller, Node neighbor, boolean isInitiator) {
		ArrayList<Node> sample = new ArrayList<Node>();
		ArrayList<Node> clone = new ArrayList<Node>(this.partialView);
		ArrayList<Node> parent = new ArrayList<Node>(caller.partialView.partialView);

		if(this.partialView.size() > 0) {
			// now if add pv of the parent
            Iterator<Node> it = parent.iterator();
            while(it.hasNext()){
                Node tmp = it.next();
                if(!clone.contains(tmp)) {
                    clone.add(tmp);
                }
            }
			//now rank them among their profile
            List<Node> sorted = this.sortSample(clone, caller);

            int sampleSize = sorted.size();
            if (!isInitiator) { // called from the chosen peer
                sampleSize = Math.min(sampleSize, SonPartialView.l);
            } else { // called from the initiating peer
                sampleSize = Math.min(sampleSize - 1, SonPartialView.l - 1);
                sampleSize = Math.max(sampleSize, 0);
                sorted.remove(0);
            }

            // now keep the k best ranked
            int i = 0;
			while (i < sorted.size() && sample.size() < sampleSize) {
				int rn = i; // CommonState.r.nextInt(clone.size());
				sample.add(sorted.get(rn));
				i++;
			}
		}
		return sample;
	}

	private List<Node> sortSample(List<Node> sample, Snob caller) {
        sample.sort((Node a, Node b) -> {
            Profile pa = ((Snob) a.getProtocol(Snob.pid)).profile;
            Profile pb = ((Snob) a.getProtocol(Snob.pid)).profile;
            int scoreA = caller.profile.score(pa);
            int scoreB = caller.profile.score(pb);
            // System.err.println("SA: "+scoreA+" SB:"+scoreB);
            try {
                return Math.subtractExact(scoreB, scoreA);
            } catch(ArithmeticException e) {
                return 0;
            }
        });
        return sample;
    }

	public void mergeSample(Snob caller, Node other, List<Node> newSample,
			List<Node> oldSample, boolean isInitiator) {
		ArrayList<Node> removedPeer = new ArrayList<Node>();
		ArrayList<Integer> removedAge = new ArrayList<Integer>();

		// firstly rank the new sample among us

        // now if add pv of the parent
        Iterator<Node> it = oldSample.iterator();
        while(it.hasNext()){
            Node tmp = it.next();
            if(!newSample.contains(tmp)) {
                newSample.add(tmp);
            }
        }

        //now rank them among their profile
        List<Node> sorted = this.sortSample(newSample, caller);

		// #1 remove the sent sample
		for (Node old : sorted) {
			int index = this.getIndex(old);
			if (index >= 0) {
				removedPeer.add(this.partialView.get(index));
				removedAge.add(this.ages.get(index));
				this.partialView.remove(index);
				this.ages.remove(index);
			}
		}

		// #2 remove the chosen neighbor
		this.removeNode(other);

		// #3 insert the new sample until max is reached
		for (Node fresh : sorted) {
			if (this.partialView.size() < SonPartialView.c && !this.contains(fresh) && fresh.getID() != caller.node.getID()) {
				this.partialView.add(fresh);
				// #A look into the removing if it existed
				boolean found = false;
				int i = 0;
				while (!found && i < removedPeer.size()) {
					if (removedPeer.get(i).getID() == fresh.getID()) {
						found = true;
					} else {
						++i;
					}
				}
				// #B if it existed, keep the old age
				if (found) {
					this.ages.add((Integer) removedAge.get(i));
				} else {
					// #C otherwise, it's a brand new one
					this.ages.add(new Integer(0));
				}
			}
		}

		/*// #4 fill with old elements until the maximum size is reached
		int i = removedPeer.size() - 1;
        System.err.println(this.partialView.size());
		while (i >= 0 && this.partialView.size() < SonPartialView.c) {
			if (!this.contains(removedPeer.get(i))) {
				// #A search the insert position
				int position = this.ages.size() - 1;
				boolean found = false;
				while (!found && position >= 0) {
					if (this.ages.get(position) >= removedAge.get(i)) {
						found = true;
					} else {
						--position;
					}
				}
				// #B insert at the rightful position to maintain the order
				if (!found) {
					this.partialView.add(0, removedPeer.get(i));
					this.ages.add(0, removedAge.get(i));
				} else {
					this.ages.add(position + 1, removedAge.get(i));
					this.partialView.add(position + 1, removedPeer.get(i));
				}

			}
			--i;
		}*/
	}

	@Override
	protected Object clone() throws CloneNotSupportedException {
		SonPartialView cpv = new SonPartialView(SonPartialView.c,
				SonPartialView.l);
		cpv.partialView = new ArrayList<Node>(this.partialView);
		cpv.ages = new ArrayList<Integer>(this.ages);
		return cpv;
	}

	@Override
	public boolean addNeighbor(Node peer) {
		boolean isContaining = this.contains(peer);
		if (!isContaining) {
			this.partialView.add(peer);
			this.ages.add(new Integer(0));
		}
		return !isContaining;
	}

    @Override
    public List<Node> getSample(Node caller, Node neighbor, boolean isInitiator) {
        return null;
    }

    @Override
    public void mergeSample(Node me, Node other, List<Node> newSample, List<Node> oldSample, boolean isInitiator) {

    }
}
