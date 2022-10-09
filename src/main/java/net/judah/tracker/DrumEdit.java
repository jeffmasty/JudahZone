package net.judah.tracker;

import static net.judah.util.Size.*;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.drumz.DrumSample;
import net.judah.drumz.DrumType;
import net.judah.drumz.KitView;
import net.judah.util.Constants;
import net.judah.util.Pastels;


public class DrumEdit extends TrackEdit implements ActionListener, Pastels {

	private static final int mutesHeight = (int)(TABS.height / 3f * 2);
    private static final Dimension KIT_BOUNDS = new Dimension(WIDTH_KIT, mutesHeight);
    private static final Rectangle GRID_BOUNDS = new Rectangle(
    		WIDTH_BUTTONS, 0, 750, mutesHeight - 5);
    protected static final Dimension NAMESZ = new Dimension(107, STD_HEIGHT);

	@Getter private final BeatBox grid;
	@Getter private final KitView kit;
	private final DrumTrack d;
	
	public DrumEdit(final DrumTrack track) {
		super(track);
		this.d = track;
		grid = new BeatBox(track, GRID_BOUNDS);
		JPanel shell = new JPanel();
		shell.setLayout(new BoxLayout(shell, BoxLayout.PAGE_AXIS));
		
		JPanel steps = new JPanel();
		steps.setLayout(new BoxLayout(steps, BoxLayout.LINE_AXIS));
		steps.add(grid);
		steps.add(fillKit());
		shell.add(steps);
		kit = new KitView(track.getKit(), null);
		shell.add(kit);
		add(shell);
	}

	@Override
	public void step(int step) {
		grid.step(step);
	}
	
	
	public JPanel fillKit() {
		JPanel kit = new JPanel();
		kit.setMaximumSize(KIT_BOUNDS.getSize());
		kit.setLayout(new GridLayout(0, 1));
		JPanel kitTitle = new JPanel();
		kitTitle.setLayout(new GridLayout(2, 1));
		JLabel trak = new JLabel(track.getName(), JLabel.CENTER);
		JLabel mute  = new JLabel("mutes", JLabel.CENTER);
		trak.setFont(Constants.Gui.FONT11); mute.setFont(trak.getFont());
		kitTitle.add(trak);
		kitTitle.add(mute);
		kit.add(kitTitle);
		kit.setOpaque(true);
		
		for (int i = 0; i < DrumType.values().length; i++) {
			final JButton btn = new JButton(DrumType.values()[i].name());
			final DrumSample s = d.getKit().getSamples()[i];
			btn.addActionListener(e -> {
				s.setOnMute(!s.isOnMute());
				btn.setBackground(s.isOnMute() ? Pastels.PURPLE : null);
				kit.repaint();
			});
			btn.setOpaque(true);
			kit.add(btn);
		}
		return kit;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (super.handled(e))
			return;
		
	}

	@Override
	public void setPattern(int idx) {
		super.setPattern(idx);
		grid.repaint();
	}
	
	@Override
	public void update() {
		super.update();
	}

}
