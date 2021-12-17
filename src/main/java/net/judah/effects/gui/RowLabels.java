package net.judah.effects.gui;

import java.awt.Component;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;

import lombok.Getter;
import net.judah.MainFrame;
import net.judah.controllers.KnobMode;
import net.judah.controllers.MPK;
import net.judah.effects.EffectColor;
import net.judah.effects.api.Effect;
import net.judah.mixer.Channel;
import net.judah.util.Constants.Gui;
import net.judah.util.KeyPair;

public class RowLabels extends Row2 {
	
	@Getter private final ArrayList<Component> controls = new ArrayList<>();
	private final KeyPair[] source;
	
	private Font model = Gui.FONT11;
	
	private class FxTrigger extends JLabel {
		final Effect fx;
		FxTrigger(KeyPair p) {
			super(p.getKey(), JLabel.CENTER);
			fx = (Effect)p.getValue();
			addMouseListener(new MouseAdapter() {
				@Override
				public void mouseClicked(MouseEvent e) {
					fx.setActive(!fx.isActive());
					MainFrame.updateCurrent();
					//RowLabels.this.update();
				}
			});
		}
		void update(Font font) {
			if (font != null)
				setFont(font);
			setBorder(fx.isActive() ? 
					BorderFactory.createLineBorder(EffectColor.get(fx.getClass()))
					: BorderFactory.createEmptyBorder(2, 2, 2, 2));
			repaint();
		}
	}
	
	public RowLabels(Channel ch, KnobMode mode, KeyPair[] source) {
		super(ch, mode);
		if (source.length != KNOBS) 
			throw new InvalidParameterException("source knobs " + source.length);
		this.source = source;
		for (KeyPair item : source) {
			controls.add(new FxTrigger(item));
		}
	}

	@Override
	public void update() {
		
		Font font = MPK.getMode().equals(mode) ? Gui.BOLD : Gui.FONT11;
		if (font.equals(model)) 
			font = null;
		else 
			model = font;
		
//		for (Component c : controls) {
//			c.setFont(model);
//			c.repaint();
//		}
//
		
		for (Component lbl : controls) {
			((FxTrigger)lbl).update(font);
		}
		
//		for(int i = 0; i < KNOBS; i++) {
//			Effect fx = (Effect)source[i].getValue();
//			Color target = fx.isActive() ? EffectColor.get(fx.getClass()) : Pastels.BUTTONS;
//			if (controls.get(i).getBackground() != target) {
//				controls.get(i).setBackground(target);
//				controls.get(i).repaint();
//				
//			}
//		}
//		
//		Font font = MPK.getMode().equals(mode) ? Gui.BOLD : Gui.FONT11;
//		if (font.equals(model)) return;
//		model = font;
//		for (Component c : controls) {
//			c.setFont(model);
//			c.repaint();
//		}


	}

	
}
