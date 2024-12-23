package net.judah.gui.fx;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import net.judah.fx.TimeEffect;
import net.judah.gui.MainFrame;
import net.judah.gui.Pastels;
import net.judah.gui.Updateable;
import net.judah.mixer.Channel;

public class TimePanel extends JPanel implements Updateable {
	//⏰  U+023F0  ALARM CLOCK
	//⏱  U+023F1  STOPWATCH
	//⏲  U+023F2  TIMER CLOCK
	private final TimeEffect fx;
	private final Channel channel;
	private final JButton sync = new JButton("⏲");
	private final JComboBox<String> type = new JComboBox<>(TimeEffect.TYPE);

	public TimePanel(TimeEffect effect, Channel ch) {
		super(new FlowLayout(FlowLayout.CENTER, 0, 1));
		this.channel = ch;

		fx = effect;
		type.addActionListener(e->{
			if (!type.getSelectedItem().equals(fx.getType())) {
				fx.setType(type.getSelectedItem().toString());
				fx.sync();
				MainFrame.update(channel);
			}

		});
		sync.addActionListener(e->{
			fx.setSync(!fx.isSync());
			if (fx.isSync())
				fx.sync();
			update();
			MainFrame.update(channel);
		});
		sync.setOpaque(true);
		add(type);
		add(sync);
	}

	@Override
	public void update() {
		type.setSelectedItem(fx.getType());
		sync.setBackground(fx.isSync() ? Pastels.GREEN : null);

	}

}
