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
package br.ufpa.adtn.core.registration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class MulticastRegistry<T> implements Registry<T> {
	private final Collection<Registry<T>> destinations;
	
	public MulticastRegistry(Registry<T> ... regs) {
		this.destinations = new ArrayList<Registry<T>>(regs.length);
		Collections.addAll(destinations, regs);
	}

	@Override
	public void delivery(T data) {
		for (Registry<T> reg : destinations)
			reg.delivery(data);
	}
}
