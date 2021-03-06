package net.judah.metronome;

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
import net.judah.api.Status;

@Log4j
public class MidiPlayer implements Player {
	
	@Getter private final File file; 
	@Getter private final Sequencer sequencer;
	@Getter private final Sequence sequence;
	private final MidiReceiver receiver;

	public MidiPlayer(File file, int loopCount, MidiReceiver receiver) 
			throws InvalidMidiDataException, MidiUnavailableException, IOException {
		
		this.receiver = receiver;
		this.file = file;
		
		// sequencer = new Sequenca();// TODO velocity/volume gets weird with this sequencer
		sequencer = MidiSystem.getSequencer(false);
		sequence = MidiSystem.getSequence(file);
		sequencer.setSequence(sequence);
		sequencer.setLoopCount(loopCount);
		for (Receiver old : sequencer.getReceivers()) 
			old.close();
		sequencer.getTransmitter().setReceiver(receiver);
	}
	
	@Override
	public void start() throws MidiUnavailableException {
		if (!sequencer.isOpen()) {
			sequencer.open();
		}
		log.debug("Midi player starting. " + file.getAbsolutePath());
		sequencer.start();
//		if (intro != null && intro == 0) {
//			sequenca.rollTransport();
//			sequencer.addControllerEventListener(sequenca, controllers);
//		}
		new Thread() {
			@Override public void run() {
				do {try { 
						Thread.sleep(500);
					} catch (InterruptedException e) { }
				} while (sequencer.isRunning());
				if (sequencer.isOpen())
					sequencer.close();
			}}.start();
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

	public void setGain(float gain) {
		receiver.setGain(gain);
	}
	
	@Override
	public boolean isRunning() {
		return sequencer.isRunning();
	}
	
	public void setTempo(float tempo) {
		sequencer.setTempoFactor(tempo/100f);
	}
	
	// @Override from TimeProvider
	public void setDuration(Integer intro, Integer duration) {
//		this.intro = intro;
//		this.duration = duration;
	}
	
	@Override
	public String toString() {
		return MidiPlayer.class.getSimpleName() + " (" + file + ")";
	}

	@Override
	public void update(Property prop, Object value) {
		if (value == Status.ACTIVE) 
			try { start();
			} catch (MidiUnavailableException e) {
				log.error(e.getMessage(), e);
			}
		if (value == Status.TERMINATED) stop();
		if (prop == Property.TEMPO) 
			setTempo((Float)value);
		if (prop == Property.VOLUME) 
			setGain((Float)value);
	}
	
	
//	/** for now this only handles 1 bar midi clicktracks */
//	@Override
//	public void controlChange(ShortMessage event) {
//		if (event.getData1() != 3) return;
//		int beats = ++cc3 * sequenca.getMeasure();
//		if (intro != null && beats == intro) 
//			sequenca.rollTransport();
//			sequencer.addControllerEventListener(sequenca, controllers);
//		if (duration != null && beats == duration)
//			stop();
//	}

//	@Override
//	public void meta(MetaMessage meta) {
//		// log.warn("meta midi: " + meta.getStatus() + "." + meta.getType());
//	}

}
