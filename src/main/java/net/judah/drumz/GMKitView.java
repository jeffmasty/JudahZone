package net.judah.drumz;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.mixer.GMSynth;
import net.judah.tracker.Track;
import net.judah.util.CenteredCombo;
import net.judah.util.Constants;

public class GMKitView extends JPanel {

	@Getter GMSynth current;
	
	JButton vol = new JButton("Volume");
	JButton patch = new JButton("Patch");
	JButton save = new JButton("Save");
	JComboBox<String> preset = new CenteredCombo<>();
	JComboBox<String> synths = new CenteredCombo<>();
	JComboBox<String> mapping = new JComboBox<>();

	JPanel btns = new JPanel();
	JPanel first = new JPanel();
	JPanel second = new JPanel();

	private GMKitView(String title) {
		if (title != null)
			setBorder(BorderFactory.createTitledBorder(title));
		
		mapping.addItem("HiHats");
    	preset.addItem("TR-808");
		synths.setFont(Constants.Gui.BOLD13);
    	btns.setLayout(new GridLayout(0, 1, 2, 3));

    	btns.add(synths);
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

	public GMKitView(ArrayList<GMSynth> gms) {
		this("General Midi Kits");
		
		for (GMSynth synth : gms) 
			synths.addItem(synth.getName());
		
		first.setLayout(new GridLayout(2, 2, 2, 2));
    	second.setLayout(new GridLayout(2, 2, 2, 2));

    	current = gms.get(0);
    	
    	for(int i = 0; i < 4; i++)
    		first.add(new GMPad(DrumType.values()[i], this));
    	for(int i = 4; i < 8; i++)
    		second.add(new GMPad(DrumType.values()[i], this));

	}
	
	public GMKitView(Track track) {
		this((String)null);
		
		for (GMSynth synth : JudahZone.getMixer().getGMs()) 
			synths.addItem(synth.getName());
		
		first.setLayout(new GridLayout(1, 4, 2, 2));
    	second.setLayout(new GridLayout(1, 4, 2, 2));

    	current = (GMSynth)JudahZone.getMidi().getPath(track.getMidiOut().getPort()).getChannel();
    	
    	for(int i = 0; i < 4; i++)
    		first.add(new GMPad(DrumType.values()[i], this));
    	for(int i = 4; i < 8; i++)
    		second.add(new GMPad(DrumType.values()[i], this));

	}
	//	// GMDRUM 2
//				if (track.isDrums()) {
//					ArrayList<GMDrum> drumkit = ((DrumTrack)track).getKit();
//					drumkit.set(drumkit.size() - 1, (GMDrum)Constants.ratio(data2, GMDrum.values()));
//					((DrumEdit)track.getEdit()).fillKit();
//				}
//				return true;

}
