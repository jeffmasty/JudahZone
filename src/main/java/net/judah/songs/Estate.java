package net.judah.songs;

public class Estate extends SmashHit {

	@Override
	public void startup() {
		clock.setLength(7);
		frame.sheetMusic("Estate.png");
	}
}
