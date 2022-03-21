package net.judah.tracks;

import java.awt.Color;
import java.io.File;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.ShortMessage;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.api.TimeProvider;
import net.judah.midi.JudahMidi;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Data @EqualsAndHashCode(callSuper = true)
public class MidiTrack extends Track implements Receiver {

	@Getter private Sequencer sequencer;
	@Getter private Sequence sequence;
	private int barCount, restart;
	private final JLabel filename;

	public MidiTrack(TimeProvider clock, String name, Type type, int channel, OUT port) {
		super(clock, name, type, channel, port);
		filename = new JLabel("()");
		add(filename);

		try {
			sequencer = MidiSystem.getSequencer(false);
			sequencer.setLoopCount(0); // listener fires off each repetition
			sequencer.setTempoInBPM(100);
			for (Receiver old : sequencer.getReceivers()) 
				old.close();
			sequencer.getTransmitter().setReceiver(this);
		} catch (MidiUnavailableException e) {
			RTLogger.warn(this, e);
		}
	}
	

	@Override
	public void setFile(File file) {
		stop();
		if (file != null) 
			try {
				sequence = MidiSystem.getSequence(file);
				sequencer.setSequence(sequence);
				sequencer.setMicrosecondPosition(0);
				repaint();
			} catch (Exception e) {
				RTLogger.warn(this, e);
				file = null;
			}
		this.file = file;
		filename.setText(file == null ? "--" : file.getName());
	}
	
	/*----- Player Interface ------*/
	public void start() throws MidiUnavailableException {
		restart = Constants.requeueBeats(sequence) / clock.getMeasure();

		if (file == null) 
			return;
		if (!sequencer.isOpen())
			sequencer.open();
		try {
			sequencer.setTempoInBPM(100);
			sequencer.setTempoFactor(clock.getTempo() * 0.01f);
			sequencer.start();
			barCount = 0;
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}

	public void stop() {
		if (sequencer.isRunning()) sequencer.stop();
		clock.removeListener(this);
	}
	
	@Override
	public void close() {
		stop();
		if (sequencer.isOpen()) sequencer.close();
		super.close();
	}
	
//	@Override
//	public void setActive(boolean active) {
//		super.setActive(active);
//		if (file == null) return;
//		if (active)
//			barCount = 0;
//			try {
//				start();
//			} catch (MidiUnavailableException e) {
//				RTLogger.warn(this, e);
//			}
//			stop();
//	}
	
	public void setDuration(Integer intro, Integer duration) {
		
	}

	@Override
	public void setActive(boolean active) {
		super.setActive(active);
		if (!active && sequencer.isRunning())
			sequencer.stop();
	}
	
	@Override
	public void update(Property prop, Object value) {
		// restart loop or update tempo
		if (Property.TEMPO == prop) {
			sequencer.setTempoFactor(clock.getTempo() * 0.01f);
		}
		else if (!sequencer.isRunning() && Property.STEP == prop && 0 == (int)value) {
			try {
				start();
			} catch (MidiUnavailableException e) {
				RTLogger.warn(this, e);
			}
		}
		else if (Property.BARS == prop) {
			barCount++;
			if (barCount == restart) {
				sequencer.stop();
				try {
					start();
				} catch (MidiUnavailableException e) {
					RTLogger.warn(this, e);
				}
			}
		}
	}
	

	@Override
	public void send(MidiMessage message, long timeStamp) {
		if (message instanceof ShortMessage) {
			try {
				ShortMessage current = (ShortMessage)message;
				if (current.getCommand() == ShortMessage.NOTE_ON) {
					current.setMessage(ShortMessage.NOTE_ON, current.getChannel(), current.getData1(), 
						Math.round(current.getData2() * gain));
				}
				JudahMidi.queue(current, getMidiOut());
			} catch (Exception e) {
				RTLogger.warn(this, e);
			}
		}
	
	}

	@Override
	public JPanel subGui() {
		JPanel result = new JPanel();
		result.add(new JLabel("todo"));
		result.setBackground(Color.pink);
		return result;
	}

}
