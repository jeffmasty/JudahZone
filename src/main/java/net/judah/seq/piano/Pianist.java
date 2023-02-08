package net.judah.seq.piano;

import static java.awt.event.KeyEvent.*;
import static net.judah.seq.MidiView.ratioY;

import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import net.judah.midi.Midi;
import net.judah.seq.*;
import net.judah.util.RTLogger;

public class Pianist extends Musician implements PianoSize  {
	
	private final PianoMusic grid;
	private final Piano piano;
	private final Track t;

	public Pianist(Piano piano, PianoMusic grid, MidiView view, MidiTab tab) {
		super(view, tab);
		this.piano = piano;
		this.grid = grid;
		t = track.getT();
	}
	
	@Override // absolute based tick
	public long toTick(Point p) {
		return (long) (ratioY(p.y, GRID_HEIGHT) * track.getWindow() + 
				track.getLeft());
	}

	@Override
	public int toData1(Point p) {
		return p.x / KEY_WIDTH + NOTE_OFFSET;
	}

	@Override
	public void mouseExited(MouseEvent e) {
		piano.highlight(-1);
		steps.highlight(null);
	}

	@Override
	public void mouseMoved(MouseEvent mouse) {
		Point p = mouse.getPoint();
		piano.highlight(toData1(p));
		steps.highlight(p);
	}

		
	@Override
	public void mousePressed(MouseEvent mouse) {
		Prototype dat = translate(mouse.getPoint());
		MidiPair existing = lookup(dat.tick, NOTE_ON, dat.data1);
		if (existing == null) {
			long tick = quantize(dat.tick, 
					(Gate)view.getMenu().getGate().getSelectedItem(), track.getResolution());
			on = new Prototype(dat.data1, tick);
		}
		else if (mouse.isControlDown()) {
			if (selected.contains(existing))
				selected.remove(existing);
			else 
				selected.add(existing);
		}
		else {
			selected.clear();
			selected.add(existing);
		}
		view.getGrid().repaint();
	}
	
	@Override public void mouseReleased(MouseEvent mouse) {
		if (on == null)
			return;
		if (isDrag()) {
			super.mouseReleased(mouse);
			return;
		}
		try {
			long tick = toTick(mouse.getPoint());
			MidiEvent off = create(tick, NOTE_OFF, on.getData1(), 1);
			if (on.getTick() > off.getTick()) {
				long temp = on.getTick();
				on.setTick(off.getTick());
				off.setTick(temp);
				RTLogger.log(this, "swapping " + off.getTick() + " to " + on.getTick());				
			}
			off.setTick(-1 + quantizePlus(tick, 
					(Gate)view.getMenu().getGate().getSelectedItem(), track.getResolution()));
			MidiEvent start = create(on.getTick(), 
					NOTE_ON, on.getData1(), (int) (track.getAmp() * 127f));
			t.add(start);
			t.add(off);
			selected.clear(); // add zero-based selection
			selected.add(new MidiPair(start, off)); 
			on = null;
			grid.repaint();
			drag = false;
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
		
	}
	
	private MidiEvent create(long tick, int cmd, int data1, int data2) throws InvalidMidiDataException {
		Midi midi = new Midi(cmd, track.getCh(), data1, data2);
		return new MidiEvent(midi, tick);
	}

	private MidiPair lookup(long tick, int cmd, int data1) {
		MidiEvent found = null;
		for (int i = 0; i < t.size(); i++) {

			if (false == t.get(i).getMessage() instanceof ShortMessage) continue;
			MidiEvent e = t.get(i);
			ShortMessage m = (ShortMessage) e.getMessage();
			
			if (e.getTick() <= tick) {
				if (found == null) {
					if (Midi.isNoteOn(m) && m.getData1() == data1)
						found = e;
				}
				else if (found != null) {
					if (Midi.isNoteOff(m) && m.getData1() == data1)
						found = null;		}
			}
			else { // e.getTick() > tick
				if (found == null) // opportunity passed
					return null;
				if (Midi.isNoteOff(m) && m.getData1() == data1) {
					return new MidiPair(found, e);
				}
			}
		}
		if (found != null) 
			return new MidiPair(found, null);
		return null;
	}

	public long quantizePlus(long tick, Gate type, int resolution) {
		switch(type) {
		case SIXTEENTH: return quantize(tick, type, resolution) + (resolution / 4);
		case EIGHTH:	return quantize(tick, type, resolution) + (resolution / 2);
		case QUARTER:	return quantize(tick, type, resolution) + (resolution);
		case HALF:		return quantize(tick, type, resolution) + (2 * resolution);
		case WHOLE: 	return quantize(tick, type, resolution) + (4 * resolution);
		case MICRO:		return quantize(tick, type, resolution) + (resolution / 8);
		case RATCHET:	return quantize(tick, type, resolution) + RATCHET;
		default: return tick;
		}
	}

	// TODO odd subdivision
	public long quantize(long tick, Gate type, int resolution) {
		switch(type) {
		case SIXTEENTH: return tick - tick % (resolution / 4);
		case EIGHTH: return tick - tick % (resolution / 2);
		case QUARTER: return tick - tick % resolution;
		case HALF: return tick - tick % (2 * resolution);
		case WHOLE: return tick - tick % (4 * resolution);
		case MICRO: return tick - tick % (resolution / 8);
		case RATCHET: return tick - tick % MidiConstants.RATCHET; // approx MIDI_24
		default: // NONE
			return tick;
		}
	}

	
	/**@return Z to COMMA keys are white keys, and black keys, up to 12, no match = -1*/
	public static int chromaticKeyboard(final int keycode) {
		switch(keycode) {
			case VK_Z: return 0; // low C
			case VK_S: return 1; 
			case VK_X: return 2;
			case VK_D: return 3;
			case VK_C: return 4;
			case VK_V: return 5; // F
			case VK_G : return 6;
			case VK_B : return 7; // G
			case VK_H: return 8; 
			case VK_N: return 9;
			case VK_J: return 10;
			case VK_M: return 11;
			case VK_COMMA: return 12;// high C
			default: return -1;
		}
	}

}
