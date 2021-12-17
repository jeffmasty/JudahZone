package net.judah.beatbox;

import static net.judah.util.Size.HEIGHT_TABS;
import static net.judah.util.Size.WIDTH_CLOCK;
import static net.judah.util.Size.WIDTH_KIT;
import static net.judah.util.Size.WIDTH_SONG;

import java.awt.Rectangle;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JDesktopPane;

import lombok.Getter;
import net.judah.api.TimeListener;
import net.judah.clock.JudahClock;
import net.judah.util.Console;
import net.judah.util.Pastels;
import net.judah.util.Update;

public class BeatsView extends JDesktopPane implements TimeListener, Pastels {

	@Getter private static final ConcurrentLinkedQueue<Update>
            queue = new ConcurrentLinkedQueue<>();
	
    private static final int height = HEIGHT_TABS - 30;
    private static final Rectangle BTN_BOUNDS = new Rectangle(0, 0, WIDTH_CLOCK, height);
    private static final Rectangle KIT_BOUNDS = new Rectangle(WIDTH_CLOCK, 0, WIDTH_KIT, height);
    private static final Rectangle GRID_BOUNDS = new Rectangle(WIDTH_KIT + WIDTH_CLOCK, 0,
            WIDTH_SONG - WIDTH_KIT - WIDTH_CLOCK, height);

    @Getter private static BeatsView instance;
    private static final JudahClock clock = JudahClock.getInstance();

    @Getter private final Buttons buttons;
    @Getter private final GridView grid2;
    @Getter private KitPanel kitPanel;

    public BeatsView() {

        setLayout(null);
        BeatBox sequencer = clock.getSequencer(9);
        buttons = new Buttons(sequencer);
        grid2 = new GridView(GRID_BOUNDS);

        buttons.setBounds(BTN_BOUNDS);
        grid2.setBounds(GRID_BOUNDS);

        add(grid2);
        add(buttons);

        initialize(sequencer);

        clock.addListener(this);
        instance = this;
        
        new Thread(() -> {
        	while(true) if (queue.peek() != null) queue.poll().run();}).start();
        
    }

    public void initialize(BeatBox sequencer) {
        new Thread(() -> {
            Console.info("Initialize channel " + sequencer.getMidiChannel() + " sequence");
            buttons.setSequencer(sequencer);

            if (kitPanel != null)
                remove(kitPanel);
            kitPanel = sequencer.getCurrent().createTracks();
            kitPanel.setBounds(KIT_BOUNDS);
            add(kitPanel);

            doLayout();
            grid2.repaint();


        }).start();
    }

    @Override
    public void update(Property prop, Object value) {
        // TODO
    }

    public void updateKit(BeatBox seq, KitPanel tracks) {
        new Thread( () -> {
            grid2.repaint();
            remove(kitPanel);
            kitPanel = tracks;
            kitPanel.setBounds(KIT_BOUNDS);
            add(kitPanel);
            buttons.setSequencer(seq);
        }).start();
    }

    public void update() {
        grid2.repaint();
        buttons.update();
    }

    /**@return the default step count in order to gate notes */
    public int getNoteOff() {
        return (int)buttons.getGate().getSelectedItem();
    }
    
    public static int channelCount() {
    	return instance.buttons.getChannelCombo().getItemCount();
    }
            
    public static void changeChannel(boolean up) {
    	int channel = getChannel() + (up ? 1 : -1);
    	int total = channelCount();
    	if (channel == total)
    		channel = 0;
    	if (channel == -1)
    		channel = total - 1;
    	changeChannel(channel);
    }
    
    public static void changeChannel(int ch) {
    	instance.initialize(clock.getSequencer(ch));
    }

    public static int getChannel() {
    	return instance.buttons.getChannelCombo().getSelectedIndex();
    }
    
    /**@return currently viewed sequencer/channel */
    public static BeatBox getSequencer() {
        return clock.getSequencer(getChannel());
    }

    /**@return currently viewed pattern data*/
    public static Grid getCurrent() {
        return clock.getSequencer(getChannel()).getCurrent();
    }

    
}
