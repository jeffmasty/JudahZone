package net.judah.looper.old;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Properties;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JToolBar;

import net.judah.Tab;

@SuppressWarnings("serial")
public class LooperUI extends Tab implements ActionListener {

	static LooperUI instance;

	final Looper looper;
	final String tabName;
	final ArrayList<LoopUI> loops = new ArrayList<>();

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public LooperUI(String clientId, Looper looper) {
		super(true); // custom layout
		tabName = clientId;
		this.looper = looper;
		instance = this;


		BoxLayout box = new BoxLayout(this, BoxLayout.Y_AXIS);
		setLayout(box);

        JToolBar toolbar = new JToolBar();
        toolbar.setRollover(true);
        JButton button = new JButton("File");

        toolbar.add(button);
        toolbar.addSeparator();
        toolbar.add(new JButton("Edit"));
        toolbar.add(new JComboBox(new String[] { "Opt-1", "Opt-2", "Opt-3", "Opt-4" }));
        toolbar.setPreferredSize(toolbar.getMinimumSize());
        add(toolbar, BorderLayout.NORTH);


		for (GLoop loop : looper.getLoops()) {
			LoopUI panel = new LoopUI(loop);
			add(panel);
			loops.add(panel);
		}

	}

	public void update() {
    	new Thread () {
    		@Override public void run() {
    			for (LoopUI loop : loops)
    				loop.update();
    		}}.start();


	}

	@Override
	public void actionPerformed(ActionEvent event) {
	}

	@Override
	public String getTabName() {
		return tabName;
	}

	@Override
	public boolean start() {
		return true;
	}

	@Override
	public boolean stop() {
		return false;
	}

	@Override
	public void setProperties(Properties p) {

	}

}
