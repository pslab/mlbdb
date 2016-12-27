/*
 * Created on Feb 8, 2007
 * Created by Paul Gardner
 * Copyright (C) 2007 Aelitis, All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 * 
 * AELITIS, SAS au capital de 63.529,40 euros
 * 8 Allee Lenotre, La Grille Royale, 78600 Le Mesnil le Roi, France.
 *
 */


package com.aelitis.azureus.util;

import java.util.Map;

import org.gudy.azureus2.core3.util.Debug;

import com.aelitis.azureus.core.AzureusCore;
import com.aelitis.azureus.plugins.magnet.MagnetPlugin;
import com.aelitis.azureus.plugins.magnet.MagnetPluginListener;

import org.gudy.azureus2.plugins.PluginInterface;

public class 
ExternalStimulusHandler 
{
	private static MagnetPlugin		magnet_plugin;
	
	protected static void
	initialise(
		AzureusCore		core )
	{
		PluginInterface pi = core.getPluginManager().getPluginInterfaceByClass( MagnetPlugin.class );
		
		if ( pi != null ){
		
			magnet_plugin = (MagnetPlugin)pi.getPlugin();
			
		}else{
						
			Debug.out( "Failed to resolve magnet plugin" );
		}	
		
		Debug.outNoStack( "ExternalStimulus debug" );
		
		addListener( 
				new ExternalStimulusListener()
				{
					public boolean 
					receive(
						String name, Map values ) 
					{
						System.out.println( "ExternalStimulus: " + name );
						System.out.println("  " + (values == null ? -1 : values.size())
						+ " Values: " + values);
						
						return( name.equals("ExternalStimulus.test"));
					}
				});
	}
	
	public static void
	addListener(
		final ExternalStimulusListener		listener )
	{		
		if ( magnet_plugin != null ){
			
			magnet_plugin.addListener(
				new MagnetPluginListener()
				{
					public boolean
					set(
						String		name,
						Map		values )
					{
						return( listener.receive( name, values ));
					}
				});
		}
	}
}