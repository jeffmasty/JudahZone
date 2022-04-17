package net.judah.tracks;

import static net.judah.api.AudioMode.*;

import java.io.File;
import java.util.concurrent.atomic.AtomicReference;

import javax.sound.midi.*;

import org.jaudiolibs.jnajack.JackTransportState;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import net.judah.api.AudioMode;
import net.judah.api.Notification.Property;
import net.judah.clock.JudahClock;
import net.judah.midi.JudahMidi;
import net.judah.settings.MidiSetup.OUT;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

@Data @EqualsAndHashCode(callSuper = true)
public class MidiTrack extends Track implements Receiver {

	@Getter private final MidiFeedback display = new MidiFeedback();
	@Getter private Sequencer sequencer;
	@Getter private Sequence sequence;
	private int barCount, restart;
	private AtomicReference<AudioMode> mode = new AtomicReference<>(AudioMode.NEW);

	public MidiTrack(JudahClock clock, String name, Type type, OUT port, File folder) {
		super(clock, name, type, port, folder);
		feedback = display;
	}
	
	private void open() {
		try {
			sequencer = MidiSystem.getSequencer(false);
			sequencer.open();
			sequencer.setLoopCount(0); // listener fires off each repetition
			for (Receiver old : sequencer.getReceivers()) 
				old.close();
			sequencer.getTransmitter().setReceiver(this);
		} catch (MidiUnavailableException e) {
			RTLogger.warn(this, e);
		}
	}

	@Override
	public void setFile(File file) {
		boolean wasRunning = isActive();
		this.file = file;
		if (file == null) {
			return;
		}
		new Thread(() -> {
			try {
				open();
				sequence = MidiSystem.getSequence(file);
				stop();
				sequencer.setSequence(sequence);
				restart = Constants.requeueBeats(sequence) / clock.getMeasure();
				display.setSequence(sequencer, restart);
				setup();
				if (wasRunning) 
					start();
			} catch (Exception e) {
				RTLogger.warn(this, e);
				this.file = null;
			}
		}).start();
	}
	
	/*----- Player Interface ------*/
	public void start() {
		if (file == null) 
			return;
		try {
			if (sequencer.isRunning()) sequencer.stop();
			setup();
			// TODO try to get current beat from clock, set micro position
			// JudahClock.getStep(); JudahClock.getSubdivision();
			// RTLogger.log(this, file.getName() + " bars: " + restart);
			sequencer.start();
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}

	private void setup() {
		if (file == null) 
			return;
		try {
			sequencer.setMicrosecondPosition(0);
			sequencer.setTempoInBPM(clock.getTempo() + 0.01f);
			sequencer.setTempoFactor(1f);
			barCount = 1;
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
	}
	
	public void stop() {
		if (sequencer != null && sequencer.isRunning()) 
			sequencer.stop();
		mode.set(STOPPED);
	}
	
	@Override
	public void close() {
		stop();
		if (sequencer.isOpen()) sequencer.close();
		super.close();
	}
	
	@Override
	public void setActive(boolean active) {
		if (!active && sequencer.isRunning())
			sequencer.stop();
		if (active)
			if (mode.get() != RUNNING) 
				mode.set(STARTING);
		else 
			mode.set(STOPPING);
		
		super.setActive(active);
	}
	
	@Override
	public void update(Property prop, Object value) {
		if (mode.get() == STOPPING) {
			stop();
			return;
		}
		if (Property.BARS == prop) {
			if (mode.get() == STARTING) {
				setup();
				if (!sequencer.isRunning()) 
					sequencer.start();
				mode.set(RUNNING);
				barCount = 1;
			} 
			else  if (++barCount > restart) { // restart midi loop
				start();
			} 
			display.barCount(barCount);
		}

		else if (active && Property.BEAT == prop)
			display.tick();
		
		else if (Property.TEMPO == prop) {
			if (sequencer == null || sequencer.isOpen() == false) return;
			sequencer.setTempoFactor(clock.getTempo() / sequencer.getTempoInBPM());
		}
		else if (prop == Property.TRANSPORT 
				&& JackTransportState.JackTransportStopped == value)
			stop();
		else if (active && prop == Property.TRANSPORT 
				&& JackTransportState.JackTransportStarting == value) {
			mode.set(STARTING);
			start();
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
	public boolean process(int knob, int data2) {
		switch (knob) { // TODO
			case 4: // Channel
				if (JudahMidi.getInstance().getCraveOut() == midiOut)
					return true; // no-op
				//ch = Constants.ratio(data2, 16);
				return true; 
			case 7: // 
				return true;
		}
		return false;
	}


}
