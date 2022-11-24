package net.judah.songs;

import static net.judah.JudahZone.*;

import java.io.File;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.Notification;
import net.judah.api.TimeListener;
import net.judah.drumz.DrumKit;
import net.judah.drumz.DrumMachine;
import net.judah.fluid.FluidSynth;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.mixer.LineIn;
import net.judah.mixer.MidiInstrument;
import net.judah.synth.JudahSynth;
import net.judah.tracker.DrumTrack;
import net.judah.tracker.DrumTracks;
import net.judah.tracker.PianoTrack;
import net.judah.tracker.SynthTracks;
import net.judah.tracker.Track;
import net.judah.util.RTLogger;

public abstract class SmashHit implements TimeListener {
	
	protected final JudahClock clock = getClock();
	protected final DrumTracks beats = getBeats();
	protected final DrumTrack drum1 = beats.getDrum1();
	protected final DrumTrack drum2 = beats.getDrum2();
	protected final DrumTrack hats = beats.getHats();
	protected final DrumTrack fills = beats.getFills();
	protected final SynthTracks notes = getNotes();
	protected final PianoTrack lead1 = notes.getLead1();
	protected final PianoTrack lead2 = notes.getLead2();
	protected final PianoTrack bass = notes.getBass();
	protected final PianoTrack chords = notes.getGm1();
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

	/** if different */
	protected void setFile(Track t, File f) {
		if (false == f.equals(t.getFile()))
			t.setFile(f);
	}

	protected void resetChannels() {
		for (Channel ch : JudahZone.getMixer().getChannels())
			ch.reset();
	}

	public void cycle(Track t) { 
		RTLogger.log(this, "Empty cycle() on " + t);
	}
	

	@Override
	public void update(Notification.Property prop, Object value) { }
	
}
