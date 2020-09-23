package net.judah.metronome;

import java.io.File;
import java.util.HashMap;

import lombok.extern.log4j.Log4j;
import net.judah.midi.JudahChannel;
import net.judah.midi.JudahReceiver;
import net.judah.midi.MidiPlayer;
import net.judah.settings.Command;
import net.judah.settings.Executable;
import net.judah.util.Constants;

@Log4j
public class ClickTrack extends Command implements Executable {

	private final Sequencer sequencer;
	private final Metronome metronome;
	
	/** midi file of click track (optional) */
	public static final String PARAM_MIDIFILE = "midi.file"; 
	/** total loops to play (midi file) */
	public static final String PARAM_LOOPS = "duration.loops";
	/** beats before Transport starts */
	public static final String PARAM_INTRO = "intro.beats";
	/** total beats to play */
	public static final String PARAM_DURATION = "duration.beats";
	/** channel to send trick track midi out on */
	public static final String PARAM_CHANNEL = "midi.channel";
	/** note_on note, first beat of bar (optional) */
	public static final String PARAM_DOWNBEAT = "midi.downbeat";
	/** note_on note (optional) */
	public static final String PARAM_BEAT = "midi.beat"; 
	
	public ClickTrack(Sequencer sequencer, Metronome metro) {
		super("Clicktrack", metro, generateParams(), "start a clicktrack");
		this.metronome = metro;
		this.sequencer = sequencer;
	}
	
	private static HashMap<String, Class<?>> generateParams() {
		HashMap<String, Class<?>> params = new HashMap<String, Class<?>>();
		
		params.put(PARAM_INTRO, Integer.class); 
		params.put(PARAM_DURATION, Integer.class); 
		params.put(PARAM_CHANNEL, Integer.class);
		params.put(PARAM_MIDIFILE, String.class); 
		params.put(PARAM_DOWNBEAT, Integer.class);
		params.put(PARAM_BEAT, Integer.class);
		
		return params;
	}

	@Override
	public void execute(final HashMap<String, Object> props) {
		// duration.beats intro.beats drumkv1 file  file  port
		log.warn("Click Track execute: " + Constants.prettyPrint(props));
		int channel = JudahChannel.DRUMS.getChannel();
		try {
			channel = Integer.parseInt("" + props.get(PARAM_CHANNEL));
		} catch (NumberFormatException e) {
			log.error("Couldn't setup Click Track channel, using default Drums channel.");
		}
			
		MetroPlayer ticktock = null;
		Object down = props.get(PARAM_DOWNBEAT);
		Object b = props.get(PARAM_BEAT);
		Object file = props.get(PARAM_MIDIFILE);
		
		if (down != null && b != null) {
			int beat = TickTock.DEFAULT_BEAT;
			int downbeat = TickTock.DEFAULT_DOWNBEAT;
			try {
				beat = Integer.parseInt(b.toString());
				downbeat = Integer.parseInt(down.toString());
			} catch (NumberFormatException e) {
				log.error(e.getMessage(), e);
			}
			ticktock = new TickTock(sequencer, downbeat, beat, channel);
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
						loops, new JudahReceiver(), sequencer);
			} catch (Exception e) {
				String msg = e.getMessage() + " for " + props.get(PARAM_MIDIFILE);
				log.error(msg, e);
				Constants.infoBox(msg, ClickTrack.class.getSimpleName());
			}

		Object o = props.get(PARAM_INTRO);
		Object d = props.get(PARAM_DURATION);

		ticktock = (ticktock == null) ? new TickTock(sequencer) : ticktock;

		Integer intro = null;
		Integer duration = null;
		try { intro = Integer.parseInt(o.toString());
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
