package br.ufpa.adtn.android;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class EventsReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		final String action = intent.getAction();
		Log.i("EVENTS", action);
		
		if (!action.equals(Intent.ACTION_USER_PRESENT) &&
			!action.equals(Intent.ACTION_BOOT_COMPLETED) &&
			!action.equals(Intent.ACTION_MY_PACKAGE_REPLACED)
		) return;
		
		((AlarmManager) context.getSystemService(Service.ALARM_SERVICE)).setRepeating(
				AlarmManager.RTC_WAKEUP,
				System.currentTimeMillis() + 3000,
				60000,
				PendingIntent.getService(
						context,
						0,
						new Intent(
								context,
								BPAService.class
						),
						0x3F
				)
		);
	}
}
