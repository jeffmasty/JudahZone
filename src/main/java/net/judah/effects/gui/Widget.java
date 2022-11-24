package net.judah.effects.gui;

import java.util.ArrayList;

public interface Widget {

	public void increment(boolean up);

	public int getIdx();
	
	public ArrayList<String> getList();
	
}
