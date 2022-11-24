package net.judah.effects.gui;

import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.JLabel;
import javax.swing.JPanel;

import net.judah.util.Pastels;

/** FilterType, LFOType, Presets */
public class FxCombo extends JPanel {

	private final JLabel text;
	
	private final Widget callback;
	
	private final MouseAdapter mouser = new MouseAdapter() {
		@Override public void mousePressed(MouseEvent e) {
			callback.increment(true);
			update();
		}
	};
	
	FxCombo(Widget callback) {
		
		text = new JLabel ("", JLabel.CENTER);
		this.callback = callback;
		update();
		addMouseListener(mouser);
		setOpaque(true);
		setBackground(Pastels.BUTTONS);
		add(text);
	}
	
	public final void update() {
		text.setText(callback.getList().get(callback.getIdx()));
	}

// 		pArTy/Hi  pArTy/Lo Lo/Hi	

//      DefaultComboBoxModel<String> lfoModel = new DefaultComboBoxModel<>();
//        for (Target t : LFO.Target.values())
//            lfoModel.addElement(t.name());
//        lfoTarget = new JComboBox<>(lfoModel);
//        lfoTarget.setSelectedItem(lfo.getTarget().toString());
//        lfoTarget.addActionListener(e -> { lfo.setTarget(
//                Target.valueOf(lfoTarget.getSelectedItem().toString()));});
	
//    	presets.setSelectedIndex(Constants.ratio(data2, presets.getItemCount() - 1));

	
}
