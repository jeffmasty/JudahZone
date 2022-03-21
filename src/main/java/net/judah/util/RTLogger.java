package net.judah.util;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import lombok.RequiredArgsConstructor;

public class RTLogger {

    @RequiredArgsConstructor private static class Log {
        final String clazz;
        final String msg;
        final boolean warn;
    }

    private static final BlockingQueue<Log> debugQueue = new LinkedBlockingQueue<>(); // logging for Realtime threads

    public static void log(Object o, String msg) {
        debugQueue.offer(new Log(o.getClass().getSimpleName(), msg, false));
    }

    public static void warn(Object o, String msg) {
        debugQueue.offer(new Log(o.getClass().getCanonicalName(), msg, true));
    }

    public static void warn(Object o, Throwable e) {
        warn(o, e.getLocalizedMessage());
        e.printStackTrace();
    }

    /** Sleeps between checking the debugQueue for messages */
	public static void monitor() {
		Log dat; 
		try {
			while (true) {
					
	            dat = debugQueue.poll();
	            if (dat == null) {
	            	Thread.sleep(Constants.GUI_REFRESH);
	            	continue;
	            }
	
	            if (dat.warn)
	                Console.info(dat.clazz + " WARN: " + dat.msg);
	            else
	                Console.info(dat.clazz + ": " + dat.msg);
	
			}
		} catch (Exception e) {
		}
	}


}
