package net.judah.effects.gui;

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
import net.judah.JudahZone;
import net.judah.effects.Chorus;
import net.judah.effects.Compression;
import net.judah.effects.CutFilter;
import net.judah.effects.CutFilter.Type;
import net.judah.effects.EQ;
import net.judah.effects.EQ.EqBand;
import net.judah.effects.EQ.EqParam;
import net.judah.effects.LFO;
import net.judah.effects.LFO.Target;
import net.judah.mixer.Channel;
import net.judah.mixer.MasterTrack;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Constants.Gui;
import net.judah.util.Knob;
import net.judah.util.MenuBar;
import net.judah.util.TapTempo;

public class EffectsRack extends JPanel {
    // TODO lfo recover
    final static Dimension TAP_SZ = new Dimension(65, 22);
    final static Dimension TARGET_SZ = new Dimension(80, 25);
    final static Dimension MINI_LBL = new Dimension(40, 15);
    final static Dimension MINI = new Dimension(Gui.SLIDER_SZ.width - 5, Gui.SLIDER_SZ.height);
    final static Dimension SPACER = new Dimension(2, 1);

    public static final String EQ_PARTY = "pArTy";
    public static final String EQ_LOCUT = "LoCut";
    public static final String EQ_HICUT = "HiCut";
    public static final String[] EQs = new String[]
            { EQ_LOCUT, EQ_PARTY, EQ_HICUT };

    @Getter private static EffectsRack instance;
    @Getter private Channel focus;

    private final JLabel name = new JLabel();
    private Slider volume, pan;
    private Knob overdrive;

    private EffectsButton choActive;
    private Slider choDepth, choRate, choFeedback;

    @Getter private CutFilter cutFilter;
    @Getter private LFO lfo;

    private JToggleButton eqActive;
    private Slider eqBass, eqMid, eqTreble;

    private JToggleButton compActive;
    private Slider compAtt, compRel, compThresh;

    private JToggleButton revActive;
    private Slider revRoom, revDamp; //, revWidth;
    private Slider revWet;

    private JToggleButton delActive;
    private Slider delFeedback, delTime;

    private JToggleButton lfoActive, cutActive;
    private Knob lfoMin, lfoMax;
    private Slider lfoFreq, cutFreq, cutRes;
    private JComboBox<String> lfoTarget, cutType;

