package net.judah;

import static judahzone.util.Constants.*;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;

import java.awt.EventQueue;
import java.io.Closeable;
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

import judahzone.util.AudioTools;
import judahzone.util.Constants;
import judahzone.util.Folders;
import judahzone.util.Memory;
import judahzone.util.RTLogger;
import judahzone.util.Recording;
import judahzone.util.Threads;
import lombok.Getter;
import net.judah.controllers.MPKmini;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.MainFrame;
import net.judah.gui.fx.FxPanel;
import net.judah.gui.fx.MultiSelect;
import net.judah.gui.knobs.MidiGui;
import net.judah.gui.knobs.TunerKnobs;
import net.judah.jack.BasicClient;
import net.judah.looper.Looper;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.midi.MidiInstrument;
import net.judah.mixer.DJJefe;
import net.judah.mixer.Fader;
import net.judah.mixer.Instrument;
import net.judah.mixer.LineIn;
import net.judah.mixer.Mains;
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
import net.judahzone.gui.Icons;
import net.judahzone.gui.Nimbus;
import net.judahzone.scope.JudahScope;
import net.judahzone.scope.Live.LiveData;

/* my jack sound system settings:
 * jackd -P99 -dalsa -dhw:UMC1820 -r48000 -p512 -n2
 * (samples are 48khz) */
public class JudahZone extends BasicClient {
	public static final String JUDAHZONE = JudahZone.class.getSimpleName();

	private static boolean initialized; // abort
	private static JudahZone instance;

	public static boolean isInitialized() { return initialized; }
	public static void setInitialized(boolean initialized) {JudahZone.initialized = initialized; }
	public static JudahZone getInstance() { return instance; }
	private static final ArrayList<Closeable> services = new ArrayList<Closeable>();
	public static ArrayList<Closeable> getServices() { return services; }

	private JackPort outL, outR;
	@Getter private Instrument mic;
	@Getter private Requests requests;

	@Getter private Mains mains;
	@Getter private JudahMidi midi;
	private JudahClock clock;

	@Getter private Instrument guitar; // 8
	@Getter private MidiInstrument bass; // 10
	@Getter private FluidSynth fluid; // 4
	/** midiTrack-Controlled-digital-Oscillators */
	@Getter private TacoTruck taco; // 8
	@Getter private TacoTruck tk2; // beatstep

	@Getter private DJJefe mixer; // 18
	@Getter private Seq seq; // 28
	@Getter private DrumMachine drumMachine; // 13
	@Getter private Looper looper; // 23
	@Getter private Sampler sampler; // 5
	@Getter private Chords chords; // 14
	@Getter private MainFrame frame; // 13
	@Getter private MidiGui midiGui; // 8
	@Getter private FxPanel fxRack; // 18
	@Getter private Overview overview; // 21
	@Getter private final Setlists setlists = new Setlists(); // 6
	@Getter private final MultiSelect selected = new MultiSelect(); // 6
	@Getter private Zone instruments; // 6
	@Getter private MPKmini mpkMini;

	@Getter private JudahScope scope;
	// short recording buffer (4 jack_frames = 1 fft_frame)
	private Recording realtime = new Recording();

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
		looper = new Looper(instruments, mic, clock);
		seq = new Seq(this);
		mixer = new DJJefe(clock, this, sampler, tk2);
		midiGui = new MidiGui(this, taco, fluid, bass, clock, midi.getJamstik());
		overview = new Overview(JUDAHZONE, this);
		fxRack = new FxPanel(selected, clock);
		scope = new JudahScope();
		frame = new MainFrame(JUDAHZONE, this);

		clock.setTempo(93);
		drumMachine.init("Drumz");
		EventQueue.invokeLater(()->{
			overview.newSong();
			System.gc();
			Fader.execute(Fader.fadeIn());

			MainFrame.setFocus(guitar);
			// guitar.getIR().set(0, 6);
			// guitar.setActive(guitar.getIR(), true);

			initialized = true;
			////////////////////////////
			// now the system is live //
			////////////////////////////
			RTLogger.log(this, "Greetings Prof. Falken. ");
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
		TunerKnobs tuner = MainFrame.getKnobs() instanceof TunerKnobs tune ? tune : null;
		boolean scopeLive = scope.isActive();
		if (tuner != null || scopeLive) {
			float[][] buf = Memory.STEREO.getFrame();
			selected.forEach(ch -> { // TODO generalize
				AudioTools.mix(ch.getLeft(), buf[Constants.LEFT]);
				AudioTools.mix(ch.getRight(), buf[Constants.RIGHT]);
			});
			if (tuner != null)
				tuner.process(buf);
			if (scopeLive) { // build FFT sized chunks for Scope
				realtime.add(buf);
				if (realtime.size() == JudahScope.CHUNKS) {
					MainFrame.update(new LiveData(scope
							, realtime));
					realtime = new Recording();
				}
			}
		}

		return true;
	}

}
