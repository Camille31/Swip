
package lila.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Properties;


public class Settings implements Parameter {
	
	private static String m_file;

	private static Properties m_properties;
   
	static {
		m_properties = new Properties();
	}
	
	public final static String DEFAULT = System.getProperty( "user.dir" ) + "/res/lila.properties";
	

	public static void load() throws IOException, FileNotFoundException {
		load( DEFAULT );
	}

	public static void load( String sFile ) throws IOException, FileNotFoundException {
		if( !loaded() )
		{
			m_file = sFile;
			System.out.println( "Settings: loading configuration from file " + m_file );
			m_properties.load( new FileInputStream( new File( m_file ) ) );
			System.out.println( "\nSettings: " + m_properties );
		}
	}

	public static String getString( String sKey ) {
		return m_properties.getProperty( sKey );
	}
   
	public static boolean getBoolean( String sKey ){
		return Boolean.parseBoolean( m_properties.getProperty( sKey ) );
	}
   
	public static int getInteger( String sKey ){
		return Integer.parseInt( m_properties.getProperty( sKey ) );
	}

	public static void set( String sKey, String sValue ) {
		m_properties.setProperty( sKey, sValue );
	}

	public static void save( String sFile ) throws FileNotFoundException, IOException {
		FileOutputStream fos = new FileOutputStream( sFile );
		m_properties.store( fos, "" );
	}
	
	public static String getFile(){
		return m_file;
	}
	
	public static boolean loaded(){
		return ( m_properties.keySet().size() > 0 );
	}
}