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
import net.judah.gui.Bindings.Act;
import net.judah.seq.Duration;
import net.judah.seq.Transpose;

public class TrackBindings {

	final InputMap inputs;
	final ActionMap actions;
	final HiringAgency view;

	private void bind(KeyStroke k, Action a) {
		inputs.put(k, k.toString());
		actions.put(k.toString(), a);
	}

	public TrackBindings(HiringAgency source) {
		this.view = source;
		inputs = view.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW); // WHEN_ANCESTOR_OF_FOCUSED_COMPONENT
		actions = view.getActionMap();

		bind(getKeyStroke(VK_DELETE, 0), new Act(()->view.getMusician().delete()));
		bind(getKeyStroke(VK_ESCAPE, 0), new Act(()->view.getMusician().selectNone()));
		bind(getKeyStroke(VK_SPACE, 0), new Act(()->view.getMusician().getTrack().toggle()));
		bind(getKeyStroke(VK_A, CTRL_DOWN_MASK), new Act(()->view.getMusician().selectFrame()));
		bind(getKeyStroke(VK_C, CTRL_DOWN_MASK), new Act(()->view.getMusician().copy()));
		bind(getKeyStroke(VK_D, CTRL_DOWN_MASK), new Act(()->view.getMusician().selectNone()));
		bind(getKeyStroke(VK_L, CTRL_DOWN_MASK), new Act(()->new Duration(view.getMusician())));
		bind(getKeyStroke(VK_R, CTRL_DOWN_MASK), new Act(()->view.getMusician().getTrack()
				.setCapture(!view.getMusician().getTrack().isCapture())));
		bind(getKeyStroke(VK_S, CTRL_DOWN_MASK), new Act(()->view.getMusician().getTrack().save()));
		bind(getKeyStroke(VK_T, CTRL_DOWN_MASK), new Act(()->new Transpose(view.getMusician().getTrack(), view.getMusician())));
		bind(getKeyStroke(VK_V, CTRL_DOWN_MASK), new Act(()->view.getMusician().paste()));
		bind(getKeyStroke(VK_Y, CTRL_DOWN_MASK), new Act(()->view.getMusician().redo()));
		bind(getKeyStroke(VK_Z, CTRL_DOWN_MASK), new Act(()->view.getMusician().undo()));
		bind(getKeyStroke(VK_UP, 0), new Act(()->view.getMusician().velocity(true)));
		bind(getKeyStroke(VK_DOWN, 0), new Act(()->view.getMusician().velocity(false)));
		bind(getKeyStroke(VK_LEFT, 0), new Act(()->view.getMusician().getTrack().next(true)));
		bind(getKeyStroke(VK_RIGHT, 0), new Act(()->view.getMusician().getTrack().next(false)));
		if (view.getMusician().getTrack().isDrums()) {
			bind(getKeyStroke(VK_LEFT, CTRL_DOWN_MASK), new Act(()->JudahZone.getDrumMachine().getTracks().next(false)));
			bind(getKeyStroke(VK_RIGHT, CTRL_DOWN_MASK), new Act(()->JudahZone.getDrumMachine().getTracks().next(true)));
		}


	}


}
