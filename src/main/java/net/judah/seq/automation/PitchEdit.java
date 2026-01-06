package net.judah.seq.automation;

import java.awt.FlowLayout;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.ShortMessage;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;

import judahzone.api.Midi;
import judahzone.api.MidiConstants;
import judahzone.gui.Gui;
import judahzone.util.Constants;
import judahzone.util.RTLogger;
import judahzone.widgets.Btn;
import net.judah.gui.Size;
import net.judah.seq.Edit;
import net.judah.seq.Edit.Type;
import net.judah.seq.Prototype;
import net.judah.seq.automation.Automation.AutoBox;
import net.judah.seq.track.MidiTrack;

class PitchEdit extends AutoBox {

    private MidiEvent existing;
    /** Captures the "New" edit including any generated automation points.
     *  Used to delete the whole curve later. */
    private Edit undoCurve;

    private final Btn create = new Btn("New", e -> create());
    private final Btn change = new Btn("Change", e -> change());
    private final Btn delete = new Btn("Delete", e -> delete());
    private final Btn exe    = new Btn("Exe", e -> exe());

    private final Box top  = new Box(BoxLayout.PAGE_AXIS);
    private final Box algo = new Box(BoxLayout.PAGE_AXIS);

    private final JPanel checkbox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
    private final Pitch pitch  = new Pitch(false); // start
    private final Pitch pitch2 = new Pitch(true);  // end
    private final Tick  steps  = new Tick();
    private final Tick  steps2 = new Tick();
    private final JCheckBox automation = new JCheckBox("Enabled");
    private final JTextField messages  = new JTextField("5", 3);

    protected PitchEdit() {
        super(BoxLayout.LINE_AXIS);

        Box btns = new Box(BoxLayout.LINE_AXIS);
        btns.add(Box.createHorizontalStrut(15));
        btns.add(new JLabel("Pitch Bend"));
        btns.add(Box.createHorizontalStrut(25));
        btns.add(create);
        btns.add(change);
        btns.add(delete);
        btns.add(exe);

        top.add(btns);
        top.add(pitch);
        top.add(steps);

        JLabel auto = new JLabel("Automation");
        auto.setFont(Gui.BOLD12);
        checkbox.add(auto);
        checkbox.add(automation);
        automation.addChangeListener(l -> enableAutomation(automation.isSelected()));

        checkbox.add(new JLabel(" "));
        checkbox.add(Gui.resize(messages, Size.COMBO_SIZE));
        checkbox.add(new JLabel(" Steps"));

        algo.add(checkbox);
        algo.add(pitch2);
        algo.add(steps2);
        algo.setBorder(Gui.SUBTLE);

        Box inner = new Box(BoxLayout.PAGE_AXIS);
        inner.add(Gui.wrap(top));
        inner.add(Gui.wrap(algo));

        add(Box.createHorizontalStrut(12));
        add(inner);
        add(Box.createHorizontalStrut(12));

        enableAutomation(false);
        delete.setEnabled(false);
        change.setEnabled(false);
    }

    private void enableAutomation(boolean enable) {
        steps2.setEnabled(enable);
        pitch2.setEnabled(enable);
        messages.setEnabled(enable);
        checkbox.setBackground(enable ? ENABLED : DISABLED);
        algo.setBackground(enable ? ENABLED : DISABLED);
    }

    /**
     * Populate from an existing pitch bend event.
     */
    public PitchEdit edit(MidiEvent e) {
        existing = e;
        undoCurve = null; // we only know about the single point here

        if (e != null && e.getMessage() instanceof ShortMessage msg && Midi.isPitchBend(msg)) {
            int uiVal = extractUiFromMessage(msg);
            pitch.set(uiVal);
            steps.quantizable(e.getTick());
        } else {
            pitch.set(MidiConstants.CUTOFF + 1);
            steps.quantizable(0L);
        }

        // For now, don't auto-enable automation when editing a single point
        enableAutomation(false);
        delete.setEnabled(true);
        change.setEnabled(true);
        return this;
    }

    @Override
    protected void setTrack(MidiTrack t) {
        track = t;
        steps.setTrack(t);
        steps2.setTrack(t);
    }

    @Override
    public PitchEdit init(long tick) {
        existing = null;
        undoCurve = null;

        // Default to center bend
        pitch.set(MidiConstants.CUTOFF + 1);
        steps.quantizable(tick);

        // For "new" we default to automation enabled so user can draw a ramp.
        enableAutomation(true);
        delete.setEnabled(false);
        change.setEnabled(false);
        return this;
    }

    @Override protected void pad1() { create(); }

    @Override protected void pad2() { delete(); }

    @Override
    protected boolean doKnob(int idx, int value) {
        switch (idx) {
            case 0: // start pitch
                pitch.knob(value);
                break;
            case 1: // start tick: frame
                steps.frameKnob(value);
                break;
            case 2: // start tick: beat
                steps.beatKnob(value);
                break;
            case 3: // start tick: step
                steps.stepKnob(value);
                break;
            case 4: // message count
                messages.setText("" + Constants.ratio(value, 33));
                automation.setSelected(true);
                break;
            case 5: // end pitch
                pitch2.knob(value);
                automation.setSelected(true);
                break;
            case 6: // end tick: beat
                steps2.beatKnob(value);
                automation.setSelected(true);
                break;
            case 7: // end tick: step
                steps2.stepKnob(value);
                automation.setSelected(true);
                break;
            default:
                return false;
        }
        return true;
    }

    // ---- core actions ----

