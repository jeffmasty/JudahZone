package net.judah;

import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.io.Closeable;
import java.nio.FloatBuffer;
import java.util.ArrayList;

import org.apache.log4j.xml.DOMConfigurator;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import lombok.Setter;
import net.judah.api.BasicClient;
import net.judah.drumkit.DrumMachine;
import net.judah.fx.Effect;
import net.judah.fx.Fader;
import net.judah.fx.Gain;
import net.judah.fx.PresetsDB;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.knobs.MidiGui;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.Midi;
import net.judah.midi.MidiInstrument;
import net.judah.midi.fluid.FluidSynth;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.Mains;
import net.judah.mixer.Zone;
import net.judah.sampler.Sampler;
import net.judah.scope.Scope;
import net.judah.seq.Seq;
import net.judah.seq.chords.ChordTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.song.Overview;
import net.judah.song.Song;
import net.judah.song.setlist.Setlists;
import net.judah.synth.JudahSynth;
import net.judah.synth.SynthDB;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.Memory;
import net.judah.util.RTLogger;

/* my jack sound system settings: 
 * jackd -P99 -dalsa -dhw:UMC1820 -r48000 -p512 -n2
 * (samples are 48khz) */
public class JudahZone extends BasicClient {
    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

	@Setter @Getter private static boolean initialized;
	@Getter private static JackPort outL, outR;
	@Getter private static Mains mains;
	@Getter private static JudahMidi midi;
	@Getter private static JudahClock clock;
	@Getter private static Seq seq;
	@Getter private static ChordTrack chords;
	@Getter private static Instrument guitar;
	@Getter private static Instrument mic;
	@Getter private static MidiInstrument bass;
	@Getter private static FluidSynth fluid;
	@Getter private static JudahSynth synth1;
	@Getter private static JudahSynth synth2;
	@Getter private static DrumMachine drumMachine;
	@Getter private static DJJefe mixer;
	@Getter private static Looper looper;
	@Getter private static Sampler sampler;
	@Getter private static Scope scope;
	@Getter private static MainFrame frame;
	@Getter private static MidiGui midiGui;
	@Getter private static FxPanel fxRack;
	@Getter private static Overview overview;
	@Getter private static final ArrayList<Closeable> services = new ArrayList<Closeable>();
	@Getter private static final PresetsDB presets = new PresetsDB();
	@Getter private static final SynthDB synthPresets = new SynthDB();
	@Getter private static final Zone instruments = new Zone();
	@Getter private static final Setlists setlists = new Setlists();
	@Getter private static final MultiSelect selected = new MultiSelect();
	@Getter private final Memory mem = new Memory(Constants.STEREO, Constants.bufSize());

