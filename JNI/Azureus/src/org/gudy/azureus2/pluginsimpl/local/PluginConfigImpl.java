/*
 * File    : PluginConfigImpl.java
 * Created : 10 nov. 2003
 * By      : epall
 * 
 * Azureus - a Java Bittorrent client
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details ( see the LICENSE file ).
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package org.gudy.azureus2.pluginsimpl.local;

import java.io.File;
import java.util.*;

import org.gudy.azureus2.core3.config.*;
import org.gudy.azureus2.core3.config.impl.*;
import org.gudy.azureus2.core3.util.Debug;
import org.gudy.azureus2.core3.util.FileUtil;

import org.gudy.azureus2.plugins.PluginConfig;
import org.gudy.azureus2.plugins.PluginConfigListener;
import org.gudy.azureus2.plugins.PluginInterface;
import org.gudy.azureus2.plugins.config.ConfigParameter;
import org.gudy.azureus2.pluginsimpl.local.config.*;

import com.aelitis.net.magneturi.MagnetURIHandler;

/**
 * @author Eric Allen
 *
 * To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
public class 
PluginConfigImpl
	implements PluginConfig 
{

	protected static Map	external_to_internal_key_map = new HashMap();
	
	static{
		
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC, 		CORE_PARAM_INT_MAX_UPLOAD_SPEED_KBYTES_PER_SEC );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOAD_SPEED_SEEDING_KBYTES_PER_SEC, 		"Max Upload Speed Seeding KBs" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC, 	CORE_PARAM_INT_MAX_DOWNLOAD_SPEED_KBYTES_PER_SEC );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_CONNECTIONS_GLOBAL, 				"Max.Peer.Connections.Total" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_CONNECTIONS_PER_TORRENT, 			"Max.Peer.Connections.Per.Torrent" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_DOWNLOADS, 						"max downloads" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_ACTIVE, 							"max active torrents" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_ACTIVE_SEEDING, 							"StartStopManager_iMaxActiveTorrentsWhenSeeding" );
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOADS, "Max Uploads");
		external_to_internal_key_map.put( CORE_PARAM_INT_MAX_UPLOADS_SEEDING, "Max Uploads Seeding");
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_MAX_UPLOAD_SPEED_SEEDING, "enable.seedingonly.upload.rate");
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_MAX_ACTIVE_SEEDING, "StartStopManager_bMaxActiveTorrentsWhenSeedingEnabled");
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_AUTO_SPEED_ON, "Auto Upload Speed Enabled");
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_SOCKS_PROXY_NO_INWARD_CONNECTION, 	"Proxy.Data.SOCKS.inform" );
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_NEW_SEEDS_START_AT_TOP, 			CORE_PARAM_BOOLEAN_NEW_SEEDS_START_AT_TOP );
		external_to_internal_key_map.put( CORE_PARAM_STRING_LOCAL_BIND_IP, 						"Bind IP" );
		external_to_internal_key_map.put( CORE_PARAM_BOOLEAN_FRIENDLY_HASH_CHECKING, 			"diskmanager.friendly.hashchecking" );
		
		// Note: Not in PluginConfig.java because it's an UI option and
		//       not applicable to all UIs
		// TODO: Add a smarter way
		
		// Following parameters can be set directly (we don't have an alias for these values).
		String[] passthrough_params = new String[] {
				"Open MyTorrents", "IconBar.enabled", "Wizard Completed",
				"welcome.version.lastshown",
		};
		
		for (int i=0; i<passthrough_params.length; i++) {
			external_to_internal_key_map.put(passthrough_params[i], passthrough_params[i]);
		}
	}

	private static Map		fake_values_when_disabled;
	private static int		fake_values_ref_count;
	
	public static void
	setEnablePluginCoreConfigChange(
		boolean		enabled )
	{
		synchronized( PluginConfigImpl.class ){

			if ( enabled ){
				
				fake_values_ref_count--;
				
				if ( fake_values_ref_count == 0 ){
					
						// TODO: we could try and recover the faked values at this point
					
					fake_values_when_disabled = null;
				}				

			}else{
				fake_values_ref_count++;

				if ( fake_values_ref_count == 1 ){
					
					fake_values_when_disabled = new HashMap();
				}
			}
		}
	}
	
	private static Object
	getFakeValueWhenDisabled(
		String	key,
		String	name )
	{
		if ( name.startsWith(key)){
			return( null );
		}
		
		synchronized( PluginConfigImpl.class ){
			
			if ( fake_values_when_disabled != null ){
				
				return( fake_values_when_disabled.get( name ));
			}
		}
		
		return( null );
	}
	
	private static boolean
	setFakeValueWhenDisabled(
		String	key,
		String	name,
		Object	value )
	{
		if ( name.startsWith(key)){
			return( false );
		}
		
		synchronized( PluginConfigImpl.class ){
			
			if ( fake_values_when_disabled != null ){
				
				fake_values_when_disabled.put( name, value );
				
				return( true );
			}
		}
		
		return( false );
	}
	
	private PluginInterface	plugin_interface;
	private String 			key;
  
	public 
	PluginConfigImpl(
		PluginInterface		_plugin_interface,
		String			 	_key ) 
	{
		plugin_interface	= _plugin_interface;
		
		key = _key + ".";
	}

	public boolean
	isNewInstall()
	{
		return( COConfigurationManager.isNewInstall());
	}
	
	public String
	getPluginConfigKeyPrefix()
	{
		return( key );
	}
	
	public void setPluginConfigKeyPrefix(String _key) {
		if (_key.length() > 0 || plugin_interface.isBuiltIn()) {
			key = _key;
		} else {
			throw (new RuntimeException("Can't set Plugin Config Key Prefix to '"
					+ _key + "'"));
		}
	}
	
	//
	//
	// Helper methods which do everything required to get a parameter value.
	//
	//
	private boolean getBooleanParameter(String name, boolean _default, boolean map_name, boolean set_default) {
		Object	obj = getFakeValueWhenDisabled(key,name);
		if ( obj != null ){
			return(((Boolean)obj).booleanValue());
		}
		if (map_name) {name = mapKeyName(name, false);}
		if (set_default) {COConfigurationManager.setBooleanDefault(name, _default);}
		else if (!hasParameter(name)) {return _default;}
		return COConfigurationManager.getBooleanParameter(name);
	}
	
	private byte[] getByteParameter(String name, byte[] _default, boolean map_name, boolean set_default) {
		Object	obj = getFakeValueWhenDisabled(key,name);
		if ( obj != null ){
			return((byte[])obj);
		}
		if (map_name) {name = mapKeyName(name, false);}
		if (set_default) {COConfigurationManager.setByteDefault(name, _default);}
		else if (!hasParameter(name)) {return _default;}
		return COConfigurationManager.getByteParameter(name);
	}
	
	private float getFloatParameter(String name, float _default, boolean map_name, boolean set_default) {
		Object	obj = getFakeValueWhenDisabled(key,name);
		if ( obj != null ){
			return(((Float)obj).floatValue());
		}
		if (map_name) {name = mapKeyName(name, false);}
		if (set_default) {COConfigurationManager.setFloatDefault(name, _default);}
		else if (!hasParameter(name)) {return _default;}
		return COConfigurationManager.getFloatParameter(name);
	}
	
	private int getIntParameter(String name, int _default, boolean map_name, boolean set_default) {
		Object	obj = getFakeValueWhenDisabled(key,name);
		if ( obj != null ){
			return(((Long)obj).intValue());
		}
		if (map_name) {name = mapKeyName(name, false);}
		if (set_default) {COConfigurationManager.setIntDefault(name, _default);}
		else if (!hasParameter(name)) {return _default;}
		return COConfigurationManager.getIntParameter(name);
	}

	private long getLongParameter(String name, long _default, boolean map_name, boolean set_default) {
		Object	obj = getFakeValueWhenDisabled(key,name);
		if ( obj != null ){
			return(((Long)obj).longValue());
		}
		if (map_name) {name = mapKeyName(name, false);}
		if (set_default) {COConfigurationManager.setLongDefault(name, _default);}
		else if (!hasParameter(name)) {return _default;}
		return COConfigurationManager.getLongParameter(name);
	}
	
	private String getStringParameter(String name, String _default, boolean map_name, boolean set_default) {
		Object	obj = getFakeValueWhenDisabled(key,name);
		if ( obj != null ){
			return((String)obj);
		}
		if (map_name) {name = mapKeyName(name, false);}
		if (set_default) {COConfigurationManager.setStringDefault(name, _default);}
		else if (!hasParameter(name)) {return _default;}
		return COConfigurationManager.getStringParameter(name);
	}

	//
	//
	// Variants of the methods above, but which use the values from ConfigurationDefaults.
	//
	//
	private boolean getDefaultedBooleanParameter(String name, boolean map_name) {
		Object	obj = getFakeValueWhenDisabled(key,name);
		if ( obj != null ){
			return(((Boolean)obj).booleanValue());
		}
		return getBooleanParameter(name, ConfigurationDefaults.def_boolean == 1, map_name, false);
	}
	
	private byte[] getDefaultedByteParameter(String name, boolean map_name) {
		return getByteParameter(name, ConfigurationDefaults.def_bytes, map_name, false);
	}
	
	private float getDefaultedFloatParameter(String name, boolean map_name) {
		return getFloatParameter(name, ConfigurationDefaults.def_float, map_name, false);
	}
	
	private int getDefaultedIntParameter(String name, boolean map_name) {
		return getIntParameter(name, ConfigurationDefaults.def_int, map_name, false);
	}

	private long getDefaultedLongParameter(String name, boolean map_name) {
		return getLongParameter(name, ConfigurationDefaults.def_long, map_name, false);
	}
	
	private String getDefaultedStringParameter(String name, boolean map_name) {
		return getStringParameter(name, ConfigurationDefaults.def_String, map_name, false);
	}


	
	//
	//
	// Core get parameter methods.
	//
	//

	public boolean getBooleanParameter(String name) {
		return getDefaultedBooleanParameter(name, true);
	}

	public boolean getBooleanParameter(String name, boolean default_value) {
		return getBooleanParameter(name, default_value, true, false);
	}
	
	public byte[] getByteParameter(String name) {
		return getDefaultedByteParameter(name, true);
	}

	public byte[] getByteParameter(String name, byte[] default_value) {
		return getByteParameter(name, default_value, true, false);
	}
	
	public float getFloatParameter(String name) {
		return getDefaultedFloatParameter(name, true);
	}

	public float getFloatParameter(String name, float default_value) {
		return getFloatParameter(name, default_value, true, false);
	}

	public int getIntParameter(String name) {
		return getDefaultedIntParameter(name, true);
	}

	public int getIntParameter(String name, int default_value) {
		return getIntParameter(name, default_value, true, false);
	}

	public long getLongParameter(String name) {
		return getDefaultedLongParameter(name, true);
	}

	public long getLongParameter(String name, long default_value) {
		return getLongParameter(name, default_value, true, false);
	}
	
	public String getStringParameter(String name) {
		return getDefaultedStringParameter(name, true);
	}

    public String getStringParameter(String name, String default_value) {
    	return getStringParameter(name, default_value, true, false);
    }
    
	//
	//
	// Core set parameter methods.
	//
	//
    public void setBooleanParameter(String name, boolean value) {
		if ( setFakeValueWhenDisabled(key, name, new Boolean( value))){
			return;
		}
    	COConfigurationManager.setParameter(mapKeyName(name, true), value);
    }

    public void setByteParameter(String name, byte[] value) {
		if ( setFakeValueWhenDisabled(key, name, value )){
			return;
		}
    	COConfigurationManager.setParameter(mapKeyName(name, true), value);
    }

    public void setFloatParameter(String name, float value) {
		if ( setFakeValueWhenDisabled(key, name, new Float( value))){
			return;
		}
    	COConfigurationManager.setParameter(mapKeyName(name, true), value);
    }

    public void setIntParameter(String name, int value) {
		if ( setFakeValueWhenDisabled(key, name, new Long( value))){
			return;
		}
    	COConfigurationManager.setParameter(mapKeyName(name, true), value);
    }

    public void setLongParameter(String name, long value) {
		if ( setFakeValueWhenDisabled(key, name, new Long( value))){
			return;
		}
    	COConfigurationManager.setParameter(mapKeyName(name, true), value);
    }

    public void setStringParameter(String name, String value) {
		if ( setFakeValueWhenDisabled(key, name, value)){
			return;
		}
    	COConfigurationManager.setParameter(mapKeyName(name, true), value);
    }

    
	//
	//
	// Plugin get parameter methods.
	//
	//
	public boolean getPluginBooleanParameter(String name) {
		return getDefaultedBooleanParameter(this.key + name, false);
	}

	public boolean getPluginBooleanParameter(String name, boolean default_value) {
		return getBooleanParameter(this.key + name, default_value, false, true);
	}
	
	public byte[] getPluginByteParameter(String name) {
		return getDefaultedByteParameter(this.key + name, false);
	}

	public byte[] getPluginByteParameter(String name, byte[] default_value) {
		return getByteParameter(this.key + name, default_value, false, true);
	}
	
	public float getPluginFloatParameter(String name) {
		return getDefaultedFloatParameter(this.key + name, false);
	}

	public float getPluginFloatParameter(String name, float default_value) {
		return getFloatParameter(this.key + name, default_value, false, true);
	}

	public int getPluginIntParameter(String name) {
		return getDefaultedIntParameter(this.key + name, false);
	}

	public int getPluginIntParameter(String name, int default_value) {
		return getIntParameter(this.key + name, default_value, false, true);
	}

	public long getPluginLongParameter(String name) {
		return getDefaultedLongParameter(this.key + name, false);
	}

	public long getPluginLongParameter(String name, long default_value) {
		return getLongParameter(this.key + name, default_value, false, true);
	}
	
	public String getPluginStringParameter(String name) {
		return getDefaultedStringParameter(this.key + name, false);
	}

    public String getPluginStringParameter(String name, String default_value) {
    	return getStringParameter(this.key + name, default_value, false, true);
    }
    
	//
	//
	// Plugin set parameter methods.
	//
	//
    public void setPluginParameter(String name, boolean value) {
    	COConfigurationManager.setParameter(this.key + name, value);
    }

    public void setPluginParameter(String name, byte[] value) {
    	COConfigurationManager.setParameter(this.key + name, value);
    }

    public void setPluginParameter(String name, float value) {
    	COConfigurationManager.setParameter(this.key + name, value);
    }

    public void setPluginParameter(String name, int value) {
    	COConfigurationManager.setParameter(this.key + name, value);
    }

    public void setPluginParameter(String name, long value) {
    	COConfigurationManager.setParameter(this.key + name, value);
    }

    public void setPluginParameter(String name, String value) {
    	COConfigurationManager.setParameter(this.key + name, value);
    }
    
	//
	//
	// Core "unsafe" get parameter methods.
	//
	//

	public boolean getUnsafeBooleanParameter(String name) {
		return getDefaultedBooleanParameter(name, false);
	}

	public boolean getUnsafeBooleanParameter(String name, boolean default_value) {
		return getBooleanParameter(name, default_value, false, false);
	}
	
	public byte[] getUnsafeByteParameter(String name) {
		return getDefaultedByteParameter(name, false);
	}

	public byte[] getUnsafeByteParameter(String name, byte[] default_value) {
		return getByteParameter(name, default_value, false, false);
	}
	
	public float getUnsafeFloatParameter(String name) {
		return getDefaultedFloatParameter(name, false);
	}

	public float getUnsafeFloatParameter(String name, float default_value) {
		return getFloatParameter(name, default_value, false, false);
	}

	public int getUnsafeIntParameter(String name) {
		return getDefaultedIntParameter(name, false);
	}

	public int getUnsafeIntParameter(String name, int default_value) {
		return getIntParameter(name, default_value, false, false);
	}
	
	public long getUnsafeLongParameter(String name) {
		return getDefaultedLongParameter(name, false);
	}

	public long getUnsafeLongParameter(String name, long default_value) {
		return getLongParameter(name, default_value, false, false);
	}
	
	public String getUnsafeStringParameter(String name) {
		return getDefaultedStringParameter(name, false);
	}

    public String getUnsafeStringParameter(String name, String default_value) {
    	return getStringParameter(name, default_value, false, false);
    }

	//
	//
	// Core "unsafe" set parameter methods.
	//
	//
    public void setUnsafeBooleanParameter(String name, boolean value) {
		if ( setFakeValueWhenDisabled(key, name, new Boolean( value))){
			return;
		}
    	COConfigurationManager.setParameter(name, value);
    }

    public void setUnsafeByteParameter(String name, byte[] value) {
		if ( setFakeValueWhenDisabled(key, name, value)){
			return;
		}
    	COConfigurationManager.setParameter(name, value);
    }

    public void setUnsafeFloatParameter(String name, float value) {
		if ( setFakeValueWhenDisabled(key, name, new Float( value))){
			return;
		}
    	COConfigurationManager.setParameter(name, value);
    }

    public void setUnsafeIntParameter(String name, int value) {
		if ( setFakeValueWhenDisabled(key, name, new Long( value))){
			return;
		}
    	COConfigurationManager.setParameter(name, value);
    }

    public void setUnsafeLongParameter(String name, long value) {
		if ( setFakeValueWhenDisabled(key, name, new Long( value))){
			return;
		}
    	COConfigurationManager.setParameter(name, value);
    }

    public void setUnsafeStringParameter(String name, String value) {
		if ( setFakeValueWhenDisabled(key, name, value )){
			return;
		}
    	COConfigurationManager.setParameter(name, value);
    }
    
    //
    //
    // Get/set plugin list/map methods.
    //
    //
    
	 public List getPluginListParameter(String key, List default_value) {
		return COConfigurationManager.getListParameter(this.key+key, default_value); 
	 }
	 
	 public void setPluginListParameter(String key, List value) {
		 COConfigurationManager.setParameter(this.key+key, value);
	 }

	 public Map getPluginMapParameter(String key, Map default_value) {
		return COConfigurationManager.getMapParameter(this.key+key, default_value); 
	 }
	 
	 public void setPluginMapParameter(String key, Map value) {
		 COConfigurationManager.setParameter(this.key+key, value);
	 }
	 
	 public void setPluginParameter(String key, int value, boolean global) {
		COConfigurationManager.setParameter(this.key+key, value);
		if (global) {
			MagnetURIHandler.getSingleton().addInfo(this.key+key, value);
		}
	 }

	public ConfigParameter
	getParameter(
		String		key )
	{
		return( new ConfigParameterImpl( mapKeyName(key, false)));
	}
	
	public ConfigParameter
	getPluginParameter(
	  	String		key )
	{
		return( new ConfigParameterImpl( this.key+key ));
	}
	
	public boolean removePluginParameter(String key) {
		return COConfigurationManager.removeParameter(this.key + key);
	}
	


	  public Map
	  getUnsafeParameterList()
	  {
		  Set params = COConfigurationManager.getAllowedParameters();
		  
		  Iterator	it = params.iterator();
		  
		  Map	result = new HashMap();
		  
		  while( it.hasNext()){
			  
			  try{
				  String	name = (String)it.next();
				  
				  Object val = COConfigurationManager.getParameter( name );
				  
				  if ( val instanceof String || val instanceof Long ){
					  
				  }else if ( val instanceof byte[]){
					  
					  val = new String((byte[])val, "UTF-8" );
					  
				  }else if ( val instanceof Integer ){
					  
					  val = new Long(((Integer)val).intValue());
	
				  }else if ( val instanceof List ){
					  
					  val = null;
					  
				  }else if ( val instanceof Map ){
					  
					  val = null;
					  
				  }else if ( val instanceof Boolean ){
					  
					  val = new Long(((Boolean)val).booleanValue()?1:0);
					  
				  }else if ( val instanceof Float || val instanceof Double ){
					  
					  val = val.toString();
				  }
				  
				  if ( val != null ){
					 
					  result.put( name, val );
				  }
			  }catch( Throwable e ){
				  
				  Debug.printStackTrace(e);
			  }
		  }
		  
		  return( result );
	  }
	
	public void
	save()
	{
		COConfigurationManager.save();
	}
		
	public File
	getPluginUserFile(
		String	name )
	{
		
		String	dir = plugin_interface.getUtilities().getAzureusUserDir();
		
		File	file = new File( dir, "plugins" );

		String	p_dir = plugin_interface.getPluginDirectoryName();
		
		if ( p_dir.length() != 0 ){
			
			int	lp = p_dir.lastIndexOf(File.separatorChar);
			
			if ( lp != -1 ){
				
				p_dir = p_dir.substring(lp+1);
			}
			
			file = new File( file, p_dir );
			
		}else{
			
			String	id = plugin_interface.getPluginID();
			
			if ( id.length() > 0 && !id.equals( PluginInitializer.INTERNAL_PLUGIN_ID )){
			
				file = new File( file, id );
				
			}else{
				
				throw( new RuntimeException( "Plugin was not loaded from a directory" ));
			}
		}
	
		
		FileUtil.mkdirs(file);
		
		return( new File( file, name ));
	}
	
	public void
	addListener(
		final PluginConfigListener	l )
	{
		COConfigurationManager.addListener(
			new COConfigurationListener()
			{
				public void
				configurationSaved()
				{
					l.configSaved();
				}
			});
	}

	private String mapKeyName(String key, boolean for_set) {
		String result = (String)external_to_internal_key_map.get(key);
		if (result == null) {
			if (for_set) {
				throw new RuntimeException("No permission to set the value of core parameter: " + key);
			}
			else {
				return key;
			}
		}
		return result;
	}
	
	public boolean hasParameter(String param_name) {
		// Don't see any reason why a plugin should care whether it is looking
		// at a system default setting or not, so we'll do an implicit check.
		return COConfigurationManager.hasParameter(param_name, false);
	}
	
	public boolean hasPluginParameter(String param_name) {
		// We should not have default settings for plugins in configuration
		// defaults, so we don't bother doing an implicit check.
		return COConfigurationManager.hasParameter(this.key + param_name, true);
	}
	
}