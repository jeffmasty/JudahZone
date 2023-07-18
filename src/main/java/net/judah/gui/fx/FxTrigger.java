package net.judah.gui.fx;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import net.judah.fx.Effect;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.util.KeyPair;

public class FxTrigger extends JLabel {
	
		final Effect fx;
		FxTrigger(KeyPair p, Channel ch) {
			this(p.getKey(), p.getValue() instanceof Effect ? (Effect)p.getValue() : null, ch);
		}
		public FxTrigger(String lbl, Effect effect, Channel ch) {
			super(lbl, JLabel.CENTER);
			fx = effect;
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					if (fx != null)
						fx.setActive(!fx.isActive());
					MainFrame.update(ch);
				}
			});
		}
		
	}