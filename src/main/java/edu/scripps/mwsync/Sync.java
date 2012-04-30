/* 
 *  Copyright (C) 2012 - Erik Clarke and The Scripps Research Institute
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version. Additionally
 *  this file is subject to the "Classpath" exception.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package edu.scripps.mwsync;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;

import edu.scripps.mwsync.Keys;
import edu.scripps.mwsync.Wiki.Revision;
/**
 * Sync objects represent the operation of pulling changes from a source
 * MediaWiki and pushing them to a target MediaWiki. They are repeatedly 
 * executed by a Sync.Runner object.
 * <br /><br />
 * To instantiate, the client can either inject the dependencies through
 * the constructor, or use the static methods to create a Sync object from
 * options specified in a configuration file.
 * <br /><br />
 * To run, simply call sync.run() from the app's main method. Use `cron` or similar
 * system utilities to launch on a repeating schedule.
 * <br /><br />
 * To create custom rewriting behavior, the client can create an object that
 * implements the Rewriter interface and attach it using mySync.addRewriter(myRewriter).
 * <br /><br />
 * Usage example:
 * <pre>
 * Sync sync = Sync.newFromConfigFile();
 * Rewriter r = new MyCustomRewriter();
 * sync.addRewriter(r);
 * sync.start()
 * </pre>
 * @author eclarke@scripps.edu
 *
 */
public class Sync implements Runnable {

	private static String DEFAULT_PATH = "/usr/local/bin/mwsync/";	// This is overwritten in mwsync.conf  
	private static String DEFAULT_CFG_NAME = "mwsync.conf";			// This is overwritten in mwsync.conf  
	private final static String LAST_CHECK_TIME = "lastcheck.ser";
	// This is the summary string the bot writes on the target when copying pages
	private final static String	SYNC_SUMMARY_FMT = "{[SYNC | user = %s | revid = %s | summary = %s]}";
	
	private final Wiki 		source;		// the source mediawiki
	private final Wiki 		target;		// the target mediawiki
	private final long 		period;		// the time in seconds to check for updates
	private String			root;		// the root folder (to store log and tmp files)
	private Rewriter	 	rewriter;	// a Rewriter-implementing object
	private Properties		properties;	// the Properties defined in the properties file
		
	public Wiki 	getSource() 	{ return this.source; }
	public Wiki 	getTarget() 	{ return this.target; }
	public long 	getPeriod() 	{ return this.period; }
	public Rewriter	getRewriter() 	{ return this.rewriter; }
	public Properties getProperties() { return this.properties; }
	
	/**
	 * Appending a rewriter allows the Sync to modify contents before writing
	 * to the target.
	 * @param r rewriter
	 */
	public void addRewriter(Rewriter r)	{ this.rewriter = r; } 
	
	/**
	 * Create a new Sync between the source and target MediaWiki with a  
	 * given period (in seconds) and a Rewriter module to optionally alter
	 * content before posting it to the target.
	 * @param source Wiki to pull from
	 * @param target Wiki to write to
	 * @param periodInSeconds the frequency of updates
	 * @throws IllegalArgumentException if src/tgt Wikis are null, or period
	 * less than 60s
	 */
	public Sync(Wiki source, Wiki target, long periodInSeconds)
	{
		if (source == null || target == null) {
			throw new IllegalArgumentException("Source/Target Wiki instance cannot be null.");
		}
		this.source = source;
		this.target = target;
				
		if (!(periodInSeconds < 60)) {
			throw new IllegalArgumentException("Minimum sync period is 60 seconds.");
		}
		this.period = periodInSeconds;
		this.rewriter = null;
	}
	
	/**
	 * Returns a new Sync created from options specified in the default configuration
	 * file location (usually '/etc/mwsync/mwsync.conf').
	 * Unless this path actually hosts that file, it's best to use the 
	 * newFromConfigFile(config_filename) constructor instead.
	 * @return default Sync object
	 * @throws FailedLoginException
	 * @throws IOException
	 */
	public static Sync newFromConfigFile() 
			throws FailedLoginException, IOException
	{
		return Sync.newFromConfigFile(DEFAULT_PATH+DEFAULT_CFG_NAME);
	}

