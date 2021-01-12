package net.judah.mixer;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;

import lombok.Getter;
import net.judah.looper.Sample;
import net.judah.mixer.bus.Compression;
import net.judah.mixer.bus.CutFilter;
import net.judah.mixer.bus.CutFilter.Type;
import net.judah.mixer.bus.EQ.EqBand;
import net.judah.mixer.bus.EQ.EqParam;
import net.judah.mixer.bus.LFO;
import net.judah.mixer.bus.LFO.Target;
import net.judah.mixer.bus.Reverb;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Constants.Gui;
import net.judah.util.Knob;
import net.judah.util.MenuBar;
import net.judah.util.RTLogger;

public class SoloTrack extends JPanel {
	// TODO tap tempo, lfo recover // play/stop/rec button icons
	
	public static final String EQ_PARTY = "pArTy";
	public static final String EQ_LOCUT = "LoCut";
	public static final String EQ_HICUT = "HiCut";
	public static final String[] EQs = new String[] 
			{ EQ_PARTY, EQ_LOCUT, EQ_HICUT };
	
	@Getter private static SoloTrack instance;
	@Getter private Channel focus;
	
	private final JPanel headerRow, 
		eqRow, compRow, reverbRow, delayRow, lfoRow;
	
	@Getter private CutFilter cutFilter;
	@Getter private Compression compression;
	@Getter private Reverb reverb;
	@Getter private LFO lfo;

	private final JLabel name = new JLabel();
	private JSlider volume;

	private JToggleButton eqActive;
	private Knob eqBass, eqMid, eqTreble;

	private JToggleButton compActive;
	private Knob compAtt, compRel, compThresh;
	
	private JToggleButton revActive;
	private Knob revRoom, revDamp, revWidth;

	private JToggleButton delActive;
	private JSlider delFeedback, delTime;

	private JToggleButton lfoActive, cutActive;
	private Knob lfoAmp, cutRes;
	private JSlider lfoFreq, cutFreq;
	private JComboBox<String> lfoTarget, cutType;
	
	public SoloTrack() {
		instance = this;
		setBorder(Gui.GRAY1);

		headerRow = headerRow();
		eqRow = eqRow();
		compRow = compressionRow();
		reverbRow = reverbRow();
		delayRow = delayRow();
		lfoRow = lfoCutRow();

		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		add(headerRow);
		add(eqRow);
		add(compRow);
		add(reverbRow);
		add(delayRow);
		add(lfoRow);
		
		// add(JudahZone.getPlugins().getGui());
	}
	
	/** you probably want to use MixerPane.setFocus() */
	public void setFocus(Channel bus) {
		this.focus = bus;
		reverb = bus.getReverb();
		compression = bus.getCompression();
		lfo = bus.getLfo();
		cutFilter = bus.getCutFilter();
		revActive.setEnabled(false == bus instanceof Sample); // TODO reverb not working well on output
		update();
	}
	
	public static void volume(Channel o) {
		if (o == instance.focus)
			instance.volume.setValue(o.getVolume());
	}

	public void update() {
		name.setText(focus.getName());
		revActive.setSelected(reverb.isActive());
		revRoom.setValue(Math.round(reverb.getRoomSize() * 100)); 
		revDamp.setValue(Math.round(reverb.getDamp() * 100)); 
		revWidth.setValue(Math.round(reverb.getWidth() * 100));

		compActive.setSelected(compression.isActive());
		compThresh.setValue((int) ((compression.getThreshold() + 40) * 2.5));
		int attack = (int)Math.round(compression.getAttack() / 0.75);
		if (attack > 100) attack = 100;
		compAtt.setValue(attack);
		int release = (int)Math.round(compression.getRelease() * 0.333);
		if (release > 100) release = 100;
		compRel.setValue(release);
		
		lfoActive.setSelected(lfo.isActive());
		if (lfo.isActive()) {
			lfoAmp.setValue((int)lfo.getAmplitude());
			lfoFreq.setValue((int)lfo.getFrequency());
		}
		
		cutActive.setSelected(cutFilter.isActive());
		cutType.setSelectedItem(cutFilter.getFilterType().name());
		cutFreq.setValue( CutFilter.frequencyToKnob(cutFilter.getFrequency()));
		cutRes.setValue((int)(cutFilter.getResonance() * 4)); 
		
		delFeedback.setValue(Math.round(focus.getDelay().getFeedback() * 100f));
		delTime.setValue(delTime());
		delActive.setSelected(focus.getDelay().isActive());
	}
	
	// Header ////////////////////////////////////////////////////////////////////////////
	private JPanel headerRow() {
		JPanel result = new JPanel();
		result.setLayout(new FlowLayout());
		volume = new JSlider(0, 100);
		volume.addChangeListener(l -> { focus.setVolume(volume.getValue()); });
		result.add(volume);
		name.setFont(Constants.Gui.BOLD);
		result.add(name);
		// MixerButton mute = new JLabel(Icons.muteActive); result.add(mute);
		return result;
	}
	
	// EQ ////////////////////////////////////////////////////////////////////////////////
	private JPanel eqRow() {
		JPanel result = new Row();
		result.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				// reverb.initialize(Constants._SAMPLERATE, Constants._BUFSIZE); update();
				Console.info("eq init(no-op)");
		}});
		eqActive = new JToggleButton("EQ");
		eqActive.addActionListener(listener -> { 
			focus.getEq().setActive(!focus.getEq().isActive());
			update();
			Console.info(name.getText() + " EQ: " + (focus.getEq().isActive() ? " On" : " Off"));
		});
		eqActive.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		eqBass = new Knob(val -> {eqGain(EqBand.BASS, val);});
		eqMid = new Knob(val -> {eqGain(EqBand.MID, val);});
		eqTreble = new Knob(val -> {eqGain(EqBand.TREBLE, val);});
		
		result.add(eqActive);
		result.add(Box.createHorizontalGlue());
		result.add(labelPanel("bass ", eqBass));
		result.add(labelPanel("middle ", eqMid));
		result.add(labelPanel("treble ", eqTreble));
		return result;
	}
	private void eqGain(EqBand eqBand, int val) {
		boolean negative = val < 50;
		float result = Math.abs(50 - val) / 2;
		if (negative) result *= -1;
		focus.getEq().update(eqBand, EqParam.GAIN, result);
		Console.info("eq gain: " + result);
	}

	private void eqFilterType() {
		switch(cutType.getSelectedItem().toString()) {
			case EQ_PARTY: cutFilter.setFilterType(Type.pArTy); break; 
			case EQ_HICUT: cutFilter.setFilterType(Type.LP12); break;
			case EQ_LOCUT: cutFilter.setFilterType(Type.HP12); break;
		}
	}

	// COMPRESSION ///////////////////////////////////////////////////////////////////////	
	private JPanel compressionRow() {
		
		JPanel result = new Row();
		result.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				compression.setPreset(compression.getPreset());
				update();
				Console.info("compression init()");
		}});

		compActive = new JToggleButton("Comp.");
		compActive.addActionListener(listener -> { 
			compression.setActive(!compression.isActive());
			update();
			Console.info(name.getText() + " Compression: " + (compression.isActive() ? " On" : " Off"));
		});
		compActive.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		compThresh = new Knob(val -> {compression.setThreshold((int)((val - 100) / 2.5));});
		compThresh.setToolTipText("-40 to 0");
		compAtt = new Knob(val -> {compression.setAttack((int)Math.round(val * 1.5));});
		compAtt.setToolTipText("0 to 150 milliseconds");
		compRel = new Knob(val -> {compression.setRelease(Math.round(val * 3));});
		compRel.setToolTipText("0 to 300 milliseconds");

		result.add(compActive);
		result.add(Box.createHorizontalGlue());
		result.add(labelPanel("attack", compAtt));
		result.add(labelPanel("release", compRel));
		result.add(labelPanel("threshold", compThresh));
		return result;
	}
	public static void compression(Channel o) {
		if (o == instance.focus)
			instance.compActive.setSelected(o.getCompression().isActive());
	}
	
	// REVERB /////////////////////////////////////////////////////////////////////////////
	private JPanel reverbRow() {
		JPanel result = new Row();
		result.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				reverb.initialize(Constants._SAMPLERATE, Constants._BUFSIZE);
				update();
				Console.info("reverb init()");
		}});

		revActive = new JToggleButton("Reverb");
		revActive.addActionListener(listener -> { 
			reverb.setActive(!reverb.isActive());
			update();
			Console.info(name.getText() + " Reverb: " + (reverb.isActive() ? " On" : " Off"));
		});
		// revActive.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		revRoom = new Knob(val -> {reverb.setRoomSize(val / 100f);});
		revDamp = new Knob(val -> {reverb.setDamp(val / 100f);});
		revWidth = new Knob(val -> {reverb.setWidth(val / 100f);});
		result.add(revActive);
		result.add(Box.createHorizontalGlue());
		result.add(labelPanel("room", revRoom));
		result.add(labelPanel("damp", revDamp));
		result.add(labelPanel("width", revWidth));
		return result;
	}
	public static void reverb(Channel o) {
		if (o == instance.focus)
			instance.revActive.setSelected(o.getReverb().isActive());
	}

	// DELAY /////////////////////////////////////////////////////////////////////////////
	private JPanel delayRow() {
		JPanel result = new Row();
		result.addMouseListener(new MouseAdapter() { // right click reinitialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				update(); Console.info("delay init()");
		}});
		
		delActive = new JToggleButton("Delay");
		delActive.addActionListener(listener -> {
			focus.getDelay().setActive(!focus.getDelay().isActive());
			update();
			Console.info(name.getText() + " Delay : " + 
					(focus.getDelay().isActive() ? " On" : " Off"));
			if (focus.getDelay().isActive()) focus.getDelay().reset();
		});
		// delActive.setAlignmentX(JLabel.LEFT_ALIGNMENT);
		delFeedback = new JSlider(0, 100);
		delFeedback.setMaximumSize(Gui.SLIDER_SZ);
		delFeedback.setPreferredSize(Gui.SLIDER_SZ);
		delFeedback.addChangeListener(e -> {delFeedback(delFeedback.getValue());});
		
		delTime = new JSlider(0, 100);
		delTime.setMaximumSize(Gui.SLIDER_SZ);
		delTime.setPreferredSize(Gui.SLIDER_SZ);
		delTime.addChangeListener(e -> {delTime(delTime.getValue());});

		result.add(delActive);
		result.add(Box.createHorizontalGlue());
		result.add(labelPanel("feedback ", delFeedback));
		result.add(labelPanel(" time ", delTime));
		result.add(Box.createRigidArea(new Dimension(8, 1)));
		//JButton tap = new JButton("tap"); result.add(tap);
		//tap.setAlignmentX(JLabel.RIGHT_ALIGNMENT);
		return result;
	}
	private int delTime() {
		float max = focus.getDelay().getMaxDelay();
		// result / 100 = delay / max
		return Math.round((focus.getDelay().getDelay() * max) * 100f);
	}
	private void delTime(int val) {
		float max = focus.getDelay().getMaxDelay();
		// val/100 = set()/max 
		focus.getDelay().setDelay(val * max / 100f);
	}
	private void delFeedback(int val) {
		focus.getDelay().setFeedback(val / 100f);
	}

	// LFO/CUT_EQ //////////////////////////////////////////////////////////////////////////
	private JPanel lfoCutRow() {
		JPanel lp = new JPanel();
		lp.setBorder(Gui.GRAY1);
		lfoActive = new JToggleButton("LFO");
		lfoActive.addActionListener(listener -> {lfo();});
		lfoAmp = new Knob(val -> {lfo.setAmplitude(val);});
		lfoAmp.setValue(100);
		lfoFreq = new JSlider(200, 3200);
		lfoFreq.setValue(1000);
		lfoFreq.setMaximumSize(Gui.SLIDER_SZ);
		lfoFreq.setPreferredSize(Gui.SLIDER_SZ);
		lfoFreq.addChangeListener(e -> {lfo.setFrequency(lfoFreq.getValue());});
		DefaultComboBoxModel<String> lfoModel = new DefaultComboBoxModel<String>();
		for (Target t : LFO.Target.values()) 
			lfoModel.addElement(t.name());
		lfoTarget = new JComboBox<String>(lfoModel);
		lfoTarget.setMaximumSize(new Dimension(65, 40));
		lfoTarget.addActionListener(e -> { lfo.setTarget(Target.valueOf(lfoTarget.getSelectedItem().toString()));});

		lp.setLayout(new BoxLayout(lp,BoxLayout.Y_AXIS));
		JPanel row1 = new JPanel();
		row1.add(lfoActive);
		row1.add(labelPanel("time", lfoFreq));
		JPanel row2 = new JPanel();
		row2.add(lfoTarget);
		row2.add(labelPanel("max", lfoAmp));
		lp.add(row1);
		lp.add(row2);

		cutActive = new JToggleButton("CUT");
		cutActive.addActionListener(listener -> { 
			cutFilter.setActive(!cutFilter.isActive());
			update();
			Console.info(name.getText() + " CUT: " + (cutFilter.isActive() ? " On" : " Off"));
		});
		DefaultComboBoxModel<String> cutModel = new DefaultComboBoxModel<>(EQs);
		cutType = new JComboBox<String>(cutModel);
		cutType.setMaximumSize(new Dimension(65, 40));
		cutType.addActionListener(e -> {eqFilterType();});
		cutFreq = new JSlider(0, 100);
		cutFreq.addKeyListener(MenuBar.getInstance());
		cutFreq.setMaximumSize(Gui.SLIDER_SZ);
		cutFreq.setPreferredSize(Gui.SLIDER_SZ);
		cutFreq.addChangeListener(e -> {
			cutFilter.setFrequency(CutFilter.knobToFrequency(cutFreq.getValue()));
			RTLogger.log(this, "EQ frequency: " + cutFilter.getFrequency());
		});
		cutRes = new Knob(val -> {cutFilter.setResonance(val * 0.25f);});
		
		JPanel cp = new JPanel();
		cp.setBorder(Gui.GRAY1);
		cp.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				// reverb.initialize(Constants._SAMPLERATE, Constants._BUFSIZE); update();
				Console.info("eq init(no-op)");
		}});
		cp.setLayout(new BoxLayout(cp,BoxLayout.Y_AXIS));
		row1 = new JPanel();
		row1.add(cutActive);
		row1.add(labelPanel("hz.", cutFreq));
		row2 = new JPanel();
		row2.add(cutType);
		row2.add(labelPanel("res", cutRes));
		cp.add(row1);
		cp.add(row2);
		
		JPanel result = new JPanel();
		result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
		result.add(lp);
		result.add(cp);
		return result;
	}
	public void lfo() {
		lfo.setActive(!lfo.isActive());
		// if (lfo.isActive()) lfoRecover = focus.getVolume(); else focus.setVolume(lfoRecover);
		update();
		Console.info(name.getText() + " LFO: " + (lfo.isActive() ? " On" : " Off"));
	}

	private class Row extends JPanel {
		Row() {
			setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
			add(Box.createRigidArea(new Dimension(8, 0)));
			setBorder(Gui.GRAY1);
		}
	}
	private JPanel labelPanel(String name, Component c) {
		JPanel pnl = new JPanel();
		pnl.add(new JLabel(name));
		pnl.add(c);
		return pnl;
	}
	
}

