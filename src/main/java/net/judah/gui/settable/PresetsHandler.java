package net.judah.gui.settable;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.Updateable;
import net.judah.mixer.Channel;
import net.judah.mixer.Preset;
import net.judah.mixer.PresetsDB;

/** Presets */
public class PresetsHandler extends SetCombo<Preset> implements ListCellRenderer<Preset>, Updateable {

	private static final PresetsDB presets = new PresetsDB(); // 21
	public static final PresetsDB getPresets() { return presets; }

	private final Channel ch;
	private final JLabel render = new JLabel();

	public PresetsHandler(Channel channel) {
		super(presets.array(), channel.getPreset());
		this.ch = channel;
		((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		Gui.resize(this, Size.MEDIUM);
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e))
					getPresets().replace(ch);}});
	}

	@Override
	protected void action() {
		Preset selected = (Preset)getSelectedItem();
		if (ch.getPreset() != selected)
			ch.setPreset(selected);
	}

	@Override
	public final void update() {
		Preset selected = (Preset)getSelectedItem();
		if (ch.getPreset() != selected && set != this)
			override(ch.getPreset());
	}

	public void increment(boolean up) {
		int next = getIdx() + (up ? 1 : -1);
		if (next >= presets.size())
			next = 0;
		if (next < 0)
			next = presets.size() - 1;
		midiShow(presets.get(next));
	}

	public int getIdx() {
		return presets.indexOf(getSelectedItem());
	}

	@Override public Component getListCellRendererComponent(JList<? extends Preset> list,
			Preset value, int index, boolean isSelected, boolean cellHasFocus) {
		render.setText(value == null ? "?" : value.getName());
		return render;
	}

}
