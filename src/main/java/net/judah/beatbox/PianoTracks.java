package net.judah.beatbox;

import java.awt.GridLayout;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.judah.util.Constants.Gui;
import net.judah.util.Pastels;
import net.judah.util.Slider;

public class PianoTracks extends KitPanel implements Pastels {

    private final Grid grid;
    private final ArrayList<JToggleButton> labels = new ArrayList<>();

    private JComboBox<Key> keyCombo;
    private JComboBox<Scale> scaleCombo;
    private JComboBox<Integer> octaveCombo;

    public PianoTracks(Grid grid) {
    	setLayout(new GridLayout(0, 1, 0, 0));

    	this.grid = grid;
        
        // first (scale/key) row
        scaleCombo = new JComboBox<>();
        for (Scale s : Scale.values()) scaleCombo.addItem(s);
        scaleCombo.setSelectedItem(grid.getScale());

        keyCombo = new JComboBox<>();
        for (Key k : Key.values()) keyCombo.addItem(k);
        keyCombo.setSelectedItem(grid.getKey());

        octaveCombo = new JComboBox<>();
        for (int i = 1; i <= 6; i++) octaveCombo.addItem(i);
        octaveCombo.setSelectedItem(grid.getOctave());

        JButton go = new JButton("!");
        go.setFont(Gui.BOLD);
        go.addActionListener(e -> {
            BeatsView.getInstance().getSequencer().noteOff();
            grid.setOctave((int)octaveCombo.getSelectedItem());
            grid.setKey((Key)keyCombo.getSelectedItem());
            grid.setScale((Scale)scaleCombo.getSelectedItem());
            grid.redoTracks();
        });
        firstRow.add(go);
        firstRow.add(scaleCombo);
        firstRow.add(keyCombo);
        firstRow.add(octaveCombo);

        // tracks
        int count = grid.size();
        for(int i = 0; i < count; i++) {
            final Sequence sequence = grid.get(i);
            Slider volume = new Slider(e -> {
                sequence.setVelocity(((Slider)e.getSource()).getValue() * .01f);});
            volume.setValue((int)(sequence.getVelocity() * 100));
            volume.setPreferredSize(SLIDESZ);

            JToggleButton mute = new JToggleButton(sequence.getReference().toString());
            mute.setBackground(sequence.isMute() ? RED : GREEN);
            mute.setSelected(sequence.isMute());
            // mute.setPreferredSize(BTNSZ);

            mute.addActionListener(e -> {
                sequence.setMute(mute.isSelected());
                mute.setBackground(sequence.isMute() ? RED : GREEN);
            });

            JPanel pnl = new JPanel(new GridLayout(1, 2, 0, 0));
            pnl.add(volume);
            pnl.add(mute);
            labels.add(mute);
            add(pnl);
        }
    }

    public void reLabel() {
        for (int i = 0; i < grid.size(); i++)
            labels.get(i).setText(grid.get(i).getReference().toString());
        repaint();
    }

}
