package edu.scripps.genewiki.mwsync;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

import org.junit.Test;

import edu.scripps.mwsync.Sync;
import edu.scripps.mwsync.Wiki;

public class SyncTest 
{

	/**
	 * Test that the basic instantiation as expected
	 */
	@Test
	public void testInstantiation() 
	{
		MockWiki src = new MockWiki("url1");
		MockWiki tgt = new MockWiki("url2");
		long period = 1000;
		Sync sync = new Sync(src, tgt, period);
		assertEquals(sync.getPeriod(), period);
		assertEquals(sync.getSource(), src);
		assertEquals(sync.getTarget(), tgt);
	}
	
	
	/**
	 * Test instantiation fails if given null Wiki args
	 */
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testFailedInstantiationNullWiki()
	{
		Sync sync = new Sync(null, null, 60);
	}
	
	
	/**
	 * Test instantiation fails if given < 60s for period
	 */
	@SuppressWarnings("unused")
	@Test (expected=IllegalArgumentException.class)
	public void testFailedInstantiationShortPeriod()
	{
		MockWiki src = new MockWiki("url1");
		MockWiki tgt = new MockWiki("url2");
		Sync sync = new Sync(src, tgt, 30);
	}

	
	@Test
	public void testRun() {
		MockWiki src = new MockWiki("url1");
		MockWiki tgt = new MockWiki("url2");
		long period = 1000;
		Sync sync = new Sync(src, tgt, period);
		sync.run();
		assertEquals(tgt.editText, Arrays.asList(
				"Sample Page Text", "Sample Page Text", "Sample Page Text"));
		
	}
	

	
}

class MockWiki extends Wiki 
{

	private static final long serialVersionUID = 1L;
	private Revision rev1, rev2, rev3;
	public List<String> editText;
	
	public MockWiki(String url) {
		rev1 = new Revision(1L, Calendar.getInstance(), "TestRevision1", 
				"TestSummary", "TestUser", false, false);
		rev2 = new Revision(1L, Calendar.getInstance(), "TestRevision2", 
				"TestSummary", "TestUser", false, false);
		rev3 = new Revision(1L, Calendar.getInstance(), "TestRevision3", 
				"TestSummary", "TestUser", false, false);
		editText = new ArrayList<String>();
	}
	
	@Override
	public void login(String username, char[] password) 
	{
		// do nothing
	}
	
	@Override
	public List<Revision> getChangesFromWatchlist(Calendar since, boolean includeBotEdits)
	{
		List<Revision> list = Arrays.asList(rev1, rev2, rev3);
		return list;
	}
	
	@Override
	public String getPageText(String title)
	{
		return "Sample Page Text";
	}
	
	@Override
	public Revision getTopRevision(String title)
	{
		switch(Integer.parseInt(title.substring(12, 13))){
		case 1: return rev1;
		case 2: return rev2;
		case 3: return rev3;
		default: return rev1;
		}
	}
	
	@Override
	public void edit(String title, String text, String summary, boolean minor)
	{
		editText.add(text);
	}
}
