package ed.inf.adbs.lightdb;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * DbCatalog class is to store the schema information for existing
 * relations. It also stores the root directory that stores the relations
 * to extract table values.
 * 
 * @author s2041285
 *
 */

public class DbCatalog {
	//schema map. key is table name, list of string is to store attribute names
	Map<String, List<String>> schema;
	String rootDir;
	
	public DbCatalog(String schemaFile, String rootDir) throws IOException {
		 this.schema = new HashMap<String, List<String>>();
		 this.rootDir = rootDir;
		 
		 /**
		  * to read schema information from schema file and store them in the
		  * schema variable.
		  */
		 BufferedReader reader = new BufferedReader(new FileReader(schemaFile));
		 
		 
		 while (true) {
			 String line = reader.readLine();
			 if (line == null) {
				 break;
			 }
			 String[] s = line.split(" ");
			 this.schema.put(s[0], new ArrayList<String>());
			 for (int i = 1; i < s.length; i++) {
				 this.schema.get(s[0]).add(s[i]);
			 }
		 }
		 reader.close();
	}
}
