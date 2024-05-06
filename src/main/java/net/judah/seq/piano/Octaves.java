package net.judah.seq.piano;

import static net.judah.seq.piano.PianoView.MAX_OCTAVES;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.SwingUtilities;

import net.judah.api.Key;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.widgets.CenteredCombo;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class Octaves extends CenteredCombo<Integer> {

	private final PianoView view;
	private boolean right = false;
	
	public Octaves(PianoView v) {
		this.view = v;
		for (int i = 1; i <= MAX_OCTAVES; i++)
			addItem(i);
		setSelectedItem(view.getRange() / Key.OCTAVE);
		Gui.resize(this, Size.MICRO);

		addMouseListener(new MouseAdapter() {
			@Override public void mousePressed(MouseEvent me) {
				right = SwingUtilities.isRightMouseButton(me);
				if( !right )
					return;
				Constants.execute(()->{
			        String input = Gui.inputBox("Range:");
					try {
						view.setRange(Integer.parseInt(input));
						if (view.getRange() % Key.OCTAVE == 0)
							setSelectedItem(view.getRange() / Key.OCTAVE);
					} catch (Throwable t) {RTLogger.log(this, input + ": " + t.getMessage());}
		        });
			}
		});
		addActionListener(e->trigger());
	}
	
	private void trigger() {
		if (right) { // veto action performed
			setPopupVisible(false);
	        String oldCommand = getActionCommand();
	        setActionCommand("comboBoxEdited");
	        fireActionEvent();
	        setActionCommand(oldCommand);

		}
		else 
			view.setRange((int)getSelectedItem() * Key.OCTAVE);
		
	}
}
