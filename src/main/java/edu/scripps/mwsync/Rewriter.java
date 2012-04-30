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
 * Implement this interface to add custom rewriting behavior to your MediaWiki
 * sync instance.
 * @author eclarke
 *
 */
public interface Rewriter {
	
	/**
	 * This is called before writing the text to the target, and its output overwrites
	 * the content of page title specified. No error handling is done above this; it is 
	 * expected that errors will simply result in the text remaining unchanged.
	 * 
	 * @param text the text to alter
	 * @param title the title of the page being altered
	 * @param sourceApi the source Wiki instance
	 * @param targetApi the target Wiki instance
	 * @return the processed text, or the original text if an error occurred.
	 */
	public String process(String text, String title, Wiki sourceApi, Wiki targetApi);
	
}
