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

/** Display track current progress, launch and cycle */
public class Programmer extends JPanel {
	// [<-][##-][->][Init/Total][Cycle] 
	
	private static final ArrayDeque<Programmer> instances = new ArrayDeque<>();
	private final Computer track;
	private final CycleCombo cycle;
	private final CurrentCombo bar;
	private final JButton total = new JButton();
	
	public Programmer(Computer t) {
		track = t;
		bar = new CurrentCombo(track);
		cycle = new CycleCombo(track);
		total.setText(launchCode());
		total.addActionListener(e-> track.reLaunch());
		add(new Arrow(Arrow.WEST, e->track.next(false)));
		add(bar);		
		add(new Arrow(Arrow.EAST, e->track.next(true)));
		add(total);
		add(cycle);
		Gui.resize(total, Size.MICRO);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		doLayout();
		instances.add(this);
	}

	public static void update(Computer midiTrack) {
		for (Programmer instance : instances)
			if (instance.track == midiTrack) {
					instance.total.setText(instance.launchCode());
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
