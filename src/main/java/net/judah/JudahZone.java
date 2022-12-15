package net.judah;

import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JComboBox;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.BasicClient;
import net.judah.api.SmashHit;
import net.judah.controllers.Jamstik;
import net.judah.drumkit.DrumKit;
import net.judah.drumkit.DrumMachine;
import net.judah.effects.Fader;
import net.judah.effects.api.PresetsDB;
import net.judah.effects.gui.FxPanel;
import net.judah.fluid.FluidSynth;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.MidiGui;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Ports;
import net.judah.mixer.*;
import net.judah.samples.Sampler;
import net.judah.seq.Seq;
import net.judah.synth.JudahSynth;
import net.judah.synth.SynthDB;
import net.judah.util.Constants;
import net.judah.util.RTLogger;
import net.judah.widgets.SettableCombo;

/* my jack sound system settings: 
 * jackd -P99 -dalsa -dhw:UMC1820 -r48000 -p512 -n2
 * a2jmidid -e    */
public class JudahZone extends BasicClient {
    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

    @Getter private static final ArrayList<Class<? extends SmashHit>> setlist = new ArrayList<>();
    @Getter private static boolean initialized;
    @Getter private static JackClient client;
    @Getter private static JudahMidi midi;
    @Getter private static JudahClock clock;
    @Getter private static Seq seq;
    @Getter private static JackPort outL, outR;
    @Getter private static Mains mains;
    @Getter private static Instrument guitar;
    @Getter private static Instrument mic;
    @Getter private static MidiInstrument crave;
    @Getter private static FluidSynth fluid;
    @Getter private static JudahSynth synth1;
    @Getter private static JudahSynth synth2;
    @Getter private static DrumMachine drumMachine;
    @Getter private static DJJefe mixer;
    @Getter private static Looper looper;
    @Getter private static Sampler sampler;
    @Getter private static Jamstik jamstik;
    @Getter private static SmashHit current;
    @Getter private static MainFrame frame;
    @Getter private static FxPanel fxRack;
    @Getter private static MidiGui midiGui;
    @Getter private static final ArrayList<Closeable> services = new ArrayList<Closeable>();
    @Getter private static final PresetsDB presets = new PresetsDB();
    @Getter private static final SynthDB synthPresets = new SynthDB();
    @Getter private static final Ports synthPorts = new Ports();
    @Getter private static final Ports drumPorts = new Ports();
    @Getter private static final Instruments instruments = new Instruments();
    @Getter private static final Zone noizeMakers = new Zone();
    @Getter @Setter private static boolean trigger; 
    @Getter @Setter private static boolean verse; 

