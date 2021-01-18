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
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import lombok.Getter;
import net.judah.mixer.bus.Compression;
import net.judah.mixer.bus.CutFilter;
import net.judah.mixer.bus.CutFilter.Type;
import net.judah.mixer.bus.EQ.EqBand;
import net.judah.mixer.bus.EQ.EqParam;
import net.judah.mixer.bus.LFO;
import net.judah.mixer.bus.LFO.Target;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Constants.Gui;
import net.judah.util.Knob;
import net.judah.util.MenuBar;
import net.judah.util.Slider;

public class SoloTrack extends JPanel {
    // TODO tap tempo, lfo recover // play/stop/rec button icons
    final static Dimension LBL_SZ = new Dimension(62, 15);

    public static final String EQ_PARTY = "pArTy";
    public static final String EQ_LOCUT = "LoCut";
    public static final String EQ_HICUT = "HiCut";
    public static final String[] EQs = new String[]
            { EQ_LOCUT, EQ_PARTY, EQ_HICUT };

    @Getter private static SoloTrack instance;
    @Getter private Channel focus;

    private final JPanel headerRow,
    eqRow, compRow, reverbRow, delayRow, lfoRow;

    @Getter private CutFilter cutFilter;
    @Getter private LFO lfo;

    private final JLabel name = new JLabel();
    private Slider volume;
    private Knob pan;

    // TODO overdrive, pan
    private Knob overdrive;

    private JToggleButton eqActive;
    private Knob eqBass, eqMid, eqTreble;

    private JToggleButton compActive;
    private Knob compAtt, compRel, compThresh;

    private JToggleButton revActive;
    private Knob revRoom, revDamp, revWidth;

    private JToggleButton delActive;
    private Slider delFeedback, delTime;

    private JToggleButton lfoActive, cutActive;
    private Knob lfoMin, lfoMax, cutRes;
    private Slider lfoFreq, cutFreq;
    private JComboBox<String> lfoTarget, cutType;

    public SoloTrack() {
        instance = this;
        setBorder(new BevelBorder(BevelBorder.RAISED));

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
        lfo = bus.getLfo();
        cutFilter = bus.getCutFilter();
        compActive.setEnabled(focus instanceof MasterTrack == false);
        update();
    }

    public static void volume(Channel o) {
        if (o == instance.focus) {
            instance.volume.setValue(o.getVolume());
        }

    }

    public void update() {

        name.setText(focus.getName());
        volume.setValue(focus.getVolume());
        overdrive.setValue((int)Math.round(focus.getOverdrive().getDrive() * 100));
        revActive.setSelected(focus.getReverb().isActive());
        revRoom.setValue(Math.round(focus.getReverb().getRoomSize() * 100f));
        revDamp.setValue(Math.round(focus.getReverb().getDamp() * 100f));
        revWidth.setValue(Math.round(focus.getReverb().getWidth() * 100f));

        Compression compression = focus.getCompression();
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
            lfoMax.setValue((int)lfo.getMax());
            lfoMin.setValue((int)lfo.getMin());
            lfoFreq.setValue((int)lfo.getFrequency());
        }

        cutActive.setSelected(cutFilter.isActive());
        cutType.setSelectedItem(cutFilter.getFilterType().name());
        cutFreq.setValue( CutFilter.frequencyToKnob(cutFilter.getFrequency()));
        cutRes.setValue((int)(cutFilter.getResonance() * 4));

        delFeedback.setValue(Math.round(focus.getDelay().getFeedback() * 100));
        delTime.setValue(delTime());
        delActive.setSelected(focus.getDelay().isActive());

