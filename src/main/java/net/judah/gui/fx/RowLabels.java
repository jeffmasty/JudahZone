package net.judah.gui.fx;

import java.security.InvalidParameterException;

import net.judah.mixer.Channel;
import net.judah.util.Pair;

public class RowLabels extends Row {
	public static final int KNOBS = 4;

	public RowLabels(Channel ch, Pair... source) {
		super(ch);
		if (source.length != KNOBS)
			throw new InvalidParameterException("source knobs " + source.length);
		for (Pair item : source) {
			controls.add(new FxTrigger(item, ch));
		}
	}

//	private class FxTrigger extends JLabel {
//		final Effect fx;
//		FxTrigger(KeyPair p) {
//			super(p.getKey(), JLabel.CENTER);
//			fx = p.getValue() instanceof Effect ? (Effect)p.getValue() : null;
//			addMouseListener(new MouseAdapter() {
//				@Override public void mouseClicked(MouseEvent e) {
//					if (fx != null)
//					fx.setActive(!fx.isActive());
//					MainFrame.update(channel);
//				}
//			});
//		}
//	}

}
