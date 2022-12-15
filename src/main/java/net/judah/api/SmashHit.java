package net.judah.api;

import static net.judah.JudahZone.*;

import net.judah.JudahZone;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.fluid.FluidSynth;
import net.judah.gui.MainFrame;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.mixer.MidiInstrument;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.seq.Situation;
import net.judah.seq.Snippet;
import net.judah.seq.TrackList;
import net.judah.synth.JudahSynth;
import net.judah.util.RTLogger;

public abstract class SmashHit implements TimeListener {
	
	protected final JudahClock clock = getClock();
	protected final Seq seq = getSeq();
	protected final TrackList beats = seq.getDrumTracks();
	protected final MidiTrack drum1 = beats.get(0);
	protected final MidiTrack drum2 = beats.get(1);
	protected final MidiTrack hats = beats.get(2);
	protected final MidiTrack fills = beats.get(3);
	protected final TrackList notes = seq.getSynthTracks();
	protected final MidiTrack lead1 = notes.get(0);
	protected final MidiTrack lead2 = notes.get(1);
	protected final MidiTrack bass = notes.get(2);
	protected final MidiTrack chords = notes.get(3);
	protected final Looper looper = getLooper();
	protected final LineIn guitar = getGuitar();
	protected final LineIn mic = getMic();
	protected final MidiInstrument crave = getCrave();
	protected final FluidSynth fluid = getFluid();
	protected final JudahSynth synth1 = getSynth1();
	protected final JudahSynth synth2 = getSynth2();
	protected final DrumMachine drumMachine = getDrumMachine();
	protected final DrumKit kit1 = drumMachine.getDrum1();
	protected final DrumKit kit2 = drumMachine.getDrum2();
	protected final DrumKit hatKit = drumMachine.getHats();
	protected final DrumKit fillsKit = drumMachine.getFills();
	protected final MainFrame frame = getFrame();
	
	public void startup() { 
	}
	
	public void teardown() { }

	@Override
	public final String toString() {
		return this.getClass().getSimpleName();
	}

//	/** if different */
//	protected void setFile(Track t, File f) {
//		if (false == f.equals(t.getFile()))
//			t.setFile(f);
//	}

	protected void resetChannels() {
		for (Channel ch : JudahZone.getMixer().getChannels())
			ch.reset();
	}

	public void cycle(MidiTrack t) { 
		RTLogger.log(this, "Empty cycle() on " + t);
	}
	
	public void populate(Snippet s, MidiTrack t, Situation state) {
		
	}
	
	@Override
	public void update(Notification.Property prop, Object value) { }
	
}
