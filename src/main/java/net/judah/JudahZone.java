package net.judah;

import static judahzone.util.Constants.*;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.xml.DOMConfigurator;
import org.apache.logging.log4j.Level;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.FX.Calc;
import judahzone.api.Registrar;
import judahzone.fx.Convolution;
import judahzone.gui.Icons;
import judahzone.gui.Nimbus;
import judahzone.jnajack.ZoneJackClient;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.Memory;
import judahzone.util.RTLogger;
import judahzone.util.Services;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.channel.Instrument;
import net.judah.channel.LineIn;
import net.judah.channel.Mains;
import net.judah.controllers.MPKmini;
import net.judah.drumkit.DrumDB;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.knobs.MidiGui;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiInstrument;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Fader;
import net.judah.mixer.IRDB;
import net.judah.mixer.PresetsDB;
import net.judah.mixer.Zone;
import net.judah.sampler.SampleDB;
import net.judah.sampler.Sampler;
import net.judah.seq.Seq;
import net.judah.seq.SynthRack;
import net.judah.seq.Trax;
import net.judah.seq.chords.Chords;
import net.judah.song.Overview;
import net.judah.song.setlist.Setlists;
import net.judah.synth.fluid.FluidSynth;
import net.judah.synth.taco.SynthDB;
import net.judah.synth.taco.TacoTruck;

/* my jack sound system settings:
 * jackd -P99 -dalsa -dhw:UMC1820 -r48000 -p512 -n2
 * (samples are 48khz) */
public class JudahZone extends ZoneJackClient implements Registrar {
	public static final String JUDAHZONE = JudahZone.class.getSimpleName();
	public static JudahZone getInstance() { return instance; }
	private static JudahZone instance;
	private static boolean initialized; // abort
	public static boolean isInitialized() { return initialized; }
	public static void setInitialized(boolean initialized) {JudahZone.initialized = initialized; }

	private JackPort outL, outR;
	@Getter private Mains mains;

	@Getter private JudahMidi midi;
	private JudahClock clock;
	@Getter private Seq seq;
	@Getter private DrumMachine drumMachine;
	@Getter private Looper looper;
	@Getter private Sampler sampler;
	@Getter private Chords chords;

	@Getter private MainFrame frame;
	@Getter private DJJefe mixer;
	@Getter private MidiGui midiGui;
	@Getter private FxPanel fxRack;
	@Getter private Overview overview;
	@Getter private MPKmini mpkMini;
	@Getter private Zone instruments;
	@Getter private final Setlists setlists = new Setlists();
	private final MultiSelect selected = new MultiSelect();

	/** midiTrack-Controlled-digital-Oscillators */
	@Getter private TacoTruck taco;
	@Getter private TacoTruck tk2;

	// custom
	@Getter private Instrument guitar;
	@Getter private Instrument mic;
	@Getter private MidiInstrument bass;
	@Getter private FluidSynth fluid;

	/** hot buffer, The Product */
	private final float[] left = new float[Constants.bufSize()];
	/** hot buffer, The Product */
	private final float[] right = new float[Constants.bufSize()];

	/** Thread-safe list of analyzers; each consumes left/right float[] frames. */
	private final CopyOnWriteArrayList<Calc<?>> analyzers = new CopyOnWriteArrayList<>();
	/** Register/unregister analyzers (e.g. tuner::process or transformer::process). */
	@Override
	public void registerAnalyzer(Calc<?> a) { analyzers.addIfAbsent(a); }
	@Override
	public void unregisterAnalyzer(Calc<?> a) { analyzers.remove(a); }

	private final ExecutorService dspExec = Executors.newFixedThreadPool(
	            Runtime.getRuntime().availableProcessors(),
	            r -> {	Thread t = new Thread(r, "DSP-Worker");
	                	t.setDaemon(true);
	                	return t; });
	private final List<Callable<Void>> threads = new ArrayList<>();

