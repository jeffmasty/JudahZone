package net.judah.gui.settable;

import java.awt.Color;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import lombok.Getter;
import lombok.Setter;

// Combos: MidiGui: song, file, *6 synths, Track: *prog, file, pattern? LFO: type Synth: prog?
public abstract class SetCombo<T> extends  JComboBox<T> {
	
	private static final Border HIGHLIGHT = BorderFactory.createSoftBevelBorder(
			BevelBorder.RAISED, Color.RED, Color.RED.darker());
	private final Border old;
	
	@SuppressWarnings("rawtypes")
	@Setter @Getter protected static SetCombo set;
	protected final ActionListener listener = (e)->action();
	
	public SetCombo() {
		old = getBorder();
	}

	public SetCombo(T[] options, T selected) {
		this();
		refill(options, selected);
	}

	public void refill(T[] options, T selected) {
		removeActionListener(listener);
		removeAllItems();
		for (T item : options) 
			addItem(item);
		if (selected != null)
			setSelectedItem(selected);
		addActionListener(listener);
	}
	
	public void override(T val) {
		if (getSelectedItem() != val) {
			removeActionListener(listener);
			setSelectedItem(val);
			addActionListener(listener);
		}
		setBorder(old);
	}
	
	public void midiShow(T val) {
		if (set != null)
			set.setBorder(set.old);
		override(val);
		set = this;
		setBorder(HIGHLIGHT);
	}
	
	protected abstract void action();
	
	public static void set() {
		if (set == null)
			return;
		set.setBorder(set.old);
		set.action();
		set = null;
	}
	
	
	
}
