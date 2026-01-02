package net.judah.gui.fx;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;

import judahzone.api.FX;
import judahzone.util.Pair;
import lombok.Getter;
import net.judah.channel.Channel;

public class FxTrigger extends JLabel implements FXAware {

		@Getter protected final FX fx;
		FxTrigger(Pair p, Channel ch) {
			this(p.key(), p.value() instanceof FX ? (FX)p.value() : null, ch);
		}
		public FxTrigger(String lbl, FX effect, Channel ch) {
			super(lbl, JLabel.CENTER);
			fx = effect;
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					if (fx != null)
						ch.toggle(fx);
				}
			});
		}
	}