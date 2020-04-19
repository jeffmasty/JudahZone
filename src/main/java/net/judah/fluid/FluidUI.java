package net.judah.fluid;
import static net.judah.Constants.NL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Properties;

import javax.swing.JTextField;

import org.apache.log4j.Logger;

import net.judah.JudahException;
import net.judah.Tab;

public class FluidUI extends Tab implements ActionListener {
	private static final long serialVersionUID = -3350583314013716111L;
	static final Logger log = Logger.getLogger(FluidUI.class);

	public static final String TAB_NAME = "Fluid";

	private FluidSynth fluid;
	private String history; // TODO add to parent Tab

	public FluidUI(final FluidSynth fluidSynth) {
		assert fluidSynth != null;
		fluid = fluidSynth;
//        setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
//
//        output = new JTextArea();
//        output.setEditable(false);
//        listScroller = new JScrollPane(output);
//        listScroller.setBorder(new EtchedBorder());
//        listScroller.setPreferredSize(new Dimension(680, 350));
//        listScroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
//        listScroller.setAlignmentX(LEFT_ALIGNMENT);
//        add(listScroller);
//
//		JTextField input = new JTextField(70);
//		input.setMaximumSize(new Dimension(660, 75));
//        add(input);
//        input.addActionListener(this);
//        grabFocus();

	}

	@Override
	public void actionPerformed(ActionEvent event) {

		if (event.getSource() instanceof JTextField) {
			JTextField widget = (JTextField)event.getSource();
			String text = widget.getText();
			if (text.isEmpty() && history != null) {
				addText(history);
				fluid.userCommand(text);
			}
			else {
				history = text;
				fluid.userCommand(text);
			}
			widget.setText("");
		}
	}

	//** output to console */
	public void newLine() {
		addText("" + NL);
	}

	@Override
	public String getTabName() {
		return TAB_NAME;
	}

	@Override
	public boolean stop() {
		try {
			fluid.sendCommand(FluidCommand.QUIT);
			fluid = null;
			return true;
		} catch (JudahException j) {
			addText(j.getMessage() + " :" + j.getClass().getSimpleName());
		}
		return false;
	}

	@Override
	public void setProperties(Properties p) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean start() {
		return true;
	}


}
