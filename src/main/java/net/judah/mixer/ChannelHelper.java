package net.judah.mixer;

import java.util.function.Consumer;

import org.jaudiolibs.jnajack.JackPort;

import judahzone.api.Custom;
import judahzone.api.Ports;
import judahzone.api.Ports.Connect;
import judahzone.api.Ports.IO;
import judahzone.api.Ports.Request;
import judahzone.api.Ports.Type;
import judahzone.util.RTLogger;
import net.judah.bridge.AudioEngine;
import net.judah.channel.Instrument;
import net.judah.channel.LineIn;
import net.judah.midi.MidiInstrument;


/**  Monitor progression of Port registration/connection  */
public class ChannelHelper implements Ports.PortCallback {

	private final Ports.Provider ports = AudioEngine.getPorts();
	private final LineIn target;
	private final Consumer<LineIn> callback;
	private final boolean midi;
	private final boolean mono;
	private final Custom user;

	public ChannelHelper(Consumer<LineIn> callback, LineIn barebones) {
		this.target = barebones;
		this.callback = callback;
		this.user = barebones.getUser();
		this.mono = !user.stereo();
		this.midi = user.midiPort() != null && user.midiPort().isBlank() == false;

		String name = user.name();

		// external has audioPorts: mono or stereo?

		String ourLeft = name + (mono ? "-mono" : "-left");
		Ports.Request leftReq = new Request(user, ourLeft, Type.AUDIO, IO.IN, this);
		ports.register(leftReq);

		if (!mono) {
			String ourRight = mono ? null : name + "-right";
			Ports.Request rightReq = new Request(user, ourRight, Type.AUDIO, IO.IN, this);
			ports.register(rightReq);
		}

		// hookup Midi? (MidiInstrument)
		if (midi) {
			String ourMidi = name + "-midi";
			Ports.Request midiReq = new Request(user, ourMidi, Ports.Type.MIDI, Ports.IO.OUT, this);
			ports.register(midiReq);
		}

		// TODO clock port ?
	}

	@Override
	public void registered(Request req, Ports.Wrapper reply) {

		RTLogger.debug(this, "Great, we have a port, let's connect it. " + req);

		if (req.type() == Type.MIDI) {
			if (target instanceof MidiInstrument == false) {
				RTLogger.warn(this, "Registered a MIDI port for a non-MIDI instrument!");
				return;
			}
			MidiInstrument mi = (MidiInstrument)target;
			mi.setMidiPort((JackPort)reply.port());
			// Use the provider-returned local port object as the localPort argument
			ports.connect(new Connect(user, reply, user.midiPort(), Type.MIDI, IO.IN, this));
		}
		else {
			if (target instanceof Instrument == false) {
				RTLogger.warn(this, "Registered a Jackport for a non-Instrument!");
				return;
			}
			Instrument instr = (Instrument)target;
			String portName = user.leftPort();
			// reply.name() is the requested local name (e.g. "Guitar-left"/"Guitar-right")
			if (reply.name().contains("right")) {
				portName = user.rightPort();
				instr.setRightPort((JackPort)reply.port());
			}
			else
				instr.setLeftPort((JackPort)reply.port());
			// Use the provider-returned local port object as the localPort argument
			ports.connect(new Connect(user, reply, portName, req.type(), IO.OUT, this));
		}
	}

	private	int pass;
	@Override
	public void connected(Connect con) {
		// Track progress then promote
		// Jack specific LineIn subclasses:  Instrument, MidiInstrument (FluidSynth ignored toriazu)
		if (con.type() == Type.MIDI) pass++;
		if (con.type() == Type.AUDIO) pass++;

		// Determine expected passes:
		int expected = 0;
		if (mono) expected += 1;
		else expected += 2;
		if (midi) expected += 1;

		if (pass < expected)
			return;

		// connections done: promote a full-bodied Instrument/MidiInstrument
		callback.accept(target);
	}

}
