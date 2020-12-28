package net.judah;

import static net.judah.jack.AudioTools.*;
import static net.judah.util.Constants.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.BasicClient;
import net.judah.api.Midi;
import net.judah.api.Service;
import net.judah.fluid.FluidSynth;
import net.judah.looper.Recorder;
import net.judah.metronome.Metronome;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.mixer.plugin.Plugin;
import net.judah.sequencer.Sequencer;
import net.judah.util.RTLogger;
import net.judah.util.Services;


/* Starting my jack sound system: 
/usr/bin/jackd -R -P 99 -T -v -ndefault -p 512 -r -T -d alsa -n 2 -r 48000 -p 512 -D -Chw:K6 -Phw:K6 &
a2jmidid -e & */

@Log4j
public class JudahZone extends BasicClient {

    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

    @Getter private static JudahZone instance;

	@Getter private static final ArrayList<JackPort> inPorts = new ArrayList<>();
	@Getter private static final ArrayList<JackPort> outPorts = new ArrayList<>();
	@Getter private JackPort outL, outR;
	
    // (midi handler, fluidsynth, metronome)
    @Getter private static final Services services = new Services();
    @Getter private static final CommandHandler commands = new CommandHandler(); 
	@Getter private static final ArrayList<Channel> channels = new ArrayList<>();
	@Getter private static final Looper looper = new Looper(outPorts);
	@Getter private static final ArrayList<Plugin> plugins = new ArrayList<>();
	
	@Getter private final FluidSynth synth; 
	@Getter private static JudahMidi midi;
	@Getter private static Metronome metronome;
	@Getter private Sequencer currentSong;
	
    public static void main(String[] args) {
		try {
			new JudahZone();
			while (true) 
				RTLogger.poll();
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
    }

	JudahZone() throws Throwable {
		super(JUDAHZONE);
		instance = this;
    	Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    	
    	midi = new JudahMidi("JudahMidi");
    	synth = new FluidSynth(midi);
    	File clickTrack = new File(this.getClass().getClassLoader().getResource("metronome/JudahZone.mid").getFile()); 
    	metronome = new Metronome(clickTrack, null);
    	services.addAll(Arrays.asList(new Service[] {midi, synth, metronome}));
    	
        start();
	}

	@Override
	protected void initialize() throws JackException {
		Channel guitar = new Channel("guitar", "system:capture_1", "guitar");
		guitar.setGainFactor(3f);
		guitar.setVolume(70);
		guitar.setDefaultCC(16);
		
		Channel mic = new Channel("mic", "system:capture_2", "mic");
		mic.setGainFactor(3f);
		mic.setVolume(70);
		mic.setDefaultCC(17);

		Channel drums = new Channel("drums", "system:capture_3", "drums");
		drums.setGainFactor(2.5f);
		drums.setVolume(60);
		drums.setDefaultCC(20);

		Channel synth = new Channel("synth",
				new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT}, 
				new String[] {"synthL", "synthR"});
		synth.setVolume(40);
		synth.setDefaultCC(21);
		
		Channel aux1 = new Channel("aux1", "system:capture_4", "aux1");
		aux1.setGainFactor(2.5f);
		aux1.setDefaultCC(18);
		
		Channel aux2 = new Channel("aux2", new String[]
				// {"Calf Fluidsynth:Out L", "Calf Fluidsynth:Out R"},
				{null, null}, // TODO carla fluid ports not started up yet at makeConnections(). 
				new String[] {"aux2", "aux3"});
		aux2.setMuteRecord(true);
		aux2.setDefaultCC(19);

		channels.addAll(Arrays.asList(new Channel[] { guitar, mic, drums, synth, aux1, aux2 }));
		
		JackPort port;
		for (Channel ch : channels) {
			port = jackclient.registerPort(ch.getLeftConnection(), AUDIO, JackPortIsInput);
			ch.setLeftPort(port);
			inPorts.add(port);
			if (ch.isStereo()) {
				port = jackclient.registerPort(ch.getRightConnection(), AUDIO, JackPortIsInput);
				ch.setRightPort(port);
				inPorts.add(port);
			}
		}

		for (String name : new String[] { "left", "right" /* ,"auxL", "auxR" */})
			outPorts.add(jackclient.registerPort(name, AUDIO, JackPortIsOutput));
		outL = outPorts.get(LEFT_CHANNEL);
		outR = outPorts.get(RIGHT_CHANNEL);
		
		String debug = "channels: ";
		for (Channel ch : channels)
			debug += ch.getName() + " "; 
		log.debug(debug);
		
    	looper.init();
	}
	
