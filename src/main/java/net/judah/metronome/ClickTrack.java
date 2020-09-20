package net.judah.metronome;

import java.io.File;
import java.util.HashMap;

import lombok.extern.log4j.Log4j;
import net.judah.midi.JudahChannel;
import net.judah.midi.MidiPlayer;
import net.judah.settings.Command;
import net.judah.settings.Executable;
import net.judah.util.Constants;

@Log4j
public class ClickTrack extends Command implements Executable {
// 	Drumkv1 drumkv1;
	TickTock ticktock;
	Metronome metro;
	
	/** beats before Transport starts */
	public static final String PARAM_INTRO = "intro.beats";
	/** total beats to play */
	public static final String PARAM_DURATION = "duraction.beats";
	/** channel to send trick track midi out on */
	public static final String PARAM_CHANNEL = "midi.channel";
	/** midi file of click track (optional) */
	public static final String PARAM_MIDIFILE = "midi.file"; 
	/** note_on note, first beat of bar (optional) */
	public static final String PARAM_DOWNBEAT = "midi.note.downbeat";
	/** note_on note (optional) */
	public static final String PARAM_BEAT = "midi.beat"; 
	
	public ClickTrack(Metronome metro) {
		super("Clicktrack", metro, generateParams(), "start a clicktrack");
		this.metro = metro;
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
	public void execute(HashMap<String, Object> props) {
		// duration.beats intro.beats drumkv1 file  file  port
		
		int channel = JudahChannel.DRUMS.getChannel();
		try {
			channel = Integer.parseInt("" + props.get(PARAM_CHANNEL));
		} catch (NumberFormatException e) {
			Constants.infoBox("Couldn't setup Click Track channel, using default Drums channel.", "Sequencer Initialization (Clicktrack)");
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
				ticktock = new TickTock(downbeat, beat, channel);
			} catch (NumberFormatException e) {
				log.error(e.getMessage());
			}
		}
		else if (file != null) 
			try {
				ticktock = new MidiPlayer(new File(file.toString()));
			} catch (Exception e) {
				String msg = e.getMessage() + " for " + props.get(PARAM_MIDIFILE);
				log.error(msg, e);
				Constants.infoBox(msg, ClickTrack.class.getSimpleName());
			}
		
		int intro = 4;
		try { intro = Integer.parseInt("" + props.get(PARAM_INTRO));
		} catch (NumberFormatException e) { 
			log.warn(e.getMessage() + " intro.beats=" + props.get(PARAM_INTRO)); 
		}
		int duration = 4;
		try { duration = Integer.parseInt("" + props.get(PARAM_DURATION));
		} catch (NumberFormatException e) {
			log.warn(e.getMessage() + " duration.beats=" + props.get(PARAM_DURATION));
		}
		ticktock.setDuration(intro, duration);

		metro.setPlayer(ticktock == null ? new TickTock() : ticktock);
	}

}
