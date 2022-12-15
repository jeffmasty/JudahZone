package net.judah.seq.piano;

import static java.awt.event.KeyEvent.*;
import static net.judah.seq.MidiView.*;

import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;

import lombok.RequiredArgsConstructor;
import net.judah.midi.Midi;
import net.judah.seq.Bar;
import net.judah.seq.MidiPair;
import net.judah.seq.MidiSize;
import net.judah.seq.MidiTrack;
import net.judah.seq.MidiView;
import net.judah.seq.MidiView.Source;
import net.judah.util.RTLogger;

@RequiredArgsConstructor
public class Pianist extends MouseAdapter implements MidiSize  {
	
	private final MidiTrack track;
	private final Piano piano;
	private final PianoGrid grid;
	private final PianoSteps steps;
	private final MidiView view;

	private MidiEvent on;

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

		
	@Override public void mouseClicked(MouseEvent e) { }
	@Override public void mouseEntered(MouseEvent e) { }
	
	@Override
	public void mousePressed(MouseEvent mouse) {
		boolean controlled = mouse.isControlDown();
		if (controlled) {
			RTLogger.log(this, "TODO: multi-select");
		}
		Point p = mouse.getPoint();
		if (p.y < BOUNDS_GRID.y)
			return;
		Source source = source(p);
		if (source == Source.Grid) {
			try {
				int data1 = toData1(p);
				MidiPair select = lookup(toTick(p, view.getTimeframe()), NOTE_ON, data1);
				if (select != null ) {
					RTLogger.log(this, "SELECT! " + select.getOn().getMessage());
					view.getSelected().add(select);
				} else if (!controlled) {
					long tick = toTick(p, view.getTimeframe());
					// tick = quantize(tick, track.getResolution(), (Gate)menu.getGate().getSelectedItem());
					on = create(tick, NOTE_ON, data1, (int)(view.getVelocity().getValue() * 1.27f));
					RTLogger.log(this, "pressed @" + on.getTick() + " " + on.getMessage());
				}
			} catch (Exception err) {
				RTLogger.warn(this, err);
			}
		}
	}
	
	@Override public void mouseReleased(MouseEvent mouse) {
		if (source(mouse.getPoint()) != Source.Grid)
			return;
		if (on == null)
			return;
		try {
			int data1 = ((ShortMessage)on.getMessage()).getData1();
			long tick = toTick(mouse.getPoint(), view.getTimeframe());
			// tick = quantize(tick, track.getResolution(), (Gate)menu.getGate().getSelectedItem());
			MidiEvent off = create(tick, NOTE_OFF, data1, view.getVelocity().getValue());
			if (on.getTick() > off.getTick()) {
				long temp = on.getTick();
				on.setTick(off.getTick());
				off.setTick(temp);
				RTLogger.log(this, "swapping " + off.getTick() + " to " + on.getTick());				
			}
			
			// split to 2 bar range and add to track
			final long measure = track.getTicks();
			Bar onBar = view.getSnippet().one;
			if (on.getTick() > measure) {
				onBar = view.getSnippet().two;
				on.setTick(on.getTick() - measure);
			}
			Bar offBar = view.getSnippet().one;
			if (off.getTick() > measure) {
				offBar = view.getSnippet().two;
				off.setTick(off.getTick() - measure);
			}
			onBar.add(on);
			offBar.add(off);
			track.getScheduler().hotSwap();
			grid.repaint();
			RTLogger.log(this, data1 + "From " + on.getTick() + " to " + off.getTick());
			on = null;
		} catch (Exception e) {
			RTLogger.warn(this, e);
		}
		
	}
	
	@Override
	public void mouseDragged(MouseEvent mouse) {
		mouseMoved(mouse);
		// TODO
	}
	
	@Override
	public void mouseExited(MouseEvent e) {
		piano.highlight(-1);
		steps.highlight(null);
	}

	@Override
	public void mouseMoved(MouseEvent mouse) {
		Point p = mouse.getPoint();
		Source s = source(p);
		if (s == Source.Grid || s == Source.Steps) {
			piano.highlight(toData1(p));
			steps.highlight(p);
		}
		else { 
			piano.highlight(-1);
			steps.highlight(null);
		}
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent wheel) { // TODO
//		boolean up = wheel.getPreciseWheelRotation() < 0;
//		int velocity = menu.getVelocity().getValue() + (up ? 5 : -1); // ??5
//		if (velocity < 0)
//			velocity = 0;
//		if (velocity > 100)
//			velocity = 99;
//		menu.getVelocity().setValue(velocity);
	}

	private MidiEvent create(long tick, int cmd, int data1, int data2) throws InvalidMidiDataException {
		Midi midi = new Midi(cmd, track.getCh(), data1, data2);
		return new MidiEvent(midi, tick);
	}

	private MidiPair lookup(long tick, int cmd, int data1) throws InvalidMidiDataException {
		MidiEvent on = track.getBar().reverseSearch(NOTE_ON, tick, data1);
		if (on != null) {
			MidiEvent off = track.getBar().search(NOTE_OFF, on.getTick(), data1);
			if (off == null || off.getTick() > tick)
				return new MidiPair(on, off);
		}
		return null;
	}
	public void deleteKey() {
		for (MidiPair remove : view.getSelected()) {
			track.getBar().remove(remove.getOn());
			track.getBar().remove(remove.getOff());
			grid.repaint();
		}
	}

	
}
