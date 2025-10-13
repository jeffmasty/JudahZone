package net.judah.util;

public class Debounce {

	/** default double click duration */
	public static final long DOUBLE_CLICK = 400;

	private final long duration;
	private long lastPress;
	private Object tapTarget;

	public Debounce(long millis) {
		duration = millis;
	}

	public Debounce() {
		this(DOUBLE_CLICK);
	}

	public boolean doubleTap() {
		return doubleTap(Debounce.this);
	}

	/** @return true on double tap */
	public boolean doubleTap(Object o) {

		long now = System.currentTimeMillis();

		if (tapTarget == o && now - lastPress < duration) {
			lastPress = now;
			return true;
		}
		lastPress = now;
		tapTarget = o;
		return false;
	}

}
