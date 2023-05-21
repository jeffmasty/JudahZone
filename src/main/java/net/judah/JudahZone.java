package net.judah;

import static net.judah.util.AudioTools.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.io.Closeable;
import java.io.File;
import java.util.ArrayList;

import javax.swing.JComboBox;

import org.apache.log4j.xml.DOMConfigurator;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.BasicClient;
import net.judah.api.MidiReceiver;
import net.judah.controllers.Jamstik;
import net.judah.drumkit.DrumMachine;
import net.judah.drumkit.Sampler;
import net.judah.fluid.FluidSynth;
import net.judah.fx.Fader;
import net.judah.fx.Gain;
import net.judah.fx.PresetsDB;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.knobs.MidiGui;
import net.judah.gui.settable.Songs;
import net.judah.gui.widgets.FileChooser;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.MidiInstrument;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Instrument;
import net.judah.mixer.Mains;
import net.judah.mixer.Zone;
import net.judah.seq.MidiTrack;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.song.Song;
import net.judah.song.SongTab;
import net.judah.synth.JudahSynth;
import net.judah.synth.SynthDB;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.JsonUtil;
import net.judah.util.RTLogger;

/* my jack sound system settings: 
 * jackd -P99 -dalsa -dhw:UMC1820 -r48000 -p512 -n2
 * a2jmidid -e    */
public class JudahZone extends BasicClient {
    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

    @Setter @Getter private static boolean initialized;
    @Getter private static JackClient client;
    @Getter private static JudahMidi midi;
    @Getter private static JudahClock clock;
    @Getter private static JackPort outL, outR;
    @Getter private static Mains mains;
    @Getter private static Song current;
    @Getter private static Seq seq;
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
    @Getter private static MainFrame frame;
    @Getter private static MidiGui midiGui;
    @Getter private static FxPanel fxRack;
    @Getter private static SongTab songs;
    @Getter private static final ArrayList<Closeable> services = new ArrayList<Closeable>();
    @Getter private static final PresetsDB presets = new PresetsDB();
    @Getter private static final SynthDB synthPresets = new SynthDB();
    @Getter private static final ArrayList<MidiReceiver> synths = new ArrayList<>();
    @Getter private static final Zone instruments = new Zone();
    
    public JudahZone() throws Exception {
    	super(JUDAHZONE);
        MainFrame.startNimbus();
        Runtime.getRuntime().addShutdownHook(new Thread(()-> shutdown() ));
        start();
        RTLogger.monitor();
    }
    
    public static void main(String[] args) {
    	DOMConfigurator.configure(Folders.getLog4j().getAbsolutePath());
		try {
			new JudahZone();
		} catch (Exception e) {
			e.printStackTrace();
		}
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

    	synth1 = new JudahSynth(JudahSynth.NAMES[0], outL, outR, "Synth.png");
        synth2 = new JudahSynth(JudahSynth.NAMES[1], outL, outR, "Waveform.png");
        sampler = new Sampler(outL, outR);
        clock = new JudahClock(sampler);
		midi = new JudahMidi(clock);

		while (midi.getFluidOut() == null)
        	Constants.sleep(20); // wait while midi thread creates ports
		
        left = jackclient.registerPort("fluidL", AUDIO, JackPortIsInput);
    	right = jackclient.registerPort("fluidR", AUDIO, JackPortIsInput);
    	fluid = new FluidSynth(Constants.sampleRate(), left, right, midi.getFluidOut());
    	while (midi.getCraveOut() == null)
    		Constants.sleep(20);
    	left = jackclient.registerPort("crave_in", AUDIO, JackPortIsInput);
    	crave = new MidiInstrument(Constants.CRAVE, Constants.CRAVE_PORT, 
    			left, midi.getCraveOut(), "Crave.png");

    	synths.add(synth1); synths.add(synth2); synths.add(crave); synths.add(fluid);
    	seq = makeTracks(clock);
    	midi.setKeyboardSynth(seq.getSynthTracks().get(0));

        // sequential order for the Mixer
    	instruments.add(guitar);
        instruments.add(mic);
        instruments.add(drumMachine);
        instruments.add(synth1);
        instruments.add(synth2);
        instruments.add(crave);
        instruments.add(fluid);
        instruments.init();
        looper = new Looper(outL, outR, instruments, mic, clock);
    	mixer = new DJJefe(mains, looper, instruments, drumMachine.getKits(), sampler);
    	
    	fxRack = new FxPanel();
    	songs = new SongTab();
    	jamstik = new Jamstik(services, synths);
    	midiGui = new MidiGui(midi, clock, jamstik, sampler, synth1, synth2, fluid, seq);
    	frame = new MainFrame(JUDAHZONE, fxRack, mixer, seq, looper, songs);
    	crave.send(new Midi(JudahClock.MIDI_STOP), 0);
    	clock.writeTempo(93);
    	initialized = true;
    	////////////////////////////////////////////////////////////
        //                 now the system is live                 //
        ////////////////////////////////////////////////////////////
    	setCurrent(new Song());
    	MainFrame.setFocus(guitar);
    	Constants.timer(10, ()->seq.loadDrumMachine()); 
    	Fader.execute(Fader.fadeIn());    	
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
        for (Instrument ch : new Instrument[] { guitar, mic, fluid, crave}) {
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
		ArrayList<MidiTrack> bangers = new ArrayList<>();
		bangers.add(new MidiTrack(drumMachine.getDrum1(), clock));
		bangers.add(new MidiTrack(drumMachine.getDrum2(), clock));
		bangers.add(new MidiTrack(drumMachine.getHats(), clock));
		bangers.add(new MidiTrack(drumMachine.getFills(), clock));
    	TrackList drums = new TrackList(bangers);
		
		ArrayList<MidiTrack> mpk = new ArrayList<>();
		mpk.add(new MidiTrack(JudahZone.getSynth1(), clock));
		mpk.add(new MidiTrack(JudahZone.getSynth2(), clock));
		mpk.add(new MidiTrack(JudahZone.getCrave(), clock));
		mpk.add(new MidiTrack(JudahZone.getFluid(), 1, clock));
		mpk.add(new MidiTrack(JudahZone.getFluid(), 2, clock));
		mpk.add(new MidiTrack(JudahZone.getFluid(), 3, clock));
		TrackList synths = new TrackList(mpk);

		Seq result = new Seq(drums, synths); 
		clock.setSeq(result); 
		synth1.progChange("FeelGood");
		synth2.progChange("Drops1");

		Constants.execute(() -> {
        	while (fluid.getPatches() == null)
        		Constants.sleep(500);
        	fluid.progChange("Rhodes EP", 1);
	        fluid.progChange("Rock Organ", 2);
	        fluid.progChange("Harp", 3);
        });
		return result;
    }
    
    // put algorithms through their paces
    @SuppressWarnings("unused")
	private void justInTimeCompiler() {
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
        guitar.getParty().setActive(true);
        guitar.getCompression().setActive(true);
        fluid.getLfo().setActive(true);
        
        int timer = 777;
        Constants.timer(timer, () ->{
        	c.record(false);
        	jamstik.toggle();
        	guitar.getLfo().setActive(false);
        	guitar.getReverb().setActive(false);
	        guitar.getDelay().setActive(false);
	        guitar.getChorus().setActive(false);
	        guitar.getEq().setActive(false);
	        guitar.getParty().setActive(false);
	        guitar.getGain().set(Gain.PAN, 50);
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
        		fluid.getGain().set(Gain.PAN, 50);
        		System.gc();
        		Constants.timer(50, () -> mains.setOnMute(false));});
    }
    
    public static void setCurrent(Song song) {
    	looper.flush();
    	clock.syncFlush();
    	current = song;
    	seq.loadTracks(current.getTracks());
    	mixer.loadFx(current.getFx());
    	songs.setSong(current);
    	frame.getMiniSeq().update();
    	Songs.refresh();

    	// load sheet music if song name matches an available sheet music file
    	if (current.getFile() != null) {
    		String name = current.getFile().getName();
    		for (File f : Folders.getSheetMusic().listFiles())
    			if (f.getName().startsWith(name))
    				frame.sheetMusic(f);
    	}
    	frame.getTabs().title(songs);
    }

    public static void save(File f) {
    	current.setFile(f);
    	save();
    }
    
    public static void save() {
    	if (current.getFile() == null) {
    		current.setFile(FileChooser.choose(Folders.getSetlist()));
    		if (current.getFile() == null)
    			Songs.refill();
    			return;
    	}
    	current.saveSong(mixer, seq, songs.getCurrent());
    	frame.getTabs().title(songs);
    }

    /** reload from disk, re-set current scene */
    public static void reload() {
		int idx = current.getScenes().indexOf(frame.getSongs().getCurrent());
		Song newSong = JudahZone.loadSong(current.getFile());
		MainFrame.setFocus(newSong.getScenes().get(idx));
    }
    
    public static Song loadSong(File f) {
    	if (f == null)
    		f = FileChooser.choose(Folders.getSetlist());
    	if (f == null) return null;
    	Song result = null;
		try {
			result = (Song)JsonUtil.readJson(f, Song.class);
			result.setFile(f);
			setCurrent(result);
		} catch (Exception e) {
			RTLogger.warn(JudahZone.class, e);
		}
		return result;
    }

    public static void nextSong() {
    	JComboBox<?> setlist = midiGui.getSongsCombo();
    	int i = setlist.getSelectedIndex() + 1;
    	if (i == setlist.getItemCount())
    		i = 0;
    	setlist.setSelectedIndex(i);
    	loadSong((File)setlist.getSelectedItem());
	}

    static void shutdown() {
    	mains.setOnMute(true);
    	for (Closeable s : services)
    		try { s.close(); } 
    	catch (Exception e) { System.err.println(e); }
    }
    
    ////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////

    @Override
    public boolean process(JackClient client, int nframes) {

        // channels and looper will be additive
        silence(outL.getFloatBuffer());
        silence(outR.getFloatBuffer());

        if (mains.isOnMute() || !initialized) 
        	return true;

        // mix the live streams
        for (Instrument ch : instruments.getInstruments()) {
        	if (ch.isOnMute()) continue;
        	ch.process(); // internal effects
        	mix(ch.getLeft(), outL.getFloatBuffer());
        	mix(ch.getRight(), outR.getFloatBuffer());
        }
        drumMachine.process();
        synth1.process();
        synth2.process();

        // allow looper to record and/or play loops
        looper.process();
        sampler.process(); // not recorded

        // final mix bus effects
        mains.process();
        return true;
    }
	
}



//    public void recoverMidi() { // TODO 
//        	initialized = false; 
//            try { // if (midi != null) midi.close();
//            	// Constants.sleep(60);
//            	// midi = new JudahMidi("JudahMidi", this, drumMachine, clock);
//            } catch (Exception e) {RTLogger.warn(this, e);}}

// @Getter private static Instrument piano;
// left = jackclient.registerPort("piano", AUDIO, JackPortIsInput);
// piano = new Instrument("Keys", "system:capture_2", left, "Key.png");
// instruments.add(piano);

