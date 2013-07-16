package br.ufpa.adtn.android.util;

import android.util.Log;
import br.ufpa.adtn.util.Logger.LogHandler;
import br.ufpa.adtn.util.Logger.Priority;

public class AndroidLogHandler extends LogHandler {

	@Override
	public void println(Priority priority, String tag, String message) {
		Log.println(priority.ordinal() + 2, tag, String.valueOf(message));
	}
}
