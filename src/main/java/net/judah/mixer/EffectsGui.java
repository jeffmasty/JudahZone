package net.judah.mixer;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import lombok.Getter;
import net.judah.looper.Sample;
import net.judah.mixer.EQ.Type;
import net.judah.plugin.LFO;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Constants.Gui;
import net.judah.util.Knob;
import net.judah.util.RTLogger;

public class EffectsGui extends JPanel {

	public static final String EQ_PARTY = "pArTy";
	public static final String EQ_HICUT = "HiCut";
	public static final String EQ_BAND = "BAND";
	public static final String EQ_NOTCH = "Notch";
	public static final String EQ_LOCUT = "LoCut";
	public static final String[] EQs = new String[] 
	{ /* EQ_PARTY, */EQ_HICUT, EQ_BAND, EQ_NOTCH, EQ_LOCUT};
	
	@Getter private static EffectsGui instance;
	@Getter private MixerBus focus;
	
	private EQ eq;
	private Compression compression;
	private Reverb reverb;
	private LFO lfo;

	private final JPanel header = new JPanel();
	private final JLabel name = new JLabel();
	private final Knob volume;

	private final JPanel revEffects;
	private JToggleButton revActive;
	private Knob revRoom, revDamp, revWidth;
	private JToggleButton compActive;
	
	private final JPanel compEffects;
	private Knob compAtt, compRel;
	private Knob compRatio, compThresh;

	private final JPanel lfoEffects;
	private JToggleButton lfoActive;
	private Knob lfoAmp;
	private JSlider lfoFreq;
	
	private JPanel eqEffects;
	private JToggleButton eqActive;
	private Knob eqRes;
	private JSlider eqFreq;
	private JComboBox<String> eqType;
	
	public EffectsGui() {
		instance = this;
		setBorder(Gui.GRAY1);
		// TODO mute/record checkboxes panel

		volume = new Knob(val -> {sendVolume(val);});
		eqEffects = eqPanel();
		compEffects = compressionPanel();
		revEffects = reverbPanel();
		lfoEffects = lfoPanel();

		header.setLayout(new GridLayout(2, 1));
		header.setAlignmentX(0.5f);
		name.setFont(Constants.Gui.BOLD);
		name.setAlignmentX(JLabel.CENTER_ALIGNMENT);
		header.add(name);
		header.add(volume);
		header.setBorder(new BevelBorder(BevelBorder.RAISED));
		
		JPanel btns = new JPanel();
		btns.setLayout(new BoxLayout(btns, BoxLayout.Y_AXIS));
		
		btns.add(header);
		btns.add(eqActive);
		btns.add(compActive);
		btns.add(revActive);
		btns.add(lfoActive);
		add(btns);
		
		JPanel effects = new JPanel();
		effects.setLayout(new BoxLayout(effects, BoxLayout.Y_AXIS));
		effects.add(eqEffects);
		effects.add(compEffects);
		effects.add(revEffects);
		effects.add(lfoEffects);
		add(effects);

		// add(JudahZone.getPlugins().getGui());
	}
	
	/** you probably want to use MixerPane.setFocus() */
	public void setFocus(MixerBus bus) {
		this.focus = bus;
		reverb = bus.getReverb();
		compression = bus.getCompression();
		lfo = bus.getLfo();
		eq = bus.getEq();
		revActive.setEnabled(false == bus instanceof Sample); // TODO reverb not working well on output
		update();
	}
	
	private JPanel knobPanel(String name, Knob knob) {
		JPanel pnl = new JPanel();
		JLabel lbl = new JLabel(name);
		pnl.add(knob);
		pnl.add(lbl);
		return pnl;
	}

	private JPanel lfoPanel() {
		lfoActive = new JToggleButton("LFO");
		lfoActive.addActionListener(listener -> {
			lfo.setActive(!lfo.isActive());
			update();
			Console.info(name.getText() + " LFO: " + (lfo.isActive() ? " On" : " Off"));
		});
		
		JPanel result = new JPanel();
		result.setBorder(Gui.GRAY1);
		lfoAmp = new Knob(val -> {lfo.setAmplitude(val);});
		lfoAmp.setValue(100);
		lfoFreq = new JSlider(200, 3200);
		lfoFreq.setValue(1000);
		lfoFreq.setMaximumSize(Gui.SLIDER_SZ);
		lfoFreq.setPreferredSize(Gui.SLIDER_SZ);
		
		lfoFreq.addChangeListener(e -> {lfo.setFrequency(lfoFreq.getValue());});
		
		result.add(new JLabel("freq"));
		result.add(lfoFreq);
		result.add(knobPanel("amp", lfoAmp));
		
		return result;
	}
	
	private JPanel reverbPanel() {
		revActive = new JToggleButton("Reverb");
		revActive.addActionListener(listener -> { 
			reverb.setActive(!reverb.isActive());
			update();
			Console.info(name.getText() + " Reverb: " + (reverb.isActive() ? " On" : " Off"));
		});
		
		JPanel result = new JPanel();
		result.setBorder(Gui.GRAY1);
		result.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				reverb.initialize(Constants._SAMPLERATE, Constants._BUFSIZE);
				update();
				Console.info("reverb init()");
		}});
		revRoom = new Knob(val -> {reverb.setRoomSize(val / 100f);});
		revDamp = new Knob(val -> {reverb.setDamp(val / 100f);});
		revWidth = new Knob(val -> {reverb.setWidth(val / 100f);});
		result.add(knobPanel("room", revRoom));
		result.add(knobPanel("damp", revDamp));
		result.add(knobPanel("width", revWidth));
		return result;
	}
	
	private JPanel compressionPanel() {
		
		compActive = new JToggleButton("Comp.");
		compActive.addActionListener(listener -> { 
			compression.setActive(!compression.isActive());
			update();
			Console.info(name.getText() + " Compression: " + (compression.isActive() ? " On" : " Off"));
		});
		
		JPanel result = new JPanel();
		result.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				compression.setPreset(compression.getPreset());
				update();
				Console.info("compression init()");
		}});
		result.setLayout(new GridLayout(2, 4));
		result.setBorder(Gui.GRAY1);

		compRatio = new Knob(val -> {compression.setRatio(Math.round( (val + 20) / 10));});
		compRatio.setToolTipText("2 to 12");
		compThresh = new Knob(val -> {compression.setThreshold((int)((val - 100) / 2.5));});
		compThresh.setToolTipText("-40 to 0");
		compAtt = new Knob(val -> {compression.setAttack((int)Math.round(val * 1.5));});
		compAtt.setToolTipText("0 to 150 milliseconds");
		compRel = new Knob(val -> {compression.setRelease(Math.round(val * 3));});
		compRel.setToolTipText("0 to 300 milliseconds");

		result.add(compRatio);
		result.add(new JLabel("ratio"));
		result.add(compThresh);
		result.add(new JLabel("thresh"));
		result.add(compAtt);
		result.add(new JLabel("attack"));
		result.add(compRel);
		result.add(new JLabel("release"));
		return result;
	}
	
	private JPanel eqPanel() {
		eqActive = new JToggleButton("EQ");
		eqActive.addActionListener(listener -> { 
			eq.setActive(!eq.isActive());
			update();
			Console.info(name.getText() + " EQ: " + (eq.isActive() ? " On" : " Off"));
		});

		JPanel result = new JPanel();
		result.setBorder(Gui.GRAY1);
		result.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
			@Override public void mouseReleased(MouseEvent e) {
				if (!SwingUtilities.isRightMouseButton(e)) return;
				// reverb.initialize(Constants._SAMPLERATE, Constants._BUFSIZE); update();
				Console.info("eq init(no-op)");
		}});
		
		DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>(EQs);
		eqType = new JComboBox<String>(model);
		eqType.setMaximumSize(new Dimension(65, 40));
		eqType.addActionListener(e -> {eqFilterType();});
		eqFreq = new JSlider(0, 100);
		eqFreq.setMaximumSize(Gui.SLIDER_SZ);
		eqFreq.setPreferredSize(Gui.SLIDER_SZ);
		eqFreq.addChangeListener(e -> {
			eq.setFrequency(EQ.knobToFrequency(eqFreq.getValue()));
			RTLogger.log(this, "EQ frequency: " + eq.getFrequency());
		});
		eqRes = new Knob(val -> {focus.getEq().setResonance(val * 0.25f);});
		result.add(eqType);
		result.add(new JLabel("hz."));
		result.add(eqFreq);
		result.add(eqRes);
		result.add(new JLabel("res"));
		return result;
	}

	private void eqFilterType() {
		switch(eqType.getSelectedItem().toString()) {
			// case EQ_PARTY: 
			case EQ_HICUT: eq.setFilterType(Type.LP12); break;
			case EQ_BAND: eq.setFilterType(Type.BP12); break;
			case EQ_NOTCH: eq.setFilterType(Type.NP12); break;
			case EQ_LOCUT: eq.setFilterType(Type.HP12); break;
		}
	}

	public void update() {
		name.setText(focus.getName());
		revActive.setSelected(reverb.isActive());
		revRoom.setValue(Math.round(reverb.getRoomSize() * 100)); 
		revDamp.setValue(Math.round(reverb.getDamp() * 100)); 
		revWidth.setValue(Math.round(reverb.getWidth() * 100));

		compActive.setSelected(compression.isActive());
		compRatio.setValue(compression.getRatio() * 10);
		compThresh.setValue((int) ((compression.getThreshold() + 40) * 2.5));
		int attack = (int)Math.round(compression.getAttack() / 0.75);
		if (attack > 100) attack = 100;
		compAtt.setValue(attack);
		int release = (int)Math.round(compression.getRelease() * 0.333);
		if (release > 100) release = 100;
		compRel.setValue(release);
		focus.getGui().update();	
		
		lfoActive.setSelected(lfo.isActive());
		if (lfo.isActive()) {
			lfoAmp.setValue((int)lfo.getAmplitude());
			lfoFreq.setValue((int)lfo.getFrequency());
		}
		
		eqActive.setSelected(eq.isActive());
		eqType.setSelectedItem(eq.getFilterType().name());
		eqFreq.setValue( EQ.frequencyToKnob(eq.getFrequency()));
		eqRes.setValue((int)(eq.getResonance() * 4)); 
		
	}
	
	
	public void sendVolume(int volume) {
		focus.setVolume(volume);
	}
	
	public static void volume(MixerBus o) {
		if (o == instance.focus)
			instance.volume.setValue(o.getVolume());
	}

	public static void compression(MixerBus o) {
		if (o == instance.focus)
			instance.compActive.setSelected(o.getCompression().isActive());
	}
	
	public static void reverb(MixerBus o) {
		if (o == instance.focus)
			instance.revActive.setSelected(o.getReverb().isActive());
	}
	
}