    public EffectsRack() {
        instance = this;
        setBorder(new BevelBorder(BevelBorder.RAISED));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        add(headerRow());
        add(eqRow());
        add(compressionRow());
        add(chorusRow());
        add(reverbRow());
        add(delayRow());
        add(lfoCutRow());

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

    private boolean inUpdate;
    public void update() {
        if (!JudahZone.isInitialized() || inUpdate) return;
        inUpdate = true;
        name.setText(focus.getName());
        volume.setValue(focus.getVolume());
        overdrive.setValue((int)Math.round(focus.getOverdrive().getDrive() * 100));
        revActive.setSelected(focus.getReverb().isActive());
        revRoom.setValue(Math.round(focus.getReverb().getRoomSize() * 100f));
        revDamp.setValue(Math.round(focus.getReverb().getDamp() * 100f));
        revWet.setValue(Math.round(focus.getReverb().getWidth() * 100f));

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
        chorusUpdate();
        pan.setValue(Math.round( focus.getPan() * 100));
        inUpdate = false;

    }

    // Header ////////////////////////////////////////////////////////////////////////////
    private JPanel headerRow() {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        result.setLayout(new FlowLayout());
        volume = new Slider(0, 100, 50, e -> {focus.setVolume(volume.getValue());}, "Volume");
        result.add(volume);
        name.setFont(Gui.BOLD);
        result.add(name);
        pan = new Slider(l -> {focus.setPan(pan.getValue() / 100f);});
        pan.setToolTipText("pan left/right");
        pan.setPreferredSize(MINI);
        pan.setMaximumSize(MINI);

        overdrive = new Knob(val -> {
            focus.getOverdrive().setDrive(val / 100f);
            focus.getOverdrive().setActive(val > 10);});
        overdrive.setToolTipText("Overdrive");


        JLabel panLbl = new JLabel("pan");
        panLbl.setFont(Gui.FONT11);
        result.add(pan);
        result.add(panLbl);

        JLabel driveLbl = new JLabel("<html>over<br/>drive</html>");
        driveLbl.setFont(Gui.FONT11);

        result.add(overdrive);
        result.add(driveLbl);
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


        eqBass = new Slider(e -> {eqGain(EqBand.BASS, eqBass.getValue());});
        eqBass.setPreferredSize(MINI);
        eqBass.setMaximumSize(MINI);
        eqBass.setMinimumSize(MINI);
        eqMid = new Slider(e -> {eqGain(EqBand.MID, eqMid.getValue());});
        eqMid.setPreferredSize(MINI);
        eqMid.setMinimumSize(MINI);
        eqMid.setMaximumSize(MINI);
        eqTreble = new Slider(e -> {eqGain(EqBand.TREBLE, eqTreble.getValue());});
        eqTreble.setPreferredSize(MINI);
        eqTreble.setMaximumSize(MINI);
        eqTreble.setMinimumSize(MINI);

        result.add(eqActive);
        result.add(Box.createHorizontalGlue());
        result.add(eqBass);
        result.add(new Label("bass"));
        result.add(eqMid);
        result.add(new Label("mid"));
        result.add(eqTreble);
        result.add(new Label("high"));
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
        compThresh = new Slider(l -> {
                focus.getCompression().setThreshold((int)((compThresh.getValue() - 100) / 2.5));});
        compThresh.setPreferredSize(MINI); compThresh.setMaximumSize(MINI);
        compThresh.setToolTipText("-40 to 0");
        compAtt = new Slider(l -> {
                focus.getCompression().setAttack((int)Math.round(compAtt.getValue() * 1.5));});
        compAtt.setPreferredSize(MINI); compAtt.setMaximumSize(MINI);
        compAtt.setToolTipText("0 to 150 milliseconds");
        compRel = new Slider(l -> {focus.getCompression().setRelease(Math.round(compRel.getValue() * 3));});
        compRel.setPreferredSize(MINI); compRel.setMaximumSize(MINI);
        compRel.setToolTipText("0 to 300 milliseconds");

        result.add(compActive);
        result.add(Box.createHorizontalGlue());
        result.add(compThresh);
        result.add(new Label("t/hold"));
        result.add(compAtt);
        result.add(new Label("attk."));
        result.add(compRel);
        result.add(new Label("rel."));
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
        revRoom = new Slider(l -> {focus.getReverb().setRoomSize(revRoom.getValue() / 100f);});
        revRoom.setPreferredSize(MINI); revRoom.setMaximumSize(MINI);
        revDamp = new Slider(l -> {focus.getReverb().setDamp(revDamp.getValue() / 100f);});
        revDamp.setPreferredSize(MINI); revDamp.setMaximumSize(MINI);
        revWet = new Slider(l -> {focus.getReverb().setWet(revWet.getValue() / 100f);});
        revWet.setPreferredSize(MINI); revWet.setMaximumSize(MINI);

        result.add(revActive);
        result.add(Box.createHorizontalGlue());

        result.add(revRoom);
        result.add(new Label("room"));
        result.add(revDamp);
        result.add(new Label("damp"));
        result.add(revWet);
        result.add(new Label("wet"));
        return result;
    }
    public static void reverb(Channel o) {
        if (o == instance.focus)
            instance.revActive.setSelected(o.getReverb().isActive());
    }

    // CHORUS /////////////////////////////////////////////////////////////////////////////
    private JPanel chorusRow() {
        JPanel result = new Row();
        result.addMouseListener(new MouseAdapter() { // right click reinitialize reverb
            @Override public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e)) return;
                update(); Console.info("delay init()");
            }});

        choActive = new EffectsButton("Chorus", Chorus.class);
        choRate = new Slider(e -> {focus.getChorus().setRate(choRate.getValue()/10f);});
        choRate.setPreferredSize(MINI); choRate.setMaximumSize(MINI);
        choFeedback = new Slider(e -> {focus.getChorus().setFeedback(choFeedback.getValue()/100f);});
        choFeedback.setPreferredSize(MINI); choFeedback.setMaximumSize(MINI);
        choDepth = new Slider(e -> {focus.getChorus().setDepth(choDepth.getValue()/100f);});
        choDepth.setPreferredSize(MINI); choDepth.setMaximumSize(MINI);

        result.add(choActive);
        result.add(Box.createHorizontalGlue());
        result.add(choRate);
        result.add(new Label("rate"));
        result.add(choDepth);
        result.add(new Label("depth"));
        result.add(choFeedback);
        result.add(new Label("f/b"));

        return result;
    }
    private void chorusUpdate() {
        choActive.update();
        choDepth.setValue(Math.round(focus.getChorus().getDepth() * 100));
        choRate.setValue(Math.round(focus.getChorus().getRate() * 10));
        choFeedback.setValue(Math.round(focus.getChorus().getFeedback() * 100));
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
        });
        delFeedback = new Slider(0, 100, e -> {delFeedback(delFeedback.getValue());});
        delFeedback.setMaximumSize(Gui.SLIDER_SZ);
        delFeedback.setPreferredSize(Gui.SLIDER_SZ);

        delTime = new Slider(0, 100, e -> {delTime(delTime.getValue());});
        delTime.setMaximumSize(Gui.SLIDER_SZ);
        delTime.setPreferredSize(Gui.SLIDER_SZ);


        TapTempo tapButton = new TapTempo(" time/sync ", msec -> {delayTapTempo(msec);});
        tapButton.setPreferredSize(TAP_SZ);
        tapButton.setMaximumSize(TAP_SZ);
        result.add(delActive);
        result.add(Box.createHorizontalGlue());
        result.add(delTime);
        result.add(tapButton);
        result.add(labelPanel("feedback", delFeedback));
        result.add(Box.createRigidArea(new Dimension(15, 1)));

        return result;
    }
    private void delayTapTempo(long msec) {
        if (msec > 0) {
            float old = focus.getDelay().getDelay();
            focus.getDelay().setDelay(msec / 1000f);
            Console.info("from " + old + " to " + focus.getDelay().getDelay() + " delay.");
            delTime.setValue(delTime());
        }
        else {
            Console.info("right clicked");
        }
    }
    private int delTime() {
        float max = focus.getDelay().getMaxDelay();
        // result / 100 = delay / max
        return Math.round(100 * focus.getDelay().getDelay() / max);
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

        lfoFreq = new Slider(90, 1990, 990, e -> {lfo.setFrequency(lfoFreq.getValue());}, "0.1 to 2 seconds");
        lfoFreq.setMaximumSize(Gui.SLIDER_SZ);
        lfoFreq.setPreferredSize(Gui.SLIDER_SZ);
        DefaultComboBoxModel<String> lfoModel = new DefaultComboBoxModel<>();
        for (Target t : LFO.Target.values())
            lfoModel.addElement(t.name());
        lfoTarget = new JComboBox<>(lfoModel);
        lfoTarget.addActionListener(e -> { lfo.setTarget(Target.valueOf(lfoTarget.getSelectedItem().toString()));});
        lfoTarget.setPreferredSize(TARGET_SZ);
        lfoTarget.setMaximumSize(TARGET_SZ);

        lp.setLayout(new BoxLayout(lp,BoxLayout.Y_AXIS));
        JPanel row1 = new JPanel();
        row1.setLayout(new BoxLayout(row1, BoxLayout.X_AXIS)); // new FlowLayout(FlowLayout.CENTER, 3, 0));
        row1.add(Box.createRigidArea(SPACER));
        row1.add(lfoActive);
        row1.add(Box.createHorizontalGlue());
        row1.add(lfoFreq);
        TapTempo time = new TapTempo(" time/sync ", msec -> {
            if (msec > 0) {
                focus.getLfo().setFrequency(msec);
                lfoFreq.setValue((int)Math.round(lfo.getFrequency()));
                Console.info("LFO Tap Tempo: " + lfo.getFrequency());
            }
        });
        time.setPreferredSize(TAP_SZ);
        time.setMaximumSize(TAP_SZ);
        row1.add(time);
        row1.add(Box.createRigidArea(SPACER));

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));

        row2.setLayout(new BoxLayout(row2, BoxLayout.X_AXIS)); // new FlowLayout(FlowLayout.CENTER, 3, 0));
        row2.add(Box.createRigidArea(SPACER));
        row2.add(lfoTarget);
        row2.add(Box.createHorizontalGlue());
        JLabel min = new JLabel("min");
        min.setFont(Gui.FONT11);
        row2.add(min);
        row2.add(lfoMin);

        JLabel max = new JLabel("max");
        max.setFont(Gui.FONT11);
        row2.add(max);
        row2.add(lfoMax);
        row2.add(Box.createRigidArea(SPACER));

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
        cutType.addActionListener(e -> {eqFilterType();});
        cutFreq = new Slider(0, 100, e -> {
            cutFilter.setFrequency(CutFilter.knobToFrequency(cutFreq.getValue()));
        });
        cutFreq.addKeyListener(MenuBar.getInstance());
        cutFreq.setMaximumSize(Gui.SLIDER_SZ);
        cutFreq.setPreferredSize(Gui.SLIDER_SZ);
        cutRes = new Slider(0, 100, 50,
                l -> {cutFilter.setResonance(cutRes.getValue() * 0.25f);}, "Resonance Db");
        cutRes.setPreferredSize(Gui.SLIDER_SZ);

        JPanel cp = new JPanel();
        cp.setBorder(Gui.GRAY1);
        cp.addMouseListener(new MouseAdapter() { // right click re-initialize reverb
            @Override public void mouseReleased(MouseEvent e) {
                if (!SwingUtilities.isRightMouseButton(e)) return;
                EQ eq = focus.getEq();
                eq.update(EqBand.BASS, EqParam.GAIN, 0);
                eq.update(EqBand.MID, EqParam.GAIN, 0);
                eq.update(EqBand.TREBLE, EqParam.GAIN, 0);
                Console.info("eq init()");
            }});
        cp.setLayout(new BoxLayout(cp,BoxLayout.Y_AXIS));
        row1 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        row1.add(cutActive);
        row1.add(cutFreq);
        JLabel hz = new JLabel("hz.");
        hz.setFont(Gui.FONT11);
        row1.add(hz);
        row2 = new JPanel(new FlowLayout(FlowLayout.CENTER, 3, 0));
        row2.add(cutType);
        row2.add(cutRes);
        JLabel res = new JLabel("res");
        res.setFont(Gui.FONT11);
        row2.add(res);

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
            add(Box.createRigidArea(SPACER));
            setBorder(Gui.GRAY1);
        }
    }

    private class Label extends JLabel {
        Label(String s) {
            super(" " + s + " ");
            setPreferredSize(MINI_LBL);
            setMaximumSize(MINI_LBL);
            setFont(Gui.FONT11);
        }
    }

    private JPanel labelPanel(String name, Component c) {
        JPanel pnl = new JPanel();
        pnl.add(c);
        JLabel lbl = new JLabel(name);
        lbl.setFont(Gui.FONT11);
        lbl.setPreferredSize(TAP_SZ);
        lbl.setMaximumSize(TAP_SZ);
        lbl.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                Console.info(lbl.getWidth() + " x " + lbl.getHeight());
            }
        });
        pnl.add(lbl);
        return pnl;
    }

    @Override
    public String toString() {
        return "Coming Soon: some fancy serialization..." + Constants.NL +
                focus.getReverb();
    }

}