        eqUpdate();
        pan.setValue(Math.round( focus.getPan() * 100));

    }

    // Header ////////////////////////////////////////////////////////////////////////////
    private JPanel headerRow() {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        result.setLayout(new FlowLayout());
        volume = new Slider(0, 100);
        volume.addChangeListener(l -> { focus.setVolume(volume.getValue()); });
        volume.setToolTipText("Volume");
        result.add(volume);
        name.setFont(Constants.Gui.BOLD);
        result.add(name);
        pan = new Knob(val -> {focus.setPan(val / 100f);});
        pan.setToolTipText("pan left/right");

        overdrive = new Knob(val -> {
            focus.getOverdrive().setDrive(val / 100f);
            focus.getOverdrive().setActive(val > 10);});
        overdrive.setToolTipText("Overdrive");


        result.add(pan);
        JLabel lbl = new JLabel("pan  drive");
        lbl.setFont(Constants.Gui.FONT11);
        result.add(lbl);
        result.add(overdrive);
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
        result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
        eqActive = new JToggleButton("   EQ   ");
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
        result.add(labelPanel("bass", eqBass));
        result.add(labelPanel("middle", eqMid));
        result.add(labelPanel("treble", eqTreble));
        return result;
    }
    private void eqGain(EqBand eqBand, int val) {
        boolean negative = val < 50;
        float result = Math.abs(50 - val) / 2;
        if (negative) result *= -1;
        focus.getEq().update(eqBand, EqParam.GAIN, result);
        Console.info("eq gain: " + result);
    }
    private void eqUpdate() {
        eqBass.setValue(Math.round( (focus.getEq().getGain(EqBand.BASS) * 2 + 50)));
        eqMid.setValue(Math.round( (focus.getEq().getGain(EqBand.MID) * 2 + 50)));
        eqTreble.setValue(Math.round( (focus.getEq().getGain(EqBand.TREBLE) * 2 + 50)));
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
                focus.getCompression().setPreset(focus.getCompression().getPreset());
                update();
                Console.info("compression init()");
            }});

        compActive = new JToggleButton("Comp.");
        compActive.addActionListener(listener -> {
            focus.getCompression().setActive(!focus.getCompression().isActive());
            update();
            Console.info(name.getText() + " Compression: " +
            (focus.getCompression().isActive() ? " On" : " Off"));
        });
        compActive.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        compThresh = new Knob(val -> {
                focus.getCompression().setThreshold((int)((val - 100) / 2.5));});
        compThresh.setToolTipText("-40 to 0");
        compAtt = new Knob(val -> {
                focus.getCompression().setAttack((int)Math.round(val * 1.5));});
        compAtt.setToolTipText("0 to 150 milliseconds");
        compRel = new Knob(val -> {focus.getCompression().setRelease(Math.round(val * 3));});
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
                focus.getReverb().initialize(Constants._SAMPLERATE, Constants._BUFSIZE);
                update();
                Console.info("reverb init()");
            }});
        result.setLayout(new BoxLayout(result, BoxLayout.X_AXIS));
        revActive = new JToggleButton("Reverb");
        revActive.addActionListener(listener -> {
            focus.getReverb().setActive(!focus.getReverb().isActive());
            update();
            Console.info(name.getText() + " " + focus.getReverb().getClass().getSimpleName() +
                    " Reverb: " + (focus.getReverb().isActive() ? " On" : " Off"));
        });
        revRoom = new Knob(val -> {focus.getReverb().setRoomSize(val / 100f);});
        revDamp = new Knob(val -> {focus.getReverb().setDamp(val / 100f);});
        revWidth = new Knob(val -> {focus.getReverb().setWidth(val / 100f);});
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

        delActive = new JToggleButton(" Delay ");
        delActive.addActionListener(listener -> {
            focus.getDelay().setActive(!focus.getDelay().isActive());
            Console.info(name.getText() + " Delay : " +
                    (focus.getDelay().isActive() ? " On" : " Off"));
            // if (focus.getDelay().isActive()) // focus.getDelay().reset();
        });
        // delActive.setAlignmentX(JLabel.LEFT_ALIGNMENT);
        delFeedback = new Slider(0, 100);
        delFeedback.setMaximumSize(Gui.SLIDER_SZ);
        delFeedback.setPreferredSize(Gui.SLIDER_SZ);
        delFeedback.addChangeListener(e -> {delFeedback(delFeedback.getValue());});

        delTime = new Slider(0, 100);
        delTime.setMaximumSize(Gui.SLIDER_SZ);
        delTime.setPreferredSize(Gui.SLIDER_SZ);
        delTime.addChangeListener(e -> {delTime(delTime.getValue());});

        result.add(delActive);
        result.add(labelPanel("feedback", delFeedback));
        result.add(labelPanel("time", delTime));
        return result;
    }
    private int delTime() {
        float max = focus.getDelay().getMaxDelay();
        // result / 100 = delay / max
        return Math.round(100 * focus.getDelay().getDelay() / max);
        // return Math.round((focus.getDelay().getDelay() * max) * 100f);
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
        lfoActive = new JToggleButton("  LFO   ");
        lfoActive.addActionListener(listener -> {lfo();});
        lfoMax = new Knob(val -> {lfo.setMax(val);});
        lfoMax.setValue(85);
        lfoMin = new Knob(val -> {lfo.setMin(val);});
        lfoMin.setValue(0);

        lfoFreq = new Slider(120, 2120);
        lfoFreq.setValue(1000);
        lfoFreq.setMaximumSize(Gui.SLIDER_SZ);
        lfoFreq.setPreferredSize(Gui.SLIDER_SZ);
        lfoFreq.addChangeListener(e -> {lfo.setFrequency(lfoFreq.getValue());});
        DefaultComboBoxModel<String> lfoModel = new DefaultComboBoxModel<>();
        for (Target t : LFO.Target.values())
            lfoModel.addElement(t.name());
        lfoTarget = new JComboBox<>(lfoModel);
        lfoTarget.addActionListener(e -> { lfo.setTarget(Target.valueOf(lfoTarget.getSelectedItem().toString()));});

        lp.setLayout(new BoxLayout(lp,BoxLayout.Y_AXIS));
        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        row1.add(lfoActive);
        row1.add(lfoFreq);
        row1.add(new JLabel("time"));
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        row2.add(lfoTarget);

        row2.add(lfoMin);
        row2.add(new JLabel("min/max"));
        row2.add(lfoMax);
        // row2.add(labelPanel("max", lfoMax));
        lp.add(row1);
        lp.add(row2);

        cutActive = new JToggleButton("CutEQ");
        cutActive.addActionListener(listener -> {
            cutFilter.setActive(!cutFilter.isActive());
            update();
            Console.info(name.getText() + " CUT: " + (cutFilter.isActive() ? " On" : " Off"));
        });
        DefaultComboBoxModel<String> cutModel = new DefaultComboBoxModel<>(EQs);
        cutType = new JComboBox<>(cutModel);
        //cutType.setMaximumSize(new Dimension(65, 40));
        cutType.addActionListener(e -> {eqFilterType();});
        cutFreq = new Slider(0, 100);
        cutFreq.addKeyListener(MenuBar.getInstance());
        cutFreq.setMaximumSize(Gui.SLIDER_SZ);
        cutFreq.setPreferredSize(Gui.SLIDER_SZ);
        cutFreq.addChangeListener(e -> {
            cutFilter.setFrequency(CutFilter.knobToFrequency(cutFreq.getValue()));
            // RTLogger.log(this, "EQ frequency: " + cutFilter.getFrequency());
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
        row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        row1.add(cutActive);
        row1.add(cutFreq);
        row1.add(new JLabel("hz."));
        row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
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
        if (lfo.getTarget() == Target.CutEQ)
            focus.getCutFilter().setActive(!lfo.isActive());
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
        pnl.add(c);
        JLabel lbl = new JLabel(name);
        lbl.setPreferredSize(LBL_SZ);
        lbl.setMaximumSize(LBL_SZ);
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Console.info(lbl.getWidth() + " x " + lbl.getHeight());
            }
        });
        pnl.add(lbl);
        return pnl;
    }

}

