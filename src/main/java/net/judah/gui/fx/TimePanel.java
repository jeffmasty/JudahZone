package net.judah.gui.fx;

import java.awt.FlowLayout;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import judahzone.api.TimeFX;
import judahzone.gui.Pastels;
import judahzone.gui.Updateable;
import lombok.Getter;
import net.judah.channel.Channel;
import net.judah.gui.MainFrame;
import net.judah.midi.JudahClock;

public class TimePanel extends JPanel implements Updateable, FXAware {
	//⏰  U+023F0  ALARM CLOCK
	//⏱  U+023F1  STOPWATCH
	//⏲  U+023F2  TIMER CLOCK
	@Getter private final TimeFX fx;
	private final Channel channel;
	private final JButton sync = new JButton("⏲");
	private final JComboBox<String> type = new JComboBox<>(TimeFX.TYPE);

	public TimePanel(TimeFX effect, Channel ch, JudahClock clock) {
		super(new FlowLayout(FlowLayout.CENTER, 0, 1));
		this.channel = ch;

		fx = effect;
		type.addActionListener(e-> {
			if (!type.getSelectedItem().equals(fx.getType())) {
				fx.setType(type.getSelectedItem().toString());
				fx.sync(clock.syncUnit());
				MainFrame.update(channel);
			}
		});
		sync.addActionListener(e-> {
			fx.setSync(!fx.isSync());
			if (fx.isSync())
				fx.sync(clock.syncUnit());
			update();
			MainFrame.update(channel);
		});
		sync.setOpaque(true);
		add(type);
		add(sync);
	}

	@Override public void update() {
		type.setSelectedItem(fx.getType());
		sync.setBackground(fx.isSync() ? Pastels.GREEN : null);

	}

}
