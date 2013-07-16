package br.ufpa.adtn.android.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public final class GetProp {
	
	public static String get(String prop) {
		try {
			final Process proc = Runtime.getRuntime().exec(new String[] {
					"/system/bin/getprop",
					prop
			});
			
			final BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			final String line = reader.readLine();
			reader.close();
			
			return line;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private GetProp() { }
}
