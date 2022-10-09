package net.judah;

import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.sound.midi.InvalidMidiDataException;
import javax.swing.JComboBox;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import com.illposed.osc.OSCSerializeException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.api.BasicClient;
import net.judah.carla.Carla;
import net.judah.controllers.Jamstik;
import net.judah.drumz.DrumKit;
import net.judah.drumz.DrumMachine;
import net.judah.effects.Fader;
import net.judah.effects.api.PresetsDB;
import net.judah.effects.gui.FxPanel;
import net.judah.fluid.FluidSynth;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiGui;
import net.judah.midi.Path;
import net.judah.midi.Ports;
import net.judah.mixer.*;
import net.judah.samples.Sampler;
import net.judah.songs.SmashHit;
import net.judah.synth.JudahSynth;
import net.judah.tracker.GridTab;
import net.judah.tracker.JudahBeatz;
import net.judah.tracker.JudahNotez;
import net.judah.tracker.Track;
import net.judah.tracker.Tracker;
import net.judah.util.Constants;
import net.judah.util.JudahException;
import net.judah.util.RTLogger;
import net.judah.util.SettableCombo;

/* my jack sound system settings: jackd -P99 -dalsa -dhw:UMC1820 -r48000 -p512 -n2*/
@Log4j
public class JudahZone extends BasicClient {
    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

    @Getter private static int srate;
	@Getter private static int bufSize;
    @Getter private static JackClient client;
    @Getter private static JudahMidi midi;
    @Getter private static JudahClock clock;
    @Getter private static JackPort outL, outR;
    @Getter private static Mains mains;
    @Getter private static boolean initialized;
    
    @Getter private static Instrument piano;
    @Getter private static Instrument guitar;
    @Getter private static Instrument mic;
    @Getter private static MidiInstrument crave;
    @Getter private static FluidSynth fluid;
    @Getter private static JudahSynth synth1;
    @Getter private static JudahSynth synth2;
    @Getter private static DrumMachine drumMachine;
    @Getter private static Tracker tracker;
    @Getter private static JudahBeatz beats;
    @Getter private static JudahNotez notes;
    @Getter private static DJJefe mixer;
    @Getter private static Looper looper;
    @Getter private static GridTab beatBox; 
    @Getter private static Sampler sampler;
    @Getter private static Jamstik jamstik;
    @Getter private static Carla carla;
    @Getter private static SetList setlist;
    @Getter private static SmashHit current;
    @Getter private static MainFrame frame;
    @Getter private static FxPanel fxRack;
    @Getter private static MidiGui midiGui;
    @Getter private static final ArrayList<Closeable> services = new ArrayList<Closeable>();
    @Getter private static final Instruments instruments = new Instruments();
    @Getter private static final PresetsDB presets = new PresetsDB();
    @Getter private static final Ports synthPorts = new Ports();
    @Getter private static final Ports drumPorts = new Ports();
    @Getter private static final Zone noizeMakers = new Zone();
    @Getter private static boolean override; // for SettableCombo

