package net.judah.metronome;

import static net.judah.settings.Commands.MetronomeLbls.*;

import java.io.File;
import java.util.HashMap;

import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.settings.MidiSetup;
import net.judah.util.Constants;

@Log4j
public class ClickTrack extends Command {

	private final Metronome metronome;
	
	/** midi file of click track (optional) */
	public static final String PARAM_MIDIFILE = "midi.file"; 
	/** total loops to play (midi file) */
	public static final String PARAM_LOOPS = "duration.loops";
	/** beats before Transport starts */
	public static final String PARAM_INTRO = "intro.beats";
	/** total beats to play */
	public static final String PARAM_DURATION = "duration.beats";
	/** note_on note, first beat of bar (optional) */
	public static final String PARAM_DOWNBEAT = "midi.downbeat";
	/** note_on note (optional) */
	public static final String PARAM_BEAT = "midi.beat"; 
	
	public ClickTrack(Metronome metro) {
		super(CLICKTRACK.name, CLICKTRACK.desc, generateParams());
		this.metronome = metro;
	}
	
	private static HashMap<String, Class<?>> generateParams() {
		HashMap<String, Class<?>> params = new HashMap<String, Class<?>>();
		
		params.put(PARAM_INTRO, Integer.class); 
		params.put(PARAM_DURATION, Integer.class); 
		params.put(Constants.PARAM_CHANNEL, Integer.class);
		params.put(PARAM_MIDIFILE, String.class); 
		params.put(PARAM_DOWNBEAT, Integer.class);
		params.put(PARAM_BEAT, Integer.class);
		
		return params;
	}

	@Override
	public void execute(final HashMap<String, Object> props, int midiData2) {
		log.warn("Click Track execute: " + Constants.prettyPrint(props));
		int channel = MidiSetup.OUT.DRUMS.channel;
		try {
			channel = Integer.parseInt("" + props.get(Constants.PARAM_CHANNEL));
		} catch (NumberFormatException e) { /** default use channel 9 */}
			
		Player ticktock = null;
		Object down = props.get(PARAM_DOWNBEAT);
		Object b = props.get(PARAM_BEAT);
		Object file = props.get(PARAM_MIDIFILE);
		
		if (down != null &&  b != null) {
			int beat = TickTock.DEFAULT_BEAT;
			int downbeat = TickTock.DEFAULT_DOWNBEAT;
			try {
				beat = Integer.parseInt(b.toString());
				downbeat = Integer.parseInt(down.toString());
			} catch (NumberFormatException e) {
				log.warn(e.getMessage());
			}
			ticktock = new TickTock(metronome, downbeat, beat, channel);
		}
		else if (file != null) 
			try {
				Object o = props.get(PARAM_LOOPS);
				int loops = -1; // LOOP_CONTINUOUSLY;
				if (o != null) 
					try {
						loops = Integer.parseInt(o.toString());
					} catch (NumberFormatException e) {
						log.error("unparsable clicktrack loop count " + o);
					}
				ticktock = new MidiPlayer(new File(file.toString()), 
						loops, new MidiReceiver(Metronome.getMidi()));
			} catch (Exception e) {
				String msg = e.getMessage() + " for " + props.get(PARAM_MIDIFILE);
				log.error(msg, e);
				Constants.infoBox(msg, ClickTrack.class.getSimpleName());
			}

		ticktock = (ticktock == null) ? new TickTock(metronome) : ticktock;

		Integer intro = null;
		Integer duration = null;
		try { intro = Integer.parseInt(props.get(PARAM_INTRO).toString());
		} catch (Throwable e) { 
			log.warn(e.getMessage() + " " + PARAM_INTRO + " = " + props.get(PARAM_INTRO)); 
		}
		try { duration = Integer.parseInt(props.get(PARAM_DURATION).toString());
		} catch (Throwable e) {
			log.warn(e.getMessage() + " " + PARAM_DURATION + " = " + props.get(PARAM_DURATION));
		}
		if (intro != null && duration != null)
			ticktock.setDuration(intro, duration);

		metronome.setPlayer(ticktock);
	}

}
