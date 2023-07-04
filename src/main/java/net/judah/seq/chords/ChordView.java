package net.judah.seq.chords;

import javax.swing.*;

import lombok.Setter;
import net.judah.gui.Pastels;
import net.judah.gui.widgets.Arrow;
import net.judah.song.Song;

public class ChordView extends JPanel {
	
	private final ChordTrack chords;
	@Setter private Song song;
	private Section section;
	private final JLabel bar = new JLabel("?");
	private final JLabel bars = new JLabel("/?");
	private final Arrow next;
	private final JToggleButton loop;
	
	public ChordView(ChordTrack trk) {
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setOpaque(true);
		setBackground(Pastels.BUTTONS);
		bar.setOpaque(true);		bar.setBackground(Pastels.BUTTONS); 
		bars.setOpaque(true);		bars.setBackground(Pastels.BUTTONS); 
		
		this.chords = trk;
		next = new Arrow(SwingConstants.EAST, e->chords.next());
		next.setEnabled(false);
		next.setBackground(Pastels.BUTTONS);
		loop = new JToggleButton("🔁");
		loop.setSelected(trk.getSection() != null && trk.getSection().isOnLoop());
		loop.addActionListener(e->trk.getSection().toggle(Directive.LOOP));
		loop.setEnabled(false);
		add(new ChordPlay("▶️ Chords", chords));
		add(new ChordProCombo());
		add(new SectionCombo(chords));
		add(loop);
		add(next);
		add(new ChordScroll(chords));
		add(Box.createHorizontalStrut(5));
		add(bar);
		add(bars);
	}
	
	public void setSection(Section s) {
		this.section = s;
		if (section == null) {
			bars.setText("");
			loop.setEnabled(false);
			next.setEnabled(false);
		}
		else {
			bars.setText("/" + chords.bars(section.getCount()));
			loop.setEnabled(true);
			next.setEnabled(true);
			if (section != null && loop.isSelected() != section.isOnLoop())
				loop.setSelected(section.isOnLoop());
		}
	}
	
	public void update(Chord chord) {
		bar.setText("" + (chords.getBar() + 1));
		if (section == null) {
			loop.setEnabled(false);
			next.setEnabled(false);
		}
			
	}
	
}