    public static void main(String[] args) {
        try {
            new JudahZone();
            RTLogger.monitor();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    JudahZone() throws JackException, JudahException, IOException, InvalidMidiDataException, OSCSerializeException {
        super(JUDAHZONE);
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        MainFrame.startNimbus();
        start();
    }

    @Override
    protected void initialize() throws JackException {
    	client = jackclient;
    	srate = jackclient.getSampleRate();
    	bufSize = jackclient.getBufferSize();

        outL = jackclient.registerPort("left", AUDIO, JackPortIsOutput);
        outR = jackclient.registerPort("right", AUDIO, JackPortIsOutput);

        drumMachine = new DrumMachine("Drum Machine", outL, outR, "DrumMachine.png");
    	midi = new JudahMidi("JudahMidi", this, drumMachine);
    	
    	JackPort left, right;
    	left = jackclient.registerPort("piano", AUDIO, JackPortIsInput);
    	piano = new Instrument("Key", "system:capture_2", left, "Key.png");
    	
    	left = jackclient.registerPort("guitar", AUDIO, JackPortIsInput);
    	guitar = new Instrument(Constants.GUITAR, "system:capture_1", left, "Guitar.png");
    	
    	left = jackclient.registerPort("mic", AUDIO, JackPortIsInput);
    	mic = new Instrument(Constants.MIC, "system:capture_4", left, "Microphone.png");

    	left = jackclient.registerPort("fluidL", AUDIO, JackPortIsInput);
    	right = jackclient.registerPort("fluidR", AUDIO, JackPortIsInput);
    	while (midi.getFluidSynth() == null) 
    		Constants.sleep(40);
    	fluid = midi.getFluidSynth();
    	fluid.setLeftPort(left);
    	fluid.setRightPort(right);
    	
    	left = jackclient.registerPort("crave_in", AUDIO, JackPortIsInput);
    	while (midi.getCraveSynth() == null)
    		Constants.sleep(40);
    	crave = midi.getCraveSynth();
    	crave.setLeftPort(left);
    	instruments.addAll(Arrays.asList(new Instrument[] { guitar, mic, fluid, crave, piano}));
    	synth1 = new JudahSynth(1, outL, outR, "Synth.png");
        synth2 = new JudahSynth(2, outL, outR, "Waveform.png");
        
        // sequential order for the Mixer
        noizeMakers.add(guitar);
        noizeMakers.add(mic);
        noizeMakers.add(drumMachine);
        noizeMakers.add(synth1);
        noizeMakers.add(synth2);
        noizeMakers.add(fluid);
        noizeMakers.add(crave);
        noizeMakers.add(piano);
        noizeMakers.initVolume();
        noizeMakers.initMutes();
        
        looper = new Looper(outL, outR, noizeMakers, drumMachine);
        sampler = new Sampler(outL, outR);
        mains = new Mains(outL, outR);

        try { // Initialize the Carla lv2 plugin host now that our ports are created
            carla = new Carla(Carla.NO_GUI, looper);
        } catch (Exception e) { throw new JackException(e); }
    }

    @Override
    protected void makeConnections() throws JackException {
        if (mains == null) {
            RTLogger.log(this, "Initialization failed.");
            return;
        }
        
        // inputs
        for (Instrument ch : instruments.getInstruments()) {
            if (ch.getLeftSource() != null && ch.getLeftPort() != null)
                jack.connect(jackclient, ch.getLeftSource(), ch.getLeftPort().getName());
            if (ch.getRightSource() != null && ch.getRightPort() != null)
                jack.connect(jackclient, ch.getRightSource(), ch.getRightPort().getName());
        }
                
        // main output
        jack.connect(jackclient, outL.getName(), "system:playback_1");
        jack.connect(jackclient, outR.getName(), "system:playback_2");

    }
    
    /**Start Gui. Called by JudahMidi when initialization complete, audio and midi setup complete */
    public void finalizeMidi() { 
    	while (mains == null) // sync with Audio initialization
    		Constants.sleep(30);
    	clock = midi.getClock();
    	beats = clock.getBeats();
    	notes = clock.getNotes();
    	synthPorts.add(synth1.getMidiPort());
    	synthPorts.add(synth2.getMidiPort());
    	synthPorts.add(fluid.getMidiPort());
    	synthPorts.add(crave.getMidiPort());
    	midi.setKeyboardSynth(synthPorts.get(synth1));
    	ArrayList<Path> paths = midi.getPaths();
    	paths.add(new Path(synthPorts.get(synth1), synth1));
    	paths.add(new Path(synthPorts.get(synth2), synth2));
    	paths.add(new Path(synthPorts.get(midi.getFluidOut()), fluid));
    	paths.add(new Path(synthPorts.get(midi.getCraveOut()), crave));
    	
    	for (DrumKit drums : drumMachine.getChannels())
    		drumPorts.add(drums.getMidiPort());

    	String[] patches = fluid.getPatches();
    	fluid.progChange(patches[44], 0); // strings
        fluid.progChange(patches[0], 1); // piano
        fluid.progChange(patches[32], 2); // bass
        fluid.progChange(patches[46], 3); // harp
    	
    	tracker = new Tracker(beats, notes);
    	setlist = new SetList();
    	fxRack = new FxPanel();
    	jamstik = new Jamstik(services, midi.getPaths());
    	midiGui = new MidiGui(midi, clock, jamstik);
    	mixer = new DJJefe(mains, looper, noizeMakers);
    	beatBox = new GridTab();
    	frame = new MainFrame(JUDAHZONE, fxRack, midiGui, mixer, beatBox, tracker);
    	mains.setOnMute(false);
    	MainFrame.update(frame);

    	initialized = true;
    	///////////////////////////////////////////////////////////////////
        //                    now the system is live                     //
        ///////////////////////////////////////////////////////////////////
    	loadDrumMachine();
        justInTimeCompiler();
    }

    private void loadDrumMachine() {
        synth1.getPresets().load("FeelGoodInc");
        synth2.getPresets().load("TimeAfterTime");
        
        
        drumMachine.getDrum1().setKit("BBuddy");
        drumMachine.getDrum2().setKit("Perks");
        drumMachine.getFills().setKit("Pearl");
        drumMachine.getHats().setKit("808");
        beats.getDrum1().setFile("Bossa1");
        beats.getDrum2().setFile("Swing1");
        beats.getFills().setFile("tinyDrums");
		Track hihats = beats.getHats();
        hihats.setFile("HiHats");
		hihats.setPattern("gamma");
        hihats.setGain(0.42f); 
    }
    
    // put algorithms through their paces
    private void justInTimeCompiler() {
    	MainFrame.updateTime();
        Fader.execute(Fader.fadeIn());
        
        looper.getDrumTrack().toggle();
        mains.getReverb().setActive(true);
        mic.getReverb().setActive(true);
        final Loop c = looper.getLoopC();
    	c.record(true);
        guitar.getReverb().setActive(true);
        guitar.getDelay().setActive(true);
        guitar.getChorus().setActive(true);
        guitar.getLfo().setActive(true);
        guitar.getEq().setActive(true);
        guitar.getCutFilter().setActive(true);
        fluid.getLfo().setActive(true);
        mains.setOnMute(false);
        int timer = 777;
        Constants.timer(timer, () ->{
        	c.record(false);
        	jamstik.toggle();
        	guitar.getLfo().setActive(false);
        	guitar.getReverb().setActive(false);
	        guitar.getDelay().setActive(false);
	        guitar.getChorus().setActive(false);
	        guitar.getEq().setActive(false);
	        guitar.getCutFilter().setActive(false);
	        guitar.getGain().setPan(50);
	        MainFrame.setFocus(guitar);
        });
        Constants.timer(timer * 2, () -> {
        		mains.setOnMute(true);
        		if (jamstik.isActive())
        			jamstik.toggle();
        		looper.reset();
        		mic.getReverb().setActive(false);
        		mains.getReverb().setActive(false);
        		fluid.getLfo().setActive(false);
        		fluid.getGain().setPan(50);
        		System.gc();
        		Constants.timer(30, () -> {
        			SettableCombo.highlight(null);
        			mains.setOnMute(false);});});
        }
    
    
    private class ShutdownHook extends Thread {
        @Override public void run() {
            mains.setOnMute(true);
            try {
            	for (Closeable s : services)
            		s.close();
            } catch (Exception e) {
            	System.err.println(e);
            }
        }
    }
    
    public static void nextSong() {
    	JComboBox<?> setlist = midiGui.getSetlistCombo();
    	int i = setlist.getSelectedIndex() + 1;
    	if (i == setlist.getItemCount())
    		i = 0;
    	setlist.setSelectedIndex(i);
    	loadSong();
	}

    /** load currently selected song in Setlist drop down */
    public static void loadSong() {
    	new Thread(()->{
    		@SuppressWarnings("unchecked")
			Class<? extends SmashHit> target = ((Class<? extends SmashHit>)midiGui.getSetlistCombo().getSelectedItem());
    		override = true;
    		try {
		    	SmashHit song = target.getDeclaredConstructor().newInstance();
		    	if (current != null) {
		    		clock.removeListener(current);
		    		current.teardown();
		    	}
		    	current = song;
		    	clock.addListener(current);
		    	looper.clear();
		    	current.startup();
    		} catch (Exception e) {
    			RTLogger.warn(JudahZone.class, e);
    		} finally {
    			override = false;
    		}
    	}).start();
    }
    
    ////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////

    @Override
    public boolean process(JackClient client, int nframes) {

        // channels and looper will be additive
        silence(outL.getFloatBuffer());
        silence(outR.getFloatBuffer());

        if (mains.isOnMute() || !initialized) return true;

        // mix the live streams
        for (Instrument ch : instruments) {
            if (ch.isOnMute()) continue;
            ch.process(); // internal effects

            // line-in stereo pan left channel (0.5 pan = 1.0 gain)
            float gainL = (1 - ch.getPan()) * 2;
            float gainR = ch.getPan() * 2;

            // pan and mono vs. stereo
            mix(ch.getLeftPort().getFloatBuffer(), gainL, outL.getFloatBuffer());
            mix(ch.isStereo() ? ch.getRightPort().getFloatBuffer() 
            		: ch.getLeftPort().getFloatBuffer(), gainR, outR.getFloatBuffer());
        }
        // get synth and beats processed and ready for recording
        synth1.process();
        synth2.process();
        drumMachine.process();
        
        // allow looper to record and/or play loops
        looper.process();

        // mix in Internals 
        sampler.process(); // not recorded
        mixSynth(synth1);
        mixSynth(synth2);
        mixDrumMachine(drumMachine);

        // final mix bus effects:
        mains.process();

        return true;
    }
    
    private void mixDrumMachine(DrumMachine drumz) {
        float gainL = (1 - drumz.getPan()) * 4;
        float gainR = drumz.getPan() * 4;
        mix(drumz.getBuffer()[LEFT_CHANNEL], gainL, outL.getFloatBuffer()); 
        mix(drumz.getBuffer()[RIGHT_CHANNEL], gainR, outR.getFloatBuffer());  
    }
    
    private void mixSynth(JudahSynth zynth) {
        if (zynth.hasWork()) {
            float gainL = (1 - zynth.getPan()) * 2;
            float gainR = zynth.getPan() * 2;
            mix(zynth.getBuffer()[LEFT_CHANNEL], gainL, outL.getFloatBuffer());
            mix(zynth.getBuffer()[LEFT_CHANNEL], gainR, outR.getFloatBuffer());
        }
    }
    
    public void recoverMidi() { // TODO
        new Thread(() -> {
        	if (midi != null) midi.close();
            Constants.sleep(100);
            try {
                midi = new JudahMidi("JudahMidi", this, drumMachine);
            } catch (Exception e) {
                RTLogger.warn(this, e);
            }
        }).start();
    }

}