    public JudahZone() throws Exception {
    	super(JUDAHZONE);
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());
        MainFrame.startNimbus();
        start();
    }
    
    public JudahZone(ArrayList<Class<? extends SmashHit>> songs) throws Exception {
    	this();
        if (songs != null)
        	for (Class<? extends SmashHit> song : songs)
        		setlist.add(song);
    }

	@Override
    protected void initialize() throws JackException {
    	client = jackclient;

        outL = jackclient.registerPort("left", AUDIO, JackPortIsOutput);
        outR = jackclient.registerPort("right", AUDIO, JackPortIsOutput);

        drumMachine = new DrumMachine("Drums", outL, outR, "DrumMachine.png");
    	midi = new JudahMidi("JudahMidi", this, drumMachine);
    	
    	JackPort left, right;
    	
    	left = jackclient.registerPort("guitar", AUDIO, JackPortIsInput);
    	guitar = new Instrument(Constants.GUITAR, "system:capture_1", left, "Guitar.png");
    	
    	left = jackclient.registerPort("mic", AUDIO, JackPortIsInput);
    	mic = new Instrument(Constants.MIC, "system:capture_4", left, "Microphone.png");

    	left = jackclient.registerPort("fluidL", AUDIO, JackPortIsInput);
    	right = jackclient.registerPort("fluidR", AUDIO, JackPortIsInput);
    	while (midi.getFluidSynth() == null) 
    		Constants.sleep(20);
    	fluid = midi.getFluidSynth();
    	fluid.setLeftPort(left);
    	fluid.setRightPort(right);
    	
    	left = jackclient.registerPort("crave_in", AUDIO, JackPortIsInput);
    	while (midi.getCraveSynth() == null)
    		Constants.sleep(20);
    	crave = midi.getCraveSynth();
    	crave.setLeftPort(left);

    	instruments.addAll(Arrays.asList(new Instrument[] { guitar, mic, fluid, crave}));
    	synth1 = new JudahSynth("S.One", outL, outR, "Synth.png");
        synth2 = new JudahSynth("S.Two", outL, outR, "Waveform.png");
        
        // sequential order for the Mixer
        noizeMakers.add(guitar);
        noizeMakers.add(mic);
        noizeMakers.add(drumMachine);
        noizeMakers.add(synth1);
        noizeMakers.add(synth2);
        noizeMakers.add(crave);
        noizeMakers.add(fluid);
        noizeMakers.initVolume();
        noizeMakers.initMutes();
        
        sampler = new Sampler(outL, outR);
        mains = new Mains(outL, outR);
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
    public void finalizeMidi(JudahClock midiClock) { 
    	while (mains == null) // sync with Audio initialization
    		Constants.sleep(30);
    	clock = midiClock;
    	looper = new Looper(outL, outR, noizeMakers, mic, clock);
    	seq = midiClock.getSeq();
    	clock.setSampler(sampler);
    	clock.addListener(looper);
    	
    	drumPorts.add(fluid.getMidiPort());
    	for (DrumKit kit : drumMachine.getKits())
    		drumPorts.add(kit.getMidiPort());
    	fxRack = new FxPanel();
    	jamstik = new Jamstik(services, synthPorts);
    	midiGui = new MidiGui(midi, clock, jamstik, sampler, synth1, synth2, fluid);
    	mixer = new DJJefe(mains, looper, noizeMakers);
    	frame = new MainFrame(JUDAHZONE, fxRack, mixer, seq, looper);
    	MainFrame.setFocus(guitar);
    	initialized = true;
    	///////////////////////////////////////////////////////////////////
        //                    now the system is live                     //
        ///////////////////////////////////////////////////////////////////
    	loadDrumMachine();
    	mains.setOnMute(false);
    	clock.writeTempo(93);
    	// justInTimeCompiler();
    }

    private void loadDrumMachine() {
        synth1.getSynthPresets().load("FeelGood");
        synth2.getSynthPresets().load("TimeX2");
        
        drumMachine.getDrum1().setKit("BBuddy");
        drumMachine.getDrum2().setKit("808");
        drumMachine.getFills().setKit("Pearl");
        drumMachine.getHats().setKit("808");
    }
    
    // put algorithms through their paces
    private void justInTimeCompiler() {
    	MainFrame.updateTime();
        Fader.execute(Fader.fadeIn());
        
        looper.getSoloTrack().toggle();
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
        guitar.getCompression().setActive(true);
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
	        guitar.getCompression().setActive(false);
        });
        Constants.timer(timer * 2, () -> {
        		mains.setOnMute(true);
        		if (Jamstik.isActive())
        			jamstik.toggle();
        		looper.reset();
        		mic.getReverb().setActive(false);
        		mains.getReverb().setActive(false);
        		fluid.getLfo().setActive(false);
        		fluid.getGain().setPan(50);
        		System.gc();
        		Constants.timer(30, () -> {
        			SettableCombo.highlight(null);
        			mains.setOnMute(false);
        		});});
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
    		SettableCombo.setOverride(true);
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
    			SettableCombo.setOverride(false);
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
    
    public void recoverMidi() { 
        new Thread(() -> {
        	if (midi != null) midi.close();
            Constants.sleep(60);
            try {
            	initialized = false; // TODO
                midi = new JudahMidi("JudahMidi", this, drumMachine, clock);
                
            } catch (Exception e) {
                RTLogger.warn(this, e);
            }
        }).start();
    }

}

// @Getter private static Instrument piano;
// left = jackclient.registerPort("piano", AUDIO, JackPortIsInput);
// piano = new Instrument("Keys", "system:capture_2", left, "Key.png");
// instruments.add(piano);
// noizeMakers.add(piano);

