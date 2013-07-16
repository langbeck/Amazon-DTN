package br.ufpa.adtn.android.util;

import android.os.HandlerThread;
import android.os.Looper;

public class LooperFactory {
	
	public static Looper createLooper() {
		final HandlerThread thread = new HandlerThread("-HandlerThread");
		thread.start();
		return thread.getLooper();
	}
	
	private LooperFactory() { }
}
