package net.judah.gui.knobs;

import java.awt.Component;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;

import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judahzone.gui.Updateable;


public abstract class KnobPanel extends JPanel implements Updateable, Size {

	public abstract KnobMode getKnobMode();

	/**Called when the KnobPanel is going to be displayed.
	 * @return an optional and separate set of title bar component(s) */
	public abstract JComponent getTitle(); // return Gui.wrap(new JLabel(getName()));

	public /* abstract */ boolean doKnob(int idx, int value) {
		MainFrame.setFocus(KnobMode.MIDI); // revert to Main Midi
		return true;
	}

	@Override
	public /* abstract */ void update() { }
	public void pad1() { }
	public void pad2() { }

	protected void install(Component c) {
		removeAll();
        JScrollPane scroll = new JScrollPane(c);
        scroll.setPreferredSize(Size.KNOB_PANEL);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        add(scroll);
		doLayout();
	}



}
