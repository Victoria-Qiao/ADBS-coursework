package ed.inf.adbs.lightdb;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.*;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * The ScanOperator class is used to scan the files of a base table. 
 * 
 * @author s2041285
 *
 */

public class ScanOperator extends Operator{
	
	public FromItem fromitem; 
	public String tablepath;
	public BufferedReader reader = null;
	public List<Column> colNames;
	String tableName;
	
	/**
	 * ScanOperator constructor: aims to initialize parameters such
	 * as the table name to scan, the path to the table, the corresponding FromItem of the 
	 * table to be read, and the column names of the table.
	 * 
	 * @param dataFile: the directory contains the data
	 * @param item: the table of FromItem type we want to scan
	 * @param catalog: the catalog containing the schema of existing tables.
	 */
	
	public ScanOperator(String dataFile, FromItem item, DbCatalog catalog) {
		tableName = ((Table)item).getName();
		
		this.fromitem = item;
		this.tablepath = dataFile + "/data/" + tableName + ".csv";
		this.colNames = new ArrayList<Column>();

		for (int i=0; i < catalog.schema.get(tableName).size(); i++) {
			Column c = new Column(catalog.schema.get(tableName).get(i));
			this.colNames.add(c);
		}
	}
	
	@Override
	/**
	 * open() method is used to set a new buffer reader for the table to
	 * read tuples in future.
	 */
	public void open() throws IOException {
		this.reader = new BufferedReader(new FileReader(tablepath));
//		System.out.println(this.reader);
	}
	
	@Override
	/**
	 * close() method is used to close the buffer reader for the table if
	 * the scan operation is finished.
	 */
	public void close() throws IOException {
		reader.close();
	}
	
	@Override
	/**
	 * getNextTuple() reads the next line of the file and returns the next
	 * tuple. If it reaches the end of the line, it will return null.
	 */
	public Tuple getNextTuple() throws IOException{
		
		// read the next line
		String line = reader.readLine();
		if (line == null){
			return null; 
		}
		else {
			/**
			 * split the values by ",", and turn the values to integer
			 * and add them into tuple object and add corresponding column 
			 * names (with table name and its alias if have) into tuple object.
			 */
			
			String[] values = line.split(",");
			Tuple tuple = new Tuple();
			for (int i = 0; i < values.length; i++) {
				int a = Integer.parseInt(values[i]);
				tuple.values.add(a);
			}
			tuple.colNames = this.colNames;
			for (Column c:tuple.colNames) {
				Table t = new Table(tableName);
				t.setAlias(fromitem.getAlias());
				c.setTable(t);
			}

			return tuple;
		}
	}
	
	@Override
	/**
	 * reset the ScanOperator to initial state, to let it scan 
	 * from the first line again.
	 */
	public void reset() throws IOException {
		reader.close();
		this.reader = new BufferedReader(new FileReader(tablepath));
	}
	
	@Override
	/**
	 * used to test the implementation of ScanOperator class
	 */
	public void dump() throws IOException {
		Tuple tuple = getNextTuple();	
		while (tuple != null) {
			System.out.println(tuple.values.toString());
			tuple = getNextTuple();
		}
	}
}
