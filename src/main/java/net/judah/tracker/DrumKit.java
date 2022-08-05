package net.judah.tracker;


import java.security.InvalidParameterException;
import java.util.ArrayList;

import net.judah.util.Constants;

public class DrumKit extends ArrayList<MidiBase> {
	private final boolean isDrums;
	
	public DrumKit(boolean isDrums) {
		this.isDrums = isDrums;
		init();
	}
	
	public void init() {
		clear();
		if (isDrums)
			drums();
		else 
			melodic();
	}
	
	private void drums() {
		for (GMDrum d : GMDrum.Standard)
			add(new MidiBase(d));
	}
	
	private void melodic() {
		for (int i = 0; i < 12; i++) {
			add(new MidiBase(i));
		}
	}
	
	public float velocity(int data1) {
		for (MidiBase base : this)
			if (base.getData1() == data1)
				return base.getVelocity();
		return 0f;
	}
	
	
	public static final String HEADER = "Contract";
	
	public String toFile() {
		StringBuffer sb = new StringBuffer(HEADER).append(Constants.NL);
		for (MidiBase midi : this)
			sb.append(midi.getData1()).append(",").append(midi.getVelocity()).append(Constants.NL);
		return sb.append(HEADER).append(Constants.NL).toString();
	}
	
	public void fromFile(ArrayList<String> s) {
		clear();
		if (s.size() < 2)
			throw new InvalidParameterException(s.toString());
		for (int i = 0; i < s.size(); i++) {
			String[] line = s.get(i).split(",");
			MidiBase it = new MidiBase(Integer.parseInt(line[0]));
			if (line.length > 1)
				it.setVelocity(Float.parseFloat(line[1]));
			add(it);
		}
	}
	
}
