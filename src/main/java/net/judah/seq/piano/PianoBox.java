package net.judah.seq.piano;

import static java.awt.event.KeyEvent.*;
import static net.judah.seq.MidiTools.*;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;

import net.judah.gui.Pastels;
import net.judah.midi.Midi;
import net.judah.seq.*;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.util.RTLogger;

/** display midi music in piano grid */
public class PianoBox extends MusicBox implements BeatsSize {
	public static final int VISIBLE_NOTES = 6 * 12 + 1;

	
	private final Piano piano;
	private final PianoSteps steps;
	private final int width, height;
	private final Track t;
	private float unit;
	
	public PianoBox(Rectangle r, MidiView view, PianoSteps currentBeat, Piano roll, MidiTab tab) {
		super(view, r, tab);
		width = r.width;
		height = r.height;
		this.steps = currentBeat;
		this.piano = roll;
		t = view.getTrack().getT();
		timeSig();
	}
	
	@Override public long toTick(Point p) {
		return (long) (p.y / (float)GRID_HEIGHT * track.getWindow() + track.getLeft());
	}

	@Override public int toData1(Point p) {
		return p.x / KEY_WIDTH + NOTE_OFFSET;
	}

	@Override public void paint(Graphics g) {
		super.paint(g);
		float ratio = height / (2f * track.getBarTicks());
		
		g.setColor(Pastels.FADED);
		int x; // columns (notes)
		for (int i = 0; i < VISIBLE_NOTES; i++) {
			x = i * KEY_WIDTH;
			if (piano.isLabelC(i)) {
				g.setColor(Pastels.BLUE);
				g.fillRect(x, 0, (int)unit, height);
				g.setColor(Pastels.FADED);
			}
			g.drawLine(x, 0, x, height);
		}

		int y; // rows (steps)
		for (int i = 0; i < 2 * clock.getSteps(); i++) {
			y = (int) (i * unit);
			if (steps.isBeat(i))
				g.fillRect(0, y, width, (int) unit);
			else 
				g.drawLine(0, (int)(y + unit), width, (int)(y + unit));
			if (steps.isBar(i)) {
				g.setColor(Color.GRAY);
				g.drawLine(0, y, width, y);
				g.setColor(Pastels.FADED);
			}			
		}

		scroll.populate();
		int yheight;
		for (MidiPair p : scroll) {
			
			if (p.getOn().getMessage() instanceof ShortMessage == false) continue;
			ShortMessage s = (ShortMessage)p.getOn().getMessage();
			x = KEY_WIDTH * Piano.data1ToGrid(s.getData1());
			
			
			y = (int) ((p.getOn().getTick() - track.getLeft()) * ratio);
			
			yheight = (int) ((p.getOff().getTick() - p.getOn().getTick()) * ratio);
			if (selected.isNoteSelected(p.getOn().getTick(), s.getData1()))
				g.setColor(highlightColor(s.getData2()));
			else 
				g.setColor(velocityColor(s.getData2()));
			g.fill3DRect(x, y, KEY_WIDTH, yheight, true);
		}
		g.setColor(Pastels.FADED);
		g.drawRect(0, 0, width-1, height - 1); // border
	}

	@Override public void timeSig() {
		unit = height / (2f * clock.getSteps());
		if (steps != null) steps.repaint();
		repaint();
	}
	
	@Override public void mouseExited(MouseEvent e) {
		piano.highlight(-1);
		steps.highlight(null);
	}

	@Override public void mouseMoved(MouseEvent mouse) {
		Point p = mouse.getPoint();
		piano.highlight(toData1(p));
		steps.highlight(p);
	}

		
	@Override public void mousePressed(MouseEvent mouse) {
		Prototype dat = translate(mouse.getPoint());
		MidiPair existing = lookup(dat.tick, NOTE_ON, dat.data1);
		if (existing == null) {
			long tick = quantize(dat.tick, getGate(), track.getResolution());
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
			off.setTick(-1 + quantizePlus(tick, getGate(), track.getResolution()));
			MidiEvent start = create(on.getTick(), 
					NOTE_ON, on.getData1(), (int) (track.getAmp() * 127f));
			t.add(start);
			t.add(off);
			selected.clear(); // add zero-based selection
			selected.add(new MidiPair(start, off)); 
			on = null;
			repaint();
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
