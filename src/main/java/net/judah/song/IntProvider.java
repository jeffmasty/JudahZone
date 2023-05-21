package net.judah.song;

import java.util.ArrayList;

import lombok.Getter;

public class IntProvider implements Cmdr {

	private static ArrayList<IntProvider> cache = new ArrayList<>();
	
	private int start, stop, step;
	@Getter private final String[] keys;
	
	IntProvider(int start, int stop, int step) {
		this.start = start;
		this.stop = stop;
		this.step = step;
		keys = new String[(stop - start) / step];
		for (int i = 0; i < keys.length; i++)
			keys[i] = "" + (start + i * step);
		cache.add(this);
	}
	
	public static IntProvider instance() {
		return instance(0, 100, 1);
	}
	
	public static IntProvider instance(int start, int stop, int step) {
		for (IntProvider x : cache)
			if (x.start == start && x.stop == stop && x.step == step)
				return x;
		return new IntProvider(start, stop, step);
	}

	@Override
	public String lookup(int value) {
		return "" + value;
	}

	@Override
	public Long resolve(String key) {
		return Long.parseLong(key);
	}

	@Override
	public void execute(Param p) {
		// no-op
	}
	
}
