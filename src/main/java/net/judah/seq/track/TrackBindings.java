package net.judah.seq.track;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;

import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

import net.judah.JudahZone;
import net.judah.drumkit.DrumMachine;
import net.judah.gui.Bindings.Act;

public class TrackBindings {

	final InputMap inputs;
	final ActionMap actions;

	private void bind(KeyStroke k, Action a) {
		inputs.put(k, k.toString());
		actions.put(k.toString(), a);
	}

	public TrackBindings(HiringAgency source) {
		inputs = source.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); // WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
		actions = source.getActionMap();
		MusicBox view = source.getMusician();
		MidiTrack track = view.getTrack();
		Editor ed = track.getEditor();


		bind(getKeyStroke(VK_DELETE, 0), new Act(()->ed.delete()));
		bind(getKeyStroke(VK_ESCAPE, 0), new Act(()->ed.selectNone()));
		bind(getKeyStroke(VK_SPACE, 0), new Act(()->track.toggle()));
		bind(getKeyStroke(VK_A, CTRL_DOWN_MASK), new Act(()->ed.selectFrame()));
		bind(getKeyStroke(VK_C, CTRL_DOWN_MASK), new Act(()->ed.copy()));
		bind(getKeyStroke(VK_D, CTRL_DOWN_MASK), new Act(()->ed.selectNone()));
		bind(getKeyStroke(VK_L, CTRL_DOWN_MASK), new Act(()->new Duration(view)));
		bind(getKeyStroke(VK_R, CTRL_DOWN_MASK), new Act(()->track.setCapture(!track.isCapture())));
		bind(getKeyStroke(VK_S, CTRL_DOWN_MASK), new Act(()->track.save()));
		bind(getKeyStroke(VK_T, CTRL_DOWN_MASK), new Act(()->new Transpose(track, view)));
		bind(getKeyStroke(VK_V, CTRL_DOWN_MASK), new Act(()->ed.paste()));
		bind(getKeyStroke(VK_Y, CTRL_DOWN_MASK), new Act(()->ed.redo()));
		bind(getKeyStroke(VK_Z, CTRL_DOWN_MASK), new Act(()->ed.undo()));
		bind(getKeyStroke(VK_UP, 0), new Act(()->view.velocity(true)));
		bind(getKeyStroke(VK_DOWN, 0), new Act(()->view.velocity(false)));
		bind(getKeyStroke(VK_LEFT, 0), new Act(()->track.next(true)));
		bind(getKeyStroke(VK_RIGHT, 0), new Act(()->track.next(false)));
		if (track.isDrums()) {
			DrumMachine drums= JudahZone.getInstance().getDrumMachine();
			bind(getKeyStroke(VK_LEFT, CTRL_DOWN_MASK), new Act(()->drums.getTracks().next(false)));
			bind(getKeyStroke(VK_RIGHT, CTRL_DOWN_MASK), new Act(()->drums.getTracks().next(true)));
		}


	}


}