    void create() {
        try {
            ShortMessage msg = build(pitch.get());
            long startTick = steps.getTick();
            MidiEvent evt = new MidiEvent(msg, startTick);
            existing = evt;

            Edit edit = new Edit(Type.NEW, evt);
            undoCurve = edit; // capture for possible curve delete

            if (automation.isSelected()) {
                int count;
                try {
                    count = Integer.parseInt(messages.getText());
                } catch (NumberFormatException nfe) {
                    RTLogger.warn(this, messages.getText() + ": " + nfe.getMessage());
                    return;
                }
                if (count < 2) count = 2;

                long endTick = steps2.getTick();
                double interval = (endTick - startTick) / (double) (count - 1);
                float startVal = pitch.get();
                float endVal = pitch2.get();
                float delta = (endVal - startVal) / (count - 1);

                for (int stepIdx = 1; stepIdx < count; stepIdx++) {
                    int uiVal = Math.round(startVal + stepIdx * delta);
                    long ticker = startTick + Math.round(stepIdx * interval);
                    ShortMessage unit = build(uiVal);
                    edit.getNotes().add(new MidiEvent(unit, ticker));
                }
            }

            track.getEditor().push(edit);
            delete.setEnabled(true);
            change.setEnabled(true);
        } catch (Throwable t) {
            RTLogger.warn(this, t);
        }
    }

    void change() {
        if (existing == null) {
            create();
            return;
        }
        try {
            long targetTick = steps.getTick();
            ShortMessage target = build(pitch.get());
            Edit mod = new Edit(Type.MOD, existing, new MidiEvent(target, targetTick));

            if (automation.isSelected()) {
                // "Prototype" here could convey intended end state
                mod.setDestination(new Prototype(pitch2.get(), steps2.getTick()));
                // You could also inspect undoCurve if you want to rebuild the whole ramp.
            }

            track.getEditor().push(mod);
        } catch (InvalidMidiDataException ie) {
            RTLogger.warn(this, ie);
        }
    }

    void delete() {
        if (undoCurve != null && !undoCurve.getNotes().isEmpty()) {
            // Delete the entire curve (all generated points)
            Edit del = new Edit(Type.DEL, undoCurve.getNotes());
            track.getEditor().push(del);
            undoCurve = null;
            existing = null;
            delete.setEnabled(false);
            change.setEnabled(false);
            return;
        }
        if (existing == null)
            return;

        Edit edit = new Edit(Type.DEL, existing, null);
        track.getEditor().push(edit);
        existing = null;
        delete.setEnabled(false);
        change.setEnabled(false);
    }

    void exe() {
        try {
            track.send(build(pitch.get()), 0);
        } catch (InvalidMidiDataException e) {
            RTLogger.warn(this, e);
        }
    }

    // ---- pitch bend mapping helpers ----

    private ShortMessage build(int uiVal) throws InvalidMidiDataException {
        return buildPitchMessage(track.getCh(), uiVal);
    }

    private static ShortMessage buildPitchMessage(int channel, int uiVal) throws InvalidMidiDataException {
        int bend = uiToBend(uiVal);
        int lsb = bend & 0x7F;
        int msb = (bend >> 7) & 0x7F;
        return new ShortMessage(Midi.PITCH_BEND, channel, lsb, msb);
    }

    private static int extractUiFromMessage(ShortMessage msg) {
        if (!Midi.isPitchBend(msg))
            return MidiConstants.CUTOFF + 1;
        int bend = msg.getData1() | (msg.getData2() << 7);
        return bendToUi(bend);
    }

    private static int uiToBend(int val) {
        // slider 0..127, 64 center
        int offset = val - 64;          // [-64..63]
        int maxOffset = 64;
        float norm = offset / (float) maxOffset; // ~[-1..+1)
        int bend = (int) (norm * 8191f);
        return 8192 + bend;
    }

    private static int bendToUi(int bend) {
        int center = 8192;
        int delta  = bend - center;
        float norm = delta / 8191f;
        float offset = norm * 64f;
        int ui = Math.round(64f + offset);
        if (ui < 0) ui = 0;
        if (ui > 127) ui = 127;
        return ui;
    }

    // ---- inner UI component ----

    protected class Pitch extends JPanel {

        private JLabel display = new JLabel("  0");
        private final JSlider slider = new JSlider(0, 127);

        Pitch(boolean automation) {
            super(new FlowLayout(FlowLayout.CENTER, 3, 3));
            Gui.resize(slider, WIDE);
            add(new JLabel(automation ? "End Pitch: " : "Pitch: "));
            add(slider);
            add(display);
            setEnabled(true);
            slider.setMajorTickSpacing(32);
            slider.setMinorTickSpacing(16);
            slider.setPaintTicks(true);
            slider.addChangeListener(l -> text(slider.getValue()));
            slider.setValue(MidiConstants.CUTOFF + 1);
        }

        public void knob(int value) {
            set(Constants.ratio(value, 127));
        }

        @Override
        public void setEnabled(boolean enabled) {
            super.setEnabled(enabled);
            setBackground(enabled ? ENABLED : DISABLED);
        }

        void set(int val) {
            slider.setValue(val);
            text(val);
        }

        int get() {
            return slider.getValue();
        }

        public void text(int val) {
            // Show deviation from center in "semitones approx." if you like
            int offset = val - 64;
            String out;
            if (offset == 0) {
                out = "  0";
            } else if (offset > 0) {
                out = " +" + offset;
            } else {
                out = " " + offset;
            }
            display.setText(out);
        }
    }
}