package net.judah.song;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Icons;
import net.judah.gui.Pastels;
import net.judah.gui.PlayWidget;
import net.judah.gui.Size;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Slider;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.Programmer;

 // TODO MouseWheel listener -> change pattern? 
public class SongTrack extends JPanel implements Size {
	private static final Dimension COMPUTER = new Dimension(204, 27);

	
	@Getter private final MidiTrack track;
	private Slider gain = null; // DrumTrack
				
	//  namePlay file cycle bar preview preset amp
	public SongTrack(MidiTrack t) {
		this.track = t;
		Programmer computer = new Programmer(track);

		setLayout(new FlowLayout(FlowLayout.LEFT, 4, 4));
		setOpaque(true);
		setBorder(Gui.SUBTLE);
		add(Gui.resize(new PlayWidget(t, t.getName()), SMALLER_COMBO));
		add(Gui.resize(new Program(track), COMBO_SIZE));		

		if (t.isSynth()) {
			add(new ModeCombo(((PianoTrack)track)));
			add(new TrackVol(track));
		}
		else {
			setBackground(Pastels.BUTTONS);
			computer.setBackground(Pastels.BUTTONS);
			gain = new Slider(null);
			gain.setValue((int) (track.getAmp() * 100));
			gain.addChangeListener(e->{
				if (track.getAmp() * 100f != gain.getValue()) {
					track.setAmp(gain.getValue() * 0.01f); // TODO GUI updates?
				}
			});
			Dimension GAIN_SIZE = new Dimension(105, STD_HEIGHT);
			add(Gui.resize(gain, GAIN_SIZE));
		}
		add(Gui.resize(new Folder(track), COMBO_SIZE));		
		add(Gui.resize(computer, COMPUTER));
		add(new Btn(Icons.DETAILS_VEW, e->JudahZone.getFrame().edit(track)));
		update();
	}
	
	public void update() {
		if (gain != null && gain.getValue() != track.getAmp() * 100)
			gain.setValue((int) (track.getAmp() * 100));
	}
	
}
