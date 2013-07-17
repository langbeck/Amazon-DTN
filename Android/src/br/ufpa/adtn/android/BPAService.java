package br.ufpa.adtn.android;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;
import br.ufpa.adtn.android.util.AndroidLogHandler;
import br.ufpa.adtn.android.util.GetProp;
import br.ufpa.adtn.bundle.Bundle;
import br.ufpa.adtn.bundle.BundleBuilder;
import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.EID;
import br.ufpa.adtn.core.ParsingException;
import br.ufpa.adtn.core.configuration.SimulationConfiguration;
import br.ufpa.adtn.util.Logger;

public class BPAService extends IntentService implements Constants {
	private static final Logger LOGGER = new Logger("BPAService");
	
	public BPAService() {
		super("BPAService");
		Log.i("BPAService", "New");
	}
	
	@Override
	public void onDestroy() {
		Log.i("BPAService", "onDestroy");
		super.onDestroy();
	}
	
	@Override
	public void onCreate() {
		Log.i("BPAService", "onCreate");
		super.onCreate();
		
		if (BPAgent.getState() == BPAgent.State.STARTED)
			return;
		
		if (true)
			return;
		
		try {
			final Context context = getApplicationContext();
			
			/**
			 * Implement this feature later
			 */
//			final AccountManager am = AccountManager.get(context);
//			am.getAccountsByType()
			
			/**
			 * Basic setup
			 */
			Logger.setLogHandler(new AndroidLogHandler());
			BPAgent.setHostname(GetProp.get("net.hostname"));
			BPAgent.init(true);
			
			/**
			 * Load configurations
			 */
			final AssetManager assets = getAssets();
			SimulationConfiguration.load(assets.open("contact.conf"));
			BPAgent.load(
					assets.open("config.xml"),
					context
			);
			
			/**
			 * Start components
			 */
			BPAgent.startComponents();
			
			super.onCreate();
		} catch (Exception e) {
			LOGGER.e("Start failure", e);
		}
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		Log.i("BPAService", "onHandleIntent: " + intent);
		
		if (ACTION_SEND_BUNDLE.equals(intent.getAction())) {
			onHandleSendBundle(intent);
			return;
		}
	}
	
	private void onHandleSendBundle(Intent intent) {
		final EID destination;
		try {
			final String dst = intent.getStringExtra(EXTRA_DESTINATION);
			if (dst == null) {
				LOGGER.w("Dropping SEND_BUNDLE intent. Destination not defined.");
				return;
			}
			
			destination = EID.get(dst);
		} catch (ParsingException e) {
			LOGGER.w("Dropping SEND_BUNDLE intent. Destination parsing problem.");
			return;
		}
		

		final Uri dataUri = intent.getData();
		final ByteBuffer payload;
		if (dataUri == null) {
			final byte[] data = intent.getByteArrayExtra(EXTRA_PAYLOAD);
			if (data == null) {
				LOGGER.w("Dropping SEND_BUNDLE intent. No payload defined.");
				return;
			}
			
			payload = ByteBuffer.wrap(data);
		} else {
			try {
				final InputStream ds = getContentResolver().openInputStream(dataUri);
				final ByteArrayOutputStream buffer = new ByteArrayOutputStream(ds.available());
				final byte[] buf = new byte[0x1000];
				for (int readed; (readed = ds.read(buf)) != -1;)
					buffer.write(buf, 0, readed);
				
				payload = ByteBuffer.wrap(buffer.toByteArray());
			} catch (IOException e) {
				LOGGER.w("Dropping SEND_BUNDLE intent. Can not read data content.");
				return;
			}
		}
		
		final long lifetime = intent.getLongExtra(EXTRA_LIFETIME, 3600);
		final Bundle bundle = new BundleBuilder()
			.setDestination(destination)
			.setLifetime(lifetime)
			.setPayload(payload)
			.setSource(EID.NULL)
			.build();
		
		BPAgent.addBundle(bundle);
	}
}
