package net.judah.song;

import static net.judah.gui.Pastels.GREEN;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.UIManager;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.settable.Cycle;
import net.judah.gui.settable.Folder;
import net.judah.gui.settable.Launch;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Arrow;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.MidiTrack;

 // TODO MouseWheel listener -> change pattern? 
public class SongTrack extends JPanel implements Size {
	
	@Getter private final MidiTrack track;
	private final JButton play;
	private final TrackVol vol;
	private final Cycle cycle;
	private final Launch launch;
	private final Program progChange;
	private final Folder files;
	private final Btn edit;
				
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
		play.setBackground(track.isActive() ? GREEN : track.isOnDeck() ? track.getCue().getColor() : null);
		edit.setText((1 + track.getFrame()) + "/" + track.frames());
	}
	
}
