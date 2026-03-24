package net.judah.seq.beatbox;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.TabZone;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.Musician;

public class RemapView extends KnobPanel {

	@Getter Box title = new Box(BoxLayout.LINE_AXIS);
	@Getter KnobMode knobMode = KnobMode.Remap;
	private final RemapTable table;

	public RemapView() {
		this(JudahZone.getInstance().getDrumMachine().getCurrent());
	}

	public RemapView(DrumTrack t) {
		this(TabZone.getDrummer(t));
	}

	public RemapView(Musician view) {

		DrumTrack drumz = (DrumTrack) view.getTrack();
		String lbl = " " + drumz.getName() + ": ";
		lbl += (drumz.getFile() == null) ? "??" : drumz.getFile().getName();
		title.add(new JLabel(lbl));

		table = new RemapTable(drumz, view);
		install(table);
	}

	@Override public void pad2() {
		table.remap();
	}


}