	/**
	 * Returns a new Sync created from options specified in the configuration file.
	 * @param conf_filename the configuration filename (absolute path)
	 * @return Sync object created from options in specified config file
	 * @throws FailedLoginException
	 * @throws IOException
	 */
	public static Sync newFromConfigFile(String conf_filename) 
			throws FailedLoginException, IOException 
	{
		Properties props = new Properties();
		props.load(new FileReader(conf_filename));
		
		// Instantiate a new source mediawiki from properties 
		Wiki source = null;
		String sscripts;
		if ((sscripts = props.getProperty(Keys.SRC_SCRIPTS, null)) != null){
			source = new Wiki(props.getProperty(Keys.SRC_LOCATION, null), sscripts);
		} else {
			source = new Wiki(props.getProperty(Keys.SRC_LOCATION, null));
		}
		source.setMaxLag(0);
		source.login(
				props.getProperty(Keys.SRC_USERNAME), 
				props.getProperty(Keys.SRC_PASSWORD).toCharArray());
		
		Wiki target = null;
		String tscripts;
		if ((tscripts = props.getProperty(Keys.TGT_SCRIPTS, null)) != null) {
			target = new Wiki(props.getProperty(Keys.TGT_LOCATION, null), tscripts);
		} else {
			target = new Wiki(props.getProperty(Keys.TGT_LOCATION, null));
		}

		target.setMaxLag(0);
		target.setThrottle(0);
		target.setUsingCompressedRequests(false);
		target.login(
				props.getProperty(Keys.TGT_USERNAME), 
				props.getProperty(Keys.TGT_PASSWORD).toCharArray());
		
		long period = Long.parseLong(props.getProperty(Keys.SYNC_PERIOD));
		String root = props.getProperty(Keys.SYNC_ROOT, DEFAULT_PATH);
		Sync s = new Sync(source, target, period);
		s.properties = props;
		s.root = root;
		return s;
	}
	
	/**
	 * Runs one iteration of a synchronization operation: find the changes
	 * since the last check from the source, and write them to the target.
	 * If the last checked time is not available, it goes back a full day.
	 */
	public void run()
	{
		try {
			Calendar lastChecked;
			try { 
				lastChecked = (Calendar) serializeIn(root+LAST_CHECK_TIME); 
			} catch (FileNotFoundException e) {
				System.out.println("No previous serialization found, backing up 24 hours...");
				lastChecked = Calendar.getInstance();
				lastChecked.add(Calendar.HOUR, -24);
			}
			
			List<String> changed = getRecentChanges(lastChecked);
			System.out.printf("Last checked date: %s\n", lastChecked.getTime().toString());
			System.out.printf("Found %d changed pages...\n", changed.size());
			serializeOut(root+LAST_CHECK_TIME, Calendar.getInstance());
			writeChangedArticles(changed);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Runs one iteration of the sync operation with a specified timeframe:
	 * Find the changes made since a time given by the current time minus the
	 * specified number of hours and write them to the target.
	 * 
	 * @param hoursAgo the hours from the current time to look for changes
	 */
	public void runChangesFromHoursAgo(int hoursAgo)
	{
		Calendar lastChecked = Calendar.getInstance();
		lastChecked.add(Calendar.HOUR, -hoursAgo);
		try {
			List<String> changed = getRecentChanges(lastChecked);
			serializeOut(root+LAST_CHECK_TIME, Calendar.getInstance());
			writeChangedArticles(changed);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	public void runForPages(String... titles)
	{
		writeChangedArticles(Arrays.asList(titles));
	}
	
	
	private List<String> getRecentChanges(Calendar since) 
			throws IOException
	{
		List<Revision> live = source.getChangesFromWatchlist(since, true);
		Set<String> changed = new HashSet<String>(live.size());
		for (Revision rev : live) {
			changed.add(rev.getTitle());
		}
		return new ArrayList<String>(changed);
	}
	
	
	private void writeChangedArticles(List<String> changed)
	{
		for (String title : changed) {
			try {
				String text = source.getPageText(title);
				Revision rev = source.getTopRevision(title);
				String summary = rev.getSummary();
				String author = rev.getUser();
				String revid = rev.getRevid()+"";
				summary = String.format(SYNC_SUMMARY_FMT, author, revid, summary); 
				
				/** 
				 * If we have a rewriter object, feed the text through first
				 * before posting to target
				 */
				if (rewriter != null) {
					text = rewriter.process(text, title, source, target);
				}
				
				target.edit(title, text, summary, false);
			} catch (IOException e) {
				e.printStackTrace();
			} catch (LoginException e) {
				e.printStackTrace();
			}
			
		}
	}
	
	private static Object serializeIn(String filename) throws FileNotFoundException {
		try {
			FileInputStream fIn = new FileInputStream(filename);
			ObjectInputStream in = new ObjectInputStream(fIn);
			Object result = in.readObject();
			return result;
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException(e.getMessage());
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static void serializeOut(String filename, Serializable object) {
		try{
			FileOutputStream fOut = new FileOutputStream(filename);
			ObjectOutputStream out = new ObjectOutputStream(fOut);
			out.writeObject(object);
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
