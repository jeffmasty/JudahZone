package net.judah.gui.widgets;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import net.judah.fx.Filter;
import net.judah.fx.Filter.Type;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;

public class FilterType extends JComboBox<Type> {
	
	private final Filter filter;
	public FilterType(Filter filter, Channel ch) {
		super(Filter.Type.values());
		this.filter = filter;
		setSelectedItem(filter.getFilterType());
		addMouseListener(new MouseAdapter() {
			@Override public void mouseClicked(MouseEvent e) {
				if (SwingUtilities.isRightMouseButton(e)) {
					filter.setActive(!filter.isActive());
					MainFrame.update(ch);
				}
			}
		});
		((JLabel)getRenderer()).setHorizontalAlignment(SwingConstants.CENTER);
		addActionListener(e->filter.setFilterType((Type)getSelectedItem()));
	}

	public void update() {
		if (!getSelectedItem().equals(filter.getFilterType()))
			setSelectedItem(filter.getFilterType());
	}
	
}
