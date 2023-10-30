package net.judah.seq.track;

import java.util.ArrayDeque;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.settable.CurrentCombo;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.CycleCombo;

/** Display track's current bar, launch and cycle <br/><br/>
 * 	[<-] [##-] [->] [Init/Total] [Cycle]  */
public class Programmer extends JPanel {
	
	private static final ArrayDeque<Programmer> instances = new ArrayDeque<>();
	private final Computer track;
	private final CycleCombo cycle;
	private final CurrentCombo bar;
	private final JButton launch = new JButton();
	
	public Programmer(Computer t) {
		track = t;
		bar = new CurrentCombo(track);
		cycle = new CycleCombo(track);
		launch.setText(launchCode());
		launch.addActionListener(e-> track.setLaunch(track.getFrame() * 2));
		add(new Arrow(Arrow.WEST, e->track.next(false)));
		add(bar);		
		add(new Arrow(Arrow.EAST, e->track.next(true)));
		add(launch);
		add(cycle);
		Gui.resize(launch, Size.MICRO);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		instances.add(this);
	}

	public static void update(Computer midiTrack) {
		for (Programmer instance : instances)
			if (instance.track == midiTrack) {
					instance.launch.setText(instance.launchCode());
					instance.bar.update();
			}
	}
	
	private String launchCode() {
		String code = (1 + track.getLaunch() / 2) + "/" + track.frames();
		if (code.length() < 5)
			code = " " + code;
		if (code.length() < 5)
			code = code + " ";
		return code;
	}
	
}
