package net.judah;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
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
import net.judah.sequencer.Sequencer;
import net.judah.song.SheetMusic;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.MenuBar;

@Log4j
public class MainFrame extends JFrame {


    public static final int WIDTH_FRAME = 1279;
    public static final int HEIGHT_FRAME = 740;
    public static final int HEIGHT_MENU = 21;
    public static final int WIDTH_MIXER = 448;
    public static final int WIDTH_SONG = WIDTH_FRAME - WIDTH_MIXER;
    public static final int HEIGHT_CONSOLE = 138;
    public static final int HEIGHT_TABS = HEIGHT_FRAME - (HEIGHT_CONSOLE + 40);

    private static MainFrame instance;
    private final JDesktopPane songPanel;
    private final JPanel consoles;
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
        // setJMenuBar(MenuBar.getInstance());

        content = (JPanel)getContentPane();
        content.setLayout(null);

        tabs = new JTabbedPane();
        tabs.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        songPanel = new JDesktopPane();

        songPanel.setLayout(null);
        songPanel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY));

        MenuBar menu = MenuBar.getInstance();
        songPanel.add(menu);
        songPanel.add(tabs);

        content.add(songPanel);

        mixer = new MixerPane();
        MenuBar.getInstance().setMixerPane(mixer);

        tabs.setBounds(0, HEIGHT_MENU, WIDTH_SONG - 1, HEIGHT_TABS);
        menu.setBounds(0, 0, WIDTH_SONG, HEIGHT_MENU);

        consoles = new JPanel();
        consoles.setLayout(new BoxLayout(consoles, BoxLayout.Y_AXIS));
        consoles.setBounds(0, HEIGHT_TABS + HEIGHT_MENU, WIDTH_SONG - 1, HEIGHT_CONSOLE);
        songPanel.add(consoles);
        consoles.add(Console.getInstance().getScroller());

        consoles.add(new Footer());

        songPanel.setBounds(0, 0, WIDTH_SONG, HEIGHT_FRAME);
//        mixer.setBounds(WIDTH_SONG, 0, WIDTH_MIXER, HEIGHT_FRAME - 40);
        // mixer.doLayout();

        content.add(mixer);
        setLocation(0, 0);
        setSize(WIDTH_FRAME, HEIGHT_FRAME);
        // setExtendedState(java.awt.Frame.MAXIMIZED_BOTH);
        mixer.setBounds(WIDTH_SONG, 0, WIDTH_MIXER, HEIGHT_FRAME);
        invalidate();
        setVisible(true);


    }


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
                songPanel.add(consoles);
                sheetMusic.doLayout();
            } catch (IOException e) { Console.warn(Sequencer.getCurrent().getSheetMusic().getAbsolutePath(), e); }
        }
        else {
            sheetMusicOff();
            songPanel.removeAll();
            songPanel.add(tabs);
            tabs.setVisible(true);
            songPanel.add(consoles);
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

    private class Footer extends JPanel {
        Footer() {
            add(Console.getInstance().getInput());
        }
    }

    public void openPage(SongPane page) {
        for(int i = 0; i < tabs.getTabCount(); i++)
            if (tabs.getTitleAt(i).contains(Constants.CUTE_NOTE))
                tabs.setTitleAt(i, tabs.getTitleAt(i).replace(Constants.CUTE_NOTE, ""));
        String name = Constants.CUTE_NOTE + page.getName();
        tabs.add(name, page);
        tabs.setSelectedComponent(page);
        setTitle(prefix + " - " + page.getName());
        sheetMusic();
    }

    public static MainFrame get() {
        return instance;
    }
}
