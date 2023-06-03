package net.judah.gui.fx;

import static net.judah.JudahZone.getPresets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Comparator;

import javax.swing.*;

import net.judah.JudahZone;
import net.judah.fx.Preset;
import net.judah.fx.PresetsDB;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.ModalDialog;
import net.judah.mixer.Channel;
import net.judah.util.Constants;
import net.judah.util.Folders;
import net.judah.util.RTLogger;

public class PresetsGui extends JPanel {

	final static Dimension BTN_SZ = new Dimension(80, 26);
	
    private JList<String> list;
    JComboBox<String> target;
    private final int offset = 5;
    private final int buttonsWidth = 95;
    private final int presetsHeight = 500;
    private final int presetsWidth = 265;
    private final int buttonsX = presetsWidth + offset;
	private final PresetsDB presets;
    
	public PresetsGui(PresetsDB presets) {
		setLayout(null);
		this.presets = presets;
		Dimension size = new Dimension(presetsWidth + buttonsWidth + 10, presetsHeight);
        setPreferredSize(size);
        list = new JList<>(presetModel());
        JScrollPane scrollPane = new JScrollPane(list);
        JPanel buttons = new JPanel();
        target = createMixerCombo();
        target.addActionListener( e -> {applyTo();} );
        target.setMaximumSize(new Dimension(buttonsX - 10, BTN_SZ.height));
        presetsBtns(buttons);
        scrollPane.setBounds(offset, offset + 35, presetsWidth, presetsHeight);
        buttons.setBounds(buttonsX, offset + 35, buttonsWidth, presetsHeight);
        add(scrollPane);
        add(buttons);
        Dimension dialog = new Dimension(size.width + 10, size.height + 20);
        new ModalDialog(this, dialog, MainFrame.getKnobMode());
	}

	private JComboBox<String> createMixerCombo() {
	    ArrayList<String> channels = new ArrayList<>();
        for (Channel c : JudahZone.getMixer().getChannels())
            channels.add(c.getName());
        return new JComboBox<>(channels.toArray(new String[channels.size()]));
	}
	
	private void presetsBtns(JPanel buttons) {
    	buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBorder(BorderFactory.createLineBorder(Color.GRAY));

        JButton save 	= new Button(" Save ", e->current(list.getSelectedIndex()));
        JButton create  = new Button("Create", e->create());
        JButton delete  = new Button("Delete", e->delete(list.getSelectedIndex()));
        JButton copy    = new Button(" Copy ", e->copy(list.getSelectedIndex()));
        JButton close 	= new Button("Close", e->ModalDialog.getInstance().setVisible(false));
        
    	buttons.add(Box.createRigidArea(new Dimension(1, 15)));
        buttons.add(save);
        buttons.add(create);
        buttons.add(delete);
        buttons.add(copy);
        buttons.add(Box.createRigidArea(new Dimension(1, 5)));
        buttons.add(Box.createRigidArea(new Dimension(1, 8)));
        buttons.add(new JLabel("apply to:"));
        buttons.add(target);
        buttons.add(close);
        buttons.add(Box.createVerticalGlue());
        for (Component c : buttons.getComponents())
            if (c instanceof JComponent)
                ((JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel title = new JLabel("Presets", JLabel.CENTER);
        title.setBounds(30, offset, 200, 30);
        add(title);
    }

	
	private void copy(int idx) {
		if (list.getSelectedIndex() < 0) return;
		String name = Gui.inputBox("New Name:");
		if (name == null || name.isBlank()) return;
		
		
		Preset source = getPresets().get(idx);
		presets.add(new Preset(name, source));
		save();
	}

	private void applyTo() {
        if (list.getSelectedIndex() < 0) return;
        String search = "" + target.getSelectedItem();
        Channel ch = JudahZone.getInstruments().byName(search);
        if (ch == null)
        	ch = JudahZone.getLooper().byName(search);
        if (ch == null)
        	ch = JudahZone.getMains();
        Preset p = presets.get(list.getSelectedIndex());
        ch.setPreset(p);
        ch.setPresetActive(true);
    }

    class Button extends Btn {
        Button(String lbl, ActionListener e) {
            super(lbl, e);
            setPreferredSize(BTN_SZ);
            setMaximumSize(BTN_SZ);
        }
    }

    public void delete(int idx) {
        if (idx < 0 || idx >= getPresets().size())
            throw new InvalidParameterException("" + idx);
        getPresets().remove(idx);
    }

    public void create() {
        String name = Gui.inputBox("Preset Name:");
        if (name == null || name.isEmpty()) return;
        presets.add(JudahZone.getFxRack().getChannel().toPreset(name));
        save();
    }

    public void current(int idx) {
        if (idx < 0 || idx >+ getPresets().size())
            throw new InvalidParameterException("" + idx);


        Channel channel = JudahZone.getFxRack().getChannel();
        Preset old = presets.get(idx);
        Preset replace = channel.toPreset(old.getName());
        getPresets().set(idx, replace);
        save();
    }

    public void save() {
        try {
            Constants.writeToFile(Folders.getPresetsFile(), getPresets().toString());
            list.setModel(presetModel());
        } catch (Exception e) {RTLogger.warn(this, e);}
    }

    public DefaultListModel<String> presetModel() {
        DefaultListModel<String> model = new DefaultListModel<>();
        ArrayList<Preset> list = JudahZone.getPresets();
        list.sort(new Comparator<Preset>() {
			@Override public int compare(Preset o1, Preset o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
        for(Preset p : list)
            model.addElement(p.getName() + p.condenseEffects());
        return model;
    }

}
