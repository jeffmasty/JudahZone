package net.judah.mixer;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackPortFlags;
import org.jaudiolibs.jnajack.JackPortType;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahException;
import net.judah.Tab;
import net.judah.jack.AudioTools;
import net.judah.jack.BasicClient;
import net.judah.jack.Status;
import net.judah.looper.Loop;
import net.judah.mixer.MixerPort.PortDescriptor;
import net.judah.mixer.MixerPort.Type;
import net.judah.settings.Command;
import net.judah.settings.Patch;
import net.judah.settings.Patchbay;
import net.judah.settings.Service;
import net.judah.settings.Services;

@Log4j
public class Mixer extends BasicClient implements Service {

	public static final String GAIN_PROP = "Gain";
	public static final String CHANNEL_PROP = "Channel";
	public static final String PLUGIN_PROP = "Plugin Name";
	public static final String GAIN_COMMAND = "Mix Volume";
	public static final String PLUGIN_COMMAND = "Load Plugin";

	
	private final MixerCommands mixerCommands;
	
	private final Patchbay patchbay;
	
	private final List<MixerPort> inputPorts = new ArrayList<>();
	private final List<MixerPort> outputPorts = new ArrayList<>();
	@Getter private final List<Loop> loops = new ArrayList<>();
	
	@SuppressWarnings("unused")
	private float masterGain = 1f;
	private List<FloatBuffer> buf;

	
	public Mixer(Services services, Patchbay patchbay) throws JackException {
		super(patchbay.getClientName());
		this.patchbay = patchbay;
		mixerCommands = new MixerCommands(this); 
		start();
	}


	@Override
	protected void initialize() throws JackException {
		buffersize = jackclient.getBufferSize();
		samplerate = jackclient.getSampleRate();
		
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

		loops.add(new Loop("Boss", jackclient, inputPorts, outputPorts));
		loops.add(new Loop("Little Pup", jackclient, inputPorts, outputPorts));
		//	for (LoopSettings loop : patchbay.getLoops()) {
		//		loops.add(new Bloop(loop.getName(), jackclient, inputPorts, outputPorts)); }
		
		buf = new ArrayList<FloatBuffer>(outputPorts.size());
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
	public List<Command> getCommands() {
		return mixerCommands;
	}

	@Override
	public void execute(Command cmd, Properties props) throws Exception {

		mixerCommands.execute(cmd, props); 
		
		//		if (cmd.getName().equals(GAIN_COMMAND)) {
		//			gainCommand(cmd, props);
		//		} else if (cmd.getName().equals(PLUGIN_COMMAND)) {
		//			pluginCommand(cmd, props);
		//		} else throw new JudahException("Unknown Command: " + cmd);
	}

	@SuppressWarnings("unused")
	private void gainCommand(Command cmd, Properties props) throws JudahException {
		if (!props.containsKey(GAIN_PROP)) throw new JudahException("No Volume. " + cmd + " " );
		float gain = (Float)props.get(GAIN_PROP);
		Object o = props.get(CHANNEL_PROP);
		if (o == null) {
			masterGain = gain;
			return;
		}

		log.debug("channel " + o + " gain: " + gain);
		int idx = (Integer)o;
		
		if (idx >= 0 && idx < inputPorts.size()) {
			MixerPort p = inputPorts.get(idx);
			p.setGain(gain);
			if (p.isStereo()) {
				if (p.getType() == Type.LEFT) 
					inputPorts.get(idx + 1).setGain(gain);
				else 
					inputPorts.get(idx -1).setGain(gain);
			}
		}
		else {
			masterGain = gain;
		}
	}

	@Override
	public Tab getGui() {
		return null;  // TODO !!
	}

    ////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////
	// for every input, set levels, hand a copy off to loopers and get off the thread.
	@Override
	public boolean process(JackClient client, int nframes) {

		buf.clear();
		for (MixerPort p : outputPorts)
			buf.add(p.getPort().getFloatBuffer());
		AudioTools.processSilence(buf);

		for (MixerPort p : outputPorts)
			AudioTools.processSilence(p.getPort().getFloatBuffer());
		
		// do any recording or playing
		for (Loop loop : loops) {
			loop.process(nframes);
		}
		return Status.ACTIVE == state.get();
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

