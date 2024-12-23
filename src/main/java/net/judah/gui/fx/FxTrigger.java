package net.judah.gui.fx;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import net.judah.fx.Effect;
import net.judah.gui.MainFrame;
import net.judah.mixer.Channel;
import net.judah.omni.Pair;

public class FxTrigger extends JLabel {

		final Effect fx;
		FxTrigger(Pair p, Channel ch) {
			this(p.key(), p.value() instanceof Effect ? (Effect)p.value() : null, ch);
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