package net.judah.jack;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.Properties;

import org.apache.commons.lang3.math.NumberUtils;

import lombok.extern.log4j.Log4j;
import net.judah.jack.ProcessAudio.Type;
import net.judah.looper.Loop;
import net.judah.looper.Recording;
import net.judah.looper.Sample;
import net.judah.midi.JudahReceiver;
import net.judah.midi.MidiClient;
import net.judah.midi.MidiPlayer;
import net.judah.mixer.Mixer;
import net.judah.plugin.Carla;
import net.judah.settings.Service;
import net.judah.settings.Services;
import net.judah.util.Tab;


@Log4j
public class JackTab extends Tab {
	private static final String saveHelp = "save loop_num filename (saves looper to disc)"; 
	private static final String readHelp = "read loop_num filename (reads bytes from disc into looper)";
	private static final String playHelp = "play filename or sample index (play a sample)";
	private static final String stopHelp = "stop index (stop a sample)";
	private static final String samples = "samples : list samples";
	
	boolean firsttime = true;
	
	public JackTab() {
		new Thread() {
			@Override public void run() {
				try { Thread.sleep(100); } catch (InterruptedException e) { }
				help(); 
			}}.start();
	}

	@Override
	public String getTabName() {
		return "Jack";
	}

	@Override
	public void setProperties(Properties p) {
		// TODO Auto-generated method stub

	}

	@Override
	public void actionPerformed(ActionEvent event) {
		String text = super.parseInputBox(event);
		String[] split = text.split(" ");
		
		if (text.equalsIgnoreCase("help")) {
			help();
			return;
		}

		if (text.startsWith("xruns") || text.equalsIgnoreCase("xrun")) {
			for (Service s : Services.getInstance())
				if (s instanceof BasicClient) 
					addText(((BasicClient)s).xrunsToString());
			return;
		}
		
		//  set_active	set_parameter_value set_volume 
		if (text.startsWith("volume ") && split.length == 3) {
			getCarla().setVolume(Integer.parseInt(split[1]), Float.parseFloat(split[2]));
		}
		else if (text.startsWith("active ") && split.length == 3) {
			getCarla().setActive(Integer.parseInt(split[1]), Integer.parseInt(split[2]));
		}
		else if (text.startsWith("parameter ") && split.length == 4) {
			getCarla().setParameterValue(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Float.parseFloat(split[3]));
		}
		else if (text.startsWith("midi")) {
			try {
				File midiFile = null;
				if (split.length == 2) {
					try {
						midiFile = new File(split[1]);
						if (!midiFile.isFile()) {
							throw new FileNotFoundException(split[1]);
						}
					} catch (Throwable t) {
						addText(t.getMessage());
						log.info(t.getMessage(), t);
					}
				}
				if (midiFile == null) {
					addText("uh-oh, no midi file.");
					midiFile = new File("/home/judah/Tracks/midi/dance/dance21.mid");
				}
				
				MidiPlayer playa = new MidiPlayer(midiFile, 80, 0, 
						new JudahReceiver(MidiClient.getInstance()));
				addText(playa.getSequencer().getDeviceInfo() + " / " + playa.getSequencer().getMasterSyncMode());
				playa.start();
			} catch (Throwable t) {
				addText(t.getMessage());
				log.error(t.getMessage(), t);
			}
		}
		
		if (text.startsWith("save "))
			save(split);
		else if (text.startsWith("read ")) 
			read(split);
		else if (text.startsWith("play "))
			play(split);
		else if (text.equals("stop")) {
			Mixer.getInstance().stopAll();
		}
		else if (text.startsWith("stop "))
			stop(split);
		else if (text.equals("samples")) {
			addText( Arrays.toString(((Mixer)Services.byClass(Mixer.class)).getSamples().toArray()));
		}
		else 
			addText(":( unknown command. try help");
	}

	private void help() {
		addText("xrun");
		addText("volume carla_plugin_index value");
		addText("active carla_plugin_index 1_or_0");
		addText("parameter carla_plugin_index parameter_index value");
		addText(saveHelp);
		addText(readHelp);
		addText(playHelp);
		addText(stopHelp);
		addText(samples);
	}

	private void play(String[] split) {
		if (split.length != 2) {
			addText("usage: " + playHelp);
			return;
		}
		String file = split[1];
		try {
			Sample sample = new Sample(new File(file).getName(), Recording.readAudio(file));
			sample.setType(Type.LOOP);
			Mixer.getInstance().addSample(sample);
			sample.play(true);
		} catch (Throwable t) {
			addText(t.getMessage());
			log.error(t.getMessage(), t);
		}
	}

	private void stop(String[] split) {
		if (split.length != 2) {
			addText("usage: " + stopHelp);
			return;
		}
		Integer idx = Integer.parseInt(split[1]);
		Mixer.getInstance().removeSample(idx);
		addText("Sample removed");
		
	}
	private void read(String[] split) {
		if (split.length != 3 || !NumberUtils.isDigits(split[1])) {
			addText("Format: " + readHelp);
			return;
		}
		int loopNum = Integer.parseInt(split[1]);
		String filename = split[2];
		int loopMax = Mixer.getInstance().getLoops().size();
		if (loopNum < 0 || loopNum >= loopMax) {
			addText("loop " + loopNum + " does not exist.");
			return;
		}
		Loop loop = Mixer.getInstance().getLoops().get(loopNum);

		try {
			Recording recording = Recording.readAudio(filename);
			loop.setRecording(recording);
			float seconds = recording.size() / MidiClient.getInstance().getSamplerate(); 
			addText(seconds + " of " + filename + " read, stored in loop " + loopNum + ". " + recording.getNotes());
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			addText(e.getMessage());
		}
	}

	private void save(String[] split) {
		if (split.length != 3 || !NumberUtils.isDigits(split[1])) {
			addText("Format: " + saveHelp);
			return;
		}
		int loopNum = Integer.parseInt(split[1]);
		String filename = split[2];
		int loopMax = Mixer.getInstance().getLoops().size();
		if (loopNum < 0 || loopNum >= loopMax) {
			addText("loop " + loopNum + " does not exist.");
			return;
		}
		Loop loop = Mixer.getInstance().getLoops().get(loopNum);
		if (!loop.hasRecording()) {
			addText("Nothing in Loop " + loopNum);
			return;
		}
		
		Recording recording = loop.getRecording();
		if (recording == null || recording.isEmpty()) {
			addText("No recording in looper " + loopNum + ".");
			return;
		}
		try {
			recording.setNotes("Hello, world");
			recording.saveAudio(filename);
			float seconds = recording.size() / MidiClient.getInstance().getSamplerate();
			addText(seconds + " of loop " + loopNum + " saved to " + filename);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			addText(e.getMessage());
		}
	}

	private Carla getCarla() {
		return (Carla)Services.byClass(Carla.class);
	}
	
}
