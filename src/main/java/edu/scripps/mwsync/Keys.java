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

/**
 * Provides mappings to the property fields.
 * <br>
 * For example:<pre>
 * Properties p = new Properties(...);
 * p.getProperty(Key.TGT_USERNAME);</pre>
 * <br>
 * This allows the property field names to change
 * and only require updating in one place.
 * 
 * @author eclarke@scripps.edu
 *
 */
public final class Keys {
	
	static final String TGT_LOCATION = "target.location";
	static final String TGT_SCRIPTS  = "target.scripts";
	static final String TGT_USERNAME = "target.username";
	static final String TGT_PASSWORD = "target.password";

	static final String SRC_LOCATION = "source.location";
	static final String SRC_SCRIPTS  = "source.scripts";
	static final String SRC_USERNAME = "source.username";
	static final String SRC_PASSWORD = "source.password";

	static final String SYNC_PERIOD	= "sync.period";
	static final String SYNC_ROOT	= "sync.root";
	
}
