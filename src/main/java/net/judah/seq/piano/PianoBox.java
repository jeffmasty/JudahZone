package net.judah.seq.piano;

import static net.judah.seq.MidiTools.*;

import java.awt.*;
import java.awt.event.MouseEvent;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import net.judah.api.Signature;
import net.judah.gui.Pastels;
import net.judah.midi.Midi;
import net.judah.seq.*;
import net.judah.seq.Edit.Type;
import net.judah.seq.beatbox.BeatsSize;
import net.judah.util.RTLogger;

/** display midi music in piano grid */
public class PianoBox extends MusicBox implements BeatsSize {
	/** six octaves */
	public static final int VISIBLE_NOTES = 6 * 12 + 1;

	private final Piano piano;
	private final PianoSteps steps;
	private final int width, height;
	private float unit;
	
	public PianoBox(Rectangle r, MidiView view, PianoSteps currentBeat, Piano roll, MidiTab tab) {
		super(view, r, tab);
		width = r.width;
		height = r.height;
		this.steps = currentBeat;
		this.piano = roll;
		timeSig(view.getTrack().getClock().getTimeSig());
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
				g.fillRect(x, 0, KEY_WIDTH, height);
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
			if (selected.contains(p.getOn().getTick(), s.getData1()))
				g.setColor(highlightColor(s.getData2()));
			else 
				g.setColor(velocityColor(s.getData2()));
			g.fill3DRect(x, y, KEY_WIDTH, yheight, true);
		}
		g.setColor(Pastels.FADED);
		g.drawRect(0, 0, width-1, height - 1); // border
		
		if (mode == null)
			return;
		
		if (mode == DragMode.SELECT || mode == DragMode.CREATE) {
			// highlight drag region
			Point origin = getLocationOnScreen();
			Point mouse = MouseInfo.getPointerInfo().getLocation();
			int ex = mouse.x - origin.x;
			int why = mouse.y - origin.y;
			Rectangle square = new Rectangle(drag.x, drag.y, ex - drag.x, why - drag.y);
			Graphics2D g2d = (Graphics2D)g;
			Composite original = g2d.getComposite();
			g2d.setPaint(mode == DragMode.SELECT ? Pastels.BLUE : Pastels.ORANGE);
			g2d.setComposite(transparent);
			g2d.fill(square);
			g2d.setComposite(original);
		}		
		// else if (mode == DragMode.TRANSLATE) // shown as selected
	}

	@Override public void timeSig(Signature sig) {
		unit = height / (2f * sig.steps);
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
		MidiPair existing = MidiTools.lookup(dat.tick, dat.data1, t);

		if (mouse.isShiftDown()) { // drag-n-select;
			drag = mouse.getPoint();
			mode = DragMode.SELECT;
		}
		else if (existing == null) {
			click = new Prototype(dat.data1, track.quantize(dat.tick));
			drag = mouse.getPoint();
			mode = DragMode.CREATE;
		}
		else if (mouse.isControlDown()) {
			if (selected.contains(existing))
				selected.remove(existing);
			else 
				selected.add(existing);
		}
		else {
			if (selected.contains(existing)) {
				dragStart(mouse.getPoint()); // already selected, start drag-n-drop
			}
			else { // simple select click
				selected.clear();
				selected.add(existing);
			}
		}
		repaint();
	}
	
	@Override public void mouseReleased(MouseEvent mouse) {
		if (mode != null) {
			switch(mode) {
			case CREATE: try {
				long tick = 1 + toTick(mouse.getPoint());
				MidiEvent off = Midi.createEvent(tick, NOTE_OFF, track.getCh(), click.getData1(), 1);
				if (click.getTick() > off.getTick()) {
					long temp = click.getTick();
					click.setTick(off.getTick());
					off.setTick(temp);
					// RTLogger.log(this, "swapping " + off.getTick() + " to " + on.getTick());				
				}
				off.setTick(track.quantizePlus(tick));
				MidiEvent start = Midi.createEvent(click.getTick(), NOTE_ON, 
						track.getCh(), click.getData1(), (int) (track.getAmp() * 127f));
				push(new Edit(Type.NEW, new MidiPair(start, off)));
				} catch (Exception e) {
					RTLogger.warn(this, e); }
				break;
			case SELECT: 
				if (drag == null) break;
				Prototype a = translate(drag);
				Prototype b = translate(mouse.getPoint());
				drag = null;
				selectArea(a.getTick(), b.getTick(), a.getData1(), b.getData1());
				break;
			case TRANSLATE: 
				drop(mouse.getPoint());
				break;
			}
			mode = null;
			repaint();
		}
		
	}
	
	@Override
	public void drag(Point mouse) {
		piano.highlight(toData1(mouse));
		steps.highlight(mouse);
		super.drag(mouse);
	}
	
//	@Override
//	public void keyTyped(KeyEvent e) {
//		super.keyTyped(e);
//		if (chromaticKeyboard(e.getKeyCode()) > 0) {
//			
//		}
//		
//	}
	/**@return Z to COMMA keys are white keys, and black keys, up to 12, no match = -1*/
	public static int chromaticKeyboard(final char key) {
		switch(key) {
			case 'z': return 0; // low C
			case 's': return 1; 
			case 'x': return 2;
			case 'd': return 3;
			case 'c': return 4;
			case 'v': return 5; // F
			case 'g' : return 6;
			case 'b' : return 7; // G
			case 'h': return 8; 
			case 'n': return 9;
			case 'j': return 10;
			case 'm': return 11;
			case ',': return 12;// high C
			default: return -1;
		}
	}
	
}


//	public boolean keyPressed(final int keycode) { 
//		int note = chromaticKeyboard(keycode);
//		if (note < 0) {
//			if (keycode == VK_UP) 
//				return setOctave(true); 
//			else if (keycode == VK_DOWN)
//				return setOctave(false);
//			else 
//				return false;
//		}
//		track.getMidiOut().send(Midi.create(Midi.NOTE_ON, track.getCh(), note + (octave * 12), 99), JudahMidi.ticker());
//		return true;
//	}
//	public boolean keyReleased(int keycode) {
//		int note = chromaticKeyboard(keycode);
//		if (note < 0)
//			return false;
//		track.getMidiOut().send(Midi.create(Midi.NOTE_OFF, track.getCh(), note + (octave * 12), 99), JudahMidi.ticker());
//		return true;
//	}
