package net.judah.song;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Pastels;
import net.judah.gui.PlayWidget;
import net.judah.gui.Qwerty;
import net.judah.gui.Size;
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

 // TODO MouseWheel listener -> change pattern?
public class SongTrack extends JPanel implements Size {
	private static final Dimension COMPUTER = new Dimension(204, 27);
	private static final Dimension GAIN_SIZE = new Dimension(105, STD_HEIGHT);

	@Getter private final MidiTrack track;

	//  namePlay file cycle bar preview preset amp
	public SongTrack(MidiTrack t) {
		this.track = t;
		Programmer computer = new Programmer(track);

		setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
		setOpaque(true);
		setBorder(Gui.SUBTLE);
		add(Gui.resize(new PlayWidget(t, t.getName()), SMALLER_COMBO));
		add(Gui.resize(new Program(track), COMBO_SIZE));

		if (t instanceof DrumTrack d) {
			setBackground(Pastels.BUTTONS);
			computer.setBackground(Pastels.BUTTONS);
			add(Gui.resize(new TrackGain(d), GAIN_SIZE));
		}
		else if (t instanceof PianoTrack p) {
			add(new ModeCombo(p));
			add(new TrackVol(track));
		}
		add(Gui.resize(new Folder(track), COMBO_SIZE));
		add(Gui.resize(computer, COMPUTER));
		add(new Btn(Icons.DETAILS_VEW, e->Qwerty.edit(track)));
	}

}
