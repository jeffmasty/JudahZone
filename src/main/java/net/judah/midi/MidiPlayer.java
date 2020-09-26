package net.judah.midi;

import static javax.sound.midi.Sequencer.*;

import java.io.File;
import java.io.IOException;

import javax.sound.midi.ControllerEventListener;
import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MetaEventListener;
import javax.sound.midi.MetaMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.ShortMessage;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.JudahZone;
import net.judah.sequencer.MetroPlayer;
import net.judah.sequencer.Sequencer;

@Log4j
public class MidiPlayer implements MetroPlayer, ControllerEventListener, MetaEventListener {
	/** loop count for continuous looping */
	private static final int[] controllers = new int[] {3};
	
	@Getter private final File file; 
	@Getter private final javax.sound.midi.Sequencer sequencer;
	@Getter private final Sequence sequence;
	private final Sequencer sequenca;
	
	private Thread listener;
	private final JudahReceiver receiver;

	private Integer intro;
	private Integer duration;
	private int cc3 = -1;
	
	public MidiPlayer(File file, int loopCount, JudahReceiver receiver, Sequencer sequenca) 
			throws InvalidMidiDataException, MidiUnavailableException, IOException {
		
		this.sequenca = sequenca;
		this.receiver = receiver;
		
		this.file = file;
		sequencer = MidiSystem.getSequencer(false);
		sequence = MidiSystem.getSequence(file);
		sequencer.setSequence(sequence);
		sequencer.setLoopCount(loopCount);
		sequencer.setTempoInBPM(100);

		// sequencer.addMetaEventListener(this);
		sequencer.addControllerEventListener(this, controllers);

		for (Receiver old : sequencer.getReceivers()) 
			old.close();
		sequencer.getTransmitter().setReceiver(receiver);
		
	}
	
	public MidiPlayer(File file, Sequencer sequencer) throws InvalidMidiDataException, MidiUnavailableException, IOException {
		this(file, LOOP_CONTINUOUSLY, new JudahReceiver(), sequencer);
	}
	
	public MidiPlayer(File file) throws MidiUnavailableException, InvalidMidiDataException, IOException {
		this(file, LOOP_CONTINUOUSLY, new JudahReceiver(), null);
	}

	@Override
	public void start() throws MidiUnavailableException {
		if (!sequencer.isOpen()) {
			sequencer.open();
		}
		sequencer.setTempoFactor(JudahZone.getCurrentSong().getTempo()/100f);
		log.info("Midi player starting. " + file.getAbsolutePath());
		sequencer.start();
		if (intro != null && intro == 0)
			sequenca.rollTransport();
			sequencer.addControllerEventListener(sequenca, controllers);
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
	
	public void setTempo(float tempo) {
		sequencer.setTempoFactor(tempo/100f);
	}
	
	@Override
	public void setDuration(Integer intro, Integer duration) {
		this.intro = intro;
		this.duration = duration;
	}
	
	@Override
	public String toString() {
		return MidiPlayer.class.getSimpleName() + " (" + file + ")";
	}
	
	
	/** for now this only handles 1 bar midi clicktracks */
	@Override
	public void controlChange(ShortMessage event) {
		if (event.getData1() != 3) return;
		int beats = ++cc3 * sequenca.getMeasure();
		if (intro != null && beats == intro) 
			sequenca.rollTransport();
			sequencer.addControllerEventListener(sequenca, controllers);
		if (duration != null && beats == duration)
			stop();
	}

	@Override
	public void meta(MetaMessage meta) {
		// log.warn("meta midi: " + meta.getStatus() + "." + meta.getType());
	}

}
