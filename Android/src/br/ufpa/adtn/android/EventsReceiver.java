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
