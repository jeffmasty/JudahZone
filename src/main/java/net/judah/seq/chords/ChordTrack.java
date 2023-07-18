package net.judah.seq.chords;

import static net.judah.seq.chords.ChordPro.SUFFIX;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.api.Notification.Property;
import net.judah.api.TimeListener;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.FileChooser;
import net.judah.midi.JudahClock;
import net.judah.midi.Signature;
import net.judah.song.Scene;
import net.judah.song.Song;
import net.judah.song.cmd.Cmd;
import net.judah.song.cmd.Cmdr;
import net.judah.song.cmd.Param;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

@Getter
public class ChordTrack implements TimeListener, Cmdr {

	private final JudahClock clock;
	private boolean active = false;
	private boolean onDeck = false;
	/** current sequenced chord */
	private Chord chord = new Chord("C", "JudahZone", 16);
	/** current section */
	private Section section;
	private int step;
	/** absolute bars since start */
	private int bar;
	
	private final List<Section> sections = new ArrayList<>();
	private final List<Directive> directives = new ArrayList<>();
	private ChordProData data;
	private Signature sig = Signature.FOURFOUR;
	private final ChordView view = new ChordView(this);
	private final ChordSheet chordSheet = new ChordSheet(this);
	private final Cmdr player = new PlayCmdr();
	private boolean loading;
	private ArrayList<ChordListener> listeners = new ArrayList<>();
	public void addListener(ChordListener l) { if (!listeners.contains(l)) listeners.add(l); }
	public void removeListener(ChordListener l) {listeners.remove(l);}
	public int bars() { return beats() / sig.beats; }
	public int bars(int steps) { return steps / sig.div / sig.beats; }
	public void toggle() { setActive(!active); }
	
	public ChordTrack(JudahClock clock) {
		this.clock = clock;
		sig = clock.getTimeSig();
		clock.addListener(this);
	}
	@Override public void update(Property prop, Object value) {
		if (prop == Property.SIGNATURE) {
			sig = (Signature)value;
		}
		// else if (prop == Property.TRANSPORT && value == JackTransportState.JackTransportStarting) 
		else if (prop == Property.BARS) { 
			if (onDeck) {
				active = true; 
    			onDeck = false;
    			ChordPlay.update();
			} 
    	}
    }
    
	private void setChord(Chord next) {
		if (chord == next) return;
		for (ChordListener l : listeners)
			l.chordChange(chord, next);
		chord = next;
		
		MainFrame.update(chord);
	}

	public void step(int time) {
		if (!active || section == null) return;
		step = time + 1; // ?
		if (step >= sig.steps)
			step = 0;
		if (step == 0)
			bar++;
		int inSection = bar * sig.steps + step;
		if (inSection > section.getCount()) 
			next();
		else 
			setChord(section.getChordAt(inSection));
	}
	
	public void preview(Preview result) {
		result.clear();
		if (section == null || chord == null) return;
		int now = section.getStepsAt(chord);
		int half = 2 * sig.div;
		if (step % sig.steps < half) // 1st half of bar
			for (int i : new int[] {now - half, now, now + half, now + 2 * half, now + 3 * half})
				result.add(search(i));
		else {
			result.middle = true;
			for (int i : new int[] {now - 2 * half, now - half, now, now + half, now + 2 * half})
				result.add(search(i));
		}
	}
	
	private Chord search(int step, Section sec) {
		int count = 0;
		Chord previous = null;
		for (Chord c : sec) {
			if (count == step)
				return c;
			if (count > step)
				return previous;
			count += c.steps;
			previous = c;
		}
		return null;
	}

	private Chord search(int step) {
		if (step < 0) {
			if (section.isOnLoop()) 
				step += section.getCount();
			else if (sections.indexOf(section) > 0) {
				Section prev = sections.get(sections.indexOf(section) - 1);
				return search(prev.getCount() + step, prev);
			}
			else return null;
		}
		else if (step >= section.getCount()) {
			if (section.isOnLoop())
				step -= section.getCount();
			else if (sections.indexOf(section) + 1 < sections.size()){
				Section next = sections.get(sections.indexOf(section) + 1);
				return search(step - section.getCount(), next);
			} else if (directives.contains(Directive.LOOP)) {
				Section loop = sections.get(0);
				if (loop.getPart() == Part.INTRO && sections.size() > 1) 
					loop = sections.get(1);
				return search(step - section.getCount(), loop);
			}
			else 
				return null;
		}
		return search(step, section);
	}
	
	public void load(Song song) {
		view.setSong(song);
		File f;
		if (song == null || song.getChordpro() == null || song.getChordpro().isBlank())
			f = null;
		else
			f = fromSong(song);
    	load(f);
	}

