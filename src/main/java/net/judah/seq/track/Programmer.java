package net.judah.seq.track;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.settable.CurrentCombo;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.CycleCombo;

/** Display track's current bar, launch and cycle <br/><br/>
 * 	[<-] [##-] [->] [Init/Total] [Cycle]  */
@Getter
public class Programmer extends Box {

	private final Computer track;
	private final CycleCombo cycle;
	private final CurrentCombo current;
	private final JButton launch = new JButton();

	public Programmer(Computer t) {
		super(BoxLayout.X_AXIS);
		track = t;
		current = new CurrentCombo(track);
		cycle = new CycleCombo(track);
		launch.setText(launchCode());
		launch.addActionListener(e-> track.setLaunch(track.getFrame() * 2));
		add(new Arrow(Arrow.WEST, e->track.next(false)));
		add(current);
		add(new Arrow(Arrow.EAST, e->track.next(true)));
		add(launch);
		add(cycle);
		Gui.resize(launch, Size.MICRO);
	}

	public void liftOff() {
		launch.setText(launchCode());
		current.update();
	}

	private String launchCode() {
		String code = (1 + track.getLaunch() / 2) + "/" + track.getFrames();
		if (code.length() < 5)
			code = " " + code;
		if (code.length() < 5)
			code = code + " ";
		return code;
	}

}