	public JudahZone() throws Exception {
		super(JUDAHZONE);
		MainFrame.startNimbus();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));
		start(); // super calls initialize(), makeConnections()
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
		outL = jackclient.registerPort("left", AUDIO, JackPortIsOutput);
		outR = jackclient.registerPort("right", AUDIO, JackPortIsOutput);
		mains = new Mains(outL, outR, "Mains");
		sampler = new Sampler(outL, outR);
		clock = new JudahClock(sampler);
		chords = new ChordTrack(clock);
		midi = new JudahMidi(clock);
		drumMachine = new DrumMachine(outL, outR, clock, mains);
		synth1 = new JudahSynth(0, outL, outR, clock);
		synth2 = new JudahSynth(1, outL, outR, clock);

		guitar = new Instrument(Constants.GUITAR, Constants.GUITAR_PORT, 
				jackclient.registerPort("guitar", AUDIO, JackPortIsInput), "Guitar.png");
		mic = new Instrument(Constants.MIC, Constants.MIC_PORT, 
				jackclient.registerPort("mic", AUDIO, JackPortIsInput), "Microphone.png");

		while (midi.getFluidOut() == null)
			Constants.sleep(20); // wait while midi thread creates ports
		fluid = new FluidSynth(Constants.sampleRate(), midi.getFluidOut(), clock,
				jackclient.registerPort("fluidL", AUDIO, JackPortIsInput), 
				jackclient.registerPort("fluidR", AUDIO, JackPortIsInput));
		
		while (midi.getCraveOut() == null)
			Constants.sleep(20);
		bass = new MidiInstrument(Constants.BASS, Constants.CRAVE_PORT, 
				jackclient.registerPort("crave_in", AUDIO, JackPortIsInput), "Crave.png", midi.getCraveOut());
		bass.getTracks().add(new PianoTrack("Bass", bass, 0, clock, PianoTrack.MONOPHONIC));
		bass.setPreamp(0.75f);
		
		// sequential order for the Mixer
		instruments.add(guitar);
		instruments.add(mic);
		instruments.add(drumMachine);
		instruments.add(synth1);
		instruments.add(synth2);
		instruments.add(fluid);
		instruments.add(bass);
		// extra audio input: instruments.add(new Instrument("Keys", "system:capture_2", jackclient.registerPort("piano", AUDIO, JackPortIsInput), "Key.png"));
		instruments.init();

		looper = new Looper(outL, outR, instruments, mic, clock, mem);
		mixer = new DJJefe(clock, mains, looper, instruments, drumMachine, sampler);
		seq = new Seq(instruments, chords, clock);
		fxRack = new FxPanel(selected);
		scope = new Scope(selected, mem); // TODO
		midiGui = new MidiGui(midi, clock, sampler, synth1, synth2, fluid, seq, setlists);
		overview = new Overview(clock, chords, setlists, seq, looper, mixer);
		frame = new MainFrame(JUDAHZONE, clock, fxRack, mixer, seq, looper, 
				overview, chords, midiGui, drumMachine, sampler, scope, guitar);

		// housekeeping
		bass.send(new Midi(JudahClock.MIDI_STOP), 0);
		clock.writeTempo(93);
		overview.setSong(new Song(seq, (int) clock.getTempo()));
		drumMachine.init();
		instruments.initSynths();
		System.gc();
		guitar.setMuteRecord(false);
		synth1.setMuteRecord(false);
		mains.setOnMute(false);
		Fader.execute(Fader.fadeIn());
		initialized = true;
		////////////////////////////
		// now the system is live //
		////////////////////////////
		RTLogger.log(this, "Greetings Prof. Falken.");
	}

	@Override
	protected void makeConnections() throws JackException {
		if (mains == null) {
			RTLogger.log(this, "Initialization failed.");
			return;
		}

		// Hookup external instruments/mic
		for (LineIn input : instruments) {
			if (input instanceof Instrument == false)
				continue; // internal engine
			Instrument ch = (Instrument)input;
			if (ch.getLeftSource() != null && ch.getLeftPort() != null)
				jack.connect(jackclient, ch.getLeftSource(), ch.getLeftPort().getName());
			if (ch.getRightSource() != null && ch.getRightPort() != null)
				jack.connect(jackclient, ch.getRightSource(), ch.getRightPort().getName());
		}

		// main output
		jack.connect(jackclient, outL.getName(), Constants.LEFT_PORT);
		jack.connect(jackclient, outR.getName(), Constants.RIGHT_PORT);
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
		final Effect[] fx = { guitar.getReverb(), guitar.getDelay(), guitar.getChorus(), guitar.getLfo(),
				guitar.getEq(), guitar.getFilter1(), guitar.getFilter2(), guitar.getCompression() };
		for (Effect effect : fx)
			effect.setActive(true);
		looper.getLoopC().trigger();
		fluid.getLfo().setActive(true);
		int timer = 777;
		Constants.timer(timer, () -> {
			looper.getLoopC().record(false);
			midi.getJamstik().toggle();
			for (Effect effect : fx)
				effect.setActive(false);
			guitar.getGain().set(Gain.PAN, 25);
		});
		Constants.timer(timer * 2 + 100, () -> {
			if (midi.getJamstik().isActive())
				midi.getJamstik().toggle();
			looper.clear();
			mains.getReverb().setActive(false);
			fluid.getLfo().setActive(false);
			looper.getSoloTrack().solo(false);
			guitar.getGain().set(Gain.PAN, 50);
			mic.getReverb().setActive(false);
			mains.getGain().setGain(restore);
			// try { Tape.toDisk(sampler.get(7).getRecording(), 
			// new File("/home/judah/djShadow.wav"), sampler.get(7).getLength()); 
			// } catch (Throwable t) { RTLogger.warn("JudahZone.JIT", t); }
			looper.get(0).load("Satoshi2", true);
		});
	}

	static void shutdown() {
		mains.setOnMute(true);
		for (int i = services.size() - 1; i >= 0; i--)
			try {
				services.get(i).close();
			} catch (Exception e) {
				System.err.println(e);
			}
	}
	
	//////////////////////////////
	// 		PROCESS AUDIO 		//
	//////////////////////////////
	@Override
	public boolean process(JackClient client, int nframes) {
		FloatBuffer left = outL.getFloatBuffer();
		FloatBuffer right = outR.getFloatBuffer();
		// channels and looper will be additive
		AudioTools.silence(left);
		AudioTools.silence(right);

		if (!initialized)
			return true;
		if (mains.isOnMute()) {
			if (mains.isHotMic()) {
				mic.process();
				AudioTools.mix(mic.getLeft(), left);
				AudioTools.mix(mic.getRight(), right);
				mains.process();
			}
			if (clock.isActive())
				looper.silently(); // keep looper in sync w/ clock
			return true;
		}

		// mix the live streams
		for (Instrument ch : instruments.getInstruments()) {
			if (ch.isOnMute())
				continue;
			ch.process(); // internal effects
			AudioTools.mix(ch.getLeft(), left);
			AudioTools.mix(ch.getRight(), right);
		}
		// internal engines
		drumMachine.process();
		synth1.process();
		synth2.process();

		looper.process();  	// looper records and/or plays loops
		sampler.process(); 	// not recorded
		mains.process();	// final mix bus effects
		mixer.process(2); 	// 2 channels of db feedback on mixer panel per cycle
		scope.process();
		return true;
	}

}
