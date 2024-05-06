package net.judah.seq;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;

import javax.sound.midi.ShortMessage;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Setter;
import net.judah.drumkit.DrumType;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.Integers;
import net.judah.gui.widgets.ModalDialog;
import net.judah.seq.Edit.Type;
import net.judah.seq.track.MidiTrack;

/** Provides a ModalDialog to enter specific transposition amounts 
 * @see net.judah.gui.widgets.ModalDialog*/
public class Transpose {
	
	private final MidiTrack track;
	private final MusicBox view;
	private final Integers steps;
	private final Integers tones = new Integers(-11, 11); 
	private final Integers octaves = new Integers(-5, 5);
	private final JComboBox<DrumType> drum = new JComboBox<>(DrumType.values());
	@Setter static private int delta;
	
	public Transpose(MidiTrack t, MusicBox view) {
		
		this.track = t;
		this.view = view;
		
		steps = new Integers(track.getClock().getSteps() * -1, track.getClock().getSteps());
		
		JPanel pnl = new JPanel(new GridLayout(0, 2));

		pnl.add(new JLabel("Steps"));  
		pnl.add(steps);

		if (t.isSynth()) {
			pnl.add(new JLabel("Octaves")); // +/- 5
			pnl.add(octaves);
			pnl.add(new JLabel("SemiTones")); // +/- 11
			pnl.add(tones);
		}
		else {
			pnl.add(new JLabel("Drum "));
			pnl.add(drum);
		}
		
		pnl.add(new Btn("Ok", e->ok()));
		pnl.add(new Btn("Cancel", e->ModalDialog.getInstance().setVisible(false)));
		pnl = Gui.wrap(pnl);
		pnl.setName("Transpose");
		new ModalDialog(pnl, new Dimension(220, 160), MainFrame.getKnobMode());
	}

	private void ok() {
		ModalDialog.getInstance().setVisible(false);
		Edit e = new Edit(Type.TRANS, new ArrayList<MidiPair>(view.selected));
		int data1 = track.isSynth()
				? (Integer)octaves.getSelectedItem() * 12 + (Integer)tones.getSelectedItem() 
				: ((DrumType)drum.getSelectedItem()).getData1() - ((ShortMessage)view.selected.get(0).getOn().getMessage()).getData1();
		long tick = (Integer)steps.getSelectedItem() *  (track.getResolution() / track.getClock().getTimeSig().div) ;
		e.setDestination(new Prototype(data1, tick));
		view.push(e);
	}

}
