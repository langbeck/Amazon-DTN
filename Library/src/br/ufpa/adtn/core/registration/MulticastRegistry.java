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
