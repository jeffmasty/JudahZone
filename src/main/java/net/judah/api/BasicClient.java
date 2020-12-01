package net.judah.api;

import static net.judah.api.Status.*;

import java.util.EnumSet;
import java.util.concurrent.atomic.AtomicReference;

import org.jaudiolibs.jnajack.Jack;
import org.jaudiolibs.jnajack.JackClient;
import org.jaudiolibs.jnajack.JackException;
import org.jaudiolibs.jnajack.JackOptions;
import org.jaudiolibs.jnajack.JackProcessCallback;
import org.jaudiolibs.jnajack.JackShutdownCallback;
import org.jaudiolibs.jnajack.JackStatus;

import lombok.Getter;

public abstract class BasicClient extends Thread implements JackProcessCallback, JackShutdownCallback  {

    static final EnumSet<JackOptions> OPTIONS = EnumSet.of(JackOptions.JackNoStartServer);
    static final EnumSet<JackStatus> STATUS = EnumSet.noneOf(JackStatus.class);

    protected final Jack jack;
    protected JackClient jackclient;
    protected AtomicReference<Status> state = new AtomicReference<>(NEW);
    protected final String clientName;
    @Getter private int buffersize;
	@Getter private int sampleRate;
    
    public BasicClient(String name) throws JackException {
    	clientName = name;
		jack = Jack.getInstance();
    	setPriority(Thread.MAX_PRIORITY);
    }

    /** NOTE: blocks while Midi jack client is initialized */
	public JackClient getJackclient() {
    	while (jackclient == null) { // wait for initialization
    		try {
				Thread.sleep(10);
			} catch (InterruptedException e) { }
    	}
    	return jackclient;
    }

	/* Create a Thread to run our server. All servers require a Thread to run in. */
	@Override public void run() {
        if (!state.compareAndSet(NEW, INITIALISING)) {
            throw new IllegalStateException("" + state.get());
        }
        try {
        	jackclient = jack.openClient(clientName, OPTIONS, STATUS);
        	sampleRate = jackclient.getSampleRate();
        	buffersize = jackclient.getBufferSize();
            initialize();
	        if (state.compareAndSet(INITIALISING, ACTIVE)) {
	                jackclient.setProcessCallback(this);
	                jackclient.onShutdown(this);
	                jackclient.activate();
	                makeConnections();
		            while (state.get() == Status.ACTIVE) {
		                Thread.sleep(100); // @TODO switch to wait()
		            }
	        }
        } catch (InterruptedException e) {
        } catch (JackException e) {
        	System.err.println("JackException " + clientName + " " + e.getMessage());
        }

        close();
        state.set(TERMINATED);
	}

	public void close() {
		if (TERMINATED == state.get()) return;
		state = new AtomicReference<>(CLOSING);
		System.out.println("Closing Jack client " + clientName);
		if (jackclient != null)
	        try {
	            jackclient.close();
	        } catch (Throwable t) {System.err.println(t.getMessage());}
        state = new AtomicReference<>(TERMINATED);
    }

    @Override
	public final void clientShutdown(JackClient client) {
    	System.out.println("---- " + client.getName() + " / " + this.getClass().getCanonicalName() + " disposed by Jack. ----");
    	jackclient = null;
    	close();
    }

	/** Jack Client created but not started. Register ports. */
	protected abstract void initialize() throws JackException;
	/** Jack Client has been started */
	protected abstract void makeConnections() throws JackException;


}
