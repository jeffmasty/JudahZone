package net.judah.effects.gui;
import static net.judah.JudahZone.getPresets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.io.File;
import java.security.InvalidParameterException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDesktopPane;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.ControlPanel;
import net.judah.effects.api.Preset;
import net.judah.effects.api.PresetsHandler;
import net.judah.mixer.Channel;
import net.judah.util.Console;
import net.judah.util.Constants;

public class PresetsGui extends JDesktopPane {

    final static Dimension BTN_SZ = new Dimension(80, 26);

    private JList<String> list;
    JComboBox<String> target;
    private final PresetsHandler presets;

    public PresetsGui(PresetsHandler presets) {
        this.presets = presets;
        int offset = 6;
        int buttonsWidth = 100;
        int presetsWidth = MainFrame.WIDTH_MIXER - buttonsWidth - (int)(2.5 * offset) - 50;
        int presetsHeigth = 333;
        int buttonsX = presetsWidth + offset;

        setLayout(null);

        list = new JList<>(presetModel());
        JScrollPane scrollPane = new JScrollPane(list);

        JPanel buttons = new JPanel();
        buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        scrollPane.setBounds(offset, offset, presetsWidth, presetsHeigth);
        buttons.setBounds(buttonsX, offset, buttonsWidth, presetsHeigth);

        add(scrollPane);
        add(buttons);

        JButton current = new Button(" Save ");
        JButton create  = new Button("Create");
        JButton delete  = new Button("Delete");
        JButton copy    = new Button(" Copy ");
        JButton paste   = new Button(" Paste");
        JButton up      = new Button("  Up  ");
        JButton down    = new Button(" Down ");

        target = Constants.createMixerCombo();
        target.addActionListener( e -> {applyTo();} );
        target.setMaximumSize(new Dimension(buttonsX - 10, BTN_SZ.height));
        create.addActionListener(e -> {create();});
        current.addActionListener(e -> {current(list.getSelectedIndex());});

        buttons.add(Box.createRigidArea(new Dimension(1, 15)));
        buttons.add(current);
        buttons.add(create);
        buttons.add(delete);
        buttons.add(copy);
        buttons.add(paste);
        buttons.add(Box.createRigidArea(new Dimension(1, 5)));
        buttons.add(up);
        buttons.add(down);
        buttons.add(Box.createRigidArea(new Dimension(1, 8)));
        buttons.add(new JLabel("apply to:"));
        buttons.add(target);
        buttons.add(Box.createVerticalGlue());

        for (Component c : buttons.getComponents())
            if (c instanceof JComponent)
                ((JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);

    }

    private void applyTo() {
        if (list.getSelectedIndex() < 0) return;
        Channel ch = Constants.getChannel(target.getSelectedItem().toString());
        Preset p = JudahZone.getPresets().get(list.getSelectedIndex());
        p.applyPreset(ch, true);
    }

    class Button extends JButton {
        Button(String lbl) {
            super(lbl);
            setPreferredSize(BTN_SZ);
            setMaximumSize(BTN_SZ);
        }
    }

    public void delete(int idx) {
        if (idx < 0 || idx >= getPresets().size())
            throw new InvalidParameterException("" + idx);
    }

    public void create() {
        String name = Constants.inputBox("Preset Name:");
        if (name == null || name.isEmpty()) return;
        JudahZone.getPresets().add(ControlPanel.getInstance().getChannel().toPreset(name));
        save();
    }

    public void current(int idx) {
        if (idx < 0 || idx >+ getPresets().size())
            throw new InvalidParameterException("" + idx);


        Channel channel = ControlPanel.getInstance().getChannel();
        Preset old = getPresets().get(idx);
        Preset replace = channel.toPreset(old.getName());
        getPresets().set(idx, replace);
        save();
    }

    public void save() {
        try {
            File file = new File(System.getProperty("user.dir"), "presets.zone");
            Constants.writeToFile(file, getPresets().toString());
            list.setModel(presetModel());
            Console.info("Preset saved.");
        } catch (Exception e) {Console.warn(e);}
    }

    public DefaultListModel<String> presetModel() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for(Preset p : presets)
            model.addElement(p.getName() + p.condenseEffects());
        return model;
    }

}
