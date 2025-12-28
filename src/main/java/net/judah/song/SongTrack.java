package net.judah.song;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

import lombok.Getter;
import net.judah.gui.Gui;
import net.judah.gui.Icons;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.TabZone;
import net.judah.gui.Updateable;
import net.judah.gui.settable.ModeCombo;
import net.judah.gui.settable.Program;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.PlayWidget;
import net.judah.gui.widgets.TogglePreset;
import net.judah.gui.widgets.TrackGain;
import net.judah.gui.widgets.TrackVol;
import net.judah.seq.automation.Automation;
import net.judah.seq.track.ChannelTrack;
import net.judah.seq.track.Computer.Update;
import net.judah.seq.track.MidiTrack;
import net.judah.seq.track.PianoTrack;
import net.judah.seq.track.Programmer;

/**  show/change MidiTrack state */
@Getter public class SongTrack extends JPanel implements Size, Updateable {
	private static final Dimension COMPUTER = new Dimension(204, 27);

	private final MidiTrack track;
	private final Automation automation;
	private final CCTrack ccTrack;
	private final Program program;
	private final Programmer programmer;
	private final ModeCombo mode;
	private final PlayWidget play;
	private final TrackGain gain;
	private final TrackVol amp;
	private JLabel folder = new JLabel("");
	private TogglePreset preset;

	// TODO MouseWheel listener -> change pattern?
	public SongTrack(MidiTrack t, Automation auto) {
		this.track = t;
		this.automation = auto;
		play = new PlayWidget(this);
		mode = t instanceof PianoTrack p ? new ModeCombo(p) : null;
		gain = t instanceof PianoTrack ? null : new TrackGain(t);
		amp = new TrackVol(track);
		program = new Program(track);
		programmer = new Programmer(track);
		ccTrack = new CCTrack(track, auto);

		JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING, 2, 2));
		top.add(Gui.resize(play, MODE_SIZE));
		if (gain != null)
			top.add(gain);
		if (mode != null)
			top.add(mode);
		if (t instanceof ChannelTrack) {
			preset = new TogglePreset(track.getChannel());
			top.add(preset);
		}
		else
			top.add(amp);

		top.add(Gui.resize(program, COMBO_SIZE));
		top.add(Gui.resize(programmer, COMPUTER));

		if (t instanceof ChannelTrack)
			top.add(new Btn(Icons.DETAILS_VEW, e->MainFrame.setFocus(t.getChannel())));
		else
			top.add(new Btn(Icons.DETAILS_VEW, e->TabZone.edit(track)));

		if (MainFrame.isBundle())
			top.add(new Btn(" ⏲ ", l->expand()));

		else if (false == t instanceof ChannelTrack) {
			folder.addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(java.awt.event.MouseEvent e) {
					track.load(); }});
			folder.setBorder(Gui.SUBTLE);
			if (track.getFile() != null)
				folder.setText(track.getFile().getName());
			top.add(Gui.resize(folder, MEDIUM));
		}
		Gui.resize(this, Overview.TRACK);
		setBorder(Gui.SUBTLE);
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(top);
	}

	public boolean isExpanded() {
		return getSize().equals(BIG);
	}

	private static Dimension BIG = new Dimension(Overview.TRACK.width, 2 * Overview.TRACK_HEIGHT);
	public void expand() {
		boolean wasBig = isExpanded();
		Gui.resize(this, wasBig ? Overview.TRACK : BIG);

		if (!wasBig) {
			automation.init(track);
			add(ccTrack);
			ccTrack.requestFocus();
		} else
			remove(ccTrack);

		invalidate();
		doLayout();
		getParent().getParent().doLayout();
		getParent().getParent().repaint();
		if (ccTrack.isVisible())
			ccTrack.repaint();
	}

	public void step(int oneStep) {
		if (track.isActive() == false || isExpanded() == false)
			return;
		ccTrack.step(oneStep);
	}

	@Override public void update() {
		if (gain != null)
			gain.update();
		if (preset != null)
			preset.update();
		// program.update();
	}

	public void update(Update type) {
		if (Update.PROGRAM == type)
			program.update();
		else if (Update.ARP == type && mode != null)
			mode.update();
		else if (Update.CYCLE == type)
			programmer.getCycle().update();
		else if (Update.CAPTURE == type || Update.PLAY == type)
			play.update();
		else if (Update.CURRENT == type) {
			programmer.getCurrent().update();
			if (isExpanded())
				ccTrack.repaint();
		}
		else if (Update.LAUNCH == type)
			programmer.liftOff();
		else if (Update.AMP == type)
			amp.update();
		else if (Update.FILE == type) {
			folder.setText(track.getFile() == null ? "" : track.getFile().getName());
			programmer.liftOff();
			if (isExpanded())
				ccTrack.repaint();
		}
		else if (Update.REZ == type) {
			if (isExpanded())
				ccTrack.repaint();
		}
		else if (Update.EDIT == type) {
			programmer.liftOff();
			if (isExpanded())
				ccTrack.repaint();
		}

	}

}
