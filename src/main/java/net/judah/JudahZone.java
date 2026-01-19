package net.judah;

import static judahzone.util.Constants.LEFT_OUT;
import static judahzone.util.Constants.RIGHT_OUT;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.xml.DOMConfigurator;
import org.apache.logging.log4j.Level;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.Ports.Type;
import judahzone.fx.Convolution;
import judahzone.gui.Icons;
import judahzone.gui.Nimbus;
import judahzone.jnajack.ZoneJackClient;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.Services;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.bridge.Analyzers;
import net.judah.channel.Channel;
import net.judah.channel.LineIn;
import net.judah.channel.Mains;
import net.judah.channel.PresetsDB;
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
import net.judah.mixer.Channels;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Fader;
import net.judah.mixer.IRDB;
import net.judah.sampler.SampleDB;
import net.judah.sampler.Sampler;
import net.judah.seq.Seq;
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
public class JudahZone extends ZoneJackClient {
	public static final String JUDAHZONE = JudahZone.class.getSimpleName();
	public static JudahZone getInstance() { return instance; }
	private static JudahZone instance;
	private static boolean initialized; // abort
	public static boolean isInitialized() { return initialized; }
	public static void setInitialized(boolean initialized) {JudahZone.initialized = initialized; }

	private JackPort outL, outR;
	@Getter private Mains mains;
	/** hot buffer, The Product */
	private final float[] left = new float[Constants.bufSize()];
	/** hot buffer, The Product */
	private final float[] right = new float[Constants.bufSize()];

	@Getter private final Chords chords;
	@Getter private final JudahMidi midi;
	private final JudahClock clock;
	@Getter private final Channels channels;
	@Getter private final TacoTruck taco; // midiTrack-Controlled-digital-Oscillator
	@Getter private final Sampler sampler;
	@Getter private final Looper looper;
	@Getter private final DrumMachine drumMachine;
	@Getter private final Seq seq;

	@Getter private FluidSynth fluid; // promote to system for current commit
	@Getter private MainFrame frame;
	@Getter private DJJefe mixer;
	@Getter private MidiGui midiGui;
	@Getter private FxPanel fxRack;
	@Getter private Overview overview;
	@Getter private MPKmini mpkMini;
	@Getter private final Setlists setlists = new Setlists();
	private final MultiSelect selected = new MultiSelect();

	/** Thread-safe list of analyzers; each consumes left/right float[] frames. */
	@Getter private final Analyzers analysis = new Analyzers(selected);

	private final ExecutorService processExec = Executors.newFixedThreadPool(
	            Runtime.getRuntime().availableProcessors(),
	            r -> {	Thread t = new Thread(r, "DSP-Worker");
	                	t.setDaemon(true);
	                	return t; });
	private final List<Callable<Void>> threads = new ArrayList<>();

	private JudahZone() throws Exception {
		super(JUDAHZONE);
		Nimbus.start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));
		Threads.execute(() -> DrumDB.init());
		Threads.execute(() -> SampleDB.init());
		Threads.execute(() -> Convolution.setIRDB(IRDB.instance) );
		Threads.execute(() -> SynthDB.init(Folders.getSynthPresets()));
		Threads.execute(() -> PresetsDB.init(Folders.getPresetsFile()));

		instance = this;
		channels = new Channels(this, Folders.getUserChannels());
		channels.load(); // custom channel prototypes ready to register ports
		mpkMini = new MPKmini(this);
		midi = new JudahMidi(this);
		clock = JudahMidi.getClock();
		channels.setClock(clock);
		looper = new Looper(clock, channels);
		mixer = new DJJefe(this);
		chords = new Chords(this, clock);
		taco = new TacoTruck(Trax.TK1.getName(), Icons.get("taco.png"));
		taco.setOnMixer(true);
		sampler = new Sampler();
		drumMachine = new DrumMachine();
		seq = new Seq(this);
		start(); // super calls initialize(), makeConnections()

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

		mains = new Mains();
		outL = jackclient.registerPort("left", AUDIO, JackPortIsOutput);
		outR = jackclient.registerPort("right", AUDIO, JackPortIsOutput);

		while (midi.getFluidOut() == null)
			Threads.sleep(10); // wait while midi thread creates ports
		fluid = new FluidSynth(midi.getFluidOut(),
				jackclient.registerPort("fluid-left", AUDIO, JackPortIsInput),
				jackclient.registerPort("fluid-right", AUDIO, JackPortIsInput));
		fluid.setOnMixer(true);

		channels.initialize(Type.AUDIO);

	}

	@Override
	protected void makeConnections() throws Exception {
		if (mains == null) {
			RTLogger.log(this, "Initialization failed.");
			return;
		}

		// main output
		jack.connect(jackclient, outL.getName(), LEFT_OUT);
		jack.connect(jackclient, outR.getName(), RIGHT_OUT);

		// FluidSynth is currently system
		jack.connect(jackclient, FluidSynth.LEFT_PORT, fluid.getLeftPort().getName());
		jack.connect(jackclient, FluidSynth.RIGHT_PORT, fluid.getRightPort().getName());

		channels.makeConnections(Type.AUDIO); // user defines

		EventQueue.invokeLater(() -> gui());
	}

	private void gui() {
		looper.gui();
		seq.gui();
		mixer.gui();
		channels.gui(sampler, drumMachine, taco, fluid);

		midiGui = new MidiGui(this, taco, fluid, channels.getBass(), clock, midi.getJamstik());
		overview = new Overview(JUDAHZONE, this);
		fxRack = new FxPanel(selected, clock, mains);
		frame = new MainFrame(JUDAHZONE, this);

		EventQueue.invokeLater(()->{
			clock.setTempo(93);
			overview.newSong();
			drumMachine.init("Drumz");
			System.gc();
			Fader.execute(Fader.fadeIn());

			Channel guitar = channels.getGuitar();
			if (guitar != null) {
				guitar.getIR().set(0, 6); // Marshall
				guitar.setActive(guitar.getIR(), true);
				MainFrame.setFocus(guitar);
			}

			initialized = true;
			////////////////////////////
			// now the system is live //
			////////////////////////////
			RTLogger.log(this, "Java" + Runtime.version().feature() + "  Greetings Prof. Falken. ");
			test();
		});
	}

	void test() {

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

		// the sound will be additive
		Arrays.fill(left, 0f);
		Arrays.fill(right, 0f);

		if (mains.isOnMute()) {
		//	if (mains.isHotMic()) { // TODO Revive HOT MIC function
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

		threads.clear();
		// Run each channel+FX multi-threaded (synth engines are channels)
		List<LineIn> lines = channels.getAudio();
		int channels = lines.size();
		for (int i = 0; i < channels; i++) {
			final LineIn channel = lines.get(i);
		    threads.add(() -> { channel.process(); return null;});
		}

		try {
		    processExec.invokeAll(threads);
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}

		// join/mix channels/threads
		for (int i = 0; i < channels; i++)
			lines.get(i).mix(left, right);
		looper.process(left, right);  // looper records previous channels and/or plays loops
		mains.process(left, right);	  // final mix bus effects

		// output to Jack
		AudioTools.copy(left, outL.getFloatBuffer().rewind());
		AudioTools.copy(right, outR.getFloatBuffer().rewind());

		// utilities/analyzers
		requests.process(); // register/connect ports
		mixer.process(); 	// RMS feedback for mixer panel
		analysis.process();

		return true;
	}

}
