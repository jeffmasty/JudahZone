package net.judah.gui.knobs;

import javax.swing.JComponent;
import javax.swing.JPanel;

import net.judah.gui.Size;
import net.judah.gui.Updateable;


public abstract class KnobPanel extends JPanel implements Updateable, Size {

	public abstract KnobMode getKnobMode();

	public abstract boolean doKnob(int idx, int value);

	@Override
	public abstract void update();

	/**Called when the KnobPanel is going to be displayed.
	 * @return an optional and separate set of title bar component(s) */
	public abstract JComponent getTitle(); // return Gui.wrap(new JLabel(getName()));

	public abstract void pad1();

	public void pad2() { }

}
