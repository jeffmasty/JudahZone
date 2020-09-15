package net.judah.mixer;

import static net.judah.jack.AudioTools.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.fluid.FluidSynth;
import net.judah.jack.AudioTools;
import net.judah.jack.BasicClient;
import net.judah.jack.ProcessAudio;
import net.judah.jack.Status;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.mixer.MixerPort.ChannelType;
import net.judah.mixer.MixerPort.PortDescriptor;
import net.judah.mixer.Widget.Type;
import net.judah.mixer.gui.MixerTab;
import net.judah.mixer.widget.CarlaVolume;
import net.judah.mixer.widget.FluidVolume;
import net.judah.settings.Command;
import net.judah.settings.Patch;
import net.judah.settings.Patchbay;
import net.judah.settings.Service;

@Log4j
public class Mixer extends BasicClient implements Service {

	@Getter private static Mixer instance;
	
	@Getter private final MixerCommands commands;
	@Getter private final MixerTab gui = new MixerTab();
	
// 	@Getter private final List<Loop> loops = new ArrayList<>();
	
	@Getter private final List<Sample> samples = new ArrayList<>();
	@Getter private final List<Channel> channels = new ArrayList<>();
	private final List<MixerPort> inputPorts = new ArrayList<>();
	private final List<MixerPort> outputPorts = new ArrayList<>();
	private final Patchbay patchbay;

	public Mixer() throws JackException {
		this(carlaPatchbay());
	}
	
	public Mixer(Patchbay patchbay) throws JackException {
		super(patchbay.getClientName());
		assert instance == null;
		instance = this;
		this.patchbay = patchbay;
		commands = new MixerCommands(this);
		start();
	}

	@Override
	protected void initialize() throws JackException {
		
		MixerPort p;
		for (PortDescriptor meta : patchbay.getPorts()) {
			try {
				if (meta.getPortType().equals(JackPortType.AUDIO) == false)  continue;  // not doing midi here
				
				p  = new MixerPort(meta, jackclient.registerPort(meta.getName(), meta.getPortType(), meta.getPortFlag()));
				if (meta.getPortFlag().equals(JackPortFlags.JackPortIsInput)) 
					inputPorts.add(p);
				else 
					outputPorts.add(p);
			} catch (JackException e) {
				log.error("Port Name: " + meta.getName());
				throw e;
			} 
		}

		// TODO Song settings
		//	loops.add(new Loop("Boss", jackclient, inputPorts, outputPorts));
		//	loops.add(new Loop("LilPup", jackclient, inputPorts, outputPorts));
		//	for (LoopSettings loop : patchbay.getLoops()) {
		//		loops.add(new Loop(loop.getName(), jackclient, inputPorts, outputPorts)); }
		
		samples.add(new Recorder("Boss", ProcessAudio.Type.FREE, inputPorts, outputPorts));
		samples.add(new Recorder("lil pup", ProcessAudio.Type.FREE, inputPorts, outputPorts));
		
		initializeChannels();
		gui.setup(channels, samples);
	}

	private void initializeChannels() {
		
		Instrument g = new Instrument("Guitar", Type.SYS, new String[] {"system:capture_1"}, null); 
		Instrument m = new Instrument("Mic", Type.SYS, new String[] {"system:capture_2"}, null);  
		Instrument d = new Instrument("Drums", Type.SYS, new String[] {"system:capture_4"}, null);
		Instrument s = new Instrument("Synth", Type.SYNTH, new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT}, null);
		
