package net.judah.gui.fx;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionListener;
import java.security.InvalidParameterException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import judahzone.fx.Gain;
import judahzone.gui.Gui;
import judahzone.util.Constants;
import judahzone.util.Threads;
import judahzone.widgets.Btn;
import lombok.Getter;
import net.judah.JudahZone;
import net.judah.channel.Channel;
import net.judah.channel.Preset;
import net.judah.channel.PresetsDB;
import net.judah.gui.MainFrame;
import net.judah.gui.knobs.KnobMode;
import net.judah.gui.knobs.KnobPanel;
import net.judah.gui.widgets.LengthCombo;
import net.judah.midi.JudahClock;
import net.judah.midi.JudahMidi;

public class PresetsView extends KnobPanel  {
	public static final Dimension BTN_SZ = new Dimension(80, STD_HEIGHT);

	@Getter private final KnobMode knobMode = KnobMode.Presets;
	@Getter private final JPanel title = new JPanel();
	private final JList<String> list = new JList<>();
    private final JComboBox<String> target;
    private final JudahZone zone;

	public PresetsView(JudahZone judahZone) {
		this.zone = judahZone;
        list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        renew();
        target = createMixerCombo();
        target.addActionListener( e -> {applyTo();} );
        Gui.resize(target, BTN_SZ);
        JScrollPane scrollPane = new JScrollPane(list);
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
        add(scrollPane);
        add(Gui.resize(presetsBtns(), new Dimension(BTN_SZ.width + 8, HEIGHT_KNOBS - 30)));
        validate();
	}

	private JComboBox<String> createMixerCombo() {
	    ArrayList<String> channels = new ArrayList<>();
        for (Channel c : zone.getChannels().getAll())
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

		Preset source = PresetsDB.get(idx);
		PresetsDB.add(new Preset(name, source));
		save();
	}

	private void applyTo() {
        if (list.getSelectedIndex() < 0) return;
        String search = "" + target.getSelectedItem();
        Channel ch = zone.getChannels().byName(search);
        if (ch == null)
        	ch = zone.getLooper().byName(search);
        if (ch == null)
        	ch = zone.getMains();
        Preset p = PresetsDB.get(list.getSelectedIndex());
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
        if (idx < 0 || idx >= PresetsDB.size())
            throw new InvalidParameterException("" + idx);
        PresetsDB.remove(idx);
        renew();
    }

    public void create() {
        String name = Gui.inputBox("Preset Name:");
        if (name == null || name.isEmpty()) return;
        PresetsDB.add(zone.getFxRack().getChannel().toPreset(name));
        save();
    }

    public void current(int idx) {
        if (idx < 0 || idx >+ PresetsDB.size())
            throw new InvalidParameterException("" + idx);
        Channel channel = zone.getFxRack().getChannel();
        Preset old = PresetsDB.get(idx);
        Preset replace = channel.toPreset(old.getName());
        PresetsDB.set(idx, replace);
        save();
    }

    public void save() {
    	PresetsDB.save();
    	renew();
    }

    private void renew() {
        DefaultListModel<String> model = new DefaultListModel<>();
        PresetsDB.getDb().sort((o1, o2) -> o1.getName().compareTo(o2.getName()));
        for(Preset p : PresetsDB.getDb())
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
    		JudahClock clock = JudahMidi.getClock();
			if (data2 == 0)
				clock.setLength(1);
			else
				clock.setLength((int) Constants.ratio(data2, LengthCombo.LENGTHS));
			break;
    	case 1: // Select preset
    		Threads.execute(()->list.setSelectedIndex(Constants.ratio(data2, list.getModel().getSize() - 1)));
    		break;
    	case 2: // apply to
    		// Constants.execute(()->target.setSelectedIndex(Constants.ratio(data2, target.getItemCount() - 1)));
    		break;
    	case 3:
    		zone.getMains().getGain().set(Gain.VOLUME, data2);
    		MainFrame.update(zone.getMains());
    		break;
		}
		return true;
	}

	@Override
	public void pad1() {
		applyTo();
	}

}