	private JudahZone() throws Exception {
		super(JUDAHZONE);
		Nimbus.start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));
		Threads.execute(() -> SynthDB.init(Folders.getSynthPresets()));
		Threads.execute(() -> PresetsDB.init(Folders.getPresetsFile()));
		Threads.execute(() -> DrumDB.init());
		Threads.execute(() -> SampleDB.init());
		Threads.execute(() -> Convolution.setIRDB(IRDB.instance) );

		start(); // super calls initialize(), makeConnections()
		instance = this;
	}

	public static void main(String[] args) {
		DOMConfigurator.configure(Folders.getLog4j().getAbsolutePath());
		RTLogger.setLevel(Level.DEBUG /* INFO */);
		try {
			new JudahZone();
		} catch (Exception e) { e.printStackTrace(); }
	}

	@Override
	protected void initialize() throws Exception {
		mpkMini = new MPKmini(this);
		midi = new JudahMidi(this);
		clock = JudahMidi.getClock();
		outL = jackclient.registerPort("left", AUDIO, JackPortIsOutput);
		outR = jackclient.registerPort("right", AUDIO, JackPortIsOutput);

		mains = new Mains();
		drumMachine = new DrumMachine(mains);
		chords = new Chords(this, clock);
		taco = new TacoTruck(Trax.TK1.getName(), Icons.get("taco.png"));
		tk2 = new TacoTruck(Trax.TK2.getName(), Icons.get("Waveform.png"));

// custom defines
		guitar = new Instrument(GUITAR, GUITAR_PORT, jackclient.registerPort(
				"guitar", AUDIO, JackPortIsInput), "Guitar.png", 85, 12000);

		mic = new Instrument(MIC, MIC_PORT, jackclient.registerPort(
				"mic", AUDIO, JackPortIsInput), "Microphone.png", 85, 11000);
		// mic.getGain().setGain(0.25f); // trim studio noise

		while (midi.getFluidOut() == null)
			Threads.sleep(10); // wait while midi thread creates ports

		fluid = new FluidSynth(midi.getFluidOut(),
				jackclient.registerPort("fluidL", AUDIO, JackPortIsInput),
				jackclient.registerPort("fluidR", AUDIO, JackPortIsInput));

		while (midi.getCraveOut() == null)
			Threads.sleep(10);
		bass = new MidiInstrument(BASS, CRAVE_PORT,
				jackclient.registerPort("crave_in", AUDIO, JackPortIsInput), "Crave.png", midi.getCraveOut());
// end custom defines

		SynthRack.register(bass); // custom
		SynthRack.register(taco);
		SynthRack.register(tk2);
		SynthRack.register(fluid); // custom

		sampler = new Sampler();
		// recordable instruments, sequential order for the Mixer
		instruments = new Zone(this, guitar, mic, bass, taco, fluid);

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
		looper = new Looper(instruments, mic, clock);
		seq = new Seq(this);
		mixer = new DJJefe(clock, this, sampler, tk2);
		midiGui = new MidiGui(this, taco, fluid, bass, clock, midi.getJamstik());
		overview = new Overview(JUDAHZONE, this);
		fxRack = new FxPanel(selected, clock, mains);
		frame = new MainFrame(JUDAHZONE, this);

		clock.setTempo(93);
		drumMachine.init("Drumz");
		EventQueue.invokeLater(()->{
			overview.newSong();
			System.gc();
			Fader.execute(Fader.fadeIn());

			MainFrame.setFocus(guitar);
			guitar.getIR().set(0, 6);
			// guitar.setActive(guitar.getIR(), true);

			initialized = true;
			////////////////////////////
			// now the system is live //
			////////////////////////////
			RTLogger.log(this, "Java" + Runtime.version().feature() + "  Greetings Prof. Falken. ");
			test();
		});
	}

	@Override
	protected void registerPort(Request req) throws JackException {
		JackPort port = jackclient.registerPort(req.portName(), req.type(), req.inOut());
		Threads.execute(()->req.callback().ready(req, port));
	}

	public static void test() {

	}

	static void shutdown() {
		if (instance.mains != null)
			instance.mains.setOnMute(true);
		Services.shutdown();
		Threads.shutdown();
	}

	//////////////////////////////
	//		PROCESS AUDIO		//
	//////////////////////////////
	@Override
	public boolean process(JackClient client, int nframes) {

		if (!initialized)
			return true;

		// your sound will be additive
		Arrays.fill(left, 0f);
		Arrays.fill(right, 0f);

		if (mains.isOnMute()) {
		//	if (mains.isHotMic()) { // TODO Revive HOT MIC functions
		//		mic.process();
		//		mic.mix(left, right);
		//		mains.process(left, right);
		//	}
			if (clock.isActive())
				looper.silently(); // keep looper in sync w/ clock
			// output silence to Jack
			AudioTools.copy(left, outL.getFloatBuffer().rewind());
			AudioTools.copy(right, outR.getFloatBuffer().rewind());
			return true;
		}

		// Each channel's DSP multi-threaded
		threads.clear();
		threads.add(() -> { drumMachine.process(); return null; });
		threads.add(() -> { guitar.process();      return null; });
		threads.add(() -> { mic.process();         return null; });
		threads.add(() -> { sampler.process();     return null; });
		for (var engine : SynthRack.getEngines())
		    threads.add(() -> { engine.process();  return null; });

		try {
		    dspExec.invokeAll(threads);
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}

		// mix joined threads together
		guitar.mix(left, right);
		mic.mix(left, right);
		SynthRack.getEngines().forEach(engine->engine.mix(left, right));
		drumMachine.mix(left, right);
		sampler.mix(left, right);
		looper.process(left, right);  // looper records previous and/or plays loops

		mains.process(left, right);	  // final mix bus effects

		// output to Jack
		AudioTools.copy(left, outL.getFloatBuffer().rewind());
		AudioTools.copy(right, outR.getFloatBuffer().rewind());

		// recording/ monitoring
		mixer.monitor(2); 	// collect 2 channels of dB feedback for mixer panel per cycle
		requests.process();

	    // run registered analyzers on the mixed buffer (thread-safe iteration)
		// Reuse pooled stereo frame and return it to the pool after use.
		if (!analyzers.isEmpty()) {
		    final float[][] buf = Memory.STEREO.getFrame();
		    try {
		        final float[] l = buf[0];
		        final float[] r = buf[1];
		        selected.forEach(ch -> {
		            AudioTools.mix(ch.getLeft(), l);
		            AudioTools.mix(ch.getRight(), r);
		        });

		        try {
		            for (Calc<?> analyzer : analyzers)
		                analyzer.process(l, r);
		        } catch (Throwable t) {
		            RTLogger.warn(this, t);
		        }
		    } finally {
		        // Return frame to pool instead of letting it be GC'd.
		        Memory.STEREO.release(buf);
		    }
		}
		return true;
	}

}
