package net.judah;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import lombok.Getter;
import lombok.extern.log4j.Log4j;
import net.judah.beatbox.BeatsView;
import net.judah.sequencer.Sequencer;
import net.judah.song.SheetMusic;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.JudahMenu;
import net.judah.util.Size;

@Log4j
public class MainFrame extends JFrame implements Size {


    private static MainFrame instance;
    private final JDesktopPane songPanel;
    private final JPanel header;
    private MusicPanel sheetMusic;

    @Getter private final MixerPane mixer;
    private final JTabbedPane tabs;
    private final JPanel content;
    private final String prefix;

    MainFrame(String name) {
        super(name);
        try {
            UIManager.setLookAndFeel ("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                | UnsupportedLookAndFeelException e) { log.info(e.getMessage(), e); }
        instance = this;
        prefix = name;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        content = (JPanel)getContentPane();
        content.setLayout(null);

        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
        tabs.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON2) {
                    Console.info("right click");
                }
            }
        });

        songPanel = new JDesktopPane();
        songPanel.setLayout(null);
        songPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));
        mixer = new MixerPane();
        JudahMenu.setMixerPane(mixer);
        Rectangle footerBounds = new Rectangle(0, 0, WIDTH_SONG - 1, HEIGHT_CONSOLE);
        header = new Header(footerBounds);
        tabs.setBounds(0, HEIGHT_CONSOLE, WIDTH_SONG - 1, HEIGHT_TABS);
        songPanel.add(tabs);
        songPanel.add(header);
        songPanel.setBounds(0, 0, WIDTH_SONG, HEIGHT_FRAME);
        mixer.setBounds(WIDTH_SONG, 0, WIDTH_MIXER, HEIGHT_FRAME);
        content.add(songPanel);
        content.add(mixer);
        try { setIconImage(Toolkit.getDefaultToolkit().getImage(
                new File(Constants.ROOT, "icon.png").toURI().toURL()));
        } catch (Throwable t) {log.error(t.getMessage(), t); }
        setLocation(0, 0);
        setSize(WIDTH_FRAME, HEIGHT_FRAME);


        // setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        invalidate();
        setVisible(true);
    }

    public void beatBox() {
        BeatsView gui = new BeatsView();
        tabs.add("BeatBox", gui);
        tabs.setSelectedComponent(gui);

    }
//    public void beatBox() {
//        BeatBoxGui gui = JudahClock.getInstance().getOld().getGui();
//        tabs.add("BeatBox", gui);
//        tabs.setSelectedComponent(gui);
//    }
//    public void noteBox() {
//        NoteBoxGui gui = JudahClock.getInstance().getNoteBox().getGui();
//        tabs.add("NoteBox", gui);
//        tabs.setSelectedComponent(gui);
//    }

    public void closeTab(SongPane c) {
        c.getSequencer().stop();
        tabs.remove(c);
    }

    public void sheetMusicOff() {
        if (sheetMusic == null) return;
        sheetMusic.setVisible(false);
        songPanel.remove(sheetMusic);
        sheetMusic = null;
    }

    public void sheetMusic() {
        if (Sequencer.getCurrent() == null || Sequencer.getCurrent().getSheetMusic() == null)
            return;
        if (sheetMusic == null) {
            try {
                sheetMusic = new MusicPanel(Sequencer.getCurrent().getSheetMusic());
                sheetMusic.setBounds(tabs.getBounds());
                tabs.setVisible(false);
                songPanel.removeAll();
                songPanel.add(sheetMusic);
                songPanel.add(header);
                sheetMusic.doLayout();
            } catch (IOException e) { Console.warn(Sequencer.getCurrent().getSheetMusic().getAbsolutePath(), e); }
        }
        else {
            sheetMusicOff();
            songPanel.removeAll();
            songPanel.add(tabs);
            tabs.setVisible(true);
            songPanel.add(header);

        }

        songPanel.doLayout();
        songPanel.invalidate();
        Console.getInstance().getInput().grabFocus();
    }

    private class MusicPanel extends JPanel {
        Image image;
        JLabel labelImage;

        MusicPanel(File musicImage) throws IOException {

            image = ImageIO.read(musicImage);
            labelImage = new SheetMusic();
            labelImage.setIcon(new ImageIcon(image));
            labelImage.setIcon(new ImageIcon(image));


            setLayout(new GridBagLayout());
            GridBagConstraints constraints = new GridBagConstraints();
            constraints.insets = new Insets(3, 3, 3, 3);
            constraints.fill = GridBagConstraints.NONE;
            constraints.anchor = GridBagConstraints.NORTHWEST;
            constraints.fill = GridBagConstraints.BOTH;
            constraints.weightx = 1.0;
            constraints.weighty = 1.0;

            constraints.gridy = 1;
            constraints.gridx = 0;
            constraints.gridwidth = 3;
            labelImage.setMaximumSize(new Dimension(650, 470));
            labelImage.setPreferredSize(new Dimension(get().songPanel.getWidth(), get().songPanel.getHeight()-200));
            add(labelImage, constraints);

        }
    }

//    private class Footer extends JPanel {
//        Footer() {
//            JPanel clock = JudahClock.getInstance().getGui();
//            JPanel tuner = new Tuner();
//            JTextField input = Console.getInstance().getInput();
////            add(JudahClock.getInstance().getGui());
//            add(new Tuner());
//            add(Console.getInstance().getInput());
//
//        }
//    }

    public void openPage(SongPane page) {
        for(int i = 0; i < tabs.getTabCount(); i++)
            if (tabs.getTitleAt(i).contains(Constants.CUTE_NOTE))
                tabs.setTitleAt(i, tabs.getTitleAt(i).replace(Constants.CUTE_NOTE, ""));
        String name = Constants.CUTE_NOTE + page.getName();
        tabs.add(name, page);
        tabs.setSelectedComponent(page);
        setTitle(prefix + " - " + page.getName());
        // sheetMusic();
    }

    public static MainFrame get() {
        return instance;
    }
}
