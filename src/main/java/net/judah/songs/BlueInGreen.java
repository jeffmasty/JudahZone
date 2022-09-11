package net.judah.songs;

import static net.judah.api.Key.*;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.looper.Looper;
import net.judah.midi.JudahMidi;
import net.judah.settings.Channels;
import net.judah.tracker.Track;
import net.judah.tracker.Track.Cue;
import net.judah.tracker.Tracker;
import net.judah.tracker.Transpose;
import net.judah.util.RTLogger;

public class BlueInGreen extends SmashHit {

	final int OCTAVE = 12;
	int count;
	
	@Override
	public void startup(Tracker t, Looper loops, Channels ch) {
		super.startup(t, loops, ch);
		t.getClock().setLength(10);
		
		t.getDrum1().setFile("Bossa1");
		t.getDrum2().setFile("HiHats");
		t.getDrum3().setFile("AllMyLovin");
		
		Track harp = t.getLead1();
		harp.setFile("BlueInGreen");
		
		loops.getLoopB().setArmed(true);
		ch.getGuitar().setPreset(JudahZone.getPresets().byName("Freeverb"));
		ch.getGuitar().setPresetActive(true);
		ch.getGuitar().getLatchEfx().latch(loops.getLoopA());
		
		Track bass = t.getBass();
		bass.setFile("str8eight");

		MainFrame.get().sheetMusic("BlueInGreen.png");
		ch.getFluid().setMuteRecord(true);
		ch.getCrave().setMuteRecord(true);

		bass.setActive(false);
		bass.getCycle().setCustom(this);
		bass.setCue(Cue.Loop);
		bass.setLatch(true);
		Transpose.setAmount(Bb.ordinal() - OCTAVE);
		Transpose.setActive(true);
		bass.setActive(true);
		
		
		harp.setMidiOut(JudahMidi.getInstance().getFluidOut());
		harp.setCurrent(harp.get(harp.size() - 2));
		harp.setCue(Cue.Bar);
		harp.setActive(true);
		t.getDrum2().setCurrent(t.getDrum2().get(4)); 
		t.getDrum1().setActive(true);
		t.getDrum2().setActive(true);
		t.getDrum3().setActive(true);

		count = -1;
		RTLogger.log(this, "2 bar intro");
	}

	// set bass root transpose note
	@Override public void cycle(Track t) {
		count++;
		switch (count) {
		case 0: Transpose.setAmount(Bb.ordinal() - 2 * OCTAVE); break; // Bbmaj7 
		case 1: Transpose.setAmount(A.ordinal() - 2 * OCTAVE); break; // A7b9
		case 2: Transpose.setAmount(D.ordinal() - OCTAVE); break; // D-7, Db7#11 
		case 3: Transpose.setAmount(C.ordinal() - OCTAVE); break; // C-7, F7b9 
		case 4: Transpose.setAmount(Bb.ordinal() - 2 * OCTAVE); break; // Bbmaj7
		case 5: Transpose.setAmount(A.ordinal() - 2 * OCTAVE); break; // A7b9
		case 6: Transpose.setAmount(D.ordinal() - OCTAVE); break; // D-7
		case 7: Transpose.setAmount(E.ordinal() - OCTAVE); break; // E7alt
		case 8: Transpose.setAmount(A.ordinal() - OCTAVE); break; // A-7
		case 9: Transpose.setAmount(D.ordinal() - OCTAVE); break; // D-6
		default:
			count = -1;
		}
	}
	
	@Override
	public void teardown() {
		Transpose.setActive(false);
		Transpose.setAmount(0);
		t.getBass().setLatch(false);
		t.getBass().getCycle().setCustom(null);
		t.getLead1().setActive(false);
	}
	
}
