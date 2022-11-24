package net.judah.effects.gui;

import java.util.ArrayList;

import lombok.Getter;
import net.judah.effects.CutFilter;
import net.judah.mixer.Channel;

public class FilterHandler implements Widget {

	CutFilter a;
	CutFilter b;
	
	private final Channel ch;

	@Getter private final ArrayList<String> list = new ArrayList<>();

	public FilterHandler(Channel ch) {
		this.ch = ch;
		list.add("pArTy/Hi");
		list.add("pArTy/Lo");
		list.add("Hi/Lo");
	}

	@Override
	public void increment(boolean up) {
		
		
	}

	@Override
	public int getIdx() {
		return 0;
	}
	
}
