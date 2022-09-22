package net.judah.drumz;

import java.awt.GridLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.JudahZone;
import net.judah.samples.SamplerView;
import net.judah.util.Constants;

public class KitzView extends JPanel {
	public static final String NAME = "Kits";
			
	public KitzView() {
		
		JPanel left = new JPanel(), right = new JPanel();
		left.setLayout(new GridLayout(2, 1, 4, 7));
		left.add(new DrumzView(JudahZone.getBeats()));
		left.add(new DrumzView(JudahZone.getBeats2()));
		right.add(new GMKitView(JudahZone.getMixer().getGMs()));
		add(Constants.wrap(left, new JLabel(" "), right));
		add(Constants.wrap(new SamplerView(JudahZone.getSampler())));
	}
	
}
