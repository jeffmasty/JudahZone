package net.judah.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import net.judah.fx.Filter;
import net.judah.fx.StereoBiquad.FilterType;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Updateable;
import net.judah.mixer.Channel;

public class CutTrigger extends JLabel implements Updateable {

	private static final int IDX = Filter.Settings.Type.ordinal();
	private final Filter filter;

	public CutTrigger(Filter hilo, Channel ch) {
		super("", JLabel.CENTER);
		this.filter = hilo;
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					filter.set(IDX, filter.get(IDX) > 50 ? 0 : 100);
				} else
					filter.setActive(!filter.isActive());
				MainFrame.update(ch);
			}});
		setBorder(Gui.SUBTLE);
	}

	@Override public void update() {
		boolean loCut = filter.get(IDX) < 50 ? true : false;
		FilterType target = loCut ? FilterType.LowPass : FilterType.HighPass;
		if (false == getText().equals(target.getDisplay()))
			setText(target.getDisplay());

	}

}
