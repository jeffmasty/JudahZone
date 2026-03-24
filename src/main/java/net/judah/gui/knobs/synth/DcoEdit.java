package net.judah.gui.knobs.synth;

import java.awt.Component;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import judahzone.data.Shape;
import judahzone.synth.Dco.DcoJson;
import judahzone.synth.WavSynth;
import judahzone.widgets.CenteredCombo;
import judahzone.widgets.Integers;
import net.judah.gui.widgets.Slider;

public class DcoEdit extends JPanel {

	// +/- 12 vs. fractional cents
	// shape, level, detune coarse, detune fine

	final WavSynth synth;
	final int idx;

	CenteredCombo<Shape> shape = new CenteredCombo<>(Shape.values());
	Slider level = new Slider(0, 100, null);
	Integers coarse = new Integers(-12, 12);
	Slider fine = new Slider(0, 100, null);

	public DcoEdit(WavSynth synth, int idx) {
	    this.synth = synth;
	    this.idx = idx;
	    coarse.setSelectedItem(Integer.valueOf(0));
	    update();

	    shape.addActionListener(l -> synth.setShape(idx, (Shape)shape.getSelectedItem()));
	    level.addChangeListener(l -> synth.setLevel(idx, level.getValue()));
	    coarse.addActionListener(l -> synth.setCoarse(idx, (int) coarse.getSelectedItem()));
	    fine.addChangeListener(l -> synth.setFine(idx, fine.getValue()));

	    // TODO resize
	    setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	    add(shape);
	    add(level);
	    add(coarse);
	    add(fine);

	    // add right-click popup menu for add/remove DCO via shared context menu
	    MouseAdapter ma = new MouseAdapter() {
	        @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
	        @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
	        @Override public void mouseClicked(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
	        private void showMenu(MouseEvent e) {
	            // convert the event point (which may be from a subcomponent) to coordinates relative to the DcoBar
	            Component src = (Component) e.getSource();
	            Point p = SwingUtilities.convertPoint(src, e.getPoint(), DcoEdit.this);
	            DcoMenu menu = new DcoMenu(synth, DcoEdit.this, idx);
	            menu.show(p.x, p.y);
	        }
	    };
	    addMouseListener(ma);
	    shape.addMouseListener(ma);
	    level.addMouseListener(ma);
	    
	    // Right-click on coarse/fine resets them to 0
	    MouseAdapter detuneReset = new MouseAdapter() {
	        @Override public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) resetDetune(e); }
	        @Override public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) resetDetune(e); }
	        @Override public void mouseClicked(MouseEvent e) { if (e.isPopupTrigger()) resetDetune(e); }
	        private void resetDetune(MouseEvent e) {
	            synth.setCoarse(idx, 0);
	            synth.setFine(idx, 50); // 50 = 0 cents on 0-100 knob
	        }
	    };
	    coarse.addMouseListener(detuneReset);
	    fine.addMouseListener(detuneReset);

	}

	public void update() {
	    update(synth.getAlgo()[idx]);
	}

	public void update(DcoJson dco) {
        if (shape.getSelectedItem() != dco.shape())
            shape.setSelectedItem(dco.shape());
        if (level.getValue() != (int) (dco.level() * 100))
            level.setValue((int) (dco.level() * 100));
        if (coarse.getSelectedItem() == null || (int) coarse.getSelectedItem() != dco.coarse())
            coarse.setSelectedItem(dco.coarse());
        if (fine.getValue() != (int) (dco.fine() * 50 + 50))
        	fine.setValue((int) (dco.fine() * 50 + 50));
    }

}