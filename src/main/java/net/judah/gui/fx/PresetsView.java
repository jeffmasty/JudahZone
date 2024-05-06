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

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.fx.Gain;
import net.judah.fx.Preset;
import net.judah.fx.PresetsDB;
import net.judah.gui.Gui;
import net.judah.gui.MainFrame;
import net.judah.gui.Size;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.widgets.Btn;
import net.judah.gui.widgets.LengthCombo;
import net.judah.midi.JudahClock;
import net.judah.mixer.Channel;
import net.judah.util.Constants;

public class PresetsView extends KnobPanel  {
	public static final Dimension BTN_SZ = new Dimension(80, Size.STD_HEIGHT);

	@Getter private final KnobMode knobMode = KnobMode.Presets;
	@Getter private final JPanel title = new JPanel();
	private final PresetsDB presets;
	private final JList<String> list = new JList<>();
    private final JComboBox<String> target;

	public PresetsView(PresetsDB presets) {
		this.presets = presets;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        renew();
        target = createMixerCombo();
        target.addActionListener( e -> {applyTo();} );
        Gui.resize(target, BTN_SZ);
        JScrollPane scrollPane = new JScrollPane(list);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        add(scrollPane);
        add(Gui.resize(presetsBtns(), new Dimension(BTN_SZ.width + 8, Size.HEIGHT_KNOBS - 30)));
        validate();
	}

	private JComboBox<String> createMixerCombo() {
	    ArrayList<String> channels = new ArrayList<>();
        for (Channel c : JudahZone.getMixer().getChannels())
            channels.add(c.getName());
        return new JComboBox<>(channels.toArray(new String[channels.size()]));
	}
	
	private JPanel presetsBtns() {
		JPanel buttons = new JPanel();
    	buttons.setLayout(new BoxLayout(buttons, BoxLayout.Y_AXIS));
        buttons.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JButton save 	= new Button(" Save ", e->current(list.getSelectedIndex()));
        JButton create  = new Button("Create", e->create());
        JButton delete  = new Button("Delete", e->delete(list.getSelectedIndex()));
        JButton copy    = new Button(" Copy ", e->copy(list.getSelectedIndex()));
        
    	buttons.add(Box.createVerticalStrut(4));
        buttons.add(save);
        buttons.add(create);
        buttons.add(copy);
        buttons.add(delete);
        buttons.add(Box.createVerticalStrut(7));
        buttons.add(new JLabel("apply to:"));
        buttons.add(target);
        buttons.add(Box.createVerticalGlue());
        for (Component c : buttons.getComponents())
            if (c instanceof JComponent)
                ((JComponent) c).setAlignmentX(Component.CENTER_ALIGNMENT);
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

    @Override
	public void update() {
    	
    }

	@Override
	public boolean doKnob(int idx, int data2) {
		switch (idx) {
    	case 0: // sync loop length 
    		JudahClock clock = JudahZone.getClock();
			if (data2 == 0) 
				clock.setLength(1);
			else 
				clock.setLength((int) Constants.ratio(data2, LengthCombo.LENGTHS));
			break;
    	case 1: // Select preset
    		Constants.execute(()->list.setSelectedIndex(Constants.ratio(data2, list.getModel().getSize() - 1)));
    		break;
    	case 2: // apply to
    		// Constants.execute(()->target.setSelectedIndex(Constants.ratio(data2, target.getItemCount() - 1)));
    		break;
    	case 3: 
    		JudahZone.getMains().getGain().set(Gain.VOLUME, data2);
    		MainFrame.update(JudahZone.getMains());
    		break;
		}
		return true;
	}
    
	@Override
	public void pad1() {
		applyTo();
	}
	
}
