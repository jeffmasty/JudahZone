package net.judah.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import net.judah.omni.Icons;

public class Detached extends JFrame {

	public static interface Floating {
		void resized(int w, int h);
	}

	public Detached(Component content, TabZone tabs) {
		super(content.getName());

		setIconImage(Icons.get("icon.png").getImage());
		setLocation(50, 30);
		setSize(Size.SCREEN_SIZE);
		// Pop-up on other monitor, if available
        GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
        if (devices.length > 1) {
            GraphicsDevice screen = devices[0];
            Rectangle screenBounds = screen.getDefaultConfiguration().getBounds();
            setLocation(screenBounds.x, screenBounds.y);
            setSize(screenBounds.width, screenBounds.height);
        }
		setResizable(true);
		getContentPane().setLayout(new GridLayout(1, 1, 0, 0));
        content.setSize(getContentPane().getSize());
		getContentPane().add(content);
		validate();
		setVisible(true);

		tabs.getFrames().add(content);
        addComponentListener(new ComponentAdapter() {
            @Override public void componentResized(ComponentEvent e) {
            	if (content instanceof Floating resize) {
            		Dimension box = getContentPane().getSize();
            		resize.resized(box.width, box.height);
            	}
            }
        });
		addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                tabs.closed(content);
            }});
        setExtendedState(JFrame.MAXIMIZED_BOTH);

	}
}