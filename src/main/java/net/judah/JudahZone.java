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
import net.judah.fx.Effect;
import net.judah.fx.Fader;
import net.judah.fx.Gain;
import net.judah.fx.PresetsDB;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.knobs.MidiGui;
import net.judah.gui.settable.SongsCombo;
import net.judah.gui.widgets.FileChooser;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.MidiInstrument;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Instrument;
import net.judah.mixer.Mains;
import net.judah.mixer.Zone;
import net.judah.seq.Seq;
import net.judah.seq.TrackList;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.song.Song;
import net.judah.song.SongTab;
import net.judah.song.setlist.Setlists;
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
    @Getter private static Song current;
    @Getter private static JackClient client;
    @Getter private static JudahMidi midi;
    @Getter private static JudahClock clock;
    @Getter private static Seq seq;
    @Getter private static ChordTrack chords;
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
    @Getter private static MainFrame frame;
    @Getter private static MidiGui midiGui;
    @Getter private static FxPanel fxRack;
    @Getter private static SongTab songs;
    @Getter private static final ArrayList<Closeable> services = new ArrayList<Closeable>();
    @Getter private static final PresetsDB presets = new PresetsDB();
    @Getter private static final SynthDB synthPresets = new SynthDB();
    @Getter private static final ArrayList<MidiReceiver> synths = new ArrayList<>();
    @Getter private static final Zone instruments = new Zone();
    @Getter private static Setlists setlists = new Setlists();
    
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
    	chords = new ChordTrack(clock);
        midi = new JudahMidi(clock);

        left = jackclient.registerPort("fluidL", AUDIO, JackPortIsInput);
    	right = jackclient.registerPort("fluidR", AUDIO, JackPortIsInput);
        while (midi.getFluidOut() == null)
        	Constants.sleep(20); // wait while midi thread creates ports
    	fluid = new FluidSynth(Constants.sampleRate(), left, right, midi.getFluidOut());

    	left = jackclient.registerPort("crave_in", AUDIO, JackPortIsInput);
    	while (midi.getCraveOut() == null)
    		Constants.sleep(20);
    	crave = new MidiInstrument(Constants.CRAVE, Constants.CRAVE_PORT, 
    			left, midi.getCraveOut(), "Crave.png");
    	crave.setMono();
    	crave.setFactor(1.5f);

    	synths.add(synth1); synths.add(synth2); synths.add(crave); synths.add(fluid);

    	seq = makeTracks(clock);
    	midi.setKeyboardSynth(seq.getSynthTracks().get(0));

        // sequential order for the Mixer
    	instruments.add(guitar);
        instruments.add(mic);
        instruments.add(drumMachine);
        instruments.add(synth1);
        instruments.add(synth2);
        instruments.add(fluid);
        instruments.add(crave);
        instruments.init();
        looper = new Looper(outL, outR, instruments, mic, clock, jackclient);
    	mixer = new DJJefe(mains, looper, instruments, drumMachine.getKits(), sampler);
    	
    	fxRack = new FxPanel();
    	songs = new SongTab(clock, chords.getView(), setlists, seq, looper, mixer);
    	jamstik = new Jamstik(services, synths);
    	midiGui = new MidiGui(midi, clock, jamstik, sampler, synth1, synth2, fluid, seq, setlists);
    	frame = new MainFrame(JUDAHZONE, clock, fxRack, mixer, seq, looper, songs, chords);
    	crave.send(new Midi(JudahClock.MIDI_STOP), 0);
    	clock.writeTempo(93);
    	setCurrent(new Song(seq, (int)clock.getTempo()));
    	guitar.setMuteRecord(false);
    	synth1.setMuteRecord(false);
    	Constants.timer(10, ()-> {
    		initSynths();
    		seq.loadDrumMachine();
    		MainFrame.setFocus(guitar); 
    		//justInTimeCompiler();
    		}); 
    	System.gc();
    	mains.setOnMute(false);
    	Fader.execute(Fader.fadeIn());    	
    	initialized = true;
    	////////////////////////////////////////////////////////////
        //                 now the system is live                 //
        ////////////////////////////////////////////////////////////
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
		mpk.add(new MidiTrack(synth1, clock));
		mpk.add(new MidiTrack(synth2, clock));
		mpk.add(new MidiTrack(fluid, 0, clock));
		mpk.add(new MidiTrack(fluid, 1, clock));
		mpk.add(new MidiTrack(fluid, 2, clock));
		mpk.add(new MidiTrack(crave, clock));
		TrackList synths = new TrackList(mpk);

		Seq result = new Seq(drums, synths, chords); 
		clock.setSeq(result); 
		return result;
    }
    
    private void initSynths() {
        synth1.progChange("FeelGood");
		synth2.progChange("Drops1");
		seq.byName(synth1.getName()).load("0s");
		seq.byName(synth2.getName()).load("16ths");
		seq.byName("Fluid1").load("8ths");
		seq.byName("Fluid2").load("CRDSKNK");
		seq.byName("Fluid3").load("Synco");
		seq.byName("Bass").load("Bass2");
		while (fluid.getPatches() == null)
			Constants.sleep(100);
    	fluid.progChange("Strings", 0);
        fluid.progChange("Palm Muted Guitar", 1);
        fluid.progChange("Harp", 2);
    }
    
    public static void setCurrent(Song song) {
    	looper.clear();
    	clock.reset();
    	drumMachine.getKits().forEach(kit->kit.reset());

    	current = song;
    	clock.setTimeSig(current.getTimeSig());
    	seq.loadTracks(current.getTracks());
    	mixer.loadFx(current.getFx());
    	mixer.mutes(current.getRecord());
    	songs.setSong(current);
    	chords.load(current);
    	
    	frame.getHq().sceneText();
    	frame.getMiniSeq().update();
    	SongsCombo.refresh();

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
    		current.setFile(FileChooser.choose(setlists.getDefault()));
    		if (current.getFile() == null)
    			SongsCombo.refill();
    			return;
    	}
    	current.saveSong(mixer, seq, songs.getCurrent(), chords);
    	frame.getTabs().title(songs);
    }

    /** reload from disk, re-set current scene */
    public static void reload() {
		int idx = current.getScenes().indexOf(frame.getSongs().getCurrent());
		Song newSong = JudahZone.loadSong(current.getFile());
		if (newSong == null)
			return;
		MainFrame.setFocus(newSong.getScenes().get(idx));
    }
    
    public static Song loadSong(File f) {
    	if (f == null)
    		f = FileChooser.choose(setlists.getDefault());
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

    // put algorithms through their paces
	public static void justInTimeCompiler() {

		looper.onDeck(looper.getSoloTrack());
    	looper.getSoloTrack().solo(true);
    	mains.getReverb().setActive(true);
    	final float restore = mains.getGain().getGain();
    	mains.getGain().setGain(0.05f);
    	mains.setOnMute(false);
    	mic.getReverb().setActive(true);
		final Effect[] fx = {guitar.getReverb(), guitar.getDelay(), guitar.getChorus(), 
				guitar.getLfo(), guitar.getEq(), guitar.getParty(), guitar.getCompression()};
    	for (Effect effect : fx)
    		effect.setActive(true);
		looper.getLoopC().trigger();
        fluid.getLfo().setActive(true);
        int timer = 777;
        Constants.timer(timer, () ->{
        	looper.getLoopC().record(false);
        	jamstik.toggle();
        	for (Effect effect : fx)
        		effect.setActive(false);
	        guitar.getGain().set(Gain.PAN, 25);
        });
        Constants.timer(timer * 2 + 100, () -> {
    		if (Jamstik.isActive())
    			jamstik.toggle();
    		looper.clear();
    		mains.getReverb().setActive(false);
    		fluid.getLfo().setActive(false);
    		looper.getSoloTrack().solo(false);
    		guitar.getGain().set(Gain.PAN, 50);
    		mic.getReverb().setActive(false);
    		mains.getGain().setGain(restore);
	    	// Chord.test(); 
			// try { Tape.toDisk(sampler.get(7).getRecording(), new File("/home/judah/djShadow.wav"), 
			//	 sampler.get(7).getLength()); } catch (Throwable t) { RTLogger.warn("JudahZone.JIT", t); }
        	looper.get(0).load("Satoshi2", true);
        });
    }

    static void shutdown() {
    	mains.setOnMute(true);
    	for (int i = services.size() - 1; i >= 0; i--)
    		try {services.get(i).close();}
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

        if (!initialized) return true;
        if (mains.isOnMute()) {
        	if (mains.isHotMic()) {
        		mic.process();
        		mix(mic.getLeft(), outL.getFloatBuffer());
        		mix(mic.getRight(), outR.getFloatBuffer());
        		mains.process();
        	}
        	return true;
        }

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

// @Getter private static Instrument piano;
// left = jackclient.registerPort("piano", AUDIO, JackPortIsInput);
// piano = new Instrument("Keys", "system:capture_2", left, "Key.png");
// instruments.add(piano);

