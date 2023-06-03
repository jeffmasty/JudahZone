package net.judah.song;

import static net.judah.gui.Pastels.*;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
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
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Slider;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.MidiTrack;
import net.judah.seq.Mode;

 // TODO MouseWheel listener -> change pattern? 
public class SongTrack extends JPanel implements Size {
	private static Dimension MODE = new Dimension(70, 26);
	
	@Getter private final MidiTrack track;
	private final JButton play;
	private final Folder files;
	private final Program progChange;
	private final Cycle cycle;
	private final Launch launch;
	private final Btn edit;
	private final TrackVol vol;
	private Slider gain = null; // drumTrack
	private JComboBox<Mode> mode; // synthTrack
				
	//  namePlay file cycle bar preview preset amp
	public SongTrack(MidiTrack t) {
		this.track = t;
		launch = new Launch(track);
		files = new Folder(track);
		vol = new TrackVol(track);
		cycle = new Cycle(track);
		progChange = new Program(track.getMidiOut(), track.getCh()); 
		play = new Btn("▶️ " + t.getName(), e-> track.trigger());
		play.setOpaque(true);
		edit = new Btn("", e->JudahZone.getFrame().edit(track));
		edit.setIcon(UIManager.getIcon("FileChooser.detailsViewIcon"));
		edit.addActionListener(e->JudahZone.getFrame().edit(track));
		setLayout(new FlowLayout(FlowLayout.LEFT, 0, 6));
		
		Gui.resize(play, SMALLER_COMBO);
		Gui.resize(cycle, Size.SMALLER_COMBO);
		Gui.resize(progChange, COMBO_SIZE);
		Gui.resize(files, COMBO_SIZE);
		
		
		setOpaque(true);
		setBorder(Gui.SUBTLE);
		add(play);
		
		add(files);		
		add(progChange);		
		add(cycle);		
		add(launch);		
		add(edit);
		add(new Arrow(Arrow.WEST, e->next(false)));
		add(new Arrow(Arrow.EAST, e->next(true)));
		add(vol);	
		if (t.isSynth()) {
			mode = new JComboBox<>(Mode.values());
			mode.setOpaque(true);
			mode.setSelectedItem(t.getArp().getMode());
			mode.addActionListener(e->track.getArp().setMode((Mode)mode.getSelectedItem()));
			add(Gui.resize(mode, MODE));
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
			setBackground(Pastels.BUTTONS);
		}
		update();
	}
	
	private void next(boolean fwd) {
		int next = track.getFrame() + (fwd ? 1 : -1);
		if (next < 0)
			next = 0;
		if (next >= launch.getItemCount())
			next = 0;
		track.setFrame(next);
	}
	
	public void update() {
		edit.setText((1 + track.getFrame()) + "/" + track.frames());
		if (track.isSynth())
			mode.setBackground(track.getArp().getMode().getColor());
		play.setBackground(track.isActive() ? 
				track.isSynth() ? track.getArp().getMode().getColor() : GREEN : 
					track.isOnDeck() ? track.getCue().getColor() : BUTTONS);
		if (mode != null && mode.getSelectedItem() != track.getArp().getMode())
			mode.setSelectedItem(track.getArp().getMode());
		if (gain != null && gain.getValue() != track.getMidiOut().getGain().get(Gain.VOLUME))
			gain.setValue(track.getMidiOut().getGain().get(Gain.VOLUME));
	}
	
}
