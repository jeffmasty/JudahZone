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
import net.judah.drumz.JudahDrumz;
import net.judah.effects.Fader;
import net.judah.effects.api.PresetsDB;
import net.judah.effects.gui.FxPanel;
import net.judah.fluid.FluidSynth;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.*;
import net.judah.mixer.Channels;
import net.judah.mixer.DJJefe;
import net.judah.mixer.GMSynth;
import net.judah.mixer.Instrument;
import net.judah.mixer.Mains;
import net.judah.samples.Sampler;
import net.judah.songs.SmashHit;
import net.judah.synth.JudahSynth;
import net.judah.tracker.GridTab;
import net.judah.tracker.JudahBeatz;
import net.judah.tracker.Track;
import net.judah.util.Constants;
import net.judah.util.Icons;
import net.judah.util.JudahException;
import net.judah.util.RTLogger;
import net.judah.util.SettableCombo;

/* my jack sound system settings: jackd -P99 -dalsa -dhw:UMC1820 -r48000 -p512 -n2*/
@Log4j
public class JudahZone extends BasicClient {
    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

    @Getter private static final ArrayList<Closeable> services = new ArrayList<Closeable>();
    @Getter private static boolean initialized;
    @Getter private static Channels instruments;
    @Getter private static JackClient client;
    @Getter private static JudahMidi midi;
    @Getter private static JudahClock clock;
    @Getter private static JudahSynth synth, synth2;
    @Getter private static JudahDrumz beats, beats2;
    @Getter private static DJJefe mixer;
    @Getter private static Looper looper;
    @Getter private static JudahBeatz tracker;
    @Getter private static GridTab beatBox; 
    @Getter private static Sampler sampler;
    @Getter private static SetList setlist = new SetList();
    @Getter private static SmashHit current;
    @Getter private static Jamstik jamstik;
    @Getter private static FluidSynth fluid;
    @Getter private static Carla carla;
    @Getter private static MainFrame frame;
    @Getter private static FxPanel fxPanel;
    @Getter private static MidiGui midiGui;
    @Getter private static TimeSigGui timeSig;
    @Getter private static final PresetsDB presets = new PresetsDB();
    @Getter private static final Ports synthPorts = new Ports();
    @Getter private static final Ports drumPorts = new Ports();
    @Getter private static Mains mains;
    @Getter private static JackPort outL, outR;
    @Getter private static JackPort reverbL1, reverbL2, reverbR1, reverbR2; // external Carla reverb
    
