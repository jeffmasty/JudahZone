package net.judah.plugin;

import static net.judah.util.Constants.Gui.*;

import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.SystemColor;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.border.BevelBorder;

import net.judah.JudahZone;
import net.judah.plugin.BeatBuddy.Dir;
import net.judah.plugin.BeatBuddy.Drumset;
import net.judah.plugin.BeatBuddy.Track;
import net.judah.util.Constants;
import net.judah.util.Knob;
import net.judah.util.MenuBar;

public class BeatGui extends JPanel {

    private static final int PAD = 1;

    private final BeatBuddy buddy;

    private final JButton startBtn = new JButton("Start");
    private final JButton pauseBtn = new JButton("Pause");
    private final JButton stopBtn = new JButton("Stop");

    private JLabel bpmText;
    private JSlider bpm;
    private Knob volume;

    boolean changingFolders;
    JComboBox<String> drumsets;

    private JComboBox<String> folder;
    private DefaultComboBoxModel<String> tracks;
    private JComboBox<String> song;
    private JButton transBtn;
    private JPanel tapPanel;

    public BeatGui(BeatBuddy drum) {
        this.buddy = drum;
        gui();
        actionListeners();
        keyHandler();
        folder.setSelectedIndex(1); // brazillian <3 <3 <3
    }

    private void keyHandler() {
        // bpm.addKeyListener(MenuBar.getInstance());
        transBtn.addKeyListener(MenuBar.getInstance());
        song.addKeyListener(MenuBar.getInstance());
        folder.addKeyListener(MenuBar.getInstance());
        drumsets.addKeyListener(MenuBar.getInstance());
        startBtn.addKeyListener(MenuBar.getInstance());
        pauseBtn.addKeyListener(MenuBar.getInstance());
        stopBtn.addKeyListener(MenuBar.getInstance());
    }

