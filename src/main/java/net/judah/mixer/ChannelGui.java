package net.judah.mixer;

// import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;

import lombok.Getter;
import net.judah.JudahZone;
import net.judah.MainFrame;
import net.judah.api.AudioMode;
import net.judah.effects.Fader;
import net.judah.looper.Recorder;
import net.judah.looper.Sample;
import net.judah.sequencer.Sequencer;
import net.judah.util.Console;
import net.judah.util.Constants;
import net.judah.util.Icons;
import net.judah.util.Knob;
import net.judah.util.MenuBar;

public abstract class ChannelGui extends JPanel {
    final static Dimension LBL_SZ = new Dimension(65, 38);
    public static final Font BOLD = new Font("Arial", Font.BOLD, 11);

    @Getter protected final Channel channel;

    @Getter protected final JToggleButton labelButton;
    protected final Knob volume;
    protected Knob overdrive;
    protected final MixButton onMute;
    protected final List<MixButton> customActions;

    public ChannelGui(Channel channel) {
        this.channel = channel;

        setLayout(new FlowLayout(FlowLayout.CENTER, 0, 0));

        setPreferredSize(new Dimension(MainFrame.WIDTH_MIXER / 2 - 1, LBL_SZ.height + 10));
        setMaximumSize(new Dimension(MainFrame.WIDTH_MIXER / 2 - 1, LBL_SZ.height + 10));

        labelButton = new JToggleButton();
        if (channel.getIcon() == null)
            labelButton.setText(channel.getName());
        else
            labelButton.setIcon(channel.getIcon());

        labelButton.setPreferredSize(LBL_SZ);
        labelButton.setFont(BOLD);
        labelButton.setForeground(Color.DARK_GRAY);
        labelButton.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "none");
        labelButton.getInputMap().put(KeyStroke.getKeyStroke("ENTER"), "none");

        volume = new Knob(vol -> {channel.setVolume(vol);});

        add(labelButton);
        add(volume);

        JPanel boxes = new JPanel();

        onMute = new MixButton(Icons.MUTE, channel);
        onMute.setToolTipText("Mute Audio");
        onMute.addMouseListener(new MouseAdapter() {
            @Override public void mouseReleased(MouseEvent e) {
                channel.setOnMute(!channel.isOnMute());
            }});

        boxes.add(onMute);
        customActions = customInit();
        for (MixButton btn : customActions) {
            assert btn != null;
            btn.addMouseListener(new MouseAdapter() {
                @Override public void mouseReleased(MouseEvent e) {
                    customAction(btn);
                }});
            boxes.add(btn);
        }
        add(boxes);
        if (this instanceof Input) {
            overdrive = new Knob(val -> {overdrive(val);});
            overdrive.setToolTipText("Overdrive");
            add(overdrive);
        }

        labelButton.addActionListener(listener -> {
            if (labelButton.isSelected())
                MainFrame.get().getMixer().setFocus(channel);
        });

