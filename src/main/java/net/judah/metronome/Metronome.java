package net.judah.metronome;

import static net.judah.settings.Commands.MetronomeLbls.TEMPO;
import static net.judah.settings.Commands.MetronomeLbls.TICKTOCK;
import static net.judah.settings.Commands.MetronomeLbls.VOLUME;
import static net.judah.util.Constants.Param.BPM;
import static net.judah.util.Constants.Param.GAIN;
import static net.judah.util.Constants.Param.MEASURE;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.swing.JFrame;

import org.apache.commons.lang3.StringUtils;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackTransportState;

import com.illposed.osc.OSCSerializeException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Command;
import net.judah.api.Service;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.clock.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.util.Console;
import net.judah.util.Constants;

@Log4j
/**Metronome uses it's own somewhat persistent Carla instance with
 * custom OSC ports started from a custom executable script */
public class Metronome implements Service, TimeProvider, TimeListener {

	public static final String CLIENT_NAME = "JudahTime";
	public static final String DRUM_PORT = "drums_out";
	public static int CARLA_UDP_PORT = 11199;
	public static int CARLA_TCP_PORT = 11198;

	public static final String PARAM_FILE = "midi.file";

	@Getter private final String serviceName = Metronome.class.getSimpleName();

	@Getter private final JudahMidi midi;
	@Getter private final TimeProvider timeProvider;
	private static final ArrayList<TimeListener> listeners = new ArrayList<>();

	/** bpm */
	private int tempo = 100;
	/** beats per measure */
	@Getter private int measure = 4;
	@Getter private float gain = 0.75f;
	/** if null, generate midi notes ourself */
	@Getter private File midiFile;

	private Player playa;
	private MetroGui gui;
	private boolean clicktrack;
	public boolean hasClicktrack() {return clicktrack;}

	@Getter private final List<Command> commands = new ArrayList<>();

    public Metronome(JudahMidi queue, File midiFile)
            throws JackException, IOException, InvalidMidiDataException, OSCSerializeException {

    	this.midi = queue;
        this.midiFile = midiFile;
    	//if (master == null)
    	    timeProvider = this;
    	//else {timeProvider = master;timeProvider.addListener(this);}

        commands.add(new Command(TICKTOCK.name, TICKTOCK.desc) {
    		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
    			if (midiData2 == 0) end();
    			else try { begin(); } catch (Exception e) {
    				Console.warn(e.getMessage(), e); }}});
        commands.add(new Command(TEMPO.name, TEMPO.desc) {
    		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
    			setTempo((midiData2 + 35) * 1.15f);}});
        commands.add(new Command(VOLUME.name, VOLUME.desc,
        		Constants.template(GAIN, Float.class)) {
    		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
    			if (midiData2 >= 0)
    				setVolume(midiData2 * 0.01f);
    			else setVolume(Float.parseFloat("" + props.get(GAIN)));}});
//		HashMap<String, Class<?>> template = new HashMap<>();
//		template.put(CHANNEL, Integer.class);
//		template.put(PRESET, Integer.class);

