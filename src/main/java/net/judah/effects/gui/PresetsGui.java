package net.judah.effects.gui;

import static net.judah.JudahZone.getPresets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.security.InvalidParameterException;

import javax.swing.*;

import net.judah.JudahZone;
import net.judah.effects.api.Preset;
import net.judah.effects.api.PresetsDB;
import net.judah.mixer.Channel;
import net.judah.util.Constants;
import net.judah.util.RTLogger;

public class PresetsGui extends JPanel {

	final static Dimension BTN_SZ = new Dimension(80, 26);
	
    private JList<String> list;
    JComboBox<String> target;
    private final int offset = 5;
    private final int buttonsWidth = 95;
    private final int presetsHeight = 480;
    private final int presetsWidth = 190;
    private final int buttonsX = presetsWidth + offset;
	
	public PresetsGui() {
		setLayout(null);
        setPreferredSize(new Dimension(presetsWidth + buttonsWidth + 10, presetsHeight));
        list = new JList<>(presetModel());
        JScrollPane scrollPane = new JScrollPane(list);
        JPanel buttons = new JPanel();
        target = Constants.createMixerCombo();
        target.addActionListener( e -> {applyTo();} );
        target.setMaximumSize(new Dimension(buttonsX - 10, BTN_SZ.height));
        presetsBtns(buttons);
        scrollPane.setBounds(offset, offset + 35, presetsWidth, presetsHeight);
        buttons.setBounds(buttonsX, offset + 35, buttonsWidth, presetsHeight);
        add(scrollPane);
        add(buttons);
	}
    private void presetsBtns(JPanel buttons) {
    	buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JButton save = new Button(" Save ");
        JButton create  = new Button("Create");
        JButton delete  = new Button("Delete");
        JButton copy    = new Button(" Copy ");
        JButton paste   = new Button(" Paste");
        JButton up      = new Button("  Up  ");
        JButton down    = new Button(" Down ");
        create.addActionListener(e -> {create();});
        save.addActionListener(e -> {current(list.getSelectedIndex());});

    	buttons.add(Box.createRigidArea(new Dimension(1, 15)));
        buttons.add(save);
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
        JLabel title = new JLabel("Presets", JLabel.CENTER);
        title.setBounds(30, offset, 200, 30);
        add(title);
    }

	
	private void applyTo() {
        if (list.getSelectedIndex() < 0) return;
        Channel ch = Constants.getChannel(target.getSelectedItem().toString());
        Preset p = JudahZone.getPresets().get(list.getSelectedIndex());
        ch.setPreset(p);
        ch.setPresetActive(true);
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
            Constants.writeToFile(PresetsDB.FILE, getPresets().toString());
            list.setModel(presetModel());
        } catch (Exception e) {RTLogger.warn(this, e);}
    }

    public DefaultListModel<String> presetModel() {
        DefaultListModel<String> model = new DefaultListModel<>();
        for(Preset p : JudahZone.getPresets())
            model.addElement(p.getName() + p.condenseEffects());
        return model;
    }

}
