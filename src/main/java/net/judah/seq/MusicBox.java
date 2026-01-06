package net.judah.seq;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.sound.midi.Track;
import javax.swing.JPanel;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.gui.Floating;
import lombok.Getter;
import net.judah.gui.MainFrame;
import net.judah.gui.TabZone;
import net.judah.midi.JudahClock;
import net.judah.seq.Edit.Type;
import net.judah.seq.track.Editor;
import net.judah.seq.track.NoteTrack;

public abstract class MusicBox extends JPanel implements Musician, Floating, MidiConstants {

	public static enum DragMode { CREATE, TRANSLATE, SELECT }
	protected static final Composite transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

	@Getter protected final NoteTrack track;
	protected final Editor editor;
	protected final Track t;
	protected final JudahClock clock;

	/** absolute notes */
	@Getter protected final Notes selected = new Notes();
	protected final Measure scroll;
	protected ArrayList<MidiNote> dragging = null;
	protected Prototype click;
	protected Point drag = null;
	protected DragMode mode = null;
	protected Prototype recent;
	protected int width, height;

	// TODO new notes?, select them
	public MusicBox(NoteTrack midiTrack) {
		this.track = midiTrack;
		this.editor = midiTrack.getEditor();
		this.t = track.getT();
		this.clock = track.getClock();
		scroll = new Measure(track);
		addMouseListener(this);
		addMouseMotionListener(this);
		addMouseWheelListener(this);
	}

	@Override public void delete() {
		track.getEditor().push(new Edit(Type.DEL, selected));
	}

	@Override public void copy() {
		Editor.clipboard.copy(selected, track);
	}

	@Override public void mouseDragged(MouseEvent mouse) {
		if (mode == null)
			return;
		switch(mode) {
			case CREATE:
			case SELECT:
				repaint();
				break;
			case TRANSLATE:
				drag(mouse.getPoint());
				break;
		}
	}

	@Override public abstract void mouseReleased(MouseEvent e); // subclass implement
	@Override public abstract void mousePressed(MouseEvent e); // subclass implement;
	@Override public final void mouseEntered(MouseEvent e) {TabZone.instance.requestFocusInWindow();}
	@Override public void mouseMoved(MouseEvent e) { }
	@Override public void mouseExited(MouseEvent e) { }
	@Override public final void mouseClicked(MouseEvent e) { }

	@Override public final void mouseWheelMoved(MouseWheelEvent wheel) {
		boolean up = wheel.getPreciseWheelRotation() < 0;
		if (wheel.isControlDown())
			shiftVelocity(up);
		else if (wheel.isShiftDown())
			translate(up); // shift selected note timing

		else if (wheel.isAltDown()) {
			transpose(up); // shift selected note pitch
		}
		else
			track.next(!up);
	}

	@Override public void velocity(boolean up) {
		float velocity = track.getAmp() + (up ? 0.05f : -0.05f);
		if (velocity < 0)  velocity = 0;
		if (velocity > 1)  velocity = 1;
		track.setAmp(velocity);
	}

	private void shiftVelocity(boolean up) {
		if (selected.isEmpty()) {
			velocity(up);
			return;
		}
		// change selected notes' velocity
		float ratio = up ? 1.1f : 0.9f;
		ArrayList<MidiNote> replace = new ArrayList<>();
		int data2;
		ShortMessage source, create;
		for (MidiNote p : selected) {
			if (p.getMessage() instanceof ShortMessage == false)
				continue;
			source = (ShortMessage) p.getMessage();
			data2 = (int) (source.getData2() * ratio);
			if (data2 == source.getData2())
				data2 += up ? 1 : -1;
			if (data2 > 127)
				data2 = 127;
			else if (data2 < 1) // no ghosts
				data2 = 1;
			create = Midi.create(source.getCommand(), source.getChannel(), source.getData1(), data2);
			replace.add(new MidiNote(new MidiEvent(create, p.getTick()) ,p.getOff()));
		}
		// TODO
//		editDel(selected);
//		editAdd(replace);
	}

	private void transpose(boolean up) {
		if (selected.isEmpty()) return;
		MidiNote a = selected.getFirst();
		int src = ((ShortMessage)a.getMessage()).getData1();
		int dest = src + (up ?  1 : -1);
		if (dest < 0 || dest > 127)
			return;
		Edit e = new Edit(Type.TRANS, selected);
		e.setDestination(new Prototype(dest, a.getTick()));
		editor.push(e);
	}

	private void translate(boolean up) {
		if (selected.isEmpty()) return;
		MidiNote a = selected.getFirst();
		long src = a.getTick();
		long plus = track.quantizePlus(src);
		long dest = up ? plus : src - (plus - src);

		if (dest > track.getLeft() + track.getWindow())
			dest -= track.getWindow();
		if (dest < track.getLeft())
			dest += track.getWindow();

		Edit e = new Edit(Type.TRANS, selected);
		e.setDestination(new Prototype(((ShortMessage)a.getMessage()).getData1(), dest));
		editor.push(e);
	}

	@Override public final Prototype translate(Point p) {
		return new Prototype(toData1(p), toTick(p));
	}

	@Override
	public void selectNone() {
		selected.clear();
		MainFrame.update(track);
	}

	public Notes selectArea(long start, long end) {
		selected.clear();
		long tick;
		if (track.isDrums()) {
			scroll.populate();
			for (MidiNote p : scroll) {
				tick = p.getTick();
				if (tick < start) continue;
				if (tick >= end) break;
				selected.add(p);
			}
		}
		else {
			for (int i = 0; i < t.size(); i++) {
				tick = t.get(i).getTick();
				if (tick < start) continue;
				if (tick >= end) break;
				if (Midi.isNoteOn(t.get(i).getMessage()))
					selected.add(MidiTools.noteOff(t.get(i), t));
			}
		}
		repaint();
		return selected;
	}

	protected void select(List<MidiNote> drug) {
		selected.clear();
		for (MidiNote p : drug)
			if (p.getOff() == null)
				selected.add(new MidiNote(p));
			else
				selected.add(new MidiNote(p, p.getOff()));
	}

	public void select(Notes notes) {
		selected.clear();
		selected.addAll(notes);
		repaint();
	}

	public Notes selectBar(boolean left) {
		long start = left ? track.getLeft() : track.getRight();
		long end = start + track.getBarTicks();
		selectArea(start, end);
		return selected;
	}

	@Override
	public Notes selectFrame() {
		long start = track.getLeft();
		long end = start + track.getWindow();
		selectArea(start, end);
		return selected;
	}

	private final Rectangle shadeRect = new Rectangle();
	protected Rectangle shadeRect(int x, int y) {
		if (drag.x < x) {
			if (drag.y < y)
				shadeRect.setBounds(drag.x, drag.y, x - drag.x, y - drag.y);
			else
				shadeRect.setBounds(drag.x, y, x - drag.x, drag.y - y);
		} else {
			if (drag.y < y)
				shadeRect.setBounds(x, drag.y, drag.x - x, y - drag.y);
			else
				shadeRect.setBounds(x, y, drag.x - x, drag.y - y);
		}
		return shadeRect;
	}

	// Color cache
	static final int GRADIENTS = 32;
	private static final Color[] VELOCITIES = new Color[GRADIENTS];
	private static final Color[] HIGHLIGHTS = new Color[GRADIENTS];
	static {
		for (int i = 0; i < GRADIENTS; i++) {
		    int alpha = (int) ((i / (float)(GRADIENTS - 1)) * 255);
            VELOCITIES[i] = new Color(0, 112, 60, alpha); // Dartmouth Green
			HIGHLIGHTS[i] = new Color(0xFF, 0xA5, 0x00, alpha); // Orange
		}
	}
    public static Color velocityColor(int data2) {
    	return VELOCITIES[data2 / 4];}
    public static Color highlightColor(int data2) {
    	return HIGHLIGHTS[data2 / 4];
    }

}

