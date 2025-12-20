package net.judah;

import static net.judah.util.Constants.*;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.awt.EventQueue;
import java.io.Closeable;
import java.nio.FloatBuffer;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import org.apache.log4j.Level;
import org.apache.log4j.xml.DOMConfigurator;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HammingWindow;
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
import net.judah.gui.knobs.TunerKnobs;
import net.judah.gui.scope.JudahScope;
import net.judah.looper.Loop;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiInstrument;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.Mains;
import net.judah.mixer.Zone;
import net.judah.omni.AudioTools;
import net.judah.omni.Icons;
import net.judah.omni.Threads;
import net.judah.sampler.Sampler;
import net.judah.seq.Clipboard;
import net.judah.seq.Seq;
import net.judah.seq.SynthRack;
import net.judah.seq.Trax;
import net.judah.seq.chords.Chords;
import net.judah.song.Overview;
import net.judah.song.setlist.Setlists;
import net.judah.synth.fluid.FluidSynth;
import net.judah.synth.taco.SynthDB;
import net.judah.synth.taco.TacoTruck;
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

	@Getter private static Instrument guitar;
	@Getter private static Instrument mic;
	@Getter private static MidiInstrument bass;
	@Getter private static FluidSynth fluid;
	/** midiTrack-Controlled-digital-Oscillators */
	@Getter private static TacoTruck taco, tk2;

	@Getter private static DJJefe mixer;
	@Getter private static Seq seq;
	@Getter private static DrumMachine drumMachine;
	@Getter private static Looper looper;
	@Getter private static Sampler sampler;
	@Getter private static Chords chords;
	@Getter private static MainFrame frame;
	@Getter private static MidiGui midiGui;
	@Getter private static FxPanel fxRack;
	@Getter private static Overview overview;
	@Getter private static final ArrayList<Closeable> services = new ArrayList<Closeable>();
	@Getter private static final PresetsDB presets = new PresetsDB();
	@Getter private static SynthDB synthPresets;
	@Getter private static final Setlists setlists = new Setlists();
	@Getter private static final Clipboard clipboard = new Clipboard();
	@Getter private static final MultiSelect selected = new MultiSelect();
	@Getter private static JudahScope scope;

	@Getter private static Zone instruments;
	@Getter private static final Memory mem = new Memory(STEREO, bufSize());
	@Getter static Requests requests;

	public JudahZone() throws Exception {
		super(JUDAHZONE);
		MainFrame.startNimbus();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));
		start(); // super calls initialize(), makeConnections()
	}

	public static void main(String[] args) {
		DOMConfigurator.configure(Folders.getLog4j().getAbsolutePath());
		RTLogger.setLevel(Level.DEBUG);
		try {
			new JudahZone();
		} catch (Exception e) { e.printStackTrace(); }
	}

	@Override
	protected void initialize() throws Exception {
		synthPresets = new SynthDB();
		midi = new JudahMidi();
		clock = midi.getClock();
		outL = jackclient.registerPort("left", AUDIO, JackPortIsOutput);
		outR = jackclient.registerPort("right", AUDIO, JackPortIsOutput);
		mains = new Mains();
		drumMachine = new DrumMachine(mains);
		sampler = new Sampler();
		chords = new Chords(clock);
		guitar = new Instrument(GUITAR, GUITAR_PORT,
				jackclient.registerPort("guitar", AUDIO, JackPortIsInput), "Guitar.png");
		guitar.setFilter(85, 14000);

		mic = new Instrument(MIC, MIC_PORT,
				jackclient.registerPort("mic", AUDIO, JackPortIsInput), "Microphone.png");
		mic.setFilter(85, 10000);

		while (midi.getFluidOut() == null)
			Threads.sleep(20); // wait while midi thread creates ports

		fluid = new FluidSynth(midi.getFluidOut(),
				jackclient.registerPort("fluidL", AUDIO, JackPortIsInput),
				jackclient.registerPort("fluidR", AUDIO, JackPortIsInput));

		while (midi.getCraveOut() == null)
			Threads.sleep(20);
		bass = new MidiInstrument(BASS, CRAVE_PORT,
				jackclient.registerPort("crave_in", AUDIO, JackPortIsInput), "Crave.png", midi.getCraveOut());
		bass.setFilter(40, 10000);

		taco = new TacoTruck(Trax.TK1.getName(), Icons.get("taco.png"));
		tk2 = new TacoTruck(Trax.TK2.getName(), Icons.get("Waveform.png"));

		SynthRack.register(bass);
		SynthRack.register(taco);
		SynthRack.register(tk2);
		SynthRack.register(fluid);

		// recordable instruments, sequential order for the Mixer
		instruments = new Zone(guitar, mic, bass, taco, fluid);

		requests = new Requests(jackclient);
		EventQueue.invokeLater(() -> gui());
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
		jack.connect(jackclient, outL.getName(), LEFT_OUT);
		jack.connect(jackclient, outR.getName(), RIGHT_OUT);
	}

	private void gui() {
		looper = new Looper(instruments, mic, clock, mem);
		seq = new Seq(drumMachine, chords, sampler, bass, mains);
		mixer = new DJJefe(clock, mains, looper, instruments, drumMachine, fluid, sampler, tk2);
		midiGui = new MidiGui(taco, fluid, bass, clock, midi.getJamstik(), sampler, setlists);
		overview = new Overview(JUDAHZONE, seq);
		fxRack = new FxPanel(selected);
		scope = new JudahScope();
		frame = new MainFrame(JUDAHZONE, clock, fxRack, mixer, seq, looper, overview,
				midiGui, drumMachine, guitar, presets, setlists, chords, sampler, scope);

		clock.setTempo(93);
		drumMachine.init("Drumz");
		for (LineIn i : instruments) i.getGui();
		for (Loop l : looper) l.getGui();
		EventQueue.invokeLater(()->{
			overview.newSong();
			System.gc();
			Fader.execute(Fader.fadeIn());
			initialized = true;
			////////////////////////////
			// now the system is live //
			////////////////////////////
			RTLogger.log(this, "Greetings Prof. Falken.");
			test();
		});
	}

	@Override protected void registerPort(Request req) throws JackException {
		JackPort port = jackclient.registerPort(req.portName(), req.type(), req.inOut());
		Threads.execute(()->req.callback().ready(req, port));
	}

	public static void error() {
		final int FFT_SIZE = 4096; // any power of 2 should work
	    final FFT noWindow = new FFT(FFT_SIZE); // pass
	    final FFT withWindow = new FFT(FFT_SIZE, new HammingWindow()); // cause of later error

	    final float[] test1 = new float[FFT_SIZE * 2];
	    final float[] test2 = new float[FFT_SIZE * 2];
	    float[] magnitudes; // FFT_SIZE / 2
	    float max; // test var

		// generate a 440 Hz tone at amplitude 0.6
		final float[] sinWave = new float[FFT_SIZE];
		final double S_RATE = 48000.0; // any should work
	    final double TWO_PI = 2.0 * Math.PI;
		final double hz = 440.0;
	    final double amplitude = 0.6;
	    final double step = TWO_PI * hz / S_RATE;
		double phase = 0;
	    for (int i = 0; i < sinWave.length; i++) {
	        sinWave[i] = (float) (amplitude * Math.sin(phase));
	        phase += step;
	        if (phase >= TWO_PI) // clamp
	        	phase -= TWO_PI * Math.floor(phase / TWO_PI);
	    }

	    // copy audio into first half of test arrays  (zeros are in last half)
		System.arraycopy(sinWave, 0, test1, 0, FFT_SIZE);
		System.arraycopy(sinWave, 0, test2, 0, FFT_SIZE);

		max = 0;
		noWindow.forwardTransform(test1);
		magnitudes = new float[FFT_SIZE / 2];
		noWindow.modulus(test1, magnitudes);
		for (int i = 0 ; i < magnitudes.length; i++) {
		    float m = magnitudes[i];
		    if (!Float.isFinite(m) || m < 0f)
		    	throw new InvalidParameterException(i + ": " + m);
		    if (m > max)
		    	max = m;
		}
		assert max > 0;
		System.out.println("no-window transform passed: " + max);

		max = 0;
		withWindow.forwardTransform(test2); // <-- window causes error
		magnitudes = new float[FFT_SIZE / 2];
		withWindow.modulus(test2, magnitudes);
		for (int i = 0 ; i < magnitudes.length; i++) {
		    float m = magnitudes[i];
		    if (!Float.isFinite(m) || m < 0f)
		    	throw new InvalidParameterException(i + ": " + m);
		    if (m > max)
		    	max = m;
		}
		assert max > 0;
		System.out.println("window transform passed: " + max);
	}

	public static void test() {
		// error();
	}

