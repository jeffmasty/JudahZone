package net.judah.beatbox;

import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.Box;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.util.CenteredCombo;
import net.judah.util.Constants;
import net.judah.util.Pastels;
import net.judah.util.Size;
import net.judah.util.Slider;

public class DrumTracks extends KitPanel implements Pastels {

    public DrumTracks(Grid grid) {
    	
    	setLayout(null);
    	
        int rowHeight = BeatsView.getInstance().getGrid2().getRowHeight(); 
    	// first row
        CenteredCombo<String> kits = new CenteredCombo<>();
        for (String key : GMDrum.KITS.keySet())
            kits.addItem(key);
        kits.addActionListener(e -> {
            String kit = kits.getSelectedItem().toString();
            if (!kit.equals(grid.getKit())) {
                grid.setKit(kit);
                grid.initialize();
            }});
        kits.setSelectedItem(grid.getKit());

        Dimension rigid = new Dimension(15, 1);
        firstRow.add(Box.createRigidArea(rigid));
        firstRow.add(new JLabel("Kit"));
        firstRow.add(Box.createRigidArea(rigid));
        firstRow.add(kits);
        firstRow.add(Box.createRigidArea(rigid));
        firstRow.setBounds(0, 0, Size.WIDTH_KIT, rowHeight);
        // tracks
        int count = grid.size();
        for(int i = 0; i < count; i++) {
            final Sequence sequence = grid.get(i);
            Slider volume = new Slider(e -> {
                sequence.setVelocity(((Slider)e.getSource()).getValue() * .01f);});
            volume.setValue((int)(sequence.getVelocity() * 100));
            volume.setPreferredSize(SLIDESZ);

            final JComboBox<GMDrum> drumCombo = new JComboBox<>(GMDrum.values());
                drumCombo.setSelectedItem(sequence.getReference().getDrum());
            drumCombo.setFont(Constants.Gui.FONT11);
            drumCombo.setPreferredSize(NAMESZ);

            drumCombo.addActionListener(e -> {
                sequence.setReference(new MidiBase((GMDrum)drumCombo.getSelectedItem()));
            });

            JToggleButton mute = new JToggleButton("▷");
            mute.setBackground(sequence.isMute() ? RED : GREEN);
            mute.setSelected(sequence.isMute());
            mute.setPreferredSize(BTNSZ);

            mute.addActionListener(e -> {
                sequence.setMute(mute.isSelected());
                mute.setBackground(sequence.isMute() ? RED : GREEN);
            });

            JPanel pnl = new JPanel(new FlowLayout(FlowLayout.CENTER, 1, 0));
            pnl.add(volume);
            pnl.add(drumCombo);
            pnl.add(mute);
            pnl.setBounds(0, rowHeight * (i + 1) + 5, Size.WIDTH_KIT, rowHeight);
            add(pnl);
        }
        doLayout();
    }
}