    private void gui() {

        setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        JPanel topRow = new JPanel(new FlowLayout(FlowLayout.CENTER, PAD, PAD));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.CENTER, PAD, PAD));
        buttons.setBorder(BorderFactory.createTitledBorder("Control"));

        buttons.add(startBtn);
        buttons.add(pauseBtn);
        buttons.add(stopBtn);
        topRow.add(buttons);
        DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
        for (Drumset set : BeatBuddy.Drumset.values())
            if (set.ordinal() != 0) // drumset 0 not used
                model.addElement(set.name());
        drumsets = new JComboBox<>(model);
        JPanel drums = new JPanel(new FlowLayout(FlowLayout.CENTER, PAD, PAD));
        drums.setBorder(BorderFactory.createTitledBorder("Drumset"));
        drums.add(drumsets);
        topRow.add(drums);
        add(topRow);


        JLabel tempoLbl = new JLabel("Tempo");
        tempoLbl.setFont(FONT12);

        bpm = new JSlider(JSlider.HORIZONTAL, 30, 230, Math.round(buddy.getTempo()));
        bpm.setFont(FONT9);
        bpm.setMajorTickSpacing(40);
        bpm.setPaintTicks(false);
        bpm.setPaintLabels(true);

        bpmText= new JLabel();
        bpmText.setFont(BOLD);
        bpmText.setAlignmentX(-1f);
        JLabel bpmLbl = new JLabel(" BPM ");
        bpmLbl.setFont(FONT11);
        bpmLbl.setAlignmentX(-1f);
        tapPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PAD, PAD));
        tapPanel.setLayout(new BoxLayout(tapPanel, BoxLayout.Y_AXIS));
        tapPanel.setBorder(BorderFactory.createDashedBorder(SystemColor.inactiveCaption));

        tapPanel.add(Box.createRigidArea(new Dimension(1, 4)));
        tapPanel.add(bpmText, Box.LEFT_ALIGNMENT);
        tapPanel.add(bpmLbl, Box.LEFT_ALIGNMENT);
        tapPanel.add(Box.createRigidArea(new Dimension(1, 4)));
        tapPanel.setToolTipText("Tap Tempo");
        tapPanel.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                buddy.tapTempo();
            }
        });

        JLabel volumeLbl = new JLabel("Vol", SwingConstants.CENTER);
        volumeLbl.setFont(FONT11);
        volume = new Knob(val -> {buddy.setVolume(volume.getValue()); });

        JPanel tempoPnl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        tempoPnl.setBorder(BorderFactory.createTitledBorder("Tempo"));
        tempoPnl.add(tapPanel);
        tempoPnl.add(bpm);

        JPanel volPnl = new JPanel(new FlowLayout(FlowLayout.CENTER, PAD, PAD));
        volPnl.setBorder(BorderFactory.createTitledBorder("Vol"));
        volPnl.add(volume);

        JPanel bottomRow = new JPanel(new FlowLayout(FlowLayout.CENTER, PAD, PAD));
        bottomRow.add(tempoPnl);
        bottomRow.add(volPnl);
        add(bottomRow);

        DefaultComboBoxModel<String> folders = new DefaultComboBoxModel<>();
        for (Dir dir : buddy.directories)
            folders.addElement(dir.getName());
        folder = new JComboBox<>(folders);

        tracks = new DefaultComboBoxModel<>();
        song = new JComboBox<>(tracks);

        JPanel songPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, PAD, PAD));
        songPanel.setBorder(BorderFactory.createTitledBorder("Drum Track"));
        songPanel.add(folder);
        songPanel.add(song);

        transBtn = new JButton("A/B");
        songPanel.add(transBtn);
        add(songPanel);

        if (JudahZone.getMetronome() != null)
            add(JudahZone.getMetronome().getGui());

    }
    private void actionListeners() {
        startBtn.addActionListener(e -> {
            buddy.getQueue().offer(BeatBuddy.PLAY_MIDI);});
        pauseBtn.addActionListener(e -> {
            buddy.getQueue().offer(BeatBuddy.PAUSE_MIDI);});
        stopBtn.addActionListener(e -> {
            buddy.getQueue().offer(BeatBuddy.STOP_MIDI); });

        bpm.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                buddy.setTempo(bpm.getValue());
        }});

        bpm.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_RIGHT)
                    buddy.setTempo(Math.round(buddy.getTempo()) + 3);
                else if (e.getKeyCode() == KeyEvent.VK_LEFT)
                    buddy.setTempo(Math.round(buddy.getTempo()) - 3);
            }
        });

        drumsets.addActionListener(e -> {
            if (changingFolders) return;
            Drumset selected = BeatBuddy.Drumset.valueOf(
                    drumsets.getSelectedItem().toString());
            buddy.drumset(selected.ordinal());
        });

        folder.addActionListener(e -> {
            changingFolders = true;
            song.removeAllItems();
            for(Track t : buddy.tracks.get(folder.getSelectedItem().toString()))
                tracks.addElement(t.getName());
            changingFolders = false;
        });

        song.addActionListener(e -> {
            if (changingFolders || song.getSelectedItem() == null) return;
            int dir = -1;
            int idx = -1;
            String file = folder.getSelectedItem().toString();

            String track = song.getSelectedItem().toString();
            for (int i = 0; i < buddy.directories.size(); i++)
                if (file.equals(buddy.directories.get(i).name)) {
                    dir = i;
                    break;
                }
            for (Track t : buddy.tracks.get(file))
                if (track.equals(t.getName())) {
                    idx = t.getIdx();
                    break;
                }
            if (dir == -1 || idx == -1) return;
            buddy.song(dir, idx);
            new Thread() {
                @Override
                public void run() {
                    Constants.sleep(111);
                    buddy.getQueue().offer(BeatBuddy.PLAY_MIDI);
                }
            }.start();
        });

        transBtn.addActionListener(e -> {
            buddy.transission();});

    }

    public void tempo(int tempo) {
        bpm.setValue(tempo);
        bpmText.setText((tempo < 100 ? "  " : " ") + tempo);
    }

    public void song(int folderIdx, int songNum) {
        changingFolders = true;

        folder.setSelectedIndex(folderIdx);
        song.setSelectedIndex(songNum - 1);

        changingFolders = false;

    }

}
