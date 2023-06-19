package net.judah.gui.settable;

import java.awt.Color;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.JComboBox;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import lombok.Getter;
import lombok.Setter;
import net.judah.util.Constants;

// Combos: MidiGui: song, file, *6 synths, Track: *prog, file, pattern? LFO: type Synth: prog?
public abstract class SetCombo<T> extends  JComboBox<T> {
	
	protected static final Border HIGHLIGHT = BorderFactory.createSoftBevelBorder(
			BevelBorder.RAISED, Color.RED, Color.RED.darker());
	protected final Border old;
	
	private T override;
	
	@SuppressWarnings("rawtypes")
	@Setter @Getter protected static SetCombo set;
	protected final ActionListener listener = (e)->action();
	
	public SetCombo() {
		old = getBorder();
	}

	public SetCombo(T[] options, T selected) {
		this();
		if (options == null) 
			return;
		for (T item : options) 
			addItem(item);
		if (selected != null)
			setSelectedItem(selected);
		addActionListener(listener);
	}

	public void refill(T[] options, T selected) {
		removeActionListener(listener);
		removeAllItems();
		addItem(null);
		for (T option : options) {
			addItem(option);
			if (selected == option)
				setSelectedItem(option);
		}
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
		
		if (set != null && set != this) {
			@SuppressWarnings("rawtypes")
			final SetCombo old = set;
			Constants.execute(()-> old.override(old.override));
		}
		if (set != this) {
			override = (T)getSelectedItem();
			set = this;
		}
		
		if (getSelectedItem() != val) {
			final T midiVal = val;
			Constants.execute(()->{
				removeActionListener(listener);
				setSelectedItem(midiVal);
				addActionListener(listener);
				setBorder(HIGHLIGHT);
			});
		}
	}
	
	protected abstract void action();
	
	public static void set() {
		if (set == null)
			return;
		set.setBorder(set.old);
		SetCombo engage = set;
		set = null;
		engage.action();
	}
	
	
	
}
