package net.judah.synth.fluid;

import java.io.IOException;
import java.security.InvalidParameterException;

import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.Custom;
import judahzone.api.Ports;
import judahzone.api.Ports.Connect;
import judahzone.api.Ports.Request;
import judahzone.api.Ports.Wrapper;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import judahzone.util.Threads;
import lombok.ToString;
import net.judah.JudahZone;
import net.judah.bridge.AudioEngine;
import net.judah.seq.MetaMap;

/** Creates (async) a new FluidSynth instance with associated ports
    connected via the Ports.Provider API.   Like a JackHelper for FluidSynths*/
public class FluidAssistant implements Ports.PortCallback {

	@ToString
	class Triumvirate {
		JackPort midi;
		JackPort left;
		JackPort right;
	}

	private FluidSynth runtime;
	private final Triumvirate tri = new Triumvirate();
	private final String engineName;
	private final String trackName;
	private MetaMap map;
	private final int num;
	private final String suffix;
	private final JudahZone zone;
	private final Custom user;

	/** Creates new FluidSynth engines and manages port creation/connection. */
	public FluidAssistant(String trackName, JudahZone judahZone) {
		this.zone = judahZone;
		this.trackName = trackName;
		this.num = zone.getSeq().getFluids().length;
		if (num > 9)
			throw new InvalidParameterException("Too many Fluid instances");
		this.engineName = Constants.FLUID + num;
		this.suffix = "-0" + num;

		String lPort = engineName + "-left";
		String rPort = engineName + "-right";
		String mPort = engineName + "-midi";

		Float preamp = null; // set in commandLine
		user = new Custom("engineName", true, false, lPort, rPort, mPort, "Waveform.png", null, null, "Fluid", false,
				preamp);

		Ports.Provider ports = AudioEngine.getPorts();
		// Request MIDI port (output from FluidSynth perspective)
		ports.register(new Request(user, engineName, Ports.Type.MIDI, Ports.IO.OUT, this));
		// Request audio ports (input to FluidSynth)
		ports.register(new Request(user, lPort, Ports.Type.AUDIO, Ports.IO.IN, this));
		ports.register(new Request(user, rPort, Ports.Type.AUDIO, Ports.IO.IN, this));
	}

	public FluidAssistant(String name, JudahZone zone, MetaMap map) {
		// this(name, map.getString(Meta.TRACK_NAME));
		this(name, zone);
		this.map = map;
	}

	@Override
	public void registered(Request req, Wrapper reply) {
		if (!(reply.port() instanceof JackPort port))
			return;

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
			runtime = new FluidSynth(engineName, tri.midi, tri.left, tri.right);
			zone.getChannels().accept(runtime);
			Threads.sleep(50);
			if (map == null)
				zone.getSeq().createTrack(trackName, runtime);
			else
				zone.getSeq().addTrack(map, runtime);

			Ports.Provider ports = AudioEngine.getPorts();
			ports.connect(new Connect(user, new Wrapper(tri.midi.getName(), tri.midi), "midi" + suffix, Ports.Type.MIDI,
					Ports.IO.IN, this));
			ports.connect(new Connect(user, new Wrapper(tri.left.getName(), tri.left), "midi" + suffix,
					Ports.Type.AUDIO, Ports.IO.OUT, this));
			ports.connect(new Connect(user, new Wrapper(tri.right.getName(), tri.right), "midi" + suffix,
					Ports.Type.AUDIO, Ports.IO.OUT, this));
		} catch (IOException e) {
			RTLogger.log(this, e.getMessage());
		}
	}

	@Override
	public void connected(Connect con) {
		// no-op: connection established, FluidSynth handles routing
	}
}
