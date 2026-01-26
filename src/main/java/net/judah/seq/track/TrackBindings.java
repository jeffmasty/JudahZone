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

	private final HiringAgency provider;

	private void bind(KeyStroke k, Action a) {
		inputs.put(k, k.toString());
		actions.put(k.toString(), a);
	}

	public TrackBindings(HiringAgency source) {
		this.provider = source;
		inputs = source.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); // WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
		actions = source.getActionMap();
		// MusicBox view = source.getMusician();
		// MidiTrack track = view.getTrack();
		//Editor ed = track.getEditor();


		bind(getKeyStroke(VK_DELETE, 0), new Act(()->getEditor().delete()));
		bind(getKeyStroke(VK_ESCAPE, 0), new Act(()->getEditor().selectNone()));
		bind(getKeyStroke(VK_SPACE, 0), new Act(()->getTrack().toggle()));
		bind(getKeyStroke(VK_A, CTRL_DOWN_MASK), new Act(()->getEditor().selectFrame()));
		bind(getKeyStroke(VK_C, CTRL_DOWN_MASK), new Act(()->getEditor().copy()));
		bind(getKeyStroke(VK_D, CTRL_DOWN_MASK), new Act(()->getEditor().selectNone()));
		bind(getKeyStroke(VK_L, CTRL_DOWN_MASK), new Act(()->new Duration(getView())));
		bind(getKeyStroke(VK_R, CTRL_DOWN_MASK), new Act(()->getTrack().setCapture(!getTrack().isCapture())));
		bind(getKeyStroke(VK_S, CTRL_DOWN_MASK), new Act(()->getTrack().save()));
		bind(getKeyStroke(VK_T, CTRL_DOWN_MASK), new Act(()->new Transpose(getTrack(), getView())));
		bind(getKeyStroke(VK_V, CTRL_DOWN_MASK), new Act(()->getEditor().paste()));
		bind(getKeyStroke(VK_Y, CTRL_DOWN_MASK), new Act(()->getEditor().redo()));
		bind(getKeyStroke(VK_Z, CTRL_DOWN_MASK), new Act(()->getEditor().undo()));
		bind(getKeyStroke(VK_UP, 0), new Act(()->getView().velocity(true)));
		bind(getKeyStroke(VK_DOWN, 0), new Act(()->getView().velocity(false)));
		bind(getKeyStroke(VK_LEFT, 0), new Act(()->getTrack().next(true)));
		bind(getKeyStroke(VK_RIGHT, 0), new Act(()->getTrack().next(false)));
		if (getTrack().isDrums()) {
			DrumMachine drums= JudahZone.getInstance().getDrumMachine();
			bind(getKeyStroke(VK_LEFT, CTRL_DOWN_MASK), new Act(()->drums.getTracks().next(false)));
			bind(getKeyStroke(VK_RIGHT, CTRL_DOWN_MASK), new Act(()->drums.getTracks().next(true)));
		}


	}

	private MusicBox getView() {
		return provider.getMusician();
	}

	private Editor getEditor() {
		return provider.getMusician().getEditor();
	}

	private MidiTrack getTrack() {
		return provider.getMusician().getTrack();
	}

}
