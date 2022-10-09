package net.judah.carla;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import org.jaudiolibs.jnajack.JackException;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.OSCSerializeException;
import com.illposed.osc.transport.udp.OSCPortOut;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.looper.Looper;
import net.judah.util.RTLogger;

/**
 * Use UDP Port
 *
<pre>
  	OSC Implemented:
/set_active					<i-value>
/set_parameter_value 		<i-index> <f-value>
/set_volume 		 		<f-value>
/set_drywet 				<f-value>

	TODO:
/set_balance_left	 		<f-value>
/set_balance_right   		<f-value>
/set_panning 		 		<f-value>
/set_parameter_midi_cc		<i-index> <i-cc>
/set_parameter_midi_channel	<i-index> <i-channel>
/set_program 				<i-index>
/set_midi_program 			<i-index>
/note_on   					<i-channel> <i-note> <i-velo>
/note_off  					<i-channel> <i-note
</pre> */
@Log4j
public class Carla extends ArrayList<Plugin> implements Closeable {
    public static final String CARLA_SHELL_COMMAND = "carla";
    public static final boolean NO_GUI = false;
    public static final boolean SHOW_GUI = true;
    
	@Getter private static Carla instance;
	private static boolean isFirst = true; // adjust prefix

	private final String prefix;
	@Getter private final File settings;
	private Process process;
	private OSCPortOut out;

	//private Plugin harmonizer;
	//private Plugin talReverb;
	//private Plugin talReverb2;
	// @Getter TalReverb reverb;
	// private TalReverb reverb2;

	/** Default JudahZone load, initializes {@link #plugins} hard-coded from the settings file
	 * @throws JackException */
	public Carla(boolean showGui, Looper looper) throws IOException, JackException {
		this(new File(System.getProperty("user.dir"), "carla/JudahZone.carxp"), showGui);

		//talReverb = new Plugin(TalReverb.NAME, 0, LineType.CARLA);
		//talReverb2 = new Plugin(TalReverb.NAME + "2", 1, LineType.CARLA);

		//harmonizer = new Plugin("harmonizer", 2, LineType.CARLA,
		//		new String[] {"rkr Harmonizer (no midi):Audio In L",
		//			"rkr Harmonizer (no midi):Audio In R"},
		//		new String[] {"rkr Harmonizer (no midi):Audio Out L",
		//			"rkr Harmonizer (no midi):Audio Out R"},
		//		null, false);

		//addAll(Arrays.asList(new Plugin[] {
		//        talReverb, talReverb2, harmonizer})); 
		JudahZone.getServices().add(0, this);
		// reverb = new TalReverb(this, talReverb, 550);
		// reverb2 = new TalReverb(this, talReverb2, 900);
		//looper.setReverb(reverb, reverb2);
	}

	// default ports
	public Carla(File carlaSettings, boolean showGui) throws IOException, JackException {
		this(carlaSettings, 11176, 11177, showGui);
	}

	public Carla(File carlaSettings, int tcpPort, int udpPort, boolean showGui) throws IOException, JackException {
		this.settings = carlaSettings;

		ArrayList<String> cmds = new ArrayList<>();
		cmds.add(CARLA_SHELL_COMMAND);
		if (!showGui) cmds.add("--no-gui");
		cmds.add(carlaSettings.getAbsolutePath());
    	ProcessBuilder builder = new ProcessBuilder(cmds);
    	builder.environment().put("CARLA_OSC_TCP_PORT", "" + tcpPort);
    	builder.environment().put("CARLA_OSC_UDP_PORT", "" + udpPort);
    	process = builder.start();

    	prefix = (isFirst) ? "/Carla/" : "/Carla_01/";
    	isFirst = false;

    	out = new OSCPortOut(InetAddress.getLocalHost(), udpPort);
    	instance = this;
	}

	/** @return true if message sent
	 * @throws IOException
	 * @throws OSCSerializeException */
	public void setVolume(int pluginIdx, float gain) throws OSCSerializeException, IOException {
		List<Object> param = new ArrayList<>();
		param.add(gain);
		String address = prefix + pluginIdx + "/set_volume";
		send(address, param);
	}

	public void setActive(int pluginIdx, boolean tOrF) throws OSCSerializeException, IOException {
		setActive(pluginIdx, tOrF ? 1 : 0);
	}

	/** @return true if message sent
	 * @throws IOException
	 * @throws OSCSerializeException */
	public void setActive(int pluginIdx, int tOrF) throws OSCSerializeException, IOException {
		List<Object> param = new ArrayList<>();
		param.add(tOrF);
		String address = prefix + pluginIdx + "/set_active";
		send(address, param);
	}

	public void setDryWet(int pluginIdx, float val) throws OSCSerializeException, IOException {
		List<Object> param = new ArrayList<>();
		param.add(val);
		String address = prefix + pluginIdx + "/set_drywet";
		send(address, param);
	}

	/** @return true if message sent
	 * @throws IOException
	 * @throws OSCSerializeException */
	public void setParameterValue(int pluginIdx, int paramIdx, float value) {
		String address = prefix + pluginIdx + "/set_parameter_value";
		List<Object> param = new ArrayList<>();
		param.add(paramIdx);
		param.add(value);
	    // Console.info("OSC: " + plugins.get(pluginIdx).getName() + " -> " + Arrays.toString(param.toArray()));
		send(address, param);
	}

	private void send(String address, List<Object> params) {
		try {
			if (!out.isConnected())
				out.connect();
			out.send(new OSCMessage(address, params));
		} catch (Exception e) {
			RTLogger.warn(this, "tempo " + e.getMessage());
		}
	}

	@Override
	public void close() {
		if (out != null && out.isConnected()) {
			try {
				out.disconnect();
			} catch (IOException e) {
				log.error(e);
			}
		}
		if (process != null) process.destroyForcibly();
		instance = null;
		out = null;
	}

}

// Plugin midiClock("MidiClock", 4, LineType.OTHER) completely controlled over midi
//	fluid = new Plugin("Calf Fluid", 0, LineType.SYNTH, null,
//			new String[] {"Calf Fluidsynth:Out L", "Calf Fluidsynth:Out R"},
//					"Calf Fluidsynth:events-in", false);

// private final Jack jack; getInstance()
//	private void toAux2(Plugin plugin, LineIn ch, boolean active) throws JackException {
//		JackClient client = JudahZone.getInstance().getJackclient();
//		LineIn aux2 = JudahZone.getChannels().getCrave();
//
//		if (active) {
//			// disconnect standard stage
//			jack.disconnect(client, ch.getLeftSource(), ch.getLeftConnection());
//
//			// connect plugin to System port.
//			jack.connect(client, ch.getLeftSource(), plugin.getInports()[LEFT_CHANNEL]);
//			if (plugin.isStereo()) {
//				if (ch.isStereo())
//					jack.connect(client, ch.getRightSource(), plugin.getInports()[RIGHT_CHANNEL]);
//				else
//					jack.connect(client, ch.getLeftSource(), plugin.getInports()[RIGHT_CHANNEL]);
//			}
//			else if (ch.isStereo())
//				jack.connect(client, ch.getRightSource(), plugin.getInports()[LEFT_CHANNEL]);
//
//			// connect plugin to JudahZone (aux2)
//			jack.connect(client, plugin.getOutports()[LEFT_CHANNEL], aux2.getLeftConnection());
//			if (plugin.isStereo())
//				jack.connect(client, plugin.getOutports()[RIGHT_CHANNEL], aux2.getRightConnection());
//			else
//				jack.connect(client, plugin.getOutports()[LEFT_CHANNEL], aux2.getRightConnection());
//
//		}
//		else {
//			// disconnect plugin from JudahZone (aux2)
//			jack.disconnect(client, plugin.getOutports()[LEFT_CHANNEL], aux2.getLeftConnection());
//			if (plugin.isStereo())
//				jack.disconnect(client, plugin.getOutports()[RIGHT_CHANNEL], aux2.getRightConnection());
//			else
//				jack.connect(client, plugin.getOutports()[LEFT_CHANNEL], aux2.getRightConnection());
//
//			// disconnect plugin from system
//			jack.disconnect(client, ch.getLeftSource(), plugin.getInports()[LEFT_CHANNEL]);
//			if (plugin.isStereo()) {
//				if (ch.isStereo())
//					jack.disconnect(client, ch.getRightSource(), plugin.getInports()[RIGHT_CHANNEL]);
//				else
//					jack.disconnect(client, ch.getLeftSource(), plugin.getInports()[RIGHT_CHANNEL]);
//			}
//			else if (ch.isStereo())
//				jack.disconnect(client, ch.getRightSource(), plugin.getInports()[LEFT_CHANNEL]);
//
//			// connect standard stage
//			jack.connect(client, ch.getLeftSource(), ch.getLeftConnection());
//			if (ch.isStereo())
//				jack.connect(client, ch.getRightSource(), ch.getRightConnection());
//
//		}
//	}
//	public void octaver(boolean active) throws OSCSerializeException, IOException, JackException {
//		JackClient client = JudahZone.getInstance().getJackclient();
//		Instrument guitar = JudahZone.getChannels().getGuitar();
//		Instrument aux2 = JudahZone.getChannels().getFluid();
//
//		// setActive(bassEQ.getIndex(), active);
//		setActive(harmonizer.getIndex(), active);
//
//		// plugin bypass settings
//		if (active) {
//			setParameterValue(harmonizer.getIndex(), 0, active ? 0f : 1f);
//			// setParameterValue(bassEQ.getIndex(), 0, active ? 1f : 0f);
//		}
//
//		// mute/unmute normal guitar channel
//		guitar.setOnMute(active);
//		guitar.setMuteRecord(active);
//
//		// jack port mapping to aux2
//		if (active) {
//			jack.connect(client, guitar.getLeftSource(), harmonizer.getInports()[LEFT_CHANNEL]);
//			jack.connect(client, guitar.getLeftSource(), harmonizer.getInports()[RIGHT_CHANNEL]);
//
//			jack.connect(client, harmonizer.getOutports()[LEFT_CHANNEL], aux2.getLeftConnection());
//			jack.connect(client, harmonizer.getOutports()[RIGHT_CHANNEL], aux2.getRightConnection());
//			RTLogger.log(this, "Bass beast mode engaged.");
//		}
//		else {
//			try {
//			jack.disconnect(client,  guitar.getLeftSource(), harmonizer.getInports()[LEFT_CHANNEL]);
//			jack.disconnect(client, guitar.getLeftSource(), harmonizer.getInports()[RIGHT_CHANNEL]);
//
//			jack.disconnect(client, harmonizer.getOutports()[LEFT_CHANNEL], aux2.getLeftConnection());
//			jack.disconnect(client, harmonizer.getOutports()[RIGHT_CHANNEL], aux2.getRightConnection());
//			RTLogger.log(this, "Octaver off");
//			} catch (Throwable t) {log.debug("disconnect: " + t.getMessage());}
//		}
//	}

