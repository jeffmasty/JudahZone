package net.judah;

import static net.judah.util.AudioTools.*;
import static net.judah.util.Constants.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComboBox;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.api.BasicClient;
import net.judah.api.MidiReceiver;
import net.judah.api.SmashHit;
import net.judah.controllers.Jamstik;
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
import net.judah.midi.Midi;
import net.judah.midi.MidiInstrument;
import net.judah.midi.MidiPort;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Instrument;
import net.judah.mixer.Instruments;
import net.judah.mixer.Mains;
import net.judah.mixer.Zone;
import net.judah.samples.Sampler;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.song.Sched;
import net.judah.song.Song;
import net.judah.song.SongTab;
import net.judah.synth.JudahSynth;
import net.judah.synth.SynthDB;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.JsonUtil;
import net.judah.util.RTLogger;
import net.judah.widgets.FileChooser;
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
    @Getter private static Song current;
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
    @Getter private static SongTab songs;
    @Getter private static MainFrame frame;
    @Getter private static FxPanel fxRack;
    @Getter private static MidiGui midiGui;
    @Getter private static final ArrayList<Closeable> services = new ArrayList<Closeable>();
    @Getter private static final PresetsDB presets = new PresetsDB();
    @Getter private static final SynthDB synthPresets = new SynthDB();
    @Getter private static final Instruments instruments = new Instruments();
    @Getter private static final ArrayList<MidiReceiver> synths = new ArrayList<>();
    @Getter private static final Zone noizeMakers = new Zone();

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
    protected void initialize() throws Exception {
    	client = jackclient;
        outL = jackclient.registerPort("left", AUDIO, JackPortIsOutput);
        outR = jackclient.registerPort("right", AUDIO, JackPortIsOutput);
        mains = new Mains(outL, outR);

        drumMachine = new DrumMachine("Drums", outL, outR, "DrumMachine.png");

        JackPort left, right;
    	left = jackclient.registerPort("guitar", AUDIO, JackPortIsInput);
    	guitar = new Instrument(Constants.GUITAR, "system:capture_1", left, "Guitar.png");
    	left = jackclient.registerPort("mic", AUDIO, JackPortIsInput);
    	mic = new Instrument(Constants.MIC, "system:capture_4", left, "Microphone.png");

    	synth1 = new JudahSynth("S.One", outL, outR, "Synth.png");
        synth2 = new JudahSynth("S.Two", outL, outR, "Waveform.png");

        sampler = new Sampler(outL, outR);
        clock = new JudahClock(sampler);
		
		midi = new JudahMidi("JudahMidi", clock);
		// pause while midi ports are created
        while (midi.getFluidOut() == null)
        	Constants.sleep(20);
        left = jackclient.registerPort("fluidL", AUDIO, JackPortIsInput);
    	right = jackclient.registerPort("fluidR", AUDIO, JackPortIsInput);
    	fluid = new FluidSynth(Constants.sampleRate(), left, right, midi.getFluidOut(), true);

        fluid.setMidiPort(new MidiPort(midi.getFluidOut()));
    	while (midi.getCraveOut() == null)
    		Constants.sleep(20);
    	left = jackclient.registerPort("crave_in", AUDIO, JackPortIsInput);
    	crave = new MidiInstrument(Constants.CRAVE, Constants.CRAVE_PORT, left, midi.getCraveOut(), "Crave.png");

    	synths.add(synth1); synths.add(synth2); synths.add(crave); synths.add(fluid);
    	midi.setKeyboardSynth(synth1);

    	seq = makeTracks(clock);
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
    	instruments.addAll(Arrays.asList(new Instrument[] { guitar, mic, fluid, crave}));
        looper = new Looper(outL, outR, noizeMakers, mic, clock);
    	mixer = new DJJefe(mains, looper, noizeMakers);

    	fxRack = new FxPanel();
    	jamstik = new Jamstik(services, synths);
    	midiGui = new MidiGui(midi, clock, jamstik, sampler, synth1, synth2, fluid);
    	songs = new SongTab(seq);
    	frame = new MainFrame(JUDAHZONE, fxRack, mixer, seq, looper, songs);
    	MainFrame.setFocus(guitar);
    	loadDrumMachine();
    	clock.writeTempo(93);
    	crave.send(new Midi(JudahClock.MIDI_STOP), 0) ;
    	initialized = true;
    	///////////////////////////////////////////////////////////////////
        //                    now the system is live                     //
        ///////////////////////////////////////////////////////////////////
    	setCurrent(new Song());
    	mains.setOnMute(false);
    	//justInTimeCompiler();
    	
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

    private Seq makeTracks(JudahClock clock) throws Exception {
		TrackList drums = new TrackList();
		drums.add(new MidiTrack(drumMachine.getDrum1(), clock));
		drums.add(new MidiTrack(drumMachine.getDrum2(), clock));
		drums.add(new MidiTrack(drumMachine.getHats(), clock));
		drums.add(new MidiTrack(drumMachine.getFills(), clock));
		TrackList synths = new TrackList();
		synths.add(new MidiTrack(JudahZone.getSynth1(), clock));
		synths.add(new MidiTrack(JudahZone.getSynth2(), clock));
		synths.add(new MidiTrack(JudahZone.getCrave(), clock));
		synths.add(new MidiTrack(JudahZone.getFluid(), 1, clock));
		synths.add(new MidiTrack(JudahZone.getFluid(), 2, clock));
		synths.add(new MidiTrack(JudahZone.getFluid(), 3, clock));
		Seq result = new Seq(drums, synths); 
		clock.setSeq(result);        
		return result;
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
    @SuppressWarnings("unused")
	private void justInTimeCompiler() {
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
        		looper.getSoloTrack().solo(false);
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
    

    public static void setCurrent(Song song) {
    	if (songs != null && songs.getSongView() != null)
    		clock.removeListener(songs.getSongView());
    	current = song;
    	for (int i = 0; i < current.getMidi().size(); i++) {
    		if (current.getMidi().get(i) == null) continue;
    		File path = new File(current.getMidi().get(i));
    		if (path.isFile()) 
    			seq.get(i).load(path); 
    	}
    	List<Sched> tracks = current.getScenes().get(0).getTracks();
    	for (int i = 0; i < tracks.size(); i++) {
    		seq.get(i).getScheduler().setState(tracks.get(i));
    	}
    	
    	songs.setSong(current);
    	clock.addListener(songs.getSongView());
    	// looper.clear();?

    	// load sheet music if song name matches an available sheet music file
    	if (current.getFile() != null) {
    		String name = current.getFile().getName();
    		for (File f : Folders.getSheetMusic().listFiles())
    			if (f.getName().startsWith(name))
    				frame.sheetMusic(f);
    	}
    }

    public static void save(File f) {
    	current.setFile(f);
    	save();
    }
    
    public static void save() {
    	if (current.getFile() == null)
    		current.setFile(FileChooser.choose(Folders.getSetlist()));
    	if (current.getFile() == null)
    		return;
    	current.saveSong();
    	frame.getSongs().getSongView().fill();
    }
    
    public static Song loadSong(File f) {
    	if (f == null)
    		f = FileChooser.choose(Folders.getSetlist());
    	if (f == null) return null;
    	Song result = null;
    	SettableCombo.setOverride(true);
		try {
			result = (Song)JsonUtil.readJson(f, Song.class);
			result.setFile(f);
			setCurrent(result);
		} catch (Exception e) {
			RTLogger.warn(JudahZone.class, e);
		} finally {
			SettableCombo.setOverride(false);
		}
		return result;
    }

    public static void nextSong() {
    	JComboBox<?> setlist = midiGui.getSetlistCombo();
    	int i = setlist.getSelectedIndex() + 1;
    	if (i == setlist.getItemCount())
    		i = 0;
    	setlist.setSelectedIndex(i);
    	loadSong((File)setlist.getSelectedItem());
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
    

}

//    public void recoverMidi() { // TODO 
//        new Thread(() -> {
//        	initialized = false; 
//            try {
//            	// if (midi != null) midi.close();
//            	// Constants.sleep(60);
//            	// midi = new JudahMidi("JudahMidi", this, drumMachine, clock);
//            } catch (Exception e) {
//                RTLogger.warn(this, e);
//            }
//        }).start();
//    }

// @Getter private static Instrument piano;
// left = jackclient.registerPort("piano", AUDIO, JackPortIsInput);
// piano = new Instrument("Keys", "system:capture_2", left, "Key.png");
// instruments.add(piano);
// noizeMakers.add(piano);

