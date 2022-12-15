package net.judah.controllers;

import net.judah.JudahZone;
import net.judah.gui.knobs.KnobPanel;

public enum KnobMode {
	Midi, 
	Track, 
	Synth,
	LFO,
	Kits,
	Samples;
	
	public static KnobPanel getView(KnobMode mode) {
		switch (mode) {
			case Midi: return JudahZone.getMidiGui();
//			case Kit: return JudahZone.getDrumMachine().getKnobPanel();
//			case Track: return JudahZone.getSeq().getKnobPanel(); 
//			case LFO: return JudahZone.getFxRack().getChannel().getLfoKnobs(); 
//			case Synth: return SynthEngines.getKnobPanel(); 
//			case Sample: return JudahZone.getSampler().getKnobPanel(); 
		}
		return null;

	}
	
}