/**
 * Amazon-DTN - Lightweight Delay Tolerant Networking Implementation
 * Copyright (C) 2013  Dórian C. Langbeck
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package br.ufpa.adtn.routing.dlife;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.SystemClock;
import br.ufpa.adtn.util.Logger;

/**
 * 
 * @author Dórian Langbeck
 * @author Douglas Cirqueira
 */
public class SocialInformation {
	/**
	 * January 1, 2000 [UCT]
	 */
	private static final long TIME_START = 946684800916L;
	
	private static final Logger LOGGER = new Logger("SocialInformation");
	public static final float DEFAULT_DUMPING_FACTOR = 0.8f;
	
	public static long millis() {
		return SystemClock.millis() - TIME_START;
	}
	
	
	/**
	 * Instance-global neighbor instances map reference.
	 * 
	 * PS: All Neighbor's instances present in a Sample
	 * must be present here too.
	 */
	private final Map<EID, Neighbor> gNeighbors;
	
	/**
	 * Storage for all DS of a cycle.
	 */
	private final Sample ds[];
	
	/**
	 * DS duration in milliseconds.
	 */
	private final int dsdur;
	
	/**
	 * Amount of DS's per cycle.
	 */
	private final int dslen;
	
	/**
	 * DS offset when this Social cache was created for
	 * the first time.
	 * 
	 * PS: This field must be persisted, but it's immutable.
	 */
	private final int firstDS;
	
	/**
	 * Dumping factor
	 */
	private final float dFactor;
	
	/**
	 * Current DS state. This field 
	 */
	private int cds;
	
    
    public SocialInformation(int dslen, int dsdur, float dFactor) {
    	if (dsdur < 60)
    		throw new IllegalArgumentException("DS duration must be greater than 1 minute");
    	
    	if (dslen < 2)
    		throw new IllegalArgumentException("DS length must be greater than 2");
    	
    	this.gNeighbors = new HashMap<EID, Neighbor>();
    	this.ds = new Sample[dslen];
    	this.dFactor = dFactor;
    	this.dsdur = dsdur;
    	this.dslen = dslen;

    	for (int i = 0; i < dslen; i++)
    		ds[i] = new Sample(i);

    	// FIXME This must be defined just in the first time
    	this.firstDS = getCurrentDS();
    	this.cds = firstDS;
    }
    
    public int getSamplesInCycle() {
    	return dslen;
    }
    
    public int getSampleDuration() {
    	return dsdur;
    }
    
    public int getElapsedCycles() {
    	return ((cds - firstDS) / dslen) + 1;
    }
    
    public int getCurrentDS() {
		return getDS(SystemClock.millis());
	}
    
    public int getDS(long when) {
		final long cds = (when - TIME_START) / (dsdur * 1000);

		if (cds > Integer.MAX_VALUE || cds <= 0)
			throw new InternalError("Check the system time");
		
		return (int) cds;
	}
    
    public int getInternalDS() {
    	return cds;
    }
	
	public float getWeight(EID eid) {
		return ds[cds % dslen].getNeighborTECD(eid);
	}

    public float getTECDi() {
        return ds[cds % dslen].tecdi;
    }
    
    public void setNeighborTECDi(EID eid, float value) {
    	ds[cds % dslen].updateTCDI(eid, value);
    }
    
    public void updateTCT(EID eid, float seconds) {
    	ds[cds % dslen].updateTCT(eid, seconds);
    }
    
    public NeighborWeight[] getSampleWeights() {
        final int cds = this.cds % dslen;
        final Collection<Neighbor> neighbors = ds[cds].neighbors;
        final NeighborWeight[] weights = new NeighborWeight[neighbors.size()];
        
        int i = 0;
    	for (Neighbor n : ds[cds].neighbors) {
        	weights[i++] = new NeighborWeight(n.eid, n.tecd[cds]);
        	
        	/**
        	 * TODO Extra debug check
        	 * 
        	 * If this case never occurs, we can place a NeighborWeight inside
        	 * every Neighbor and publish him without problems.
        	 */
        	if (cds != n.cs)
        		LOGGER.w("Inconsistency detected");
    	}
        
    	return weights;
    }
    
    public void update() {
    	final int cur = getCurrentDS();
    	LOGGER.v(String.format(
    			"Updating from %d (%d) to %d (%d)",
    			cds % dslen,
    			cds,
    			cur % dslen,
    			cur
		));
    	
    	while (cds < cur) {
        	// Notifies the old DS of your termination
        	ds[cds % dslen].onLeave();
        	
        	// Increment DS
        	cds++;
        	
        	// Notifies the new DS from its beginning
        	ds[cds % dslen].onEnter();
    	}
    }
    
    private Neighbor lookupByNeighbor(EID eid) {
    	Neighbor neighbor = gNeighbors.get(eid);
    	if (neighbor == null) {
    		neighbor = new Neighbor(eid);
        	gNeighbors.put(eid, neighbor);
    	}
    	
    	return neighbor;
    }
    
    
    private class Sample {
		private final Collection<Neighbor> neighbors;
		private final int dsid;
		private float tecdi;
        
