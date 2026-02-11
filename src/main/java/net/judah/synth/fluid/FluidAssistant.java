package net.judah.synth.fluid;

import java.io.IOException;
import java.security.InvalidParameterException;
import java.util.function.Consumer;

import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.Custom;
import judahzone.api.AudioEngine;
import judahzone.api.AudioEngine.Connect;
import judahzone.api.AudioEngine.Request;
import judahzone.api.AudioEngine.Wrapper;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import lombok.ToString;
import net.judah.channel.LineIn;

/** Creates (async) a new FluidSynth instance with associated ports
    connected via the Ports.Provider API.   Like a JackHelper for FluidSynths*/
public class FluidAssistant implements AudioEngine.PortCallback {

	@ToString
	class Triumvirate {
		JackPort midi;
		JackPort left;
		JackPort right;
	}

	private static final int MAX_INSTANCES = 3;

	private FluidSynth runtime;
	private final Triumvirate tri = new Triumvirate();
	private final String engineName;
	private final int num;
	private final String suffix;
	private final Custom user;
	private final Consumer<LineIn> channels;

	/** Creates new FluidSynth engines and manages port creation/connection. */
	public FluidAssistant(Consumer<LineIn> channels, int instanceNum) {
		this.num = instanceNum;
		this.channels = channels;
		if (num > MAX_INSTANCES)
			throw new InvalidParameterException("Too many Fluid instances");
		this.engineName = Constants.FLUID + num;
		this.suffix = "-0" + num;

		String lPort = engineName + "-left";
		String rPort = engineName + "-right";
		String mPort = engineName + "-midi";

		Float preamp = null; // set in commandLine
		user = new Custom("engineName", true, false, lPort, rPort, mPort, "Waveform.png", null, null, "Fluid", false,
				preamp);

		AudioEngine.Provider ports = AudioEngine.getPorts();
		// Request MIDI port (output from FluidSynth perspective)
		ports.register(new Request(user, engineName, AudioEngine.Type.MIDI, AudioEngine.IO.OUT, this));
		// Request audio ports (input to FluidSynth)
		ports.register(new Request(user, lPort, AudioEngine.Type.AUDIO, AudioEngine.IO.IN, this));
		ports.register(new Request(user, rPort, AudioEngine.Type.AUDIO, AudioEngine.IO.IN, this));
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
			channels.accept(runtime);

			AudioEngine.Provider ports = AudioEngine.getPorts();
			ports.connect(new Connect(user, new Wrapper(tri.midi.getName(), tri.midi), "midi" + suffix, AudioEngine.Type.MIDI,
					AudioEngine.IO.IN, this));
			ports.connect(new Connect(user, new Wrapper(tri.left.getName(), tri.left), "midi" + suffix,
					AudioEngine.Type.AUDIO, AudioEngine.IO.OUT, this));
			ports.connect(new Connect(user, new Wrapper(tri.right.getName(), tri.right), "midi" + suffix,
					AudioEngine.Type.AUDIO, AudioEngine.IO.OUT, this));
		} catch (IOException e) {
			RTLogger.log(this, e.getMessage());
		}
	}

	@Override
	public void connected(Connect con) {
		// no-op: connection established, FluidSynth handles routing
	}
}