        Constants.Gui.attachKeyListener(this, MenuBar.getInstance());

    }

    protected abstract void customAction(MixButton btn);
    protected abstract List<MixButton> customInit();

    public void update() {
        if (!JudahZone.isInitialized()) return;
        volume.setValue(channel.getVolume());
        onMute.update();
        for (MixButton btn : customActions)
            btn.update();
//        if (channel == MixerPane.getInstance().getChannel())
//            MixerPane.getInstance().getEffects().update();
        if (overdrive != null)
            overdrive.setValue(Math.round(channel.getOverdrive().getDrive() * 100));

    }

    public void setVolume(int vol) {
        volume.setValue(vol);
    }

    private void overdrive(int val) {
        channel.getOverdrive().setDrive(val / 100f);
        channel.getOverdrive().setActive(val > 10);
    }

    public static class Input extends ChannelGui {

        public Input(LineIn channel) {
            super(channel);
            setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        }

        @Override protected List<MixButton> customInit() {
            MixButton muteRecord = new MixButton(Icons.MUTE_RECORD, channel);
            muteRecord.setToolTipText("Mute Recording");
            return Arrays.asList(new MixButton[] {muteRecord});
        }

        @Override protected void customAction(MixButton customAction) {
            ((LineIn)channel).setMuteRecord(customAction.isSelected());
        }

    }

    public static class Output extends ChannelGui {
        static final Color GREEN = new Color(63, 255, 63);
        MixButton recordBtn;
        MixButton playBtn;

        public Output(Channel channel) {
            super(channel);
        }

        public void armRecord(boolean active) {
            if (active)
                recordBtn.setBorder(new LineBorder(Color.RED));
            else
                recordBtn.setBorder(BorderFactory.createEmptyBorder());
        }

        void armPlay(boolean active) {
            if (active)
                playBtn.setBorder(new LineBorder(GREEN, 2));
            else
                playBtn.setBorder(BorderFactory.createEmptyBorder());
        }

        @Override
        public void update() {
            armPlay(AudioMode.ARMED == ((Sample)channel).isPlaying());
            armRecord(AudioMode.ARMED == ((Recorder)channel).isRecording());
            if (((Sample)channel).hasRecording())
                setBorder(new LineBorder(GREEN));
            else setBorder(BorderFactory.createEmptyBorder());
            super.update();
        }

        @Override protected List<MixButton> customInit() {
            recordBtn = new MixButton(Icons.MICROPHONE, channel);
            recordBtn.setToolTipText("Record on this loop");
            playBtn = new MixButton(Icons.PLAY, channel);
            playBtn.setToolTipText("Play this loop");
            return Arrays.asList(new MixButton[] {recordBtn, playBtn});
        }

        @Override protected void customAction(MixButton customAction) {
            if (Icons.PLAY.getName().equals(customAction.getName())) {
                Sample sample = (Sample)channel;
                sample.play(AudioMode.RUNNING != sample.isPlaying());
            }
            else if (Icons.MICROPHONE.getName().equals(customAction.getName()) && channel instanceof Recorder) {
                Recorder recorder = (Recorder)channel;
                recorder.record(AudioMode.RUNNING != recorder.isRecording());
                recordBtn.setBorder(BorderFactory.createEmptyBorder());
            }
            else
                Console.info("skipping custom: " + customAction);

        }

    }

    public static class Drums extends Output {
        public Drums(Channel bus) {
            super(bus);
        }
    }

    public static class Master extends ChannelGui {
        MixButton fader, transport;



        public Master(MasterTrack master) {
            super(master);
        }

        @Override
        protected List<MixButton> customInit() {
            fader = new MixButton(Icons.FADE, channel);
            fader.setToolTipText("Fade in or out");
            fader.setSelected(true);
            transport = new MixButton(Icons.PLAY, channel);
            transport.setToolTipText("Transport start/stop");
            return Arrays.asList(new MixButton[] { fader, transport });

        }

        @Override
        protected void customAction(MixButton btn) {
            if (btn == fader) {
                if (channel.getVolume() == 0 || channel.isOnMute()) {
                    JudahZone.getMasterTrack().setOnMute(false);
                    Fader.execute(Fader.fadeIn());
                }
                else
                    Fader.execute(Fader.fadeOut());
            }
            else if (btn == transport)
                Sequencer.transport();
        }

    }

    public static ChannelGui create(Channel ch) {
        if (ch instanceof DrumTrack) return new ChannelGui.Drums(ch);
        if (ch instanceof MasterTrack) return new ChannelGui.Master((MasterTrack)ch);
        if (ch instanceof LineIn) return new ChannelGui.Input((LineIn)ch);
        if (ch instanceof Sample) return new ChannelGui.Output(ch);
        throw new InvalidParameterException(ch.getClass().getCanonicalName());
    }


}

