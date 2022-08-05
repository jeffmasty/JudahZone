package net.judah.tracker;

import static net.judah.util.Size.*;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.judah.util.Constants;
import net.judah.util.Knob;
import net.judah.util.Pastels;


public class DrumEdit extends TrackEdit implements ActionListener, Pastels {

    private static final Dimension KIT_BOUNDS = new Dimension(WIDTH_KIT, TABS.height);
    private static final Rectangle GRID_BOUNDS = new Rectangle(
    		WIDTH_BUTTONS, 0, WIDTH_SONG - WIDTH_KIT - WIDTH_BUTTONS - 70, TABS.height - 30);
    protected static final Dimension NAMESZ = new Dimension(107, STD_HEIGHT);

	private final JPanel contract = new JPanel();
	private final BeatBox grid;
	
	public DrumEdit(final Track t) {
		super(t);
		
		grid = new BeatBox(track, GRID_BOUNDS);
		contract.setMaximumSize(KIT_BOUNDS.getSize());
				
		fillContract();
		add(grid);
		add(contract);
		
		// filler

	}

	@Override
	public void step(int step) {
		grid.step(step);
	}
	
	
	private void fillContract() {
		contract.setLayout(new BoxLayout(contract, BoxLayout.PAGE_AXIS));
		contract.removeAll();
		contract.add(Box.createVerticalStrut(42));
		DrumKit kit = track.getKit();
		for(int i = 0; i < kit.size() ; i++) {
			// instrument drop down, volume knob
			contract.add(contractLine(kit.get(i), i));
		}
	}

	private JPanel contractLine(MidiBase base, int idx) {
		final Knob volume = new Knob(val -> {
			track.getKit().get(idx).setVelocity(val * 0.01f);
		});
		volume.setValue((int)(base.getVelocity() * 100));

		final JComboBox<GMDrum> drumCombo = new JComboBox<>();
		for (GMDrum d : GMDrum.values()) 
			drumCombo.addItem(d);
		drumCombo.setFont(Constants.Gui.FONT11);
		drumCombo.setPreferredSize(NAMESZ);
            
		drumCombo.setSelectedItem(base.getDrum());
		drumCombo.addActionListener(e -> {
			track.getKit().get(idx).setDrum((GMDrum)drumCombo.getSelectedItem());
		});

		JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 0));
		pnl.add(drumCombo);
		pnl.add(volume);
		return pnl;
	}
	
	@Override
	public void actionPerformed(ActionEvent e) {
		if (super.handled(e))
			return;
		
	}

	@Override
	public void update() {
		grid.repaint();
	}
}
