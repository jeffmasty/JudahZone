package net.judah.synth.fluid;

import static net.judah.jack.BasicClient.INS;
import static net.judah.jack.BasicClient.OUTS;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsInput;
import static org.jaudiolibs.jnajack.JackPortFlags.JackPortIsOutput;
import static org.jaudiolibs.jnajack.JackPortType.AUDIO;
import static org.jaudiolibs.jnajack.JackPortType.MIDI;

import java.io.IOException;
import java.security.InvalidParameterException;

import org.jaudiolibs.jnajack.JackPort;

import judahzone.util.Constants;
import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.ToString;
import net.judah.JudahZone;
import net.judah.jack.BasicClient.Connect;
import net.judah.jack.BasicClient.PortBack;
import net.judah.jack.BasicClient.Request;
import net.judah.jack.BasicClient.Requests;
import net.judah.midi.JudahMidi;
import net.judah.seq.Meta;
import net.judah.seq.MetaMap;
import net.judah.seq.SynthRack;

/** Creates (async) a new FluidSynth instance with associated ports connected */
public class FluidAssistant implements PortBack {

	@ToString class Triumvirate {
		JackPort midi; JackPort left; JackPort right;
	}

	private FluidSynth runtime;
	private final Triumvirate tri = new Triumvirate();
	private final Triumvirate con = new Triumvirate();
	private final String engineName;
	private final String trackName;
	private MetaMap map;
	private String loadFile;
	private final int num;
	private final String suffix;

	// Creates new FluidSynth engines and manages the opening and connection of audio and midi ports.
	public FluidAssistant(String trackName, String file) {

		this.trackName = trackName;
		this.loadFile = file;
		this.num = SynthRack.getFluids().length;
		if (num > 9)
			throw new InvalidParameterException("Too many Fluid instances");
		this.engineName = Constants.FLUID + num;
		this.suffix = "-0" + num;
		Requests audio = JudahZone.getInstance().getRequests();

		// make midi port // TODO non-static instance
		JudahMidi.getRequests().add(new Request(this, engineName, MIDI, JackPortIsOutput));
		// make audio ports
		audio.add(new Request(this, engineName + "_L", AUDIO, JackPortIsInput));
		audio.add(new Request(this, engineName + "_R", AUDIO, JackPortIsInput));
	}

	public FluidAssistant(String name) {
		this(name, "");
	}

	public FluidAssistant(String name, MetaMap map) {
		this(name, map.getString(Meta.TRACK_NAME));
		this.map = map;
	}

	@Override
	public void ready(Request req, JackPort port) {
		if (req == null) { // 2nd callback
			if (port == tri.midi)
				con.midi = port;
			else if (port == tri.left)
				con.left = port;
			else
				con.right = port;
			if (con.right == null || con.left == null || con.midi == null)
				return;
			// connected, we're done
			return;
		}

		String nombre = port.getShortName();
		if (nombre.endsWith("_L"))
			tri.left = port;
		else if (nombre.endsWith("_R"))
			tri.right = port;
		else
			tri.midi = port;

		if (tri.right == null || tri.left == null || tri.midi == null)
			return;

		try {
			// create FluidSynth process and channel
			runtime = new FluidSynth(engineName, tri.midi, tri.left, tri.right);
			SynthRack.addEngine(runtime);
			Threads.sleep(50); // let external process create ports
			if (map == null)
				JudahZone.getInstance().getSeq().addTrack(trackName, runtime);
			else
				JudahZone.getInstance().getSeq().addTrack(map, runtime);
			// async port connections
			JudahMidi.getRequests().add(new Connect(this, tri.midi, "midi" + suffix, MIDI, INS));
			JudahZone.getInstance().getRequests().add(new Connect(this, tri.left, "midi" + suffix, AUDIO, OUTS));
			JudahZone.getInstance().getRequests().add(new Connect(this, tri.right, "midi" + suffix, AUDIO, OUTS));

			if (loadFile != null)
				runtime.getTrack().load(loadFile);
		} catch (IOException e) {
			RTLogger.log(this, e.getMessage());
		}
	}

}