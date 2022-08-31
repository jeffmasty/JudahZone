package net.judah.tracker;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;

import javax.swing.JComboBox;

import lombok.Getter;
import lombok.Setter;
import net.judah.songs.SmashHit;
import net.judah.util.CenteredCombo;

/** universal verse/trigger, cycle patterns for each track, provide/update comboBox widgets */
@Getter 
public class Cycle implements ActionListener {
	
	@Setter @Getter private static boolean verse; // vs. chorus
	@Setter @Getter private static boolean trigger;
	@Setter @Getter private SmashHit custom;

	private int selected; 
	private int count;
	@Setter private boolean odd = false;

	private final HashSet<JComboBox<String>> combos = new HashSet<>();
	
	public void setSelected(int i) {
		if (selected == i)
			return;
		selected = i;
		count = 0;
		for (JComboBox<?> combo : combos) {
			combo.setSelectedIndex(selected);
		}
	}
	
	private final Track track;
	
	public static final String[] CYCLES = new String[] {
			"[A][B]", "[AB][CD]", "[A3B][C3D]", "ABCD"
	};
	
	public Cycle(Track t) {
		track = t;
	}

	public JComboBox<String> createComboBox() {
		JComboBox<String> result = new CenteredCombo<>();
		for (String s : CYCLES)
			result.addItem(s);
		result.addActionListener(this);
		combos.add(result);
		return result;
	}
	
	
	public void cycle() {
		if (custom != null) {
			custom.cycle(track);
			return;
		}
		
		switch (selected) {
			case 0: // [A][B]
				return; 
			case 1: // [AB]
				track.next(++count % 2 == 0 ? false : true);
				return;
			case 2: // [A3B][C3D]
				count++;
				if (count == 4)
					track.next(true);
				if (count > 4) {
					track.next(false);
					count = 1;
				}
				return;
			case 3: // ABCD
				track.next(true);
				return;
//			case 4: // 
//				sleepwalk();
//				return;
//			case 5: // Air G
//				airOnG();
//				return;
			default:
				throw new IllegalArgumentException("Unexpected cycle type: " + selected);
		}
	}
	
//	private void airOnGSetup() {
//		Looper looper = JudahZone.getLooper();
//		final Loop a = looper.getLoopA();
//		final Loop b = looper.getLoopB();
//		final Loop c = looper.getLoopC();
//		TimeListener muteListener = new TimeListener() {
//			@Override public void update(Property prop, Object value) {
//				if (prop == Property.LOOP) {
//					c.record(true);
//					a.removeListener(this);
//					a.setOnMute(true);
//					b.setOnMute(true);
//		}}};
//		TimeListener airListener = new TimeListener() {
//			@Override public void update(Property prop, Object value) {
//				if (prop == Property.LOOP) {
//					track.setActive(true);
//					a.removeListener(this);
//					a.addListener(muteListener);
//					c.duplicate(); 
//					c.duplicate();
//		}}};
//		a.addListener(airListener);
//		a.setOnMute(false);
//		b.setArmed(true);
//		b.setOnMute(false);
//		track.getClock().setLength(12);
//		track.setActive(false); // on listener
//		JudahZone.getChannels().getFluid().setMuteRecord(true);
//		LineIn mic = JudahZone.getChannels().getMic();
//		mic.setMuteRecord(false);
//		mic.getGain().setVol(50);
//		LineIn gtr = JudahZone.getChannels().getGuitar();
//		gtr.setMuteRecord(false);		
//		gtr.setPreset(JudahZone.getPresets().get(Raw.Freeverb));
//		gtr.getLatchEfx().latch(a);
//		gtr.reset();
//		MainFrame.update(b);
//		MainFrame.updateTime();
//		MainFrame.update(gtr);
//		MainFrame.get().sheetMusic(new File("/home/judah/sheets/AirOnG.png"));
//
//	}
//	
//	private void airOnG() {
//		if (track.isActive() == false) 
//			return;
//		track.next(true);
//	}
//
	@Override
	public void actionPerformed(ActionEvent e) {
		int change = ((JComboBox<?>)e.getSource()).getSelectedIndex();
		if (selected != change)
			setSelected(change);
	}
	
}
