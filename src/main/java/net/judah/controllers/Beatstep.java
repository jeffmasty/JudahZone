package net.judah.controllers;

import static net.judah.JudahZone.*;

import javax.sound.midi.ShortMessage;

import net.judah.JudahZone;
import net.judah.fx.Fader;
import net.judah.fx.LFO.Target;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.knobs.KnobMode;
import net.judah.looper.Loop;
import net.judah.midi.Midi;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;

public class Beatstep implements Controller {
	
	public static final String NAME = "Arturia BeatStep"; // ALSA prefix

	private static final int TEMPO_KNOB = 7; // CC
	private static final int[] KNOBS = new int[] {
			10, 74, 71, 76, 77, 93, 73, 75, 
			114, 18, 19, 16, 17, 91, 79, 72 };

	private static final int GUITAR = 44;
	private static final int LOOPA = 36;
	private static final int MAINS = 51;
	private static final int KITS = 40;
	private static final int RECORD = 41;
	private static final int FX = 42;
	private static final int FADER = 43;
	
	private long tempoLookedUp;
	private int tempo;
	private final MultiSelect channels = new MultiSelect();
	
	@Override
	public boolean midiProcessed(Midi midi) {
		
		if (midi.getChannel() != 0) // ignore sequencer
			return true;

		int data1 = midi.getData1();
		if (Midi.isNote(midi)) { // Pads
			// GUITAR, 45(mic), 46(drums), 47(synth1), 48(synth1), 49(bass), 50(fluid), MAINS,
			// LOOPA, 37(loopB), 38(loopC),  LOOPD,     KITS,       RECORD,   FX,       FADER 
			if (data1 == KITS) { 
				if (!Midi.isNoteOn(midi)) return true;
				if (MainFrame.getKnobMode() != KnobMode.Kits)
					MainFrame.setFocus(KnobMode.Kits);
				else 
					getDrumMachine().increment();
			}
			else if (data1 == RECORD) { 
				if (!Midi.isNoteOn(midi)) return true;
				for (Channel current : getFxRack().getSelected()) 
					if (current instanceof LineIn) 
						((LineIn)current).setMuteRecord(!((LineIn)current).isMuteRecord());
					else 
						current.setOnMute(!current.isOnMute());
			}
			else if (data1 == FX) { 
				if (!Midi.isNoteOn(midi)) return true;
				for (Channel current : getFxRack().getSelected()) 
					current.setPresetActive(!current.isPresetActive());
			}
			else if (data1 == FADER) { 
				if (!Midi.isNoteOn(midi)) return true;
				for (Channel ch : getFxRack().getSelected()) {
					if (ch.isOnMute() || ch.getGain().getGain() <= 0.05f) {
						ch.setOnMute(false);
						Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, 0, 51));
					}
					else 
						Fader.execute(new Fader(ch, Target.Gain, Fader.DEFAULT_FADE, ch.getVolume(), 0));
				}
			}
			else if (data1 >= LOOPA && data1 <= MAINS) {
				multiSelect(midi.getCommand(), data1);
			}
			return true;
		}		
		
		if (Midi.isCC(midi)) {
			int data2 = midi.getData2();
			if (data1 == TEMPO_KNOB) {
				if (System.currentTimeMillis() - tempoLookedUp > 5000) // stale
					tempo = (int)JudahZone.getClock().getTempo();
				tempo += (data2 == 127 ? -2 : 2);
				JudahZone.getClock().writeTempo(tempo);
				tempoLookedUp = System.currentTimeMillis();
				return true;
			}

			for (int i = 0; i < KNOBS.length; i++)
				if (KNOBS[i] == data1) {
					for (Channel ch : getFxRack().getSelected()) {
						ch.getGui().knob(i, data2 < 64);
					}
					return true;
				}
		}
		else if (midi.getCommand() == 240 && data1 == 127) { // start/stop btns to clock
			if (getClock().isActive()) {
				for (Loop l : getLooper())
					if (l.hasRecording())
						l.setOnMute(true);
				getClock().end();
			}
			else {
				getClock().begin();
				getLooper().head();
			}
		}
		return true;
	}
	
	private void multiSelect(int command, int data1) {
		Channel ch = channel(data1);
		if (ch == null)
			return;
		if (command == ShortMessage.NOTE_OFF) {
			channels.remove(ch);
		}
		else {
			channels.add(ch);
			MainFrame.setFocus(channels.size() == 1 ? ch : channels);
		}
	}
	

	private Channel channel(int data1) {
		if (data1 < LOOPA || data1 > MAINS)
			return null;
		else {
			if (data1 == MAINS)
				return getMains();
			else if (data1 >= GUITAR)
				return getInstruments().get(data1 - GUITAR);
			else 
				return getLooper().get(data1 - LOOPA);
		}
	}
	
}
