package br.ufpa.adtn.android;

import java.io.InputStreamReader;
import java.io.PrintStream;

import android.content.res.AssetManager;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.Toast;
import br.ufpa.adtn.android.util.AndroidLogHandler;
import br.ufpa.adtn.android.util.DuplicateLogHandler;
import br.ufpa.adtn.android.util.GetProp;
import br.ufpa.adtn.core.BPAgent;
import br.ufpa.adtn.core.configuration.SimulationConfiguration;
import br.ufpa.adtn.util.Logger;

public class HomeActivity extends FragmentActivity {
	private final static Logger LOGGER = new Logger("Amazon-DTN Home");
	private boolean running;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_home);
		
		final ImageButton btn = (ImageButton) findViewById(R.id.start_stop_button);
		btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if (running) {
					if (doStop()) {
						btn.setImageResource(R.drawable.ic_action_run);
						running = false;
					}
				} else {
					if (doStart()) {
						btn.setImageResource(R.drawable.ic_action_stop);
						running = true;
					}
				}
			}
		});
		
		final PagerAdapter adapter = new PagerAdapter(getSupportFragmentManager());
		adapter.add(new TrafficSummaryFragment());
		adapter.add(new DLifeSummaryFragment());
		
		final ViewPager pager = (ViewPager) findViewById(R.id.pager);
		pager.setAdapter(adapter);
		running = false;
	}
	
	@Override
	protected void onDestroy() {
		if (running)
			doStop();
		
		super.onDestroy();
	}
	
	@Override
	public void onBackPressed() {
		if (running) {
			Toast.makeText(
					this,
					"You must stop BPA before leave application.",
					Toast.LENGTH_SHORT
			).show();
		} else {
			super.onDestroy();
		}
	}
	
	private boolean doStart() {
		try {
			/**
			 * Basic setup
			 */
			Logger.setLogHandler(new DuplicateLogHandler(
					new AndroidLogHandler(),
					new PrintStream(openFileOutput(
							"last.log",
							MODE_PRIVATE
					))
			));
			BPAgent.setHostname(GetProp.get("net.hostname"));
			
			/**
			 * Load configurations
			 */
			final AssetManager assets = getAssets();
			BPAgent.init(new SimulationConfiguration(new InputStreamReader(assets.open("contact.conf"))));
			BPAgent.load(assets.open("config.xml"), getApplicationContext());
			
			/**
			 * Start components
			 */
			BPAgent.start();
			
			return true;
		} catch (Throwable t) {
			LOGGER.e("Unexpected load failure", t);
			return false;
		}
	}
	
	private boolean doStop() {
		
		return false;
	}
}
