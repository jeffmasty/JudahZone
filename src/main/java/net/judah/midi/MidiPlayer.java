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

@Log4j
public class MidiPlayer {
	/** loop count for continuous looping */
	public static final int LOOP = Sequencer.LOOP_CONTINUOUSLY;
	
	@Getter private final File file; 
	@Getter private final Sequencer sequencer;
	@Getter private final Sequence sequence; 
	private Thread listener;
	private final float tempo;
	
	
	public MidiPlayer(File file, float bpm, int loopCount, Receiver receiver) throws InvalidMidiDataException, MidiUnavailableException, IOException {
		
		log.info("TEMPO: " + bpm);
		this.tempo = bpm;
		
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
	
	public void start() throws MidiUnavailableException {
		if (!sequencer.isOpen()) {
			sequencer.open();
		}
		sequencer.setTempoFactor(tempo/100f);
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
	
	public void stop() {
		if (sequencer.isRunning())
		sequencer.stop();
	}
	
	public void close() {
		if (sequencer.isOpen())
			sequencer.close();
	}
	
}
