package net.judah.effects.gui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.swing.JLabel;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.effects.api.Effect;
import net.judah.mixer.Channel;
import net.judah.util.KeyPair;

public class RowLabels extends Row {
	
	@Getter private final ArrayList<Component> controls = new ArrayList<>();
	
	public RowLabels(Channel ch, KeyPair[] source) {
		super(ch);
		if (source.length != KNOBS) 
			throw new InvalidParameterException("source knobs " + source.length);
		for (KeyPair item : source) {
			controls.add(new FxTrigger(item));
		}
	}

	private class FxTrigger extends JLabel {
		final Effect fx;
		FxTrigger(KeyPair p) {
			super(p.getKey(), JLabel.CENTER);
			fx = p.getValue() instanceof Effect ? (Effect)p.getValue() : null;
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					if (fx != null)
					fx.setActive(!fx.isActive());
					MainFrame.update(channel);
				}
			});
		}
	}
	
}
