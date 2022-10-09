package net.judah.songs;

public class Perfidia extends SmashHit {

	@Override
	public void startup() {
		super.startup();
		frame.sheetMusic("Perfidia.png");
		clock.setLength(8);
		bass.setFile("Perfidia");
	}
	
}