    public static void main(String[] args) {
        try {
            new JudahZone();
            RTLogger.monitor();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private JudahZone() throws JackException, JudahException, IOException, InvalidMidiDataException, OSCSerializeException {
        super(JUDAHZONE);
        MainFrame.startNimbus();
        Runtime.getRuntime().addShutdownHook(new ShutdownHook());

        fluid = new FluidSynth(Constants.sampleRate());
        services.add(fluid); 

        start();
    }

    @Override
    protected void initialize() throws JackException {
    	client = jackclient;
    	
    	JackPort left, right;
    	left = jackclient.registerPort("guitar", AUDIO, JackPortIsInput);
    	Instrument guitar = new Instrument(Channels.GUITAR, "system:capture_1", left, Icons.load("Guitar.png"));
    	
    	left = jackclient.registerPort("mic", AUDIO, JackPortIsInput);
    	Instrument mic = new Instrument(Channels.MIC, "system:capture_4", left, Icons.load("Microphone.png"));

    	left = jackclient.registerPort("fluidL", AUDIO, JackPortIsInput);
    	right = jackclient.registerPort("fluidR", AUDIO, JackPortIsInput);
    	GMSynth fluid = new GMSynth(Channels.FLUID, FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT, left, right, Icons.load("Violin.png"));
    	
    	left = jackclient.registerPort("calfL", AUDIO, JackPortIsInput);
    	right = jackclient.registerPort("calfR", AUDIO, JackPortIsInput);
    	GMSynth calf = new GMSynth(Channels.CALF, null, null, left, right, Icons.load("Calf.png"));
    	
    	left = jackclient.registerPort("crave_in", AUDIO, JackPortIsInput);
    	Instrument crave = new Instrument(Channels.CRAVE, "system:capture_3", left, Icons.load("Crave.png"));

    	instruments = new Channels(guitar, mic, crave, fluid, calf);
    	instruments.addAll(Arrays.asList(new Instrument[] { guitar, mic, fluid, calf, crave}));
    	
        reverbR1 = jackclient.registerPort("reverbR1", AUDIO, JackPortIsOutput);
        reverbR2 = jackclient.registerPort("reverbR2", AUDIO, JackPortIsOutput);
        reverbL1 = jackclient.registerPort("reverbL1", AUDIO, JackPortIsOutput);
        reverbL2 = jackclient.registerPort("reverbL2", AUDIO, JackPortIsOutput);
        outL = jackclient.registerPort("right", AUDIO, JackPortIsOutput);
        outR = jackclient.registerPort("left", AUDIO, JackPortIsOutput);

        midi = new JudahMidi("JudahMidi", this);
        synth = new JudahSynth(1, outL, outR, "Waveform.png");
        synth2 = new JudahSynth(2, outL, outR, "Synth.png");
        beats = new JudahDrumz(outL, outR, "Beats 1", Icons.load("DrumMachine.png"));
        beats2 = new JudahDrumz(outL, outR, "Beats 2", Icons.load("Drums.png"));
        sampler = new Sampler(outL, outR);
        looper = new Looper(outL, outR, instruments, new JudahSynth[] {synth, synth2}, 
        		new JudahDrumz[] {beats, beats2});
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
        // Synth input has custom Reverb
        instruments.getFluid().setReverb(fluid.getReverb());
                
        // main output
        jack.connect(jackclient, outL.getName(), "system:playback_1");
        jack.connect(jackclient, outR.getName(), "system:playback_2");

        // Initialize presets
        instruments.initVolume();
        instruments.initMutes();
        
    }
    
    /** called by JudahMidi when initialization complete, audio and midi setup complete, start GUI now */
    public void finalizeMidi() { 
    	while (mains == null) // sync with Audio initialization
    		Constants.sleep(50);
    	clock = midi.getClock();
    	instruments.getCrave().setSync(midi.getCraveOut());
    	instruments.getFluid().setMidiOut(midi.getFluidOut());
    	instruments.getCalf().setMidiOut(midi.getCalfOut());
    	synthPorts.add(new MidiPort(synth));
    	synthPorts.add(new MidiPort(synth2));
    	synthPorts.add(new MidiPort(midi.getFluidOut()));
    	synthPorts.add(new MidiPort(midi.getCalfOut()));
    	synthPorts.add(new MidiPort(midi.getCraveOut()));
    	midi.setKeyboardSynth(synthPorts.get(synth));
    	ArrayList<Path> paths = midi.getPaths();
    	paths.add(new Path(synthPorts.get(synth), synth));
    	paths.add(new Path(synthPorts.get(midi.getFluidOut()), instruments.getFluid()));
    	paths.add(new Path(synthPorts.get(midi.getCalfOut()), instruments.getCalf()));
    	paths.add(new Path(synthPorts.get(midi.getCraveOut()), instruments.getCrave()));
    	drumPorts.add(new MidiPort(beats));
    	drumPorts.add(new MidiPort(beats2));
    	drumPorts.add(new MidiPort(midi.getFluidOut()));
    	drumPorts.add(new MidiPort(midi.getCalfOut()));

    	fxPanel = new FxPanel();
    	mixer = new DJJefe();
    	jamstik = new Jamstik(services, midi.getPaths());
    	midiGui = new MidiGui(midi, clock, jamstik);
    	tracker = new JudahBeatz(clock, midi);
    	beatBox = new GridTab(tracker); 
    	timeSig = new TimeSigGui(clock, midi, tracker);
    	frame = new MainFrame(JUDAHZONE, fxPanel, midiGui, mixer, beatBox, timeSig, tracker);
    	mains.setOnMute(false);
    	MainFrame.update(frame);
    	initialized = true;
    	/////////////////////////////////////////////////////////////////////////
        //                    now the system is live                           //
        /////////////////////////////////////////////////////////////////////////
        justInTimeCompiler();
    }

    // put algorithms through their paces
    private void justInTimeCompiler() {
    	MainFrame.updateTime();
        Fader.execute(Fader.fadeIn());
        beats.setKit("VCO");
        beats2.setKit("Sunset");
        looper.getDrumTrack().toggle();
        looper.getLoopA().getReverb().setActive(true);
        looper.getLoopB().getReverb().setActive(true);
        final Loop c = looper.getLoopC();
    	c.record(true);
        final Instrument guitar = instruments.getGuitar();
        guitar.getReverb().setActive(true);
        guitar.getDelay().setActive(true);
        guitar.getChorus().setActive(true);
        guitar.getLfo().setActive(true);
        guitar.getEq().setActive(true);
        guitar.getCutFilter().setActive(true);
        mains.setOnMute(false);
		Track hihats = tracker.getDrum2();
		hihats.setFile("HiHats");
		hihats.setPattern("closed");
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
        		if (Jamstik.isActive())
        			jamstik.toggle();
        		looper.reset();
        		looper.getLoopA().getReverb().setActive(false);
        		looper.getLoopB().getReverb().setActive(false);
        		tracker.getDrum1().setFile("Bossa1");
        		synth.getPresets().load(Constants.SYNTH.listFiles()[0]);
        		synth2.getPresets().load(Constants.SYNTH.listFiles()[1]);

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
    	JComboBox<?> setlist = midiGui.getSetlist();
    	int i = setlist.getSelectedIndex() + 1;
    	if (i == setlist.getItemCount())
    		i = 0;
    	setlist.setSelectedIndex(i);
    	loadSong();
	}

    /** load currently selected song in Setlist drop down */
    public static void loadSong() {
    	new Thread(()->{
	    	SmashHit song = ((SmashHit)midiGui.getSetlist().getSelectedItem());
	    	if (current != null)
	    		current.teardown();
	    	current = song;
	    	looper.clear();
	    	current.startup(tracker, looper, instruments, frame);
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
        silence(reverbL1.getFloatBuffer());
        silence(reverbR1.getFloatBuffer());
        silence(reverbL2.getFloatBuffer());
        silence(reverbR2.getFloatBuffer());

        if (mains.isOnMute() || !initialized) return true;

        // mix the live streams
        for (Instrument ch : instruments) {
            if (ch.isOnMute()) continue;
            ch.process(); // internal effects

            // line-in stereo pan left channel (0.5 pan = 1.0 gain)
            float gainL = (1 - ch.getPan()) * 2;
            float gainR = ch.getPan() * 2;

            // pan and mono/stereo
            mix(ch.getLeftPort().getFloatBuffer(), gainL, outL.getFloatBuffer());
            mix( ch.isStereo() ? ch.getRightPort().getFloatBuffer() 
            		: ch.getLeftPort().getFloatBuffer(), gainR, outR.getFloatBuffer());
        }
        // get synth and beats processed and ready for recording
        synth.process();
        synth2.process();
        beats.process();
        beats2.process();
        
        // allow looper to record and/or play loops
        looper.process();

        // mix in Internals 
        sampler.process(); // never recorded?
        mixSynth(synth);
        mixSynth(synth2);
        mixDrumMachine(beats);
        mixDrumMachine(beats2);

        // final mix bus effects:
        mains.process();

        return true;
    }
    private void mixDrumMachine(JudahDrumz drumz) {
    	if (!drumz.hasWork()) return;
        if (drumz.hasWork()) {
            float gainL = (1 - drumz.getPan()) * 2;
            float gainR = drumz.getPan() * 2;
            mix(drumz.getBuffer()[LEFT_CHANNEL], gainL, outL.getFloatBuffer()); 
            mix(drumz.getBuffer()[RIGHT_CHANNEL], gainR, outR.getFloatBuffer());  
        }
    	
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
                midi = new JudahMidi("JudahMidi", this);
            } catch (Exception e) {
                RTLogger.warn(this, e);
            }
        }).start();
    }

}

