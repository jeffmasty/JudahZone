package net.judah.metronome;

import static net.judah.settings.CMD.MetronomeLbls.*;

import java.io.File;
import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequencer;
import javax.swing.JFrame;

import org.apache.commons.lang3.StringUtils;
import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.Player;
import net.judah.api.Status;
import net.judah.api.TimeListener;
import net.judah.api.TimeListener.Property;
import net.judah.api.TimeProvider;
import net.judah.midi.MidiClient;
import net.judah.plugin.Carla;
import net.judah.settings.Command;
import net.judah.settings.Service;
import net.judah.util.Console;
import net.judah.util.Constants;

@Log4j
/**Metronome uses it's own somewhat persistent Carla instance with 
 * custom OSC ports started from a custom executable script */ 
public class Metro implements Service, TimeProvider /* , ActionListener, ChangeListener */ {
	
	public static final String CLIENT_NAME = "JudahTime";
	public static final String DRUM_PORT = "drums_out";
	public static int CARLA_UDP_PORT = 11199;
	public static int CARLA_TCP_PORT = 11198;
	
	public static final String PARAM_GAIN = "volume";
	public static final String PARAM_TEMPO = "bpm";
	public static final String PARAM_MEASURE = "bpb";
	public static final String PARAM_FILE = "midi.file";
	
	@Getter private final String serviceName = Metro.class.getSimpleName();
	
	@Getter private final MidiClient midi;
	@Getter private final TimeProvider timeProvider;
	private final ArrayList<TimeListener> listeners = new ArrayList<>();
	
	/** bpm */
	@Getter private float tempo = 100f;
	/** beats per measure */
	@Getter private int measure = 4;
	@Getter private float gain = 0.9f;
	/** if null, generate midi notes ourself */
	@Getter private File midiFile;
	
	private Player playa;
	private Carla carla; 
	private MetroGui gui;
	private boolean clicktrack;
	private Command start = new Command(TICKTOCK.name, TICKTOCK.desc) {
		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
			if (midiData2 == 0) stop();
			else try { play(); } catch (Exception e) {
				Console.warn(e.getMessage()); log.error(e.getMessage(), e);}}};
	private Command tempoCmd = new Command(TEMPO.name, TEMPO.desc) {
		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
			setTempo((midiData2 + 35) * 1.15f);}};
	private Command volumeCmd = new Command(VOLUME.name, VOLUME.desc) {
		@Override public void execute(HashMap<String, Object> props, int midiData2) throws Exception {
			setVolume(midiData2 * 0.01f);}};
				
	@Getter private final List<Command> commands;
	
    public Metro(File midiFile, TimeProvider master) throws JackException, IOException {
    	
    	midi = new MidiClient(CLIENT_NAME, new String[] {}, new String[] {DRUM_PORT});
    	Runtime.getRuntime().addShutdownHook(new Thread() {
        	@Override public void run() { if (carla != null) carla.close(); }});
    	timeProvider = master == null ? this : master;
    	this.midiFile = midiFile;
    	
    	// CARLA
    	carla = new Carla(this.getClass().getClassLoader().getResource("carla/FluidMetronome.carxp").getFile(),
    			11198, 11199, false);
    	new Thread() {
    		@Override public void run() {
    	    	try { Thread.sleep(1111); 
        		carla.setVolume(0, 1.1f);
    	    	carla.setActive(0, 0); // initialize with fluid plugin bypassed
        	} catch (Exception e) {
        		log.error(e.getMessage(), e);
        		log.warn("Probably have to sleep longer");
        	}};}.start();

        commands = Arrays.asList(new Command[] {new ClickTrack(this), start, tempoCmd, volumeCmd}); 
    }
	
    /** Created and cached upon request */
	public MetroGui getGui() {
		if (gui == null)
			gui = new MetroGui(this);
			listeners.add(gui);
		return gui;
	}
	
	public static void main(String[] args) {
		
		File midiFile = (args.length == 1) ? new File(args[0]) : 
				new File("/home/judah/git/JudahZone/resources/metronome/Latin16.mid");
		try {
			JFrame frame = new JFrame("Metronome");
	        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.setContentPane(new Metro(midiFile, null).getGui());
	        frame.setLocation(200, 50);
	        frame.setSize(330, 165);  
	        frame.setVisible(true);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}
		
	public void setMidiFile(File file) {
		midiFile = file;
		boolean wasRunning = isRunning();
		if (wasRunning) {
			stop();
			try {
				play();
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
	
	public void stop() {
		listeners.forEach( (listener) -> {listener.update(Property.STATUS, Status.TERMINATED);});
		if (playa!= null) {
			listeners.remove(playa);
			playa.close();
		}
		try {
			carla.setActive(0, 0);
		} catch (Exception e) {
			Console.warn(e.getMessage());
		}
		clicktrack = false;
	}

	public void play() throws IOException, InvalidMidiDataException, MidiUnavailableException {
		if (isRunning())
			stop();
		else if (playa != null) {
			listeners.remove(playa);
			playa.close();
		}
		
		try {
			carla.setActive(0, 1);
		} catch (Exception e) {
			Console.warn(e.getMessage());
		}
		
		if (!clicktrack)
			playa = (midiFile == null) ?
					new TickTock(this) : 
					new MidiPlayer(midiFile, Sequencer.LOOP_CONTINUOUSLY, new MidiReceiver(midi));
		listeners.add(playa);
		listeners.forEach( listener -> {listener.update(Property.MEASURE, measure);});
		listeners.forEach( listener -> {listener.update(Property.VOLUME, gain);});
		listeners.forEach( listener -> {listener.update(Property.TEMPO, tempo);});
		listeners.forEach( listener -> {listener.update(Property.STATUS, Status.ACTIVE);});
	}
	
	/** store a ticktock object configured and ready to begin transport */
	void setPlayer(Player ticktock) {
		if (isRunning()) stop();
		playa = ticktock;
		listeners.add(playa);
		clicktrack = true;
		log.info("ClickTrack set");
		
	}

	@Override
	public boolean setTempo(float tempo) {
		this.tempo = tempo;
		listeners.forEach( listener -> {listener.update(Property.TEMPO, tempo);});
		return true;
	}

	@Override
	public void setMeasure(int bpb) {
		if (bpb < 2 || bpb > 30) return;
		boolean wasRunning = isRunning();
		if (isRunning()) stop();
		
		measure = bpb;
		if (playa != null) playa.close();
		midiFile = null;
		if (wasRunning)
			try {
				play();
			} catch (IOException | InvalidMidiDataException | MidiUnavailableException e) {
				log.error(e.getMessage(), e);
			}
	}
	
	@Override
	public void addListener(TimeListener l) {
		listeners.add(l);
	}
	@Override
	public void removeListener(TimeListener l) {
		listeners.remove(l);
	}
	

	@Override
	public boolean beat(int current) {
		if (this != timeProvider) return false;
		listeners.forEach(listener -> {listener.update(Property.BEAT, current);});
		return true;
	}

	@Override
	public void properties(HashMap<String, Object> props) {
		if (StringUtils.isNumeric("" + props.get(Constants.PARAM_BPM))) {
			setTempo(Integer.parseInt("" + props.get(Constants.PARAM_BPM)));
		}
		if (StringUtils.isNumeric("" + props.get(Constants.PARAM_MEAUSRE)))
			setMeasure(Integer.parseInt("" + props.get(Constants.PARAM_MEAUSRE)));
	}
	
}

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




