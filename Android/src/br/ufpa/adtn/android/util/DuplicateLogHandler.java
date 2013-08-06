package br.ufpa.adtn.android.util;

import java.io.PrintStream;
import java.util.Date;

import br.ufpa.adtn.util.Logger.LogHandler;
import br.ufpa.adtn.util.Logger.Priority;

public class DuplicateLogHandler extends LogHandler {
	private final LogHandler handler;
	private final PrintStream out;
	
	public DuplicateLogHandler(LogHandler handler, PrintStream out) {
		this.handler = handler;
		this.out = out;
	}

	@Override
	public void println(Priority priority, String tag, String message) {
		handler.println(priority, tag, message);
		
		out.printf(
				"[%30s] [%8s] %25s: %s\n",
				new Date(),
				priority,
				tag,
				message
		);
	}
	
	@Override
	public void flush() {
		handler.flush();
		out.flush();
	}
}
