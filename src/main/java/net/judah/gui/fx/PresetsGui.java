package net.judah.gui.fx;

import static net.judah.JudahZone.getPresets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
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

/** creates modal dialog box */
public class PresetsGui extends JPanel {

	private static final int offset = 5;
    public static final int buttonsWidth = 95;
    public static final int presetsHeight = 500;
    public static final int presetsWidth = 265;
    private static final int buttonsX = presetsWidth + offset;
	public static final Dimension BTN_SZ = new Dimension(80, 26);
	public static final Dimension SIZE = new Dimension(presetsWidth, presetsHeight);
	public static final Dimension DIALOG = new Dimension(presetsWidth + buttonsWidth + 3 * offset, presetsHeight + 75);

	
	private final PresetsDB presets;
	private final JList<String> list = new JList<>();
    private final JComboBox<String> target;
    
	public PresetsGui(PresetsDB presets) {
		this.presets = presets;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        renew();
        target = createMixerCombo();
        target.addActionListener( e -> {applyTo();} );
        target.setMaximumSize(new Dimension(buttonsX - 10, BTN_SZ.height));
        JScrollPane scrollPane = new JScrollPane(list);
        scrollPane.setBounds(offset, offset + 35, presetsWidth, presetsHeight);
		setLayout(null);
        add(scrollPane);
        add(presetsBtns(new Rectangle(buttonsX, offset + 35, buttonsWidth, presetsHeight)));
        setName("Presets");
        new ModalDialog(this, DIALOG, MainFrame.getKnobMode());
	}

	private JComboBox<String> createMixerCombo() {
	    ArrayList<String> channels = new ArrayList<>();
        for (Channel c : JudahZone.getMixer().getChannels())
            channels.add(c.getName());
        return new JComboBox<>(channels.toArray(new String[channels.size()]));
	}
	
	private JPanel presetsBtns(Rectangle rectangle) {
		JPanel buttons = new JPanel();
    	buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        buttons.setBounds(rectangle);

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
        return buttons;
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

    public static class Button extends Btn {
        public Button(String lbl, ActionListener e) {
            super(lbl, e);
            Gui.resize(this, BTN_SZ);
        }
    }

    public void delete(int idx) {
        if (idx < 0 || idx >= getPresets().size())
            throw new InvalidParameterException("" + idx);
        getPresets().remove(idx);
        renew();
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
    	presets.save();
    	renew();
    }

    private void renew() {
        DefaultListModel<String> model = new DefaultListModel<>();
        presets.sort(new Comparator<Preset>() {
			@Override public int compare(Preset o1, Preset o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
        for(Preset p : presets)
            model.addElement(p.getName() + p.condenseEffects());
        list.setModel(model);
    }

}
