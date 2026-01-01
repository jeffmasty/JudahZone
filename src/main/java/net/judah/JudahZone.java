package net.judah;

import static judahzone.util.Constants.*;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.awt.EventQueue;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.xml.DOMConfigurator;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.Live.LiveData;
import judahzone.gui.Icons;
import judahzone.gui.Nimbus;
import judahzone.jnajack.ZoneJackClient;
import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.Memory;
import judahzone.util.RTLogger;
import judahzone.util.Recording;
import judahzone.util.Services;
import judahzone.util.Threads;
import judahzone.util.WavConstants;
import lombok.Getter;
import lombok.Setter;
import net.judah.channel.Channel;
import net.judah.channel.Instrument;
import net.judah.channel.LineIn;
import net.judah.channel.Mains;
import net.judah.controllers.MPKmini;
import net.judah.drumkit.DrumMachine;
import net.judah.fx.Convolution;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.MidiGui;
import net.judah.gui.knobs.TunerKnobs;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiInstrument;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Fader;
import net.judah.mixer.IRDB;
import net.judah.mixer.Zone;
import net.judah.sampler.Sampler;
import net.judah.seq.Seq;
import net.judah.seq.SynthRack;
import net.judah.seq.Trax;
import net.judah.seq.chords.Chords;
import net.judah.song.Overview;
import net.judah.song.setlist.Setlists;
import net.judah.synth.fluid.FluidSynth;
import net.judah.synth.taco.TacoTruck;
import net.judahzone.fx.Tuner;
import net.judahzone.scope.JudahScope;

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
	@Getter private Instrument guitar;
	@Getter private Instrument mic;
	@Getter private MidiInstrument bass;
	@Getter private FluidSynth fluid;
	/** midiTrack-Controlled-digital-Oscillators */
	@Getter private TacoTruck taco;
	@Getter private TacoTruck tk2;

	@Getter private JudahMidi midi;
	private JudahClock clock;
	@Getter private Seq seq;
	@Getter private DJJefe mixer;
	@Getter private DrumMachine drumMachine;
	@Getter private Looper looper;
	@Getter private Sampler sampler;
	@Getter private Chords chords;

	@Getter private MainFrame frame;
	@Getter private MidiGui midiGui;
	@Getter private FxPanel fxRack;
	@Getter private Overview overview;

	@Getter private MPKmini mpkMini;
	@Getter private Zone instruments;
	@Getter private JudahScope scope;
	@Getter private final Setlists setlists = new Setlists();
	private final MultiSelect selected = new MultiSelect();
	// short recording buffer (1 fft_frame = 4 jack_frames)
	private Recording realtime = new Recording();
	// @Getter private Requests requests;
	@Getter @Setter private volatile Tuner tuner;

	private final ExecutorService dspExec = Executors.newFixedThreadPool(
	            Runtime.getRuntime().availableProcessors(),
	            r -> {	Thread t = new Thread(r, "DSP-Worker");
	                	t.setDaemon(true);
	                	return t; });
	private final List<Callable<Void>> threads = new ArrayList<>();

	private JudahZone() throws Exception {
		super(JUDAHZONE);
		Nimbus.start();
		Threads.execute(() -> Convolution.setIRDB(IRDB.instance) );
		Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdown()));
		start(); // super calls initialize(), makeConnections()
		instance = this;
	}

	public static void main(String[] args) {
		DOMConfigurator.configure(Folders.getLog4j().getAbsolutePath());
		// RTLogger.setLevel(Level.DEBUG);
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
		sampler = new Sampler();
		chords = new Chords(this, clock);
		guitar = new Instrument(GUITAR, GUITAR_PORT, jackclient.registerPort(
				"guitar", AUDIO, JackPortIsInput), "Guitar.png", 85, 12000);

		mic = new Instrument(MIC, MIC_PORT, jackclient.registerPort(
				"mic", AUDIO, JackPortIsInput), "Microphone.png", 85, 11000);
		mic.getGain().setGain(0.25f); // trim studio noise

		while (midi.getFluidOut() == null)
			Threads.sleep(10); // wait while midi thread creates ports

		fluid = new FluidSynth(midi.getFluidOut(),
				jackclient.registerPort("fluidL", AUDIO, JackPortIsInput),
				jackclient.registerPort("fluidR", AUDIO, JackPortIsInput));

		while (midi.getCraveOut() == null)
			Threads.sleep(10);
		bass = new MidiInstrument(BASS, CRAVE_PORT,
				jackclient.registerPort("crave_in", AUDIO, JackPortIsInput), "Crave.png", midi.getCraveOut());

		taco = new TacoTruck(Trax.TK1.getName(), Icons.get("taco.png"));
		tk2 = new TacoTruck(Trax.TK2.getName(), Icons.get("Waveform.png"));

		SynthRack.register(bass);
		SynthRack.register(taco);
		SynthRack.register(tk2);
		SynthRack.register(fluid);

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
		fxRack = new FxPanel(selected, clock);
		scope = new JudahScope(Size.WIDTH_TAB);
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
		FloatBuffer left = outL.getFloatBuffer();
		FloatBuffer right = outR.getFloatBuffer();

		// get Mains' work as merge buffer?

		// channels and looper will be additive
		AudioTools.silence(left);
		AudioTools.silence(right);

		if (!initialized)
			return true;

		if (mains.isOnMute()) {
			if (mains.isHotMic()) {
				mic.process();
				mic.mix(left, right);
				mains.process(left, right);
			}
			if (clock.isActive())
				looper.silently(); // keep looper in sync w/ clock
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
		sampler.mix(left, right);
		drumMachine.mix(left, right);

		looper.process(left, right);  // looper records and/or plays loops
		mains.process(left, right);	  // final mix bus effects

		// recording/ monitoring
		mixer.monitor(2); 	// collect 2 channels of dB feedback for mixer panel per cycle
		requests.process();

		// analysis
		boolean activeScope = scope.isActive();
		boolean activeWaveform = MainFrame.getKnobMode() == KnobMode.Tuner;

		// TunerKnobs tuner = MainFrame.getKnobs() instanceof TunerKnobs tune ? tune : null;
		if (activeScope || activeWaveform) {
			float[][] buf = Memory.STEREO.getFrame();
			selected.forEach(ch -> { // TODO generalize
				AudioTools.mix(ch.getLeft(), buf[Constants.LEFT]);
				AudioTools.mix(ch.getRight(), buf[Constants.RIGHT]);
			});
			if (activeWaveform && MainFrame.getKnobs() instanceof TunerKnobs knobs)
				knobs.process(buf);
			if (activeScope) {
				realtime.add(buf);
				if (realtime.size() == WavConstants.CHUNKS) {
					MainFrame.update(new LiveData(scope, realtime.getLeft(), realtime.getChannel(1)));
					realtime = new Recording();
				}
			}
		}

		if (tuner != null) {
			Channel tune = selected.getFirst();
			if (tune != null)
				tuner.process(tune.getLeft(), tune.getRight());
		}
		return true;
	}

}
