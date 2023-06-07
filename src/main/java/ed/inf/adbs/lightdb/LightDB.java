package ed.inf.adbs.lightdb;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.util.List;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * Lightweight in-memory database system
 *
 */
public class LightDB {

	public static void main(String[] args) {

		if (args.length != 3) {
			System.err.println("Usage: LightDB database_dir input_file output_file");
			return;
		}

		String databaseDir = args[0];
		String inputFile = args[1];
		String outputFile = args[2];

		parsingExample(inputFile,databaseDir,outputFile);
	}


	public static void parsingExample(String filename,String databaseDir, String outputFile) {
		try {
			/**
			 * set a BUfferedWriter instance to write in the specified output file.
			 */
			BufferedWriter out = new BufferedWriter(new FileWriter(outputFile));
			
			/**
			 * parse the query in the inputfile to Statement object for further processing.
			 */
			Statement statement = CCJSqlParserUtil.parse(new FileReader(filename));
			
			
			if (statement != null) {
				System.out.println("Read statement: " + statement);
				Select select = (Select) statement;
				System.out.println("Select body is " + select.getSelectBody());
				PlainSelect plain = (PlainSelect) select.getSelectBody();
				String schemaFile = databaseDir + "/schema.txt";
				DbCatalog catalog = new DbCatalog(schemaFile,databaseDir);
				
				/**
				 * create query plan for the query, and generate tree for the
				 * query to get tuples satisfying the tree and write to the
				 * output file.
				 */
				QueryPlan plan = new QueryPlan(plain,catalog);

				plan.createTree();
				plan.dump(out);
				

			}
		} catch (Exception e) {
			System.err.println("Exception occurred during parsing");
			e.printStackTrace();
		}
	}
}
