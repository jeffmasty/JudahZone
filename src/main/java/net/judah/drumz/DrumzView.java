package net.judah.drumz;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.judah.util.CenteredCombo;

public class DrumzView extends JPanel {
	private final JudahDrumz beats;
	// vol, pan, atk, rel, rvb, file
	// vol, drum mapping
	JButton vol = new JButton("Volume");
	JButton pan = new JButton("Pan");
	JButton atk = new JButton("Attack");
	JButton dcy = new JButton("Decay");
	JButton rvb = new JButton("Reverb");
	JButton hicut = new JButton("HiCut");
	JButton file = new JButton("File");
	JButton save = new JButton("Save");
	JComboBox<String> preset = new CenteredCombo<>();
	
	public DrumzView(JudahDrumz beats) {
		this.beats = beats;
		setBorder(BorderFactory.createTitledBorder(beats.getName()));
		JPanel first = new JPanel(new GridLayout(1, 4, 3, 3));
    	JPanel second = new JPanel(new GridLayout(1, 4, 3, 3));
    	for(int i = 0; i < 4; i++)
    		first.add(new DrumPad(DrumType.values()[i], beats));
    	for(int i = 4; i < 8; i++)
    		second.add(new DrumPad(DrumType.values()[i], beats));
    	
    	JPanel right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.PAGE_AXIS));
    	right.add(first);
    	right.add(second);

    	JPanel btns = new JPanel();
    	btns.setLayout(new GridLayout(4, 2, 5, 5));
    	btns.add(vol);
    	btns.add(pan);
    	btns.add(atk);
    	btns.add(dcy);
    	btns.add(hicut);
    	btns.add(rvb);
    	btns.add(file);
    	btns.add(save);
    	
    	JPanel left = new JPanel();
    	left.setLayout(new BoxLayout(left, BoxLayout.PAGE_AXIS));
    	
    	for (String s : DrumDB.getKits())
    		preset.addItem(s);
    	
    	left.add(preset);
    	left.add(btns);
    	
    	setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
    	add(left);
    	add(right);
    	update();
    	preset.addActionListener(e->beats.setKit("" + preset.getSelectedItem()));
	}

	public void update() {
		if (beats.getKit() != null)
			if (preset.getSelectedItem() != beats.getKit().getFolder().getName()) {
				preset.setSelectedItem(beats.getKit().getFolder().getName());
			}
	}
}