//private JPanel eq3Effects;
//private JToggleButton eq3Active;
//private Knob eqLow, eqMid, eqHigh;
//private JPanel eq3Panel() {
//eq3Active = new JToggleButton("EQ3");
//eq3Active.addActionListener(listener -> {focus.getEq3().setActive(!focus.getEq3().isActive());
//	update(); 
//	Console.info("3-band EQ: " + (focus.getEq3().isActive() ? " On" : "Off"));});
//// eq3Active.addKeyListener(MainFrame.get().getMenu());
//
//JPanel result = new JPanel();
//result.setBorder(Gui.GRAY1);
////TODO result.addMouseListener(new MouseAdapter() { // right click re-initialize EQ3
////	@Override public void mouseReleased(MouseEvent e) {
////		if (!SwingUtilities.isRightMouseButton(e)) return; }});
//eqLow = new Knob(val -> {focus.getEq3().setGain(EQ3.LOW, val/100f);});
//eqMid = new Knob(val -> {focus.getEq3().setGain(EQ3.MID, val/100f);});
//eqHigh = new Knob(val -> {focus.getEq3().setGain(EQ3.HIGH, val/100f);});
//result.add(knobPanel("low", eqLow));
//result.add(knobPanel("mid", eqMid));
//result.add(knobPanel("high", eqHigh));
//return result;
//}
// update:
//	EQ3 eq3 = focus.getEq3();
//	eq3Active.setSelected(eq3.isActive());
//	eqLow.setValue((int) (100 * eq3.getGain(EQ3.LOW)));
//	eqMid.setValue((int) (100 * eq3.getGain(EQ3.MID)));
//	eqHigh.setValue((int) (100 * eq3.getGain(EQ3.HIGH)));


