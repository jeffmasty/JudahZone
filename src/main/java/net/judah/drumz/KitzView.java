package net.judah.drumz;

import static net.judah.JudahZone.*;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;

import lombok.Getter;
import net.judah.drumz.KitView.Modes;
import net.judah.tracker.JudahBeatz;
import net.judah.util.Constants;
import net.judah.util.Pastels;

@Getter
public class KitzView extends JPanel {
	public static final String NAME = "Kits";
	private static final Border border = BorderFactory.createSoftBevelBorder(BevelBorder.LOWERED);
	private static final Font font = Constants.Gui.BOLD;
	private static KitzView instance;
	
	private ArrayList<ModeButton> modes = new ArrayList<>();
	private final JPanel modesPnl;
	private final HashMap<KitView, KitButton> views = new HashMap<>();
	private final JPanel banner; 
	public static KitzView getInstance() {
		if (instance == null)
			new KitzView();
		return instance;
	}

	private class KitButton extends JLabel {
		KitButton(KitView kit) {
			super(kit.getDrumz().getName(), JLabel.CENTER);
			setBorder(border);
			setFont(font);
			setOpaque(true);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					KitView.setCurrent(kit);
					updateViews();
				}
			});
		}
	}
	
	private class ModeButton extends JLabel {
		private final Modes mode;
		ModeButton(Modes mode, JPanel pnl) {
			super(mode.name(), JLabel.CENTER);
			this.mode = mode;
			setBorder(border);
			setOpaque(true);
			addMouseListener(new MouseAdapter() {
				@Override public void mouseClicked(MouseEvent e) {
					KitView.setMode(mode);
					for (KitView view : views.keySet())
						view.updateMode();
					// updateMode();
					updateButtons();
				}
			});
			pnl.add(this);
		}
	}
	
	private KitzView() {
		instance = this;
		DrumMachine drumz = getDrumMachine();
		JudahBeatz beats = getBeats();
		KitView drum1 = new KitView(drumz.getDrum1(), beats.getDrum1());
		KitView drum2 = new KitView(drumz.getDrum2(), beats.getDrum2());
		KitView hats = new KitView(drumz.getHats(), beats.getHats());
		KitView fills = new KitView(drumz.getFills(), beats.getFills());
		
		KitButton btn1 = new KitButton(drum1);
		KitButton btn2 = new KitButton(drum2);
		KitButton btnhat = new KitButton(hats);
		KitButton btnfill = new KitButton(fills);
		
		views.put(drum1, btn1);
		views.put(drum2, btn2);
		views.put(hats, btnhat);
		views.put(fills, btnfill);
		KitView.setCurrent(hats);

		banner = new JPanel();
		banner.setLayout(new BoxLayout(banner, BoxLayout.LINE_AXIS));
		banner.setOpaque(true);
		
		modesPnl = new JPanel(new GridLayout(1, Modes.values().length, 2, 1));
		modesPnl.setOpaque(true);
		for (Modes mode : Modes.values()) 
			modes.add(new ModeButton(mode, modesPnl));
		
		JPanel kitz = new JPanel(new GridLayout(1, views.size(), 2, 1));
		kitz.add(btn1); kitz.add(btn2); kitz.add(btnhat); kitz.add(btnfill);
		
		banner.add(new JLabel(" Kit Knobs: "));
		banner.add(kitz);
		
		banner.add(new JLabel(" Knob Mode: "));
		banner.add(modesPnl);
		
		JPanel overview = new JPanel();
		overview.setLayout(new GridLayout(2, 2, 10, 10));
		overview.add(drum1); overview.add(drum2);
		overview.add(hats);  overview.add(fills);
		setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

		add(Constants.wrap(overview));
		add(banner);

		// new GMKitView(JudahZone.getInstruments().getFluid();
		// private final SamplerView samples = new SamplerView(getSampler());
		// add(Constants.wrap(samples));
		updateViews();
		updateButtons();

	}
	
	private void updateButtons() {
		for (Component c : modesPnl.getComponents()) {
			if (c instanceof ModeButton) {
				ModeButton btn = (ModeButton)c;
				btn.setBackground(btn.mode == KitView.getMode() ? Pastels.GREEN : null);
			}
		}
	}
	private void updateViews() {
		for (KitView key : views.keySet()) {
			boolean highlight = key == KitView.getCurrent();
			views.get(key).setBackground(highlight ? Pastels.BLUE : null);
		}
	}

	
	public void update(DrumSample s) {
		for (KitView v : views.keySet())
			for (DrumSample drum : v.getDrumz().getSamples())
				if (drum.equals(s))
					v.update(s);
	}	

	public void update(DrumKit kit) {
		for (KitView v : views.keySet())
			if (v.getDrumz() == kit)
				v.update();
	}
	
}
