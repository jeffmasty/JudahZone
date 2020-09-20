package net.judah.midi;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.metronome.MetroPlayer;

@Log4j
public class MidiPlayer implements MetroPlayer {
	/** loop count for continuous looping */
	public static final int LOOP = Sequencer.LOOP_CONTINUOUSLY;
	
	@Getter private final File file; 
	@Getter private final Sequencer sequencer;
	@Getter private final Sequence sequence; 
	private Thread listener;
	private final JudahReceiver receiver;
	
	
	public MidiPlayer(File file, int loopCount, JudahReceiver receiver) 
			throws InvalidMidiDataException, MidiUnavailableException, IOException {
		
		this.receiver = receiver;
		
		this.file = file;
		sequencer = MidiSystem.getSequencer(false);
		sequence = MidiSystem.getSequence(file);
		sequencer.setSequence(sequence);
		sequencer.setLoopCount(loopCount);
		sequencer.setTempoInBPM(100);
		
		for (Receiver old : sequencer.getReceivers()) 
			old.close();
		sequencer.getTransmitter().setReceiver(receiver);
	}
	
	public MidiPlayer(File file) throws MidiUnavailableException, InvalidMidiDataException, IOException {
		this(file, Sequencer.LOOP_CONTINUOUSLY, new JudahReceiver());
	}

	@Override
	public void start() throws MidiUnavailableException {
		if (!sequencer.isOpen()) {
			sequencer.open();
		}
		sequencer.setTempoFactor(net.judah.metronome.Sequencer.getInstance().getTempo()/100f);
		log.info("Midi player starting. " + file.getAbsolutePath());
		sequencer.start();
		listener = new Thread() {
			@Override public void run() {
				do {try { 
						Thread.sleep(333);
					} catch (InterruptedException e) {
						log.info("Midi player stopped. " + file.getAbsolutePath()); }
				} while (sequencer.isRunning());
				if (sequencer.isOpen())
					sequencer.close();
			}};
		listener.start();
	}
	
	@Override
	public void stop() {
		if (sequencer.isRunning())
		sequencer.stop();
	}
	
	@Override
	public void close() {
		if (sequencer.isOpen())
			sequencer.close();
	}

	@Override
	public void setGain(float gain) {
		receiver.setGain(gain);
	}
	
	@Override
	public boolean isRunning() {
		return sequencer.isRunning();
	}
	
	@Override
	public void setDuration(Integer intro, Integer duration) {
		throw new IllegalAccessError("Not implemented yet on MidiPlayer.");
	}
	
}
