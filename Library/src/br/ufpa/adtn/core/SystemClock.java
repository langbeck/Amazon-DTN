package br.ufpa.adtn.core;


public final class SystemClock {
	private static Hooker HOOKER = null;

	public static long millis() {
		if (HOOKER != null)
			return HOOKER.millis();
		
		return System.currentTimeMillis();
	}

	public static long nanos() {
		if (HOOKER != null)
			return HOOKER.nanos();
		
		return System.nanoTime();
	}
	
	public static void setHooker(Hooker hooker) throws IllegalStateException {
		if (!BPAgent.isSimulated())
			throw new IllegalStateException("BPAgent is not in simulated mode.");

		if (BPAgent.getState().ordinal() >= BPAgent.State.LOADING.ordinal())
			throw new IllegalStateException("BPAgent state must be lower than LOADING");

		HOOKER = hooker;
	}
	
	
	public static interface Hooker {
		public long millis();
		public long nanos();
	}
	
	
	private SystemClock() { }
}
