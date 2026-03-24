package net.judah;

import static judahzone.util.Constants.LEFT_OUT;
import static judahzone.util.Constants.RIGHT_OUT;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;

import org.apache.log4j.xml.DOMConfigurator;
import org.apache.logging.log4j.Level;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.AudioEngine;
import judahzone.api.AudioEngine.Type;
import judahzone.drums.wav.WavDrumDB;
import judahzone.fx.Convolution;
import judahzone.gui.Nimbus;
import judahzone.jnajack.JackHelper;
import judahzone.jnajack.ZoneJackClient;
import judahzone.synth.SynthDB;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.RTLogger;
import judahzone.util.Services;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.bridge.Analyzers;
import net.judah.channel.Channel;
import net.judah.channel.Generator;
import net.judah.channel.LineIn;
import net.judah.channel.Mains;
import net.judah.channel.PresetsDB;
import net.judah.controllers.MPKmini;
import net.judah.drums.DrumMachine;
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
import net.judah.seq.Luthier;
import net.judah.seq.Seq;
import net.judah.seq.chords.Chords;
import net.judah.song.Overview;
import net.judah.song.setlist.Setlists;
import net.judah.synth.fluid.FluidSynth;

/* my jack sound system settings: (samples are 48khz)
 * jackd -P99 -dalsa -dhw:UMC1820 -r48000 -p512 -n2
 *
 * life cycle:
 *  load presets/samples/IRs (off thread)
 * 	init some internal units (e.g. clock, channels, looper, seq)
 * 	set Midi/Audio ports (initialize/makeConnections)
 * 	start MainFrame
 * 	start Channels graph (synths + MidiUpdates loop)
 * 	initialized.
 *
 */
public class JudahZone extends ZoneJackClient {
	public static final String JUDAHZONE = JudahZone.class.getSimpleName();
	private static JudahZone instance;
	public static JudahZone getInstance() { return instance; }
	private static boolean initialized; // abort
	public static boolean isInitialized() { return initialized; }

	private JackPort outL, outR;
	@Getter private Mains mains;
	/** The Product, left channel */
	private final float[] left = new float[Constants.bufSize()];
	/** The Product, right channel */
	private final float[] right = new float[Constants.bufSize()];

	private final JudahClock clock;
	@Getter private final JudahMidi midi;
	@Getter private final Channels channels;
	@Getter private final Chords chords;
	@Getter private final Sampler sampler;
	@Getter private final Looper looper;
	@Getter private final DrumMachine drumMachine;
	@Getter private final Seq seq;

	@Getter private Luthier luthier;
	@Getter private Generator taco; // midiTrack-Controlled-digital-Oscillator
	@Getter private FluidSynth fluid; // promote to system for current commit
	@Getter private MainFrame frame;
	@Getter private DJJefe mixer;
	@Getter private MidiGui midiGui;
	@Getter private FxPanel fxRack;
	@Getter private Overview overview;
	@Getter private MPKmini mpkMini;
	@Getter private final Setlists setlists = new Setlists();
	private final MultiSelect selected = new MultiSelect();

	// process each LineIn multi-threaded (join for mix)
	private final ExecutorService processExec = Threads.getProcessPool();
	private final List<Callable<Void>> threads = new ArrayList<>();
	/** Thread-safe list of analyzers; each consumes left/right float[] frames. */
	@Getter private final Analyzers analysis = new Analyzers(selected);

	private JudahZone() throws Exception {
		super(JUDAHZONE);
		Nimbus.start();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));

		Threads.execute(() -> SampleDB.init(Folders.getSamples()));
		Threads.execute(() -> WavDrumDB.init(Folders.getKits()));
		Threads.execute(() -> SynthDB.init(Folders.getSynthPresets()));
		Threads.execute(() -> Convolution.setIRDB(new IRDB(Folders.getIR())) );
		Threads.execute(() -> PresetsDB.init(Folders.getPresetsFile()));

		channels = new Channels(this, Folders.getUserChannels());
		mpkMini = new MPKmini(this);
		midi = new JudahMidi(this);
		channels.load(); // custom channel prototypes ready to register ports
		AudioEngine.register(new JackHelper(midi, this));

		clock = JudahMidi.getClock();
		channels.setClock(clock);
		looper = new Looper(clock, channels);
		mixer = new DJJefe(this);
		chords = new Chords(this, clock);
		sampler = new Sampler();
		drumMachine = new DrumMachine(Folders.getDrumsets());
		seq = new Seq(this);
		start(); // super calls initialize(), makeConnections()
		instance = this;
	}

	public static void main(String[] args) {
		DOMConfigurator.configure(Folders.getLog4j().getAbsolutePath());
		// RTLogger.setLevel(Level.DEBUG);
		RTLogger.setLevel(Level.INFO);
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
		channels.gui(sampler, drumMachine);
		overview = new Overview(JUDAHZONE, this);
		fxRack = new FxPanel(selected, clock, mains, analysis);
		midiGui = new MidiGui(this, clock, midi.getJamstik());
		frame = new MainFrame(JUDAHZONE, this);
		luthier = new Luthier(frame, seq, chords, drumMachine);
		channels.accept(fluid);
		taco = luthier.makeTaco();
		channels.accept(taco);
		midiGui.ping(taco, fluid, channels.getBass());
		clock.setTempo(93);

		EventQueue.invokeLater(()->{

			Channel guitar = channels.getGuitar();
			if (guitar != null) {
				guitar.getIR().set(0, 6); // Marshall
				guitar.setActive(guitar.getIR(), true);
				MainFrame.setFocus(guitar);
			}

			overview.newSong();
			System.gc();
			Fader.execute(Fader.fadeIn());
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
		AudioTools.silence(left);
		AudioTools.silence(right);

		if (mains.isOnMute()) {
		//	if (mains.isHotMic()) { // TODO Revive HOT MIC function, or not: Vocoder
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

		// Run each channel+FX multi-threaded (synth engines are channels)
		threads.clear();
		List<LineIn> lines = channels.getAudio();
		int channels = lines.size();
		for (int i = 0; i < channels; i++) {
		    threads.add(lines.get(i).getProcessTask());
		} // DrumMachine would like up to 4, a SynthEngine might want more than 1.. each active voice..

		try {
		    processExec.invokeAll(threads); // prism ok
		} catch (InterruptedException e) {
		    Thread.currentThread().interrupt();
		}

		// join/mix channels/threads // TODO pre-run loops (inputs built)
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
		analysis.process(); // optional scope/tuners

		return true;
	}

}
