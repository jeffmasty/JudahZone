package net.judah.seq.track;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.sound.midi.MidiEvent;
import javax.sound.midi.Track;
import javax.swing.JPanel;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.gui.Floating;
import lombok.Getter;
import net.judah.gui.TabZone;
import net.judah.midi.JudahClock;
import net.judah.seq.piano.Piano;
import net.judah.seq.track.Editor.Delta;
import net.judah.seq.track.Editor.Selection;

public abstract class MusicBox extends JPanel implements Musician, Floating, MidiConstants {

    public static enum DragMode { CREATE, TRANSLATE, SELECT }
    protected static final Composite transparent = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

    @Getter protected final NoteTrack track;
//    protected final Editor editor;
    protected final Track t;
    protected final JudahClock clock;

    /** Local cache of selected events for UI rendering, updated from the Editor. */
    @Getter protected final ArrayList<MidiEvent> selected = new ArrayList<>();

    protected final Measure scroll;
    protected ArrayList<MidiEvent> dragging = null;
    protected Prototype click;
    protected Point drag = null;
    protected DragMode mode = null;
    protected Prototype recent;
    protected int width, height;

    public MusicBox(NoteTrack midiTrack) {
        this.track = midiTrack;

        this.t = track.getT();
        this.clock = track.getClock();
        track.getEditor().addListener(this); // Listen to the editor
        scroll = new Measure(track);
        addMouseListener(this);
        addMouseMotionListener(this);
        addMouseWheelListener(this);
    }

    /**
     * Publishes a selection to the track's editor.
     * @param events The list of events to be selected.
     */
    protected void publishSelection(List<MidiEvent> events) {
        track.getEditor().publish(this, events == null ? Collections.emptyList() : events);
    }

	@Override
	public void selectionChanged(Selection selection) {
		if (selection == null || selection.originId() == this) {
			return; // Ignore own events
		}
		selected.clear();
		if (selection.events() != null) {
			for (MidiEvent event : selection.events()) {
				// Drums use simple MidiEvent wrapping
				if (Midi.isNoteOn(event.getMessage())) {
					selected.add(new MidiNote(event));
				}
			}
		}
		repaint();
	}

    @Override
    public void mouseDragged(MouseEvent mouse) {
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

    @Override public final void mouseEntered(MouseEvent e) {TabZone.getInstance().requestFocusInWindow();}
    @Override public final void mouseWheelMoved(MouseWheelEvent wheel) {
        boolean up = wheel.getPreciseWheelRotation() < 0;
        if (wheel.isShiftDown())
            shiftVelocity(up);
        else if (wheel.isControlDown() && this instanceof Piano piano) {
        	// adjust PianoMenu viewport value + extent (grow on up, shrink on down)
        	piano.getView().getMenu().adjustViewport(up);
        }
        else if (wheel.isAltDown()) {
            // TODO
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
        Editor.Selection currentSelection = track.getEditor().getSelection();
        if (currentSelection == null || currentSelection.events().isEmpty()) {
            velocity(up);
            return;
        }
    }

    @Override public final Prototype translate(Point p) {
        return new Prototype(toData1(p), toTick(p));
    }

    protected void select(List<MidiEvent> events) {
        selected.clear();
        if (events != null) {
            for (MidiEvent e : events) {
                selected.add(new MidiNote(e));
            }
        }
        track.getEditor().publish(this, events);
        repaint();
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

    @Override
    public void dataChanged(Delta time) {
    	repaint();
    }

}

//private void transpose(boolean up) {
//Editor.Selection currentSelection = editor.getSelection();
//if (currentSelection == null || currentSelection.events().isEmpty()) return;
//
//MidiEvent firstOn = currentSelection.events().stream()
//  .filter(e -> Midi.isNoteOn(e.getMessage())).findFirst().orElse(null);
//if (firstOn == null) return;
//
//int srcData1 = ((ShortMessage)firstOn.getMessage()).getData1();
//int destData1 = srcData1 + (up ?  1 : -1);
//if (destData1 < 0 || destData1 > 127) return;
//
//Edit e = new Edit(Type.TRANS, new ArrayList<>(currentSelection.events()));
//e.setDestination(new Prototype(destData1, 0)); // Tick delta is 0 for pitch-only transpose
//editor.push(e);
//}
//
//private void translate(boolean up) {
//Editor.Selection currentSelection = editor.getSelection();
//if (currentSelection == null || currentSelection.events().isEmpty()) return;
//
//MidiEvent firstOn = currentSelection.events().stream()
//  .filter(e -> Midi.isNoteOn(e.getMessage())).findFirst().orElse(null);
//if (firstOn == null) return;
//
//long tickDelta = up ? track.getStepTicks() : -track.getStepTicks();
//
//Edit e = new Edit(Type.TRANS, new ArrayList<>(currentSelection.events()));
//// Data1 delta is 0, tick delta is one step
//e.setDestination(new Prototype(0, tickDelta));
//editor.push(e);
//}
