package net.judah.seq;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.MidiReceiver;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.player.MidiPlayer;
import net.judah.midi.JudahClock;
import net.judah.midi.Midi;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter 
public class Seq extends JPanel implements Iterable<MidiTrack>, MidiConstants {
	
	Dimension size = new Dimension(Size.WIDTH_TAB / 3, (Size.HEIGHT_TAB - 50) / 5);

	public static final String NAME = "Seq";
	private final JudahClock clock;
	private final TrackList tracks = new TrackList();
	private final TrackList drumTracks = new TrackList();
	private final TrackList synthTracks = new TrackList();
	private final ArrayList<MidiPlayer> views = new ArrayList<>();
	private final ArrayList<MidiPlayer> knobs = new ArrayList<>();
	/** Total number of tracks (drums and synths) */
	private final int total;

	public Seq(JudahClock clock, DrumMachine kits) {
		setPreferredSize(Size.TAB_SIZE);

		this.clock = clock;
		drumTracks.add(new MidiTrack(kits.getDrum1(), clock));
		drumTracks.add(new MidiTrack(kits.getDrum2(), clock));
		drumTracks.add(new MidiTrack(kits.getHats(), clock));
		drumTracks.add(new MidiTrack(kits.getFills(), clock));
		synthTracks.add(new MidiTrack(JudahZone.getSynth1(), clock));
		synthTracks.add(new MidiTrack(JudahZone.getSynth2(), clock));
		synthTracks.add(new MidiTrack(JudahZone.getCrave(), clock));
		synthTracks.add(new MidiTrack(JudahZone.getFluid(), 1, clock));
		synthTracks.add(new MidiTrack(JudahZone.getFluid(), 2, clock));
		synthTracks.add(new MidiTrack(JudahZone.getFluid(), 3, clock));
		
		tracks.addAll(drumTracks);
		tracks.addAll(synthTracks);
		total = tracks.size();
		
		try {
			scale(tracks.get(4));
			new ImportMidi(tracks.get(0), new File(Folders.getMidi(), "Drum1.mid"), 0);
		} catch (InvalidMidiDataException e) {
			RTLogger.warn(this, e);
		}
		JPanel drums = new JPanel(new GridLayout(0, 4, 10, 1));
		for(int i = 0; i < 4; i++) 
			player(tracks.get(i), drums);
		JPanel synths = new JPanel(new GridLayout(0, 3, 10, 10));
		for (int i = 4; i < tracks.size(); i++)
			player(tracks.get(i), synths);
		
		add(drums);
		add(synths);
		doLayout();
	}

	public MidiTrack getCurrent() {
		return tracks.getCurrent();
	}
	public void setCurrent(MidiTrack t) {
		tracks.setCurrent(t);
	}
	
	private void player(MidiTrack track, JPanel pnl) {
		MidiPlayer view = new MidiPlayer(track, this, false);
		views.add(view);
		pnl.add(view);
		MidiPlayer knob = new MidiPlayer(track, this, true);
		knobs.add(knob);
	}
	
	public KnobPanel getKnobs(MidiTrack t) {
		for (MidiPlayer v : knobs)
			if (v.getTrack() == t)
				return v;
		return null;
	}
	
	public MidiPlayer getView(MidiTrack t) {
		for (MidiPlayer v : views)
			if (v.getTrack() == t)
				return v;
		return null;
	}

	public MidiTrack get(MidiReceiver rcv) {
		for (MidiTrack t : tracks)
			if (t.getMidiOut() == rcv)
				return t;
		return null;
	}
	
	public void process(float beat) {
		for (MidiTrack track : tracks) {
			if (track.isActive()) 
				track.getScheduler().playTo(beat);
		}
	}
	
	public static void scale(MidiTrack track) throws InvalidMidiDataException {
		for (int i = 0; i < 15; i++) {
			track.addEvent(new MidiEvent(Midi.create(NOTE_ON, track.getCh(), i * 2 + 48, 100), 
					i * track.getResolution()));
			track.addEvent(new MidiEvent(Midi.create(NOTE_OFF, track.getCh(), i * 2 + 48, 100), 
					i * track.getResolution() + track.getResolution() - 1));
		}
		track.getScheduler().init();
	}

	@Override
	public Iterator<MidiTrack> iterator() {
		return tracks.iterator();
	}

	public MidiTrack get(int idx) {
		return tracks.get(idx);
	}

	public void update() {
//		if (tab != null && tab.isVisible())
//			tab.changeTrack(current.getView());
//		if (isVisible()) 
//			for (MidiPlayer player : views)
//				player.getHighlight().setBackground(player.getTrack() == current ? 
//					Pastels.MY_GRAY : Pastels.PINK);
	}

//	public static void oomPah(MidiTrack track) throws InvalidMidiDataException {
//		Track t = track.getTrack();
//		track.getScheduler().init();
//		int res = track.getResolution();
//		clear(t);
//		int[] notes = new int[]{0, 36, 1, 40, 1, 43, 2, 31, 3, 40, 3, 43};
//		
//		for (int i = 1; i < notes.length; i+=2) {
//			t.add(new MidiEvent(new ShortMessage(NOTE_ON, notes[i], 100), notes[i-1] * res ));
//			t.add(new MidiEvent(new ShortMessage(NOTE_OFF, notes[i], 1), (notes[i-1] + 1) * res ));
//		}
//		int offset = res * 4;
//		for (int i = 1; i < notes.length; i+=2) {
//			t.add(new MidiEvent(new ShortMessage(NOTE_ON, notes[i], 100), notes[i-1] * res + offset));
//			t.add(new MidiEvent(new ShortMessage(NOTE_OFF, notes[i], 1), (notes[i-1] + 1) * res + offset));
//		}
//		RTLogger.log(MidiTools.class, "oompah size " + t.size());
//	}

}
