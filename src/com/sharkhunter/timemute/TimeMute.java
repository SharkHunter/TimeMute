package com.sharkhunter.timemute;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Timer;
import java.util.TimerTask;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.BooleanControl;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.Port;
import javax.sound.sampled.Mixer.Info;

public class TimeMute {
	
	private ArrayList<Date> muteTimes;
	
	private void doMute() {
		Port line;
		BooleanControl mCtrl;
		try {
		  Info[] mixers = AudioSystem.getMixerInfo();
		  for(int i=0;i<mixers.length;i++) {
			  Mixer m=AudioSystem.getMixer(mixers[i]);
			  try {
				  line = (Port)m.getLine(Port.Info.SPEAKER);
			  }
			  catch (Exception e1) {
				  continue;
			  }
			  line.open();
			  mCtrl = (BooleanControl) line.getControl(BooleanControl.Type.MUTE);
			  mCtrl.setValue(true);
			  line.close();
			  return;
		  }
		}
		catch (Exception e) {	
		}
	}
	
	public void addTime(String str) throws ParseException {
		GregorianCalendar gc=new GregorianCalendar();
		SimpleDateFormat sdfHour = new SimpleDateFormat("HH:mm");
		//Date now=gc.getTime();
		int y=gc.get(Calendar.YEAR)-1900;
		int m=gc.get(Calendar.MONTH);
		int d=gc.get(Calendar.DATE);
		Date t=sdfHour.parse(str);
		gc.setTime(t);
		gc.set(Calendar.YEAR, y+1900);
		gc.set(Calendar.MONTH, m);
		gc.set(Calendar.DATE, d);
		Date now=new Date(System.currentTimeMillis());
		if(now.after(gc.getTime()))
			gc.add(Calendar.DATE, 1);
		//System.out.println("first sched "+gc.getTime());
		muteTimes.add(gc.getTime());
	}
	
	public void reschedule(final int id) {
		Date d=muteTimes.get(id);
		GregorianCalendar gc=new GregorianCalendar();
		gc.setTime(d);
		gc.add(Calendar.DATE, 1);
		final TimeMute tm=this;
		TimerTask task = new TimerTask() {
		    @Override
		    public void run() {
		    	doMute();
		    	tm.reschedule(id);
		    }
		};
		//System.out.println("resched "+gc.getTime());
		Timer t=new Timer();
		t.schedule(task, gc.getTime());
	}
	
	public void schedule() {
		for(int i=0;i<muteTimes.size();i++) {
			final TimeMute tm=this;
			final int id=i;
			TimerTask task = new TimerTask() {
			    @Override
			    public void run() {
			    	doMute();
			    	tm.reschedule(id);
			    }
			};
			Timer t=new Timer();
			t.schedule(task, muteTimes.get(i));
		}
	}
	
	public TimeMute() {
		muteTimes=new ArrayList<Date>();
	}
	

	public static void main(String[] args) {
		TimeMute tm=new TimeMute();
		try {
			FileInputStream fis=new FileInputStream(args[0]);
			BufferedReader in = new BufferedReader(new InputStreamReader(fis));
			String str;
		    while ((str = in.readLine()) != null) {
		    	if(str.trim().length()==0)
		    		continue;
		    	tm.addTime(str);
		    }
		    fis.close();
		    tm.schedule();
		    for(;;)
		    	Thread.yield();
		}
		catch (Exception e) {
			System.out.println("Error reading file "+e);
		}
	}

}