	@Override
	protected void makeConnections() throws JackException {
		// inputs
		for (Channel ch : channels) {
			if (ch.getLeftSource() != null && ch.getLeftConnection() != null)
				jack.connect(jackclient, ch.getLeftSource(), 
						portName(clientName, ch.getLeftConnection()));
			if (ch.getRightSource() != null && ch.getRightConnection() != null)
				jack.connect(jackclient, ch.getRightSource(), 
						portName(clientName, ch.getRightConnection()));
		}
		
		// outputs
		jack.connect(jackclient, outL.getName(), "system:playback_1");
		jack.connect(jackclient, outR.getName(), "system:playback_2");
		
		
		// Initialize command services and start GUI
		commands.initializeCommands();
		new MainFrame(JUDAHZONE);

//		// Open a default song 
//		File file = new File("/home/judah/git/JudahZone/resources/Songs/FeelGoodInc");
//		try {
//			new Sequencer(file);
//		} catch (Exception e) { 
//			Console.warn(e.getMessage() + " " + file.getAbsolutePath(), e); }

	}
	/** if the midi msg is a hard-coded mixer setting, run the setting and return false */ 
    public static boolean midiProcessed(Midi midi) {
    	
    	if (midi.isCC()) {
    		int data1 = midi.getData1();
    		for (Channel c : JudahZone.getChannels()) {
    			if (data1 == c.getDefaultCC()) {
    				c.setVolume(midi.getData2());
    				return true;
    			}
    		} 
    		if (data1 == 14) {// loop A volume knob
    			getLooper().get(0).setVolume(midi.getData2());
    			return true;
    		}
    		
    		if (data1 == 15) {// loop B volume knob
    			getLooper().get(1).setVolume(midi.getData2());
    			return true;
    		}
    		
    		if (data1 == 31) {// record loop a knob
    			((Recorder)getLooper().get(0)).record(midi.getData2() > 0);
    			return true;
    		}
    		if (data1 == 32) {// record loop b knob
    			((Recorder)getLooper().get(1)).record(midi.getData2() > 0);
    			return true;
    		}
    		if (data1 == 35) {// play loop a knob
    			getLooper().get(0).play(midi.getData2() > 0);
    			return true;
    		}
    		if (data1 == 36) {// play loop b knob
    			getLooper().get(1).play(midi.getData2() > 0);
    			return true;
    		}
    		if (data1 == 34) { // clear loopers cc pad
    			getLooper().init();
    			return true;
    		}
    		if (data1 == 96) { // record Loop A foot pedal
    			((Recorder)getLooper().get(0)).record(midi.getData2() > 0);
    		}
    		if (data1 == 97) { // record Loop B foot pedal
    			((Recorder)getLooper().get(1)).record(midi.getData2() > 0);
    		}
    		if (data1 == 99) { // record Loop A foot pedal
    			getLooper().get(0).play(midi.getData2() > 0);
    		}
    		if (data1 == 100) { // record Loop A foot pedal
    			getLooper().get(0).play(midi.getData2() > 0);
    		}
    		// TODO 101 = octaver effect
    		
    		
    		
    	}
		return false;
	}

	
	private class ShutdownHook extends Thread {
		@Override public void run() {
			if (Sequencer.getCurrent() != null) {
				Sequencer.getCurrent().close();
				if (Sequencer.getCarla() != null)
					Sequencer.getCarla().close();
			} 
			for (Service s : services) 
				s.close();
		}
	}
	
	////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////
	
	@Override
	public boolean process(JackClient client, int nframes) {
		
		// channels and looper will be additive
		processSilence(outL.getFloatBuffer());
		processSilence(outR.getFloatBuffer());
		
		// mix the live stream 
		for (Channel ch : channels) {
			if (ch.isOnMute()) continue;
			ch.applyGain(); // for both live mix and loopers
			processAdd(ch.getLeftPort().getFloatBuffer(), outL.getFloatBuffer());
			if (ch.isStereo()) 
				processAdd(ch.getRightPort().getFloatBuffer(), outR.getFloatBuffer());
			else 
				processAdd(ch.getLeftPort().getFloatBuffer(), outR.getFloatBuffer());
			
		}

		// get looper in on process()
		looper.process(nframes);
		return true;
	}

}
