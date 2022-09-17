package net.judah.tracker;

import static net.judah.util.Size.*;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.api.Midi;
import net.judah.util.Constants;
import net.judah.util.Knob;
import net.judah.util.Pastels;


public class DrumEdit extends TrackEdit implements ActionListener, Pastels {

    private static final Dimension KIT_BOUNDS = new Dimension(WIDTH_KIT, TABS.height);
    private static final Rectangle GRID_BOUNDS = new Rectangle(
    		WIDTH_BUTTONS, 0, 680, TABS.height - 30);
    protected static final Dimension NAMESZ = new Dimension(107, STD_HEIGHT);

	private JPanel contract;
	@Getter private final BeatBox grid;
	private final DrumTrack d;
	
	public DrumEdit(final DrumTrack track) {
		super(track);
		this.d = track;
		grid = new BeatBox(track, GRID_BOUNDS);
		add(grid);
		fillKit();
	}

	@Override
	public void step(int step) {
		grid.step(step);
	}
	
	
	public void fillKit() {
		if (contract != null) remove(contract);
		contract = new JPanel();
		contract.setMaximumSize(KIT_BOUNDS.getSize());
		contract.setLayout(new BoxLayout(contract, BoxLayout.PAGE_AXIS));
		contract.removeAll();
		contract.add(Box.createVerticalStrut(44));
		ArrayList<GMDrum> drumkit = d.getKit();
		for(int i = 0; i < drumkit.size() ; i++) {
			// instrument drop down
			contract.add(contractLine(d.getKit().get(i), i));
		}
		add(contract);
		repaint();
	}

	private JPanel contractLine(GMDrum drum, int idx) {
		final Knob volume = new Knob(val -> {
			d.getVolume().set(idx, val * 0.01f);
		});
		volume.setValue((int) (d.getVolume().get(idx) * 100));

		final JComboBox<GMDrum> drumCombo = new JComboBox<>();
		for (GMDrum entry : GMDrum.values()) 
			drumCombo.addItem(entry);
		drumCombo.setFont(Constants.Gui.FONT11);
		drumCombo.setPreferredSize(NAMESZ);
            
		drumCombo.setSelectedItem(drum);
		drumCombo.addActionListener(e -> {
			GMDrum change = (GMDrum)drumCombo.getSelectedItem();
			translate(idx, change.getData1());
			d.getKit().set(idx, change);
			fillKit();
		});

		JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 0));
		pnl.add(drumCombo);
		pnl.add(volume);
		return pnl;
	}
	
	private void translate(int idx, int data1) {
		Notes n;
		int source = d.getKit().get(idx).getData1();
		for (Pattern p : track)
			for (int step = 0; step < track.getSteps(); step++) {
				n = p.getNote(step, source);
				if (n == null)
					continue;
				Midi hit = n.find(source);
				n.remove(hit);
				n.add(Midi.create(hit.getCommand(), track.getCh(), data1, hit.getData2()));
			}
			
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (super.handled(e))
			return;
		
	}

	@Override
	public void setPattern(int idx) {
		super.setPattern(idx);
		grid.repaint();
	}
	
	@Override
	public void update() {
		grid.repaint();
	}

}
