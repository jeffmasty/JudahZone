package net.judah.gui.settable;

import java.awt.Component;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;

import net.judah.JudahZone;
import net.judah.fx.Preset;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.mixer.Channel;

/** FilterType, LFOType, Presets */
public class Fx extends SetCombo<Preset> implements ListCellRenderer<Preset> {

	private final Channel ch;

	public Fx(Channel channel, Preset[] presets) {
		super(presets, channel.getPreset());
		this.ch = channel;
		((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		Gui.resize(this, Size.COMBO_SIZE);
	}

	@Override
	protected void action() {
		Preset selected = (Preset)getSelectedItem();
		if (ch.getPreset() != selected)
			ch.setPreset(selected);
	}
	
	public final void update() {
		Preset selected = (Preset)getSelectedItem();
		if (ch.getPreset() != selected && set != this)
			override(ch.getPreset());
	}

	public void increment(boolean up) {
		int next = getIdx() + (up ? 1 : -1);
		if (next >= JudahZone.getPresets().size())
			next = 0;
		if (next < 0)
			next = JudahZone.getPresets().size() - 1;
		midiShow(JudahZone.getPresets().get(next));
	}

	public int getIdx() {
		return JudahZone.getPresets().indexOf(getSelectedItem());
	}
	
	private final JLabel render = new JLabel();
	@Override
	public Component getListCellRendererComponent(JList<? extends Preset> list, Preset value, int index,
			boolean isSelected, boolean cellHasFocus) {
		render.setText(value == null ? "?" : value.getName());
		return render;
	}
	
}
