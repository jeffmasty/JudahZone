package net.judah.midi;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.ShortMessage;

import judahzone.api.Midi;
import judahzone.gui.Updateable;
import judahzone.util.RTLogger;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.judah.gui.MainFrame;
import net.judah.synth.ZoneMidi;

/** listens to and tracks note_on and note_off going out a Midi port.
 *  Provides RT-safe snapshot reads via isActiveSafe() and deferred mutations
 *  via requestRemove() + processPendingRemovals(). */
@RequiredArgsConstructor @Getter
public class Actives implements Updateable {

	protected final ZoneMidi midiOut;
	protected final int channel;
	protected boolean pedal;
	protected final ArrayList<ShortMessage> active = new ArrayList<ShortMessage>(); // realtime
	protected final ArrayList<ShortMessage> sustained = new ArrayList<ShortMessage>(); // pedal
	private volatile CopyOnWriteArrayList<Integer>
		activeSnapshot = new CopyOnWriteArrayList<>(); // GUI-facing

	/////////
	//// LIST
	public boolean isEmpty() {
		return active.isEmpty(); // activeSnapshot?
	}
	public int size() {
		return active.size(); // activeSnapshot?
	}

	/** RT-safe: queue note-on.
	 * @return expected slot of msg*/
	public int add(ShortMessage msg) {
		active.add(msg);
		MainFrame.update(this);
		return active.indexOf(msg);
	}

	/** RT-safe: queue note replacement at index. */
	public ShortMessage set(int idx, ShortMessage msg) {
		ShortMessage result = active.set(idx, msg);
		MainFrame.update(this);
		return result;
	}

	/** RT-safe: queue note removal by data1. */
	public void removeData1(int data1) {
		int idx = indexOf(data1);
		if (idx >= 0)
			active.remove(idx);
		MainFrame.update(this);
	}

	/** Process queued mutations on GUI/MIDI thread. */
	@Override public synchronized void update() {
		updateSnapshot();
	}

	/** Update volatile snapshot (call from update() only). */
	private synchronized void updateSnapshot() {
		CopyOnWriteArrayList<Integer> snap = new CopyOnWriteArrayList<>();
		for (ShortMessage m : active)
			if (m != null)
				snap.add(m.getData1());
		activeSnapshot = snap;
	}
	// index-based removeRequest
	public void removeIndex(int idx) {
		if (activeSnapshot.size() > idx)
			removeData1(activeSnapshot.get(idx));

	}

	public boolean receive(ShortMessage msg) {
	    if (Midi.isNoteOn(msg)) {
	        if (pedal && indexOf(msg.getData1()) >= 0) {
	            retrigger(msg);
	            return true;
	        }
	        add(msg);
	        return true;
	    }
	    if (Midi.isNoteOff(msg))
	    	return noteOff(msg);

	    return true;
	}

    protected void retrigger(ShortMessage msg) {
		((MidiInstrument)midiOut).write(Midi.create(
				Midi.NOTE_OFF, channel, msg.getData1(), msg.getData2()));
    }

	/** Real-time safe: check if a MIDI note (data1) is active without taking locks. */
	public boolean isActive(int data1) {
		CopyOnWriteArrayList<Integer> snap = activeSnapshot;
		return snap != null && snap.contains(data1);
	}

	/**@param ref  fill ref with data1 midi values of current voices */
	public void data1(Set<Integer> ref) {
		ref.clear();
		for (int i = 0; i < active.size(); i++)
			if (active.get(i) != null)
				ref.add(active.get(i).getData1());
	}

	public ShortMessage get(int idx) {
		return active.get(idx);
	}

	public int indexOf(int data1) {
		for (int i = 0; i < active.size(); i++)
			if (active.get(i) != null && active.get(i).getData1() == data1)
				return i;
		return -1;
	}

	public ShortMessage find(int data1) {
		int idx = indexOf(data1);
		if (idx >= 0)
			return active.get(idx);
		return null;
	}

	protected int susOf(int data1) {
		for (int i= 0; i < sustained.size(); i++)
			if (sustained.get(i).getData1() == data1)
				return i;
		return -1;
	}

	/** Polyphony class overrides for JudahSynth */
	protected boolean noteOff(ShortMessage msg) {
		if (pedal) {
			if (susOf(msg.getData1()) < 0)
				sustained.add(Midi.create(Midi.NOTE_OFF, channel, msg.getData1(), msg.getData2()));
			return false;
		}
		int idx = indexOf(msg.getData1());
		if (idx >= 0) {
			active.remove(idx);
			MainFrame.update(this);
		}
		return true;
	}

	/** engage or release the foot pedal (CC64) */
	public void setPedal(boolean pressed) {
		pedal = pressed;
		if (pedal) // engaged
			return;
		try {
			for (ShortMessage sus : sustained)
				JudahMidi.queue(new ShortMessage(ShortMessage.NOTE_OFF, channel, sus.getData1(), 0), ((MidiInstrument)midiOut).getMidiPort());
			sustained.clear();
		} catch (InvalidMidiDataException e) {RTLogger.warn(this, e);}
	}

}
