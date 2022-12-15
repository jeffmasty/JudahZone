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

    private static final BlockingQueue<Log> debugQueue = new LinkedBlockingQueue<>(2048); // logging for Realtime threads

    public static void log(Object o, String msg) {
        debugQueue.offer(new Log(o instanceof String 
        		? o.toString() : o.getClass().getSimpleName(), msg, false));
    }
    public static void log(Class<?> c, String msg) {
    	log(c.getSimpleName(), msg);
    }
    
    public static void warn(Object o, String msg) {
        debugQueue.offer(new Log(o instanceof String 
        		? o.toString() : o.getClass().getSimpleName(), msg, true));
    }

    public static void warn(Class<?> c, String msg) {
    	warn(c.getSimpleName(), msg);
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
	            	Thread.sleep(2 * Constants.GUI_REFRESH);
	            	continue;
	            }
	
	            if (dat.warn)
	                Console.info(dat.clazz + " WARN: " + dat.msg);
	            else
	                Console.info(dat.clazz + ": " + dat.msg);
	
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}


}
