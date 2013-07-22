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

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.DialogInterface.OnDismissListener;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class ShareActivity extends Activity implements OnClickListener, Constants {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		final View view = getLayoutInflater().inflate(R.layout.activity_main, null);
		final Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
		((ImageView) view.findViewById(R.id.preview)).setImageURI(uri);
		
		final AlertDialog dialog = new AlertDialog.Builder(this, android.R.style.Theme_Holo_Light_Dialog)
		.setNegativeButton(android.R.string.cancel, null)
			.setPositiveButton(android.R.string.ok, this)
			.setIcon(R.drawable.ic_launcher)
			.setTitle("Send to...")
			.setView(view)
			.create();
		
		/**
		 * Finish this activity when dialog was dismissed
		 */
		dialog.setOnDismissListener(new OnDismissListener() {
			
			@Override
			public void onDismiss(DialogInterface dialog) {
				ShareActivity.this.finish();
			}
		});
		
		dialog.show();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		final Uri uri = getIntent().getParcelableExtra(Intent.EXTRA_STREAM);
		Intent intent = new Intent(this, BPAService.class);
		intent.putExtra(EXTRA_DESTINATION, "dtn://google.com");
		intent.putExtra(EXTRA_LIFETIME, 3600 * 2);
		intent.setAction(ACTION_SEND_BUNDLE);
		intent.setData(uri);
		
		startService(intent);
	}
}
