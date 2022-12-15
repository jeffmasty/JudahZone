package net.judah.drumkit;

import java.awt.GridLayout;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.mixer.MidiInstrument;
import net.judah.util.Constants;
import net.judah.widgets.CenteredCombo;

public class GMKitView extends JPanel {

	@Getter MidiInstrument current;
	
	JButton vol = new JButton("Volume");
	JButton patch = new JButton("Patch");
	JButton save = new JButton("Save");
	JComboBox<String> preset = new CenteredCombo<>();
	JComboBox<String> mapping = new JComboBox<>();

	JPanel btns = new JPanel();
	JPanel first = new JPanel();
	JPanel second = new JPanel();

	private GMKitView() {
		setBorder(BorderFactory.createTitledBorder("Fluid Drum Kits"));
		
		mapping.addItem("HiHats");
    	preset.addItem("TR-808");
    	btns.setLayout(new GridLayout(0, 1, 2, 3));

    	btns.add(preset);
    	btns.add(vol);
    	btns.add(patch);
    	btns.add(mapping);
    	btns.add(save);

    	
    	JPanel right = new JPanel();
		right.setLayout(new BoxLayout(right, BoxLayout.PAGE_AXIS));
    	right.add(first);
    	right.add(second);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		add(Constants.wrap(btns));
		add(right);
	}

	public GMKitView(MidiInstrument fluid) {
		this();
		first.setLayout(new GridLayout(2, 2, 2, 2));
    	second.setLayout(new GridLayout(2, 2, 2, 2));

    	current = fluid;
    	
    	for(int i = 0; i < 4; i++)
    		first.add(new GMPad(DrumType.values()[i], this));
    	for(int i = 4; i < 8; i++)
    		second.add(new GMPad(DrumType.values()[i], this));

	}
	

}