// 		ConvolutionExample.main(new String[0]);

//		KJFFT fft = new KJFFT(Constants.bufSize());
//		float[] freqs = fft.calculateFrequencyTable(Constants.sampleRate());
//		for (float f : freqs)
//			System.out.println(f);
//		System.out.println("LENGTH: " + freqs.length);

//		LFO fx = guitar.getLfo();
//		int idx = LFO.Settings.MSec.ordinal();
//		for (int i = 0; i <= 100; i++) {
//			fx.set(idx, i); // logarithmic
//			RTLogger.log(this,  fx.get(idx) + " --> " + fx.getFrequency());
//		}

//		CabSim test = null;
//		for (Effect fx : taco)
//			if (fx instanceof CabSim sim)
//				test = sim;
//
//		if (test == null) {
//			test = new CabSim();
//			test.set(CabSim.Settings.Preset.ordinal(), 0);
//			taco.add(test);
//			return;
//		}
//
//		test.setActive(!test.isActive());
//		RTLogger.log("TEST", "IR Active? " + test.isActive());
//	}

	/** put algorithms through their paces */
	public static void justInTimeCompiler() {

		looper.onDeck(looper.getSoloTrack());
		looper.getSoloTrack().solo(true);
		mains.getReverb().setActive(true);
		final float restore = mains.getGain().getGain();
		mains.getGain().setGain(0.05f);
		mains.setOnMute(false);
		mic.getReverb().setActive(true);
		final Effect[] fx = { guitar.getReverb(), guitar.getDelay(), guitar.getChorus(), guitar.getLfo(),
				guitar.getEq(), guitar.getHiCut(), guitar.getLoCut(), guitar.getCompression() };
		for (Effect effect : fx)
			effect.setActive(true);
		looper.trigger(looper.getLoopC());
		int timer = 777;
		Threads.timer(timer, () -> {
			looper.getLoopC().capture(false);
			mic.getLfo2().setActive(true);
			midi.getJamstik().toggle();
			for (Effect effect : fx)
				effect.setActive(false);
			guitar.getGain().set(Gain.PAN, 25);
		});
		Threads.timer(timer * 2 + 100, () -> {
			if (midi.getJamstik().isActive())
				midi.getJamstik().toggle();
			looper.delete();
			looper.getSoloTrack().solo(false);
			// looper.get(0).load("Satoshi2", true); // load loop from disk
			mains.getReverb().setActive(false);
			guitar.getGain().set(Gain.PAN, 50);
			mic.getReverb().setActive(false);
			mic.getLfo2().setActive(false);
			mains.getGain().setGain(restore);
			// try { Tape.toDisk(sampler.get(7).getRecording(),
			// new File("/home/judah/djShadow.wav"), sampler.get(7).getLength());
			//	Threads.timer(timer * 4, () -> {
			//		looper.delete(); }); // clear loop loaded from disk
			// } catch (Throwable t) { RTLogger.warn("JudahZone.JIT", t); }
		});
	}

	static void shutdown() {
		if (mains != null)
			mains.setOnMute(true);
		for (int i = services.size() - 1; i >= 0; i--)
			try {
				services.get(i).close();
			} catch (Exception e) {
				System.err.println(e);
			}
	}

	//////////////////////////////
	//		PROCESS AUDIO		//
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
				mic.process(left, right);
				mains.process(left, right);
			}
			if (clock.isActive())
				looper.silently(); // keep looper in sync w/ clock
			return true;
		}

		// mix the live streams
		guitar.process(left, right);
		mic.process(left, right);
		SynthRack.process(left, right);
		drumMachine.process(left, right);
		looper.process(left, right);  // looper records and/or plays loops
		sampler.process(left, right); // not recorded
		mains.process(left, right);	  // final mix bus effects
		mixer.monitor(2); 	// collect 2 channels of dB feedback for mixer panel per cycle
		if (MainFrame.getKnobs() instanceof TunerKnobs waves)
			waves.process();
		scope.process();

		requests.process();
		return true;
	}


}
