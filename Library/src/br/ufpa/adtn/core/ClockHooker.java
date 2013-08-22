package br.ufpa.adtn.core;

public abstract class ClockHooker {
	public abstract long getMilliseconds();
	
	public long getNanoseconds() {
		return getMilliseconds() * 1000000L;
	}
	
	public long getSeconds() {
		return getMilliseconds() / 1000L;
	}
}
