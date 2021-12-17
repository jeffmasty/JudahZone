package net.judah.sequencer;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import lombok.Getter;
import net.judah.util.RainbowFader;
import net.judah.util.Size;

public class Overview extends JList<Rail> {
	public static final int CHANNELS = 16;
	
	@Getter private JScrollPane scroller = new JScrollPane();
	JPanel[] tracks = new JPanel[CHANNELS];
	
	private int focus = 8;
	
	public Overview() {
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		
//		for (int i = 0; i < CHANNELS; i++) {
//			tracks[i] = new JPanel();
//			tracks[i].setBackground(RainbowFader.chaseTheRainbow((int)(i * 6.25f)));
//			tracks[i].add(new JLabel("" + i));
//			add(tracks[i]);
//		}

		for (int i = 0; i < CHANNELS; i++) {
			JLabel hi = new JLabel("hi " + i);
			hi.setBackground(RainbowFader.chaseTheRainbow((int)(i * 6.25f)));
			add(hi);
		}
		
		Dimension size = new Dimension(Size.WIDTH_SONG, Size.HEIGHT_FRAME - Size.HEIGHT_FRAME);
		setMinimumSize(size);
		setSize(size);
		setPreferredSize(size);
		
		scroller.add(this);
		//setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);  
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);  
		scroller.doLayout();
	}
	
	
	
	
}
