package net.judah.effects.gui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.mixer.Channel;
import net.judah.util.Constants.Gui;
import net.judah.util.Knob;

public class EffectsRack extends JPanel implements GUI {
    // TODO lfo recover
    public static final String TAB_NAME = "Effects";

    @Getter private final Channel channel;

    private final PresetCheckBox presetActive = new PresetCheckBox();
    @Getter private Slider volume;
    private Slider pan;
    private final JLabel name;
    @Getter private Knob overdrive;

    private final Widget eq;
    private final Widget compression;
    private final Widget chorus;
    private final Widget reverb;
    private final Widget delay;
    private final LFOGui lfo;
    private final CutFilterGui cut;

    public EffectsRack(Channel channel) {

        this.channel = channel;
        name = new JLabel(channel.getName(), JLabel.CENTER);
        setBorder(new BevelBorder(BevelBorder.RAISED));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        eq = new EQGui(channel);
        compression = new CompressionGui(channel);
        chorus = new ChorusGui(channel);
        reverb = new ReverbGui(channel);
        delay = new DelayGui(channel);
        lfo = new LFOGui(channel);
        cut = new CutFilterGui(channel);

        add(headerRow());
        add(eq);
        add(compression);
        add(chorus);
        add(reverb);
        add(delay);

        JPanel insets = new JPanel();
        insets.setLayout(new BoxLayout(insets, BoxLayout.X_AXIS));
        insets.add(lfo);
        insets.add(cut);

        add(insets);

    }

    private boolean inUpdate;
    public void update() {
        if (!JudahZone.isInitialized() || inUpdate) return;
        inUpdate = true;

        volume.setValue(channel.getVolume());
        presetActive.setSelected(channel.isPresetActive());
        name.setToolTipText(presetActive.isSelected() ? channel.getPreset().getName() : null);
        overdrive.setValue(Math.round(channel.getOverdrive().getDrive() * 100));
        pan.setValue(Math.round( channel.getPan() * 100));

        eq.update();
        compression.update();
        chorus.update();
        reverb.update();
        delay.update();
        lfo.update();
        cut.update();
        inUpdate = false;
        revalidate();
        repaint();

    }

    // Header ////////////////////////////////////////////////////////////////////////////
    private JPanel headerRow() {
        JPanel result = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 0));
        result.setLayout(new FlowLayout());
        volume = new Slider(0, 100, e -> {channel.setVolume(volume.getValue());}, "Volume");
        Dimension SZ = new Dimension(150, 22);
        volume.setPreferredSize(SZ);
        volume.setMaximumSize(SZ);
        result.add(volume);

        name.setFont(Gui.FONT13);

        Dimension NAME = new Dimension(75, 21);
        name.setPreferredSize(NAME);
        name.setMaximumSize(NAME);


        JPanel namePnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        namePnl.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        namePnl.add(name);
        namePnl.add(presetActive);
        namePnl.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                JudahZone.getPresets().getMenu(channel, presetActive.isSelected())
                    .show(namePnl, e.getX(), e.getY());
            }
        });

        result.add(namePnl);
        pan = new Slider(l -> {channel.setPan(pan.getValue() / 100f);});
        pan.setToolTipText("pan left/right");
        Dimension SLIDER = new Dimension(75, 22);
        pan.setPreferredSize(SLIDER);
        pan.setMaximumSize(SLIDER);

        overdrive = new Knob(val -> {
            channel.getOverdrive().setDrive(val / 100f);
            channel.getOverdrive().setActive(val > 10);});
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

    class PresetCheckBox extends JCheckBox {
        public PresetCheckBox() {
            addItemListener(e -> {
                if (isSelected()) {
                    setBackground(Color.GREEN);
                    setOpaque(true);
                }
                else
                    setOpaque(false);
                if (inUpdate) return;
                if (channel.getPreset() == null) return;
                channel.getPreset().applyPreset(channel, isSelected());
            });
        }
    }

}

