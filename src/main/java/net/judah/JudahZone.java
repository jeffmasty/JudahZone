package net.judah;

import static net.judah.jack.AudioTools.*;
import static org.jaudiolibs.jnajack.JackPortFlags.*;
import static org.jaudiolibs.jnajack.JackPortType.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j;
import net.judah.api.BasicClient;
import net.judah.api.Service;
import net.judah.fluid.FluidSynth;
import net.judah.jack.AudioTools;
import net.judah.metronome.Metronome;
import net.judah.midi.JudahMidi;
import net.judah.mixer.Channel;
import net.judah.mixer.Instrument;
import net.judah.mixer.MixerPort;
import net.judah.mixer.MixerPort.ChannelType;
import net.judah.mixer.MixerPort.PortDescriptor;
import net.judah.mixer.plugin.CarlaVolume;
import net.judah.mixer.plugin.FluidVolume;
import net.judah.mixer.plugin.VolumeWidget;
import net.judah.sequencer.Sequencer;
import net.judah.settings.Patch;
import net.judah.util.Constants;
import net.judah.util.RTLogger;
import net.judah.util.Services;


/* Starting my jack sound system: 
/usr/bin/jackd -R -P 99 -T -v -ndefault -p 512 -r -T -d alsa -n 2 -r 48000 -p 512 -D -Chw:K6 -Phw:K6 &
a2jmidid -e & */

@Log4j
public class JudahZone extends BasicClient {

    public static final String JUDAHZONE = JudahZone.class.getSimpleName();

    // TODO
    public static final File defaultSetlist = new File("/home/judah/git/JudahZone/resources/Songs/list1.songs"); 
    public static final File defaultFolder = new File("/home/judah/git/JudahZone/resources/Songs/"); 

	// System services (fluidsynth, drumkv1)
    @Getter private static final Services services = new Services();
    
    private final Patchbay patchbay;
    @Getter private static final List<Channel> channels = new ArrayList<>();
	@Getter private static final List<MixerPort> inputPorts = new ArrayList<>();
	@Getter private static final List<MixerPort> mainOutPorts = new ArrayList<>();

	@Getter private final FluidSynth fluid; 
	@Getter private static JudahMidi midi;
	@Getter private static Metronome metronome;
	
    public static void main(String[] args) {

		try {
			new JudahZone();
			while (true) 
				RTLogger.poll();
        } catch (Throwable ex) {
            log.error(ex.getMessage(), ex);
        }
    }

	JudahZone() throws Throwable {
		super(JUDAHZONE);
    	Runtime.getRuntime().addShutdownHook(new ShutdownHook());
    	patchbay = audioConfig();
    	midi = new JudahMidi();
    	fluid = new FluidSynth(midi);
    	File clickTrack = new File(this.getClass().getClassLoader().getResource("metronome/JudahZone.mid").getFile()); 
    	metronome = new Metronome(clickTrack, null);
    	
    	services.addAll(Arrays.asList(new Service[] {midi, fluid, metronome}));
    	
    	Thread.sleep(100);
        start();
	}

	@Override
	protected void initialize() throws JackException {
		for (PortDescriptor meta : patchbay.getInPorts()) {
			inputPorts.add( new MixerPort(
					meta, jackclient.registerPort(meta.getName(), AUDIO, JackPortIsInput)));
		}
		for (PortDescriptor meta : patchbay.getOutPorts()) {
			mainOutPorts.add( new MixerPort(
					meta, jackclient.registerPort(meta.getName(), AUDIO, JackPortIsOutput)));
		}
		initializeChannels();
	}

	private void initializeChannels() {
		
		Instrument g = new Instrument("Guitar", new String[] {"system:capture_1"}); 
		Instrument m = new Instrument("Mic", new String[] {"system:capture_2"});  
		Instrument d = new Instrument("Drums", new String[] {"system:capture_4"});
		Instrument s = new Instrument("Synth", new String[] {FluidSynth.LEFT_PORT, FluidSynth.RIGHT_PORT});
		Instrument a = new Instrument("Aux", new String[] {"system:capture_3"});
		
		Channel guitar = new Channel(g, new CarlaVolume(0));
		Channel mic = new Channel(m, new CarlaVolume(2));
		Channel drums = new Channel(d, new CarlaVolume(3));
		Channel synth = new Channel(s, new FluidVolume());
		Channel aux = new Channel(a, new VolumeWidget() {
			@Override public boolean setVolume(float gain) {
				return false; /* no-op */}});
		channels.addAll(Arrays.asList(new Channel[] { guitar, mic, drums, synth, aux }));
	}
	
	
	@Override
	protected void makeConnections() throws JackException {

		// start GUI
        try { UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException
				| UnsupportedLookAndFeelException e) { log.info(e.getMessage(), e); }
		new MainFrame(JUDAHZONE);

		// Open a default song 
		File file = new File("/home/judah/git/JudahZone/resources/Songs/AndILoveHer");
		File file2 = new File("/home/judah/git/JudahZone/resources/Songs/AndIOutro");
		try {
			new Sequencer(file);
			new Sequencer(file2);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			log.error(file.getAbsolutePath());
			Constants.infoBox(file.getAbsoluteFile() + " -- " + e.getMessage(), "Song Load Failed");
		}
		
		// make any internally defined connections (typically Carla makes connections)
		for (Patch patch : patchbay.getConnections()) {
			try {
				jack.connect(jackclient, patch.getOutputPort(), patch.getInputPort());
			} catch (JackException e) {
				log.error(patch);
				throw e;
			}
		}
		
	}
	
	@RequiredArgsConstructor @Getter
	private static class Patchbay {
		private final String clientName;
		@Getter private final List<PortDescriptor> inPorts;
		@Getter private final List<PortDescriptor> outPorts;
		private final List<Patch> connections;
	}
	
	private static Patchbay audioConfig() {
		final String client = JudahZone.JUDAHZONE;
		List<Patch> connections = new ArrayList<>();
		PortDescriptor out;
		
		List<PortDescriptor> inPorts = new ArrayList<>();
		// Inputs
		inPorts.add(new PortDescriptor("guitar_left", ChannelType.LEFT));
		inPorts.add(new PortDescriptor("guitar_right", ChannelType.RIGHT));
		inPorts.add(new PortDescriptor("mic_left", ChannelType.LEFT));
		inPorts.add(new PortDescriptor("mic_right", ChannelType.RIGHT));
		inPorts.add(new PortDescriptor("drums_left", ChannelType.LEFT));
		inPorts.add(new PortDescriptor("drums_right", ChannelType.RIGHT));
		inPorts.add(new PortDescriptor("synth_left", ChannelType.LEFT));
		inPorts.add(new PortDescriptor("synth_right", ChannelType.RIGHT));
		inPorts.add(new PortDescriptor("aux_left", ChannelType.LEFT));
		inPorts.add(new PortDescriptor("aux_right", ChannelType.RIGHT));
		
		// Outputs
		List<PortDescriptor> outPorts = new ArrayList<>();
		out = new PortDescriptor("left", ChannelType.LEFT);
		outPorts.add(out);
		connections.add(new Patch(portName(client, out.getName()), "system:playback_1"));
		out = new PortDescriptor("right", ChannelType.RIGHT);
		outPorts.add(out);
		connections.add(new Patch(portName(client, out.getName()), "system:playback_2"));
		
		return new Patchbay(client, inPorts, outPorts, connections);
	}

	private class ShutdownHook extends Thread {
		@Override public void run() {
			if (Sequencer.getCurrent() != null) {
				Sequencer.getCurrent().close();
				if (Sequencer.getCarla() != null)
					Sequencer.getCarla().close();
			} 
			for (Service s : services) 
				s.close();
		}
	}
	
	////////////////////////////////////////////////////
    //                PROCESS AUDIO                   //
    ////////////////////////////////////////////////////
	
	private Sequencer seq;
	@Override
	public boolean process(JackClient client, int nframes) {
		seq = Sequencer.getCurrent();
		if (seq == null || seq.isRunning() == false) 
			return true;
		
		// any loopers playing will be additive
		for (MixerPort outport : mainOutPorts)
			AudioTools.processSilence(outport.getPort().getFloatBuffer());
		Sequencer.getCurrent().getMixer().process(nframes);
		return true;
	}

}