	public static File fromSong(Song song) {
		return new File(Folders.getChordPro(), song.getChordpro() + SUFFIX);
	}

	
	public ChordPro load(File file) {
		loading = true;
		Song song = JudahZone.getSong();
		clear();
		ChordPro chordPro = null;
		String name = null; 
		if (file == null || file.isFile() == false) {
			setActive(false);
			if (song != null)
				song.setChordpro(null);
		}
		else {
			
			name = file.getName().replace(SUFFIX, "");
		
			try {
				chordPro = new ChordPro(file, sig.steps);
				data = chordPro.getData();
				sections.addAll(chordPro.getSections());
				directives.addAll(chordPro.getDirectives());
				parseTempo(chordPro.getData().getTempo());
				if (song != null) {
					song.setChordpro(name);
					parseKey(chordPro.getData().getKey(), song);
				}
			} catch (ParseException e) {RTLogger.warn(this, e.getMessage());}
		}

		SectionCombo.refresh();
		chordSheet.refresh();	
		ChordProCombo.refresh(file);
		ChordPlay.update();
		setSection(sections.isEmpty() ? null : sections.get(0), true);
		for (Section s:sections)
			if (s.getDirectives().contains(Directive.LENGTH))
				JudahZone.getClock().setLength(bars(s.getCount()));
		loading = false;
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
		directives.clear();
		data = null;
		active = false;
		ChordProCombo.refresh(null);
		view.setSection(null);
		chordSheet.refresh();
		SectionCombo.clear();
		ChordScroll.scroll();
		ChordPlay.update();
	}
	
	public void setActive(boolean on) {
		if (!on) {
			active = false;
			listeners.forEach(l->l.chordChange(chord, null));
		}
		else if (!clock.isActive()) 
			active = true;
		else 
			onDeck = true;
		ChordPlay.update();
	}

	public int beats() {
		int steps = 0;
		for (Section s : sections)
			steps += s.getCount();
		return steps / sig.div;
	}

	/** next section in song *some details apply */
	public void next() {
		bar = 0;
		if (section == null) return;
		if (section.getDirectives().contains(Directive.LOOP)) {
			setChord(section.getChordAt(0));
			return;
		}
		int idx = sections.indexOf(section) + 1;
		if (idx < sections.size()) 
			setSection(sections.get(idx), true);
		else if (directives.contains(Directive.LOOP)) {
			Section loop = sections.get(0);
			if (loop.getPart() != Part.INTRO)
				setSection(loop, true);
			else if (sections.size() > 1) 
				setSection(sections.get(1), true);
			else // ?
				setActive(false);
		} else { // song end // ? setSection(sections.get(0), true);
			setActive(false);
		}
	}
	
	public void click(Chord chord) {
		if (this.chord == chord) return;
		
		for (Section s : sections)
			for (Chord c : s) 
				if (c == chord) {
					if (section != s) // switched sections by mouse?
						setSection(s, false);
					break;
				}
		step = 0;
		for (Chord c : section) {
			if (c == chord) break;
			step += c.steps;
		}
		bar = step / sig.steps;
		step = step % sig.steps;
		setChord(chord);
	}
	
	public void setSection(Section selected, boolean setChord) {
		if (sections.indexOf(selected) >= 0)
			section = selected;
		step = 0;
		bar = 0;
		if (section == null) return;
		if (setChord)
			setChord(section.getChordAt(0)); // else defaultChord()
		if (loading == false)
			checkDirectives();
		MainFrame.update(section);
	}
	
	public void toggle(Directive d) {
		if (directives.contains(d))
			directives.remove(d);
		else 
			directives.add(d);
		MainFrame.update(this);
	}

	private void checkDirectives() {
		if (directives.contains(Directive.MUTES)) {
			if (Part.VERSE.literal.equals(section.getName()) 
					|| Part.CHORUS.literal.equals(section.getName())) {
				JudahZone.getLooper().verseChorus();
			}
		}
		else if (active && directives.contains(Directive.SCENES)) {
			for (Scene s : JudahZone.getSong().getScenes())
				if (section.getName().equals(s.getNotes())) {
					JudahZone.setScene(s);
					return;
				}
		}
	}
	
	@Override
	public String[] getKeys() {
		String[] result = new String[sections.size()];
		for (int i = 0; i < result.length; i++)
			result[i] = sections.get(i).getName();
		return result;
	}
	@Override
	public Object resolve(String key) {
		for (Section s : sections)
			if (key.equals(s.getName()))
				return s;
		return null;
	}
	@Override
	public void execute(Param p) {
		if (p.getCmd() != Cmd.Part) 
			return;
		for (Section s : sections)
			if (p.getVal().equals(s.getName())) {
				setSection(s, true);
				return;
			}
	}
	
	class PlayCmdr implements Cmdr {
		@Getter private final String[] keys = {"play", "stop"};
		@Override public Object resolve(String key) {
			return key.equals(keys[0]);
		}

		@Override public void execute(Param p) {
			if (p.getCmd() != Cmd.Chords) 
				return;
			active = p.getVal().equals(keys[0]);
			ChordPlay.update();
		}
	}

	public int getStepsAt(Chord chord) {
		for (Section section : sections) {
			int steps = 0;
			for (Chord c : section) {
				if (c == chord)
					return steps;
				steps += c.getSteps();
			}
		}
		return -1;
	}
	public ChordPro load() {
		File f = FileChooser.choose(Folders.getChordPro());
		if (f == null) 
			return null;
		ChordPro result = load(f);
		if (result != null) 
			ChordProCombo.refill(f);
		return result;
	}
	
}