//        commands.add(new Command(METROCHANGE.name, METROCHANGE.desc, template) {
//			@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
//				int channel = 0;
//				int preset = 0;
//				try {
//					preset = Integer.parseInt(props.get("preset").toString());
//					channel = Integer.parseInt(props.get("channel").toString());
//					Midi msg = new ProgMsg(channel, preset);
//					queue.queue(msg);
//				} catch (Throwable t) {
//					log.debug(t.getMessage()); }}});

        commands.add(new ClickTrack(this));
    }

    public void openGui() {
		try {
			JFrame frame = new JFrame("Metronome");
			frame.setContentPane(getGui());
	        frame.setLocation(200, 50);
	        frame.setSize(330, 200);
	        frame.setVisible(true);
		} catch (Exception e) {
			Console.warn(e.getMessage(), e);
		}
    }

    /** Created and cached upon request */
	public MetroGui getGui() {
		if (gui == null)
			gui = new MetroGui(this);
			timeProvider.addListener(gui);
		return gui;
	}

	public void setMidiFile(File file) {
		midiFile = file;
		boolean wasRunning = isRunning();
		if (wasRunning) {
			end();
			try {
				begin();
			} catch (Exception e) {
				log.error(e.getMessage(),e);
			}
		}
	}

	public boolean isRunning() {
		return playa != null && playa.isRunning();
	}

	public void setVolume(float gain) {
		if (gain > 1 || gain < 0) throw new InvalidParameterException("volume between 0 and 1: " + gain);
		this.gain = gain;
		listeners.forEach( listener -> { listener.update(Property.VOLUME, gain);});
	}

	@Override
	public void close() {
		if (playa != null) {
			playa.stop();
			playa.close();
			playa = null;
		}
	}

	@Override
    public void end() {

		listeners.forEach( (listener) -> {listener.update(Property.STATUS, Status.TERMINATED);});
		if (playa!= null) {
			listeners.remove(playa);
			playa.close();
		}
		clicktrack = false;
	}

	@Override
    public void begin() {
	    try {
    		if (isRunning())
    			end();
    		else if (playa != null && !clicktrack) {
    			listeners.remove(playa);
    			playa.close();
    		}

    		if (!clicktrack)
    			playa = (midiFile == null) ?
    					new TickTock(this) :
//    					new MidiPlayer(midiFile, Sequencer.LOOP_CONTINUOUSLY, new MidiReceiver(midi));
    					new MidiPlayer(midiFile, Sequencer.LOOP_CONTINUOUSLY, 
    							new JackReceiver(JudahClock.getInstance().getSequencer(0).getMidiOut()));

    		addListener(playa);
    		listeners.forEach( listener -> {listener.update(Property.MEASURE, measure);});
    		listeners.forEach( listener -> {listener.update(Property.VOLUME, gain);});
    		listeners.forEach( listener -> {listener.update(Property.TEMPO, tempo * 1f);});
    		listeners.forEach( listener -> {listener.update(Property.STATUS, Status.ACTIVE);});
    		clicktrack = false;
	    } catch (IOException| InvalidMidiDataException| MidiUnavailableException e) {
	        Console.warn(e);
	    }
	}

	/** store a ticktock object configured and ready to begin transport */
	void setPlayer(Player ticktock) {
		if (isRunning()) end();
		playa = ticktock;
		listeners.add(playa);
		clicktrack = true;
		log.info("ClickTrack set");

	}

	@Override public float getTempo() {
	    return tempo;
	}

	@Override
	public boolean setTempo(float tempo) {
	    if (this.tempo != Math.round(tempo)) {
	        this.tempo = Math.round(tempo);
    	    new Thread() { // off RT thread
    	        @Override public void run() {
    	            listeners.forEach( listener -> {
    	                    listener.update(Property.TEMPO, tempo);
    	    });}}.start();
	    }
		return true;
	}

	public void setMeasure(int bpb) {
		if (bpb < 2 || bpb > 30) return;
		boolean wasRunning = isRunning();
		if (isRunning()) end();

		measure = bpb;
		if (playa != null) playa.close();
		midiFile = null;
		if (wasRunning)
		    begin();
	}

	@Override
	public void addListener(TimeListener l) {
		if (!listeners.contains(l))
			listeners.add(l);
	}
	@Override
	public void removeListener(TimeListener l) {
		listeners.remove(l);
	}


	void rollTransport() {
		if (this != timeProvider) return;
		listeners.forEach(listener -> {listener.update(Property.TRANSPORT, JackTransportState.JackTransportStarting);});
	}

	@Override
	public void properties(HashMap<String, Object> props) {
		clicktrack = false;

		if (StringUtils.isNumeric("" + props.get(BPM))) {
			setTempo(Integer.parseInt("" + props.get(BPM)));
		}
		if (StringUtils.isNumeric("" + props.get(MEASURE)))
			setMeasure(Integer.parseInt("" + props.get(MEASURE)));
	}

	public static void remove(TimeListener l) {
		listeners.remove(l);
	}

	@Override
	public long getLastPulse() {
		return -1; // not implemented yet
	}

	@Override
	public void update(Property prop, Object value) {
		if (Property.TEMPO == prop) {
			setTempo((float)value);
		}

	}

}

// private final Command start, tempoCmd, volumeCmd;
//private Request togglePlay = new Request("MidiPlay:start/stop") {
//	@Override public void process(HashMap<String, Object> props) throws Exception {
//		if (isRunning()) stop();
//		else play();}};
// @Getter private final DynamicCommand toggleStart;
// private final settings;
// private final DynamicCommand tempoCmd, volumeCmd;
//@Override
//public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
//	if (cmd instanceof ClickTrack) ((ClickTrack)cmd).execute(props);




