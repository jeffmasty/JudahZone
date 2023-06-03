package net.judah.seq.chords;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Getter;
import lombok.Setter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;
import net.judah.seq.chords.Section.Type;
import net.judah.song.Song;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Getter
public class ChordTrack implements TimeListener {

	private final JudahClock clock;
	private boolean active = false;
	private boolean onDeck = false;
	/** current sequenced chord */
	private Chord chord = new Chord("C", "JudahZone", 16);
	/** current section */
	private Section section;
	/** absolute bars since start */
	private int bar;
	/** number of steps in bar */
	private int steps;
	
	private final List<Section> sections = new ArrayList<>();
	private ChordProData data;
	private int beats, div;
	
	private final ChordView view = new ChordView(this);
	private final ChordSheet chordSheet;
	@Setter boolean loop = true;
	private ArrayList<ChordListener> listeners = new ArrayList<>();
	public void addListener(ChordListener l) { if (!listeners.contains(l)) listeners.add(l); }
	public void removeListener(ChordListener l) {listeners.remove(l);}
	public int bars() { return beats() / beats; }
	public int bars(int steps) { return steps / div / beats; }
	public void toggle() { setActive(!active); }
	
	public ChordTrack(JudahClock clock) {
		this.clock = clock;
		clock.addListener(this);
		steps = clock.getSteps();
		div = clock.getSubdivision();
		beats = clock.getMeasure();
		chordSheet = new ChordSheet(this);
	}
	
	@Override public void update(Property prop, Object value) {
		if (prop == Property.MEASURE) {
			steps = clock.getSteps();
			div = clock.getSubdivision();
			beats = clock.getMeasure();
		}
		else if (prop == Property.TRANSPORT && value == JackTransportState.JackTransportStarting) 
    		init();
		else if (prop == Property.BARS) { 
			if (onDeck) {
				active = true; 
    			onDeck = false;
    			MainFrame.update(view);
			} 
    	}
    }
    
	private void setChord(Chord next) {
		if (false == chord.equals(next))
			for (ChordListener l : listeners)
				l.chordChange(chord, next);
		chord = next;
		MainFrame.update(next);
	}

    private void init() {
    	bar = 0;
    	section = sections.isEmpty() ? null : sections.get(0);
    	if (section != null)
    		chord = section.get(0);
    }
    
	public ChordProParser load(File file, Song song) {
		clear();
		ChordProParser chordPro = ChordProParser.load(file, clock.getSteps());
		if (chordPro != null) {
			data = chordPro.getData();
			sections.addAll(chordPro.getSections());
			if (!sections.isEmpty()) 
				section = sections.get(0);
			parseTempo(chordPro.getData().getTempo());
			if (song != null) {
				song.setChordpro(file);
				parseKey(chordPro.getData().getKey(), song);
			}
		}
		else if (song != null)
			song.setChordpro(null);
		
		setActive(chordPro != null);
		
		Constants.execute(()-> {
			chordSheet.refresh();	
			view.update();
			if (section != null && !section.isEmpty())
				setChord(section.get(0));
		});
		return chordPro; // not used
	}
	
	private void parseTempo(String tempo) {
		if (tempo == null || tempo.isBlank()) return;
		try { JudahZone.getClock().writeTempo((int)Float.parseFloat(tempo));
		} catch (Throwable t) {RTLogger.log(this, "Tempo: " + tempo);}
	}
	
	private void parseKey(String key, Song song) {
		if (song == null || key == null || key.isBlank()) return;
		Chord k = new Chord(key);
		song.setKey(k.getRoot());
		song.setScale(k.isDominant() ? k.isMinor() ? Scale.MINOR : Scale.DOM7 : 
			k.isMajor()? Scale.MAJOR : Scale.HARMONIC);
	}
	
	private void clear() {
		bar = 0;
		section = null;
		sections.clear();
		data = null;
	}
	
	public void load(Song song) {
		view.setSong(song);
		if (song == null || song.getChordpro() == null || song.getChordpro().isFile() == false) 
			clear();
		else 
			load(song.getChordpro(), song);
	}
	
	public void setActive(boolean on) {
		if (!on)
			active = false;
		else if (!clock.isActive()) 
			active = true;
		else 
			onDeck = true;
		MainFrame.update(view);
	}

	public int beats() {
		int steps = 0;
		for (Section s : sections)
			steps += s.steps();
		return steps / div;
	}

	public void step(int time) {
		if (!active || section == null) return;
		int step = time + 1; // ?
		if (step >= steps)
			step = 0;
		if (step == 0)
			bar++;
		int inSection = bar * steps + step;
		if (inSection > section.stepCount()) {
			inSection = 0;
			bar = 0;
			if (section.getType() == Type.INTRO) 
				next();
			else if (section.getType() == Type.ENDING)
				setActive(false);
			else if (!loop) 
				next();
		}
		setChord(section.getChordAt(inSection));
	}
	
	/** next section in song *some details apply */
	private void next() {
		if (section.getType() == Type.ENDING) {
			section = null;
			return;
		}
		int idx = sections.indexOf(section) + 1;
		if (idx < sections.size()) 
			section = sections.get(idx);
		else {
			if (sections.get(0).getType() == Type.INTRO) {
				if (sections.size() > 1)
					section = sections.get(1);
			}
			else 
				section = sections.get(0);
		}
	}
	
	public void setCurrent(Chord chord) {
		if (this.chord == chord) return;
		
		for (Section s : sections)
			for (Chord c : s)
				if (c == chord) {
					section = s;
					break;
				}
		int step = 0;
		for (Chord c : section) {
			if (c == chord) break;
			step += c.steps;
		}
		bar = step / steps;
		setChord(chord);
	}
	
}
