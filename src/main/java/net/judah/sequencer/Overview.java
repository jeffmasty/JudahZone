package net.judah.sequencer;

import java.awt.Dimension;

import javax.swing.BoxLayout;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.judah.util.Size;

public class Overview extends JList<Rail> {
	
//	JudahClock clock = JudahClock.getInstance();
	
	private final JScrollPane scroller;
//	private final Track keyboard, drums, drum2, bass, chords, lead, bdrum, sdrum, hihat1, hihat2, 
//	drum3, drum4, bass2, chrds2, lead2; 	// trans, arp1, arp2, arp3,
//	@Getter private final Track [] tracks;
	
	//private int focus = 8;
	
	public Overview() {
//		keyboard = new Track("keys", CONTROLLER, 0, CRAVE_OUT);
//		drums = new Track("drums", STEP_DRUM, 9, CALF_OUT);
//		drum2 = new Track("drum2", MIDI_DRUM, 9, DRUMS_OUT);
//		bass = new Track("bass", MIDI_MONO, 1, SYNTH_OUT);
//		chords = new Track("chords", MIDI_POLY, 2, SYNTH_OUT);
//		lead = new Track("lead", MIDI_MONO, 3, SYNTH_OUT);
//		bdrum = new Track("bdrum", STEP_DRUM, BassDrum.toByte(), CALF_OUT);
//		sdrum = new Track("sdrum", STEP_DRUM, AcousticSnare.toByte(), CALF_OUT);
//		hihat1 = new Track("hihat1", STEP_DRUM, ClosedHiHat.toByte(), CALF_OUT);
//		hihat2 = new Track("hihat2", STEP_DRUM, OpenHiHat.toByte(), CALF_OUT);
//		drum3 = new Track("drum3", MIDI_DRUM, 9, CALF_OUT);
//		drum4 = new Track("drum4", MIDI_DRUM, 9, DRUMS_OUT);
//		bass2 = new Track("bass2", MIDI_MONO, 4, CALF_OUT);
//		chrds2 = new Track("chrds2", MIDI_POLY, 5, CALF_OUT);
//		lead2 = new Track("lead2", MIDI_POLY, 6, CALF_OUT);
////		trans = new Track("trans", ARP);
////		arp1 = new Track("arp1", ARP);
////		arp2 = new Track("arp2", ARP);
////		arp3 = new Track("arp3", ARP);
//		
//		tracks = new Track[] { keyboard, drums, drum2, bass, chords, lead, bdrum, sdrum, hihat1, hihat2, 
//			drum3, drum4, bass2, chrds2, lead2 }; // trans, arp1, arp2, arp3, 
//
//		
		JPanel background = new JPanel();
		background.setLayout(new BoxLayout(background, BoxLayout.Y_AXIS));
//		
//		Dimension sz = new Dimension(Size.WIDTH_SONG-100, 50);
//		
////		tracks[0] = new Track(clock, "keys", Type.CONTROLLER);
////		tracks[1] = new Track(clock, "drum1")
//		
////		tracks.add(keyboard); // fluid 0
////		tracks.add(drums); 
////		tracks.add(drums2);  
////		tracks.add(bass);   // fluid 1 
////		tracks.add(chords);  // fluid 2
////		tracks.add(lead); // fluid 3
////		tracks.add(bdrum); 
////		tracks.add(sdrum);
////		tracks.add(hats1);
////		tracks.add(hats2);
////		tracks.add(drums3);
////		tracks.add(drums4);
////		tracks.add(bass2); // fluid 4
////		tracks.add(chrds2); // fluid 5
////		tracks.add(lead2); // fluid 6
//
//		
//		
//		
//		
//		for (int i = 0; i < tracks.length; i++) {
//			Track t = tracks[i];
//			
//			JPanel child = new JPanel();
//			child.setPreferredSize(sz);
//
//			Color rainbow = RainbowFader.chaseTheRainbow((int)(i/(float)tracks.length * 100));
//			child.setBorder(BorderFactory.createLineBorder(rainbow));
//			child.add(new JLabel(t.getName()));
//			child.add(new JLabel("[" + t.getInitial().port + "]"));
//			child.add(new JLabel("▶"));
//			child.add(new JLabel(t.getType().toString()));
//			background.add(child);
//		}
		
		scroller = new JScrollPane(background);
		Dimension size = new Dimension(Size.WIDTH_SONG, Size.HEIGHT_FRAME - Size.HEIGHT_FRAME);
//		setMinimumSize(size);
//		setSize(size);
		scroller.setPreferredSize(size);
		
		//scroller.add(this);
		//setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);  
        scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);  
		scroller.doLayout();
		//scroller.setView
		
	}
	
	
	
	
}
