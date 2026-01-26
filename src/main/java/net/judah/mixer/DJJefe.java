package net.judah.mixer;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import judahzone.api.FX;
import judahzone.fx.Gain;
import judahzone.gui.Gui;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.LineIn;
import net.judah.gui.MainFrame;
import net.judah.looper.Loop;
import net.judah.mixer.Channels.MixBus;

/** Graphical representation of the Mixer */
public class DJJefe extends JPanel implements MixBus {

	static final int IDEAL_WIDTH = 55; // ?55 TODO onMixer ribbon/scroll

	/** Any channel available to LFO knobs, not necessarily a main mixer fader */
	private final ArrayList<MixWidget> faders = new ArrayList<MixWidget>(); // onMixer

	private final JudahZone zone;
	private final Channels registry;

	private final int REFRESHES = 4; // process RMS of this many channels per refresh
	private static final int COUNTUP = 3; // number of frames between gui refresh calls
	private int idx;
	private int countUp;

	private final JPanel loops = new JPanel();
	private final JPanel sys = new JPanel();
	private final JPanel user = new JPanel();
	private final JPanel main = new JPanel();

    public DJJefe(JudahZone judahZone) {
    	this.zone = judahZone;
    	this.registry = zone.getChannels();
    }

    public void gui() {

    	int loopCount = zone.getLooper().size();
    	loops.setLayout(new GridLayout(1, loopCount, 0, 0));

    	for (Loop loop : zone.getLooper()) // have loops register themselves as onMixer/sys?
    		addNow(loop);

    	MainsMix speakers = new MainsMix(zone.getMains());
    	main.setLayout(new GridLayout(1, 1, 0, 0));
        main.add(speakers);
        faders.add(speakers);
    	registry.subscribe(this); // now?

    	setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
    	add(loops);
    	add(sys);
    	add(user);
    	add(main);
    }

	public void process() {
		// count this invocation; act only on every 3rd call
		countUp++;
		if (countUp % COUNTUP == 0) {
			countUp = 0;
			for (int i = 0; i < REFRESHES; i++) {
			    if (++idx >= faders.size())
			        idx = 0;
			    MainFrame.update(faders.get(idx).rms);
			}
		}
	}


    private MixWidget factory(Channel ch) {
		if (ch instanceof LineIn line)
			return new LineMix(line, zone.getLooper().getSoloTrack(), zone);
		if (ch instanceof Loop loop)
			return new LoopMix(loop, zone.getLooper());
		return null;
	}

	public void update(Channel channel) {
		for (MixWidget ch : faders)
			if (ch.channel.equals(channel))
				ch.update();

	}

	public void update(Channel ch, FX fx) {
		MixWidget it = getFader(ch);
		if (it == null)
			return;
		if (fx instanceof Gain)
			it.updateVolume();
		else
			it.getIndicators().sync(fx);
	}

	public void updateAll() {
		for (MixWidget ch : faders)
			ch.update();
	}

	public void highlight(ArrayList<Channel> s) {
		for (MixWidget ch : faders) {
			ch.setBorder(s.contains(ch.channel) ? Gui.HIGHLIGHT : Gui.NO_BORDERS);
		}
	}

	public void highlight(Channel o) {
		for (MixWidget ch : faders)
			ch.setBorder(ch.channel == o ? Gui.HIGHLIGHT : Gui.NO_BORDERS);
	}

	public MixWidget getFader(Channel ch) {
		for (MixWidget fade : faders)
			if (fade.channel == ch)
				return fade;
		return null;
	}

	private void addNow(Channel ch) {
		MixWidget widget = factory(ch);
        faders.add(widget);
        widget.update();

        if (ch instanceof Loop) {
			loops.add(widget);
			loops.setLayout(new GridLayout(1, loops.getComponentCount(), 0, 0));
			loops.doLayout();
			revalidate();
			repaint();
			return;
		}

        if (ch.isSys()) {
        	sys.add(widget);
        	sys.setLayout(new GridLayout(1, sys.getComponentCount(), 0, 0));
        	sys.doLayout();
        	revalidate();
        	repaint();
        	return;
        }

        reordered();
        refresh();
	}

	private void removeNow(Channel ch) {
	    MixWidget widget = getFader(ch);
	    if (widget == null)
	    	return;
	    faders.remove(widget);
	    reordered();
	}

	@Override
	public void reordered() {
		user.removeAll();
		for (Channel ch : registry.getUserChannels()) {
			if (ch.isOnMixer()) {
				MixWidget widget = getFader(ch);
				if (widget != null)
					user.add(widget);
			}
		}
		refresh();

	}

	private void refresh() {
        user.setLayout(new GridLayout(1, faders.size(), 0, 0));
        user.invalidate();
        user.doLayout();
        revalidate();
        repaint();
	}

	@Override public void channelRemoved(final LineIn ch) {

		MixWidget fader = getFader(ch);
		if (fader == null)
			return;

		if (zone.getFxRack().getSelected().contains(fader.channel))
			MainFrame.setFocus(zone.getMains());

		SwingUtilities.invokeLater(() -> removeNow(ch));
	}

	@Override public void channelAdded(final LineIn ch) {
		if (!ch.isOnMixer())
			return;
		SwingUtilities.invokeLater(() -> addNow(ch));
	}

	// ch might have become visible or hidden or it's icon may have been changed.
	@Override public void update(LineIn ch) {
		if (ch == null)
			return;
		SwingUtilities.invokeLater(() -> {
			MixWidget widget = getFader(ch);
			if (widget != null) {
				widget.update();
				widget.checkIcon();
			}
		});
	}

	/** DOES NOT INLUCDE LOOPS OR MAINS */
	public ArrayList<LineIn> getVisible() {
		ArrayList<LineIn> visible = new ArrayList<LineIn>();
		for (MixWidget mw : faders) {
			if (mw instanceof LineMix line)
				visible.add((LineIn)line.channel);
		}
		return visible;
	}

	public LineIn getVisible(int idx) {
		int count = 0;
		for (MixWidget mw : faders) {
			if (mw instanceof LineMix) {
				if (count == idx)
					return (LineIn)mw.channel;
				count++;
			}
		}
		return null;
	}

}
