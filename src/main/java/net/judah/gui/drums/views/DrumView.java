package net.judah.gui.drums.views;

import java.awt.Dimension;
import java.awt.event.MouseEvent;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import judahzone.fx.Gain;
import judahzone.gui.Gui;
import judahzone.util.RTLogger;
import lombok.Getter;
import net.judah.drums.Drum;
import net.judah.drums.DrumUpdate;
import net.judah.drums.DrumUpdate.Mode;
import net.judah.gui.Size;
import net.judah.gui.drums.OscFilter;
import net.judah.gui.drums.OscTuning;
import net.judah.gui.drums.TwoStepEnv;
import net.judah.gui.widgets.Slider;

/** Common UI for drum oscillators: gain, pan, envelope and basic filter controls. */
public abstract class DrumView<T extends Drum> extends JPanel implements ChangeListener {

	public static interface SZ {
	    int SLIDER_MAX = 100;
	    int SLIDER_WIDTH = 128;
	    int LABEL_WIDTH = 70;
	    int VALUE_WIDTH = 50;
	    int SPACING = 8;
	    int ROW_HEIGHT = 25;
	    Dimension VAL_DIM = new Dimension(VALUE_WIDTH, ROW_HEIGHT);
	    Dimension SLIDER_DIM = new Dimension(SLIDER_WIDTH, ROW_HEIGHT);
	    Dimension SLIDER_VERT = new Dimension(ROW_HEIGHT, SLIDER_WIDTH);
	    Dimension LABEL_DIM = new Dimension(LABEL_WIDTH, ROW_HEIGHT);
	}

	// to DrumFrame.SZ
	public static final int ROW_HEIGHT = Size.STD_HEIGHT;
	public static final int SLIDER_WIDTH = 127;
	public static final int DAta_WIDTH = 75; // hz, note/oct, etc

	protected static class LBL extends JLabel {
	    public LBL(String txt) {
	        super(txt, JLabel.CENTER);
	        Gui.resize(this, SZ.LABEL_DIM);
	    }
	}

	public static final class DataLbl extends JLabel {
	    public DataLbl() {
	        super("!", JLabel.CENTER);
	        Gui.resize(this, SZ.VAL_DIM);
	    }
	}

	@Getter protected final T drum;
	protected boolean updating = false;
	private final TwoStepEnv env;
	private final OscFilter filter;
	private final Slider vol;  // vert West of Envelope
	private final Slider pan; // horiz (below/with envelope)
	private final DataLbl volFb = new DataLbl();
	protected final JPanel child = new JPanel(); // custom child class controls

	public DrumView(T drum) {
	    this.drum = drum;

	    vol = new Slider(0, SZ.SLIDER_MAX, this);
	    pan = new Slider(0, SZ.SLIDER_MAX, this);

	    env = new TwoStepEnv(drum);
	    filter = new OscFilter(drum);

	    LBL volLbl = new LBL("Vol");
	    volLbl.addMouseListener(new Gui.Mouse() {
	        @Override public void mouseClicked(MouseEvent e) {
	            preamp(e.getButton() == MouseEvent.BUTTON3);
	        }});
	    JPanel volRow = vertRow(new LBL("Vol"), vol, volFb);

	    JPanel panRow = singleRow(new LBL("L"), pan, new LBL("R"));

	    Box panenv = new Box(BoxLayout.Y_AXIS);
	    panenv.add(env);
	    panenv.add(panRow);

	    Box top = new Box(BoxLayout.X_AXIS);
	    top.add(volRow);
	    top.add(panenv);

	    setFocusable(true);
	    setBorder(Gui.SUBTLE);

	    setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
	    add(Gui.wrap(top));
	    add(filter);
	    add(Gui.wrap(child));
	    add(Box.createVerticalStrut(4));

	}

	public static JPanel singleRow(String lbl, JSlider slider, JLabel value) {
	    return singleRow(new LBL(lbl), slider, value);
	}

	/* Reusable single-row layout. */
	public static JPanel singleRow(JLabel lbl, JSlider slider, JLabel value) {
	    JPanel p = new JPanel();
	    p.setLayout(new BoxLayout(p, BoxLayout.LINE_AXIS));
	    p.add(Box.createHorizontalGlue());
	    p.add(lbl);
	    p.add(Box.createHorizontalStrut(SZ.SPACING));
	    Gui.resize(slider, SZ.SLIDER_DIM);
	    p.add(slider);
	    p.add(Box.createHorizontalStrut(SZ.SPACING));
	    Gui.resize(value, SZ.VAL_DIM);
	    p.add(value);
	    p.add(Box.createHorizontalGlue());
	    return p;
	}

	public static JPanel vertRow(JLabel lbl, JSlider slider, JLabel value) {
	    //    lbl
	    //  Vslider
	    //   value
	    JPanel p = new JPanel();
	    p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
	    p.add(lbl);
	    slider.setOrientation(JSlider.VERTICAL);
	    p.add(Box.createVerticalStrut(2));
	    Gui.resize(slider, SZ.SLIDER_VERT);
	    p.add(slider);
	    p.add(Box.createVerticalStrut(2));
	    Gui.resize(value, SZ.VAL_DIM);
	    p.add(value);

	    return p;
	}

	@Override
	public void stateChanged(ChangeEvent e) {
	    if (updating) return;

	    Object src = e.getSource();
	    if (src == vol)
	        drum.getDrama().set(new DrumUpdate(drum, Mode.BASE, 0, vol.getValue()));
	    else if (src == pan)
	        drum.getDrama().set(new DrumUpdate(drum, Mode.BASE, 1, pan.getValue()));

	}

	// TODO
	public void update(DrumUpdate targeted) {

	}

	protected abstract void updateChild();
	protected abstract OscTuning getTuning();

	public void update() {
	    // Prevent listeners from reacting to programmatic changes.
	    updating = true;
	    try {
	        int volVal = drum.getGain().get(Gain.VOLUME);
	        if (vol.getValue() != volVal)
	            vol.setValue(volVal);
	        int panVal = drum.getGain().get(Gain.PAN);
	        if (pan.getValue() != panVal)
	            pan.setValue(panVal);
	        volFb.setText("" + drum.getGain().get(Gain.VOLUME));

	        filter.update();
	        env.update();
	        if (getTuning() != null)
	            getTuning().update();
	        updateChild();
	    } finally {
	        updating = false;
	    }
	}

	private void preamp(boolean right) {
	    if (!right) {
	        vol.setValue(50);
	        return;
	    }
	    String pre = Gui.inputBox("Preamp");
	    if (pre == null || pre.isBlank())
	        return;
	    try { // mark dirty on JSON?
	        float preamp = Float.parseFloat(pre);
	        drum.getGain().setPreamp(preamp);
	    } catch (NumberFormatException ex) {
	        RTLogger.log(this, "Invalid preamp: " + pre);
	        return;
	    }
	}

	}
// Frame (<Tap>  Pitch)
/* Layout:
 "Vol"
   |
   |
   |  	TwoStepEnv
   |
   |
   |
  100  L <--pan--> R


 OscFilter */           /*
Track:  [x] "gain" volK +/-25 ##.#  "width" bwK #.#
	 ##55  < ---------------------------> 10001
		"loRews"   "gain"  "width"  "hiRes"
		loresK      volK    bwK #.#   hiresK
		##.#       +/-25     2.0?      ##.#
Mid:   			<------ x ----->   (disable: track[x])
	       25.0                          25.0				*/
