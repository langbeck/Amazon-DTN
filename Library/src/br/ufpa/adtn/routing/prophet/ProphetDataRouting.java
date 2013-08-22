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
package br.ufpa.adtn.routing.prophet;

import java.util.HashMap;
import java.util.Map;

import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.SystemClock;
import br.ufpa.adtn.util.Logger;

/**
 * This class concentrates the logic of the calculations of the protocol, with 
 * all the formulas needed to obtain the delivery predictability for each
 * neighbor. It also stores a class abstraction with necessary data for 
 * identification and characteristics of a PROPHET node.
 * 
 * @author Douglas Cirqueira
 */
public class ProphetDataRouting {
	private static final Logger LOGGER = new Logger("ProphetDataRouting");
	
	private static final float P_ENCOUNTER_MAX = .7f;
	private static final float P_ENCOUNTER_FIRST = .5f;
	private static final float P_FIRST_THRESHOLD = .1f;
	private static final float ALPHA = .5f;
	private static final float BETA = .9f;
	private static final float GAMMA = .999f;
	private static final float DELTA = .01f;
	private static final long I_TYP = 5 * 60 * 1000; // 5 minutes TODO to be settled by user.
	
	private static final float K = 24 * 3600 * 1000; // 24 h * min * sec * millis
	
	private Map<EID, Neighbor> neighbors;
	
	public ProphetDataRouting() {
		this.neighbors = new HashMap<EID, Neighbor>();
	}
	
	public NeighborPredict[] getNeighborsPredicts() {
		final int len = neighbors.size();
		final NeighborPredict[] preds = new NeighborPredict[len];
		if (len == 0)
			LOGGER.d("WARNING!!! NEIGHBORS IS EMPTY!!!");
		else
			LOGGER.d("WARNING!!! NEIGHBORS HAS VALUES!!!");
			
		int i = 0;
		for (Neighbor n : neighbors.values()){
			preds[i] = new NeighborPredict(n.eid, n.p_value);
			i++;
		}
		
		return preds;
	}
	
	public float getPredict(EID eid) {
		return lookupByNeighbor(eid).p_value;
	}
	
	public void updatePredict(EID eid) {
		final Neighbor n = lookupByNeighbor(eid);
		n.p_encounterCalc();
	}
	
	public void updateTransitivity(EID eid, float p_ab, float p_bc) {
		lookupByNeighbor(eid).transitPredictCalc(p_ab, p_bc);
	}
	
	private Neighbor lookupByNeighbor(EID eid) {
		Neighbor neighbor = neighbors.get(eid);
		if (neighbor == null){
			neighbor = new Neighbor(eid);
			neighbors.put(eid, neighbor);
		}
		return neighbor;
	}
	
	private class Neighbor {
		private final EID eid;
		private float p_value = P_ENCOUNTER_FIRST;
		private float p_encounter;
		private long last_age;
		
		public Neighbor(EID eid) {
			this.last_age = SystemClock.millis();
			this.eid = eid;
		}
		
		private void p_encounterCalc() {
			final long intvl =  SystemClock.millis() - last_age;
			LOGGER.d("INTVL = " + intvl);
			
			p_encounter = P_ENCOUNTER_MAX * (intvl <= I_TYP ? ((float) intvl / I_TYP) : 1);
			predictCalc();
		}
		
		private void predictCalc() {
			ageCalc();
			LOGGER.d("p_encounter = " + p_encounter);
			this.p_value = p_value + ((1 - DELTA - p_value) * p_encounter);
			last_age = SystemClock.millis();
			
			LOGGER.d(String.format("predictCalc: updating p_value of %s to %f", eid, p_value));
		}
		
		public void transitPredictCalc(float p_ab, float p_bc) {
			ageCalc();
			this.p_value = Math.max(p_value, (p_ab * p_bc * BETA));
			last_age = SystemClock.millis();
			
			LOGGER.d(String.format("transitPredictCalc: updating p_value of %s to %f", eid, p_value));
		}
		
		private void ageCalc() {
			final float K_fact = (last_age - SystemClock.millis()) / K;
			this.p_value = (float) (p_value * Math.pow(GAMMA, K_fact));
		}
	}
	
	public static class NeighborPredict {
    	private final float predict;
    	private final EID eid;
    	
    	public NeighborPredict(EID eid, float weight) {
    		this.predict = weight;
    		this.eid = eid;
    	}

		public float getPredict() {
			return predict;
		}

		public EID getEID() {
			return eid;
		}
    }
}
