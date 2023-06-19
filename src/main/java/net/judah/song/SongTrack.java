package net.judah.song;

import static net.judah.gui.Pastels.*;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fx.Gain;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Size;
import net.judah.gui.settable.Cycle;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.Launch;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Slider;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.MidiTrack;

 // TODO MouseWheel listener -> change pattern? 
public class SongTrack extends JPanel implements Size {
	
	@Getter private final MidiTrack track;
	private final JButton play;
	private final Btn edit;
	private final TrackVol vol;
	private Slider gain = null; // drumTrack
				
	//  namePlay file cycle bar preview preset amp
	public SongTrack(MidiTrack t) {
		this.track = t;
		vol = new TrackVol(track);
		play = new Btn("▶️ " + t.getName(), e-> track.trigger());
		play.setOpaque(true);
		edit = new Btn("", e->JudahZone.getFrame().edit(track));
		edit.setIcon(UIManager.getIcon("FileChooser.detailsViewIcon"));
		edit.addActionListener(e->JudahZone.getFrame().edit(track));

		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 6));
		setOpaque(true);
		setBorder(Gui.SUBTLE);
		
		add(Gui.resize(play, SMALLER_COMBO));
		if (t.isSynth()) {
			add(new ModeCombo(track));
			setBackground(Pastels.BUTTONS);
		}
		else {
			gain = new Slider(null);
			gain.setValue(track.getMidiOut().getGain().get(Gain.VOLUME));
			gain.addChangeListener(e->{
				Gain fx = track.getMidiOut().getGain();
				fx.set(Gain.VOLUME, gain.getValue());
				MainFrame.update(fx);
			});
			add(Gui.resize(gain, SMALLER_COMBO));
		}
		add(vol);	
		add(Gui.resize(new Folder(track), COMBO_SIZE));		
		add(Gui.resize(new Program(track.getMidiOut(), track.getCh()), COMBO_SIZE));		
		add(new Cycle(track));		
		add(new Launch(track));		
		add(new Arrow(Arrow.WEST, e->track.next(false)));
		add(edit);
		add(new Arrow(Arrow.EAST, e->track.next(true)));
		update();
	}
	
	public void update() {
		edit.setText((1 + track.getCurrent()) + "/" + track.bars());
		play.setBackground(track.isActive() ? GREEN : track.isOnDeck() ? track.getCue().getColor() : BUTTONS);
		if (gain != null && gain.getValue() != track.getMidiOut().getGain().get(Gain.VOLUME))
			gain.setValue(track.getMidiOut().getGain().get(Gain.VOLUME));
	}
	
}