        public Sample(int dsid) {
        	this.neighbors = new HashSet<Neighbor>();
        	this.tecdi = 1 - dFactor;
        	this.dsid = dsid;
        }
        
        public void onEnter() {
            for (Neighbor neighbor : neighbors) {
                if (neighbor.tct != 0) {
                	LOGGER.w(String.format(
                			"A neighbor with TCT != 0 was found during DS %d startup",
                			dsid
        			));
                	
                	neighbor.tct = 0;
                }
                
                neighbor.cs = dsid;
            }
        }
        
        public void onLeave() {
        	/*
        	 * Update average duration and TECD values for all neighbors and
        	 * update TECDi for current ending DS.
        	 */
            final int cds = SocialInformation.this.cds % dslen;
            final int n = neighbors.size();
            float tecdi = 0;
            for (Neighbor neighbor : neighbors) {
                neighbor.onUpdate();
                tecdi += (neighbor.tecd[cds] * neighbor.tecdi[cds]) / n;
            }
            
            this.tecdi = (tecdi * dFactor) + (1 - dFactor);
        }
        
        public void updateTCT(EID eid, float seconds) {
        	final Neighbor neighbor = lookupByNeighbor(eid);
        	if (!neighbors.contains(neighbor)) {
        		LOGGER.d(String.format(
        				"UpdateTCT: DS %d does not have neighbor \"%s\"",
        				dsid, eid
				));
            	neighbors.add(neighbor);
        	}
        	
        	if (neighbor.cs != dsid) {
	        	LOGGER.d(String.format(
	    				"UpdateTCT: Moving neighbor \"%s\": SRC(%d) DST(%d)",
	    				eid, neighbor.cs, dsid
				));
	        	
	        	neighbor.cs = dsid;
        	}

        	final float tct = neighbor.tct;
        	final float nval = tct + seconds;
        	LOGGER.d(String.format(
        			"Updating TCT of neighbor %s from %.3f to %.3f in sample %d",
        			neighbor.eid, tct, nval, dsid
			));
        	
        	neighbor.tct = nval;
        }
        
        public void updateTCDI(EID eid, float value) {
        	final Neighbor neighbor = lookupByNeighbor(eid);
        	if (!neighbors.contains(neighbor)) {
        		LOGGER.d(String.format(
        				"UpdateTECDI: DS %d does not have neighbor \"%s\"",
        				dsid, eid
				));
            	neighbors.add(neighbor);
        	}
        	
        	if (neighbor.cs != dsid) {
	        	LOGGER.d(String.format(
	    				"UpdateTECDI: Moving neighbor \"%s\": SRC(%d) DST(%d)",
	    				eid, neighbor.cs, dsid
				));
	        	
	        	neighbor.cs = dsid;
        	}
        	
        	if (neighbor.tecdi[dsid] == value)
        		return;
        	
        	LOGGER.d(String.format(
        			"Replacing TECDi of neighbor %s from %f to %f in sample %d",
        			neighbor.eid,
        			neighbor.tecdi[dsid],
        			value,
        			dsid
			));
        	
        	neighbor.tecdi[dsid] = value;
        }
        
        public float getNeighborTECD(EID eid) {
        	final Neighbor neighbor = gNeighbors.get(eid);
        	if (neighbor == null)
        		return Float.NaN;
        	
        	return neighbor.tecd[cds % dslen];
        }
    }
    
    
    protected class Neighbor {
        private final float tecdi[];
        private final float tecd[];
        private final float ad[];
        private final EID eid;
        private float tct;
        
        /**
         * For debug purpose only.
         * 
         * TODO Remove it in production stage
         */
        private int cs = -1;
        
        
        public Neighbor(EID eid) {
        	this.tecdi = new float[dslen];
        	this.tecd = new float[dslen];
        	this.ad = new float[dslen];
        	this.eid = eid;
        	this.tct = 0;
        }
        
        public void onUpdate() {
        	// Update AD
            final int cds = SocialInformation.this.cds % dslen;
        	final int j = getElapsedCycles();
        	ad[cds] = (tct + (j - 1) * ad[cds]) / j;
        	
        	// Update TECD using the updated AD value
    		float tecd = 0;
            for (int k = cds, e = cds + dslen; k < e; k++)
                tecd += ((float) dslen / (dslen + k - cds)) * ad[k % dslen];
            
            this.tecd[cds] = tecd;
            this.tct = 0;
        }
    }
    
    
    public static class NeighborWeight {
    	private final float weight;
    	private final EID eid;
    	
    	public NeighborWeight(EID eid, float weight) {
    		this.weight = weight;
    		this.eid = eid;
    	}

		public float getWeight() {
			return weight;
		}

		public EID getEID() {
			return eid;
		}
    }
}
