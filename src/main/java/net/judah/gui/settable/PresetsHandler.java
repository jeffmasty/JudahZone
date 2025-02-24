package net.judah.gui.settable;

import static net.judah.JudahZone.getPresets;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.judah.JudahZone;
import net.judah.fx.Preset;
import net.judah.gui.Gui;
import net.judah.gui.Size;
import net.judah.gui.Updateable;

/** Presets */
public class PresetsHandler extends SetCombo<Preset> implements ListCellRenderer<Preset>, Updateable {

	private final Presets ch;
	private final JLabel render = new JLabel();

	public PresetsHandler(Presets channel) {
		super(JudahZone.getPresets().array(), channel.getPreset());
		this.ch = channel;
		((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		Gui.resize(this, Size.MEDIUM_COMBO);
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
		if (next >= JudahZone.getPresets().size())
			next = 0;
		if (next < 0)
			next = JudahZone.getPresets().size() - 1;
		midiShow(JudahZone.getPresets().get(next));
	}

	public int getIdx() {
		return JudahZone.getPresets().indexOf(getSelectedItem());
	}

	@Override public Component getListCellRendererComponent(JList<? extends Preset> list,
			Preset value, int index, boolean isSelected, boolean cellHasFocus) {
		render.setText(value == null ? "?" : value.getName());
		return render;
	}

}
