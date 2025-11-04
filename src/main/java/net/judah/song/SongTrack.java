package net.judah.song;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.PlayWidget;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.TrackGain;
import net.judah.gui.widgets.TrackVol;
import net.judah.omni.Icons;
import net.judah.seq.track.DrumTrack;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.Programmer;

/**  show/change MidiTrack state */
public class SongTrack extends JPanel implements Size {
	private static final Dimension COMPUTER = new Dimension(204, 27);

	@Getter private final MidiTrack track;

	// TODO MouseWheel listener -> change pattern?
	public SongTrack(MidiTrack t) {
		this.track = t;
		Programmer computer = new Programmer(track);

		setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
		setOpaque(true);
		setBorder(Gui.SUBTLE);
		add(Gui.resize(new PlayWidget(t, t.getName()), SMALLER_COMBO));

		if (t instanceof DrumTrack d) {
			setBackground(Pastels.BUTTONS);
			computer.setBackground(Pastels.BUTTONS);
			add(new TrackGain(d));
		}
		else if (t instanceof PianoTrack p)
			add(new ModeCombo(p));

		add(new TrackVol(track));
		add(Gui.resize(new Program(track), COMBO_SIZE));
		add(Gui.resize(new Folder(track), COMBO_SIZE));
		add(Gui.resize(computer, COMPUTER));
		add(new Btn(Icons.DETAILS_VEW, e->TabZone.edit(track)));
	}

}
