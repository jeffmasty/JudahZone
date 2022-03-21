package net.judah.metronome;

import java.io.File;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Receiver;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import lombok.Getter;
import net.judah.api.TimeListener;
import net.judah.api.TimeProvider;
import net.judah.clock.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

/**Midi file Player/Metronome
 * @author judah
 */
public class MidiGnome implements Player, TimeListener {
		File dummy = 
		new File("/home/judah/tracks/beatbuddy/midi_sources/HIP HOP/Hip Hop Song 8/HIP HOP- Beat 21- swung hats simple kick.mid");

	
	@Getter private File file;
	@Getter private Sequencer sequencer;
	@Getter private Sequence sequence;
	@Getter private boolean running;
	private final Receiver receiver;
	private int barCount, restart;
	private final TimeProvider clock;
	
	public MidiGnome(File file) throws MidiUnavailableException {
		this(JudahClock.getInstance(), JudahMidi.getInstance());
		setFile(file);
	}
	
	public MidiGnome(TimeProvider clock, JudahMidi queue) throws MidiUnavailableException {
		this.clock = clock;
		receiver = new JackReceiver(JudahMidi.getInstance().getSynthOut());
		sequencer = MidiSystem.getSequencer(false);
		sequencer.setLoopCount(0); // listener fires off each repetition
		for (Receiver old : sequencer.getReceivers()) 
			old.close();
		
		sequencer.getTransmitter().setReceiver(receiver);
		sequencer.setTempoInBPM(100);

	}
	
	public void setFile(File file) {
		stop();
		this.file = file;
		if (file == null) 
			return;
		try {
			sequence = MidiSystem.getSequence(file);
			restart = Constants.requeueBeats(sequence) / clock.getMeasure();
			sequencer.setSequence(sequence);
		} catch (Exception e) {
			RTLogger.warn(this, e);
			file = null;
		}
		
	}
	
	@Override
	public void update(Property prop, Object value) {
		// restart loop or update tempo
		if (Property.TEMPO == prop) {
			sequencer.setTempoFactor(clock.getTempo() * 0.01f);
		}
		else if (Property.BARS == prop) {
			barCount++;
			if (barCount == restart && running) {
				sequencer.setMicrosecondPosition(0);
				sequencer.setTempoInBPM(100);
				sequencer.setTempoFactor(clock.getTempo() * 0.01f);
				sequencer.start();
				barCount = 0;
			} 
		}
		
		
	}

	@Override
	public void start() throws MidiUnavailableException {
		if (file == null) {
			RTLogger.warn(this, "No midi file to play.");
			return;
		}
		if (!sequencer.isOpen())
			sequencer.open();
		try {
			sequencer.setMicrosecondPosition(0);
			sequencer.setTempoInBPM(100);
			sequencer.setTempoFactor(clock.getTempo() * 0.01f);
			sequencer.start();
			barCount = 0;
			running = true;
			clock.addListener(this);
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}

	@Override
	public void stop() {
		if (sequencer.isRunning()) sequencer.stop();
		clock.removeListener(this);
		running = false;
	}

	@Override
	public void close() {
		stop();
		receiver.close();
		if (sequencer.isOpen()) sequencer.close();
		
	}

	@Override
	public void setDuration(Integer intro, Integer duration) {
	}

}
