package net.judah.tracks;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;

import net.judah.beatbox.JudahKit;
import net.judah.util.Constants;
import net.judah.util.Slider;

public class StepTrackView extends TrackView {

	private final StepTrack stepTrack;
	private final Box beats;

	private Slider vol2;
	private JComboBox<JudahKit> instruments2;
	private JComboBox<Character> pattern = new JComboBox<>();
	
	public StepTrackView(final StepTrack track) {
		super(track);
		stepTrack = track;
		beats = stepTrack.getBeatbox();
		custom1 = instruments2 = createInstrumentCombo(1);
		custom2 = vol2 = new Slider(e -> {track.setVol2(((Slider)e.getSource()).getValue() * 0.01f);});
		loadPatterns();
		DefaultListCellRenderer center = new DefaultListCellRenderer(); 
		center.setHorizontalAlignment(DefaultListCellRenderer.CENTER); 
		pattern.setRenderer(center);
		pattern.addActionListener(e -> {
			beats.setCurrent(beats.get(pattern.getSelectedIndex()));
		});
		
		add(pattern);
		add(stepTrack.getCycle());
		add(custom1);
		add(vol2);
	}
	
	public void loadPatterns() {
		pattern.removeAllItems();
		for (int i = 0; i < beats.size(); i++)
			pattern.addItem((char)('A' + i));
	}

	@Override public void update() {
		vol2.setValue((int) (stepTrack.getVol2() * 100f));
		super.update();
		redoInstruments();
		pattern.setSelectedIndex(beats.indexOf(beats.getCurrent()));
		// pattern...
		//stepTrack.getBeatbox().indexOf(stepTrack.getCurrent());
		//for (char c = 'a' ; c < 'c' ; c++) {
	}
	
	@Override
	public void redoInstruments() {
		int midi1 = stepTrack.getBeatbox().get(0).get(0).getReference().getData1();
		int midi2 = stepTrack.getBeatbox().get(0).get(1).getReference().getData1();
		for (JudahKit drum : JudahKit.values()) {
			if (drum.getMidi() == midi1)
				instruments.setSelectedItem(drum);
			if (drum.getMidi() == midi2)
				instruments2.setSelectedItem(drum);
		}
	}
	
	public JComboBox<JudahKit> createInstrumentCombo(int idx) {
		final JComboBox<JudahKit> drumCombo = new JComboBox<>(JudahKit.values());
		drumCombo.setSelectedItem(((StepTrack)track).getBeatbox().get(0).get(idx).getReference().getDrum());
		drumCombo.setFont(Constants.Gui.FONT11);
		return drumCombo;
	}

	public void refresh() {
		loadPatterns();
		redoInstruments();
		
	}
	
}
