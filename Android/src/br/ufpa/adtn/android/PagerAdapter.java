package br.ufpa.adtn.android;

import java.util.ArrayList;
import java.util.List;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

public class PagerAdapter extends FragmentPagerAdapter {
	private final List<FragmentPager> fragments;
	
	public PagerAdapter(FragmentManager manager) {
		super(manager);
		
		this.fragments = new ArrayList<FragmentPager>();
	}
	@Override
	public CharSequence getPageTitle(int position) {
		return fragments.get(position).getTitle();
	}

	@Override
	public Fragment getItem(int position) {
		return fragments.get(position);
	}
	
	public void add(FragmentPager pager) {
		this.fragments.add(pager);
	}

	@Override
	public int getCount() {
		return fragments.size();
	}
}
