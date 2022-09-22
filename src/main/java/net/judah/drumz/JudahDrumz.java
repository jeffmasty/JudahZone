package net.judah.drumz;

import java.io.File;
import java.nio.FloatBuffer;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;
import javax.sound.midi.ShortMessage;
import javax.swing.ImageIcon;

import org.jaudiolibs.jnajack.JackPort;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.api.Engine;
import net.judah.api.Midi;
import net.judah.mixer.LineIn;
import net.judah.util.AudioTools;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

// TODO choke OHat
public class JudahDrumz extends LineIn implements Receiver, Engine {
	public static final int TRACKS = 8;

	@Getter private final DrumSample[] tracks = new DrumSample[TRACKS];
	@Getter private DrumKit kit;

	private final FloatBuffer left = FloatBuffer.allocate(Constants.bufSize());
	private final FloatBuffer right = FloatBuffer.allocate(Constants.bufSize());
	@Getter protected final FloatBuffer[] buffer = new FloatBuffer[] {left, right};

	public JudahDrumz(JackPort left, JackPort right, String name, ImageIcon icon) {
		super(name, true);
		setIcon(icon);
		leftPort = left;
		rightPort = right;
		for (int i = 0; i < TRACKS; i++)
			tracks[i] = new DrumSample(DrumType.values()[i]);
	}

	public void setKit(String name) {
		for (DrumSample s : tracks) {
			s.setActive(false);
			s.setTapeCounter(0);
		}
		// todo custom kits
		for (File folder : Constants.KITS.listFiles()) {
			if (folder.isDirectory() == false)
				continue;
			if (folder.getName().equals(name)) {
				try {
					kit = new DrumKit(folder);
					for (int i = 0; i < tracks.length; i++)
						if (kit.get(i) != null)
							tracks[i].setRecording(kit.get(i));
				} catch (Exception e) {
					RTLogger.warn(this, e);
				}
			}
		}
	}
	
	public void play(DrumSample s, boolean on) {
		if (on) {
			s.setTapeCounter(0);
			s.setActive(true);
		}
		else {
			s.setActive(false);
		}
		MainFrame.update(s); 
	}

	@Override
	public void send(MidiMessage message, long timeStamp) {
		ShortMessage midi = (ShortMessage)message;
		if (false == Midi.isNoteOn(midi))
			return;
		int data1 = midi.getData1();
		
		for (DrumSample drum : tracks) {
			if (drum.getGmDrum().getData1() == data1)
				play(drum, true);
		}
	}

	@Override
	public boolean hasWork() {
		for (DrumSample drum : tracks)
			if (drum.isActive()) return true;
		return false;
	}
	
	@Override
	public void close() {
		for (DrumSample s : tracks) {
			play(s, false);
		}
	}

	@Override
	public void progChange(String preset) {
		try {
			kit = new DrumKit(preset);
		} catch (Exception e) {
			RTLogger.log(this, e.getMessage());
		}
	}

	
	public void process() {
		AudioTools.silence(left);
		AudioTools.silence(right);
		for (DrumSample drum: tracks) {
			drum.process(buffer);
		}
		processFx(left, right);
	}

}