		Channel guitar = new Channel(g, new CarlaVolume(0));
		Channel mic = new Channel(m, new CarlaVolume(2));
		Channel drums = new Channel(d, new CarlaVolume(3));
		Channel synth = new Channel(s, new FluidVolume());
		channels.addAll(Arrays.asList(new Channel[] {guitar, mic, drums, synth}));
	}
	
	
	@Override
	protected void makeConnections() throws JackException {
		for (Patch patch : patchbay.getConnections()) {
			try {
				jack.connect(jackclient, patch.getOutputPort(), patch.getInputPort());
			} catch (JackException e) {
				log.error(patch);
				throw e;
			}
		}
	}
	
	@Override
	public void execute(Command cmd, HashMap<String, Object> props) throws Exception {
		commands.execute(cmd, props); 
	}
	
	@Override
	public String getServiceName() {
		return Mixer.class.getSimpleName();
	}

	public void addSample(Sample s) {
		s.setOutputPorts(outputPorts);
		samples.add(s);
	}
	
	public void removeSample(int idx) {
		samples.remove(idx);
	}
	
	public void removeSample(Sample sample) {
		samples.remove(sample);
	}

	private static Patchbay carlaPatchbay() {

		final String client = JudahZone.JUDAHZONE;
		List<PortDescriptor> ports = new ArrayList<>();
		List<Patch> connections = new ArrayList<>();
		PortDescriptor in, out;
		
		// Inputs
		in = new PortDescriptor("guitar_left", ChannelType.LEFT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("guitar_right", ChannelType.RIGHT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("mic_left", ChannelType.LEFT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("mic_right", ChannelType.RIGHT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("drums_left", ChannelType.LEFT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("drums_right", ChannelType.RIGHT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("synth_left", ChannelType.LEFT, AUDIO, JackPortIsInput);
		ports.add(in);
		in = new PortDescriptor("synth_right", ChannelType.RIGHT, AUDIO, JackPortIsInput);
		ports.add(in);
		//	connections.add(new Patch(FluidSynth.RIGHT_PORT, portName(client, in.getName())));
		//	connections.add(new Patch(FluidSynth.LEFT_PORT, portName(client, in.getName())));
		// Outputs
		out = new PortDescriptor("left", ChannelType.LEFT, AUDIO, JackPortIsOutput);
		ports.add(out);
		connections.add(new Patch(portName(client, out.getName()), "system:playback_1"));
		out = new PortDescriptor("right", ChannelType.RIGHT, AUDIO, JackPortIsOutput);
		ports.add(out);
		connections.add(new Patch(portName(client, out.getName()), "system:playback_2"));
		return new Patchbay(client, ports, connections);
	}
	
    ////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////
	// for every input, set levels, hand a copy off to loopers and get off the thread.
	@Override
	public boolean process(JackClient client, int nframes) {
		if (state.get() != Status.ACTIVE) return false;
			
		// any loopers playing will be additive
		for (MixerPort outport : outputPorts)
			AudioTools.processSilence(outport.getPort().getFloatBuffer());
		
		// do any recording or playing
		for (Sample sample : samples) {
			sample.process(nframes);
		}
		return true;
	}

	public void stopAll() {
		for (Sample s : samples)
			s.play(false);
	}



}

//@Override public void close() {
//// if (vst != null) { vst.turnOffAndUnloadPlugin(); vst = null; }
//super.close(); }
//private void pluginCommand(Command cmd, Properties props) throws FileNotFoundException, JVstLoadException, JudahException {
//	if (vst != null) { // turn off
//		log.info("Turning off Plugin");
//		JVstHost2 temp = vst;
//		vst = null;
//		try {
//			Thread.sleep(10);
//		} catch (InterruptedException e) { e.printStackTrace(); }
//		temp.turnOffAndUnloadPlugin();
//		return;
//	}
//	// turn on
//	log.info("Turning ON Plugin");
//	Object o = props.get(PLUGIN_PROP);
//	if (o instanceof String == false) throw new JudahException("Invalid " + PLUGIN_PROP + ": " + o);
//	JVstHost2 temp = new VST().loadPlugin((String)o, samplerate, buffersize);
//	log.info(temp.getEffectName() + " (" + temp.getProgramName() +  (temp.isSynth() ? ") SYNTH " : ") ")
//			+ temp.numInputs() + " inputs, " + temp.numParameters() + " parameters.");
//	for (int i = 0; i < temp.numParameters(); i++)
//	  log.info(temp.getParameterName(i) + " (" + temp.getParameterDisplay(i) + ") = " + temp.getParameter(i));
//	vst = temp;
//}

