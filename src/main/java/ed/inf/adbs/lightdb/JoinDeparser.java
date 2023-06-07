package ed.inf.adbs.lightdb;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.SignedExpression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.GreaterThan;
import net.sf.jsqlparser.expression.operators.relational.GreaterThanEquals;
import net.sf.jsqlparser.expression.operators.relational.MinorThan;
import net.sf.jsqlparser.expression.operators.relational.MinorThanEquals;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.SimpleNode;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;

import java.util.Stack;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;

/**
 * JoinDeparser is to deparse join expression in WHERE clause. It judges whether a single expression
 * in the WHERE expression is selection expression or join expression.
 * 
 * @author s2041285
 *
 */

public class JoinDeparser {
	public Tuple tuple;
	public Expression expression;
	public ExpressionDeParser expressionDeParser;
	
	Map<FromItem, List<Expression>> singleExprs;
	List<Expression> joinCondition;
	/**
	 * get the list of join conditions
	 * @return
	 */
	public List<Expression> getJoinCondition() {
		return this.joinCondition;
	}
	
	/**
	 * get the hashmap of selection conditions, which uses fromitem as the key.
	 * for each fromitem key, it contains a list of selection expressions of the
	 * fromitem.
	 * 
	 * @return
	 */
	public Map<FromItem, List<Expression>> getSingleExprs() {
		return this.singleExprs;
	}
	
	/**
	 * The JoinDeparser constructor takes a hash map instance, join condition list 
	 * and the expression in WHERE clause. Then use the parameters to initialize
	 * singleExprs, joinCondition and expression. Moreover, an ExpressionDeParser
	 * instance is intialized for further deparse of the WHERE expression.
	 * 
	 * @param singleExprs
	 * @param joinCondition
	 * @param where
	 */
	public JoinDeparser(Map<FromItem, List<Expression>> singleExprs, List<Expression> joinCondition, Expression where) {
		
		this.expressionDeParser = new ExpressionDeParser();
		this.singleExprs = singleExprs;
		this.joinCondition = joinCondition;
		this.expression = where;
		
	}
	
	/**
	 * The method is to retrieve the left and right elements from a single selection expression
	 * or a single join expression. For example, if the expression is S.A = 1, then the left expression
	 * is S.A and the right expression is 1. Then the left expression is stored in the first element
	 * of the expression array and the right expression is store in the second element. Finally 
	 * the expression array is returned for further processing.
	 * 
	 * @param e
	 * @return Expression[] exp
	 * @throws JSQLParserException
	 */
	public Expression[] getElements(Expression e) throws JSQLParserException {
		Expression[] exp = new Expression[2];
		
		if (e instanceof EqualsTo) {
			Expression lefte = ((EqualsTo) e).getLeftExpression();
			Expression righte = ((EqualsTo) e).getRightExpression();
			exp[0] = lefte;
			exp[1] = righte;
		}else if (e instanceof NotEqualsTo) {
			Expression lefte = ((NotEqualsTo) e).getLeftExpression();
			Expression righte = ((NotEqualsTo) e).getRightExpression();
			exp[0] = lefte;
			exp[1] = righte;
		}else if (e instanceof GreaterThan) {
			Expression lefte = ((GreaterThan) e).getLeftExpression();
			Expression righte = ((GreaterThan) e).getRightExpression();
			exp[0] = lefte;
			exp[1] = righte;
		}else if (e instanceof GreaterThanEquals) {
			Expression lefte = ((GreaterThanEquals) e).getLeftExpression();
			Expression righte = ((GreaterThanEquals) e).getRightExpression();
			exp[0] = lefte;
			exp[1] = righte;
		}else if (e instanceof MinorThan) {
			Expression lefte = ((MinorThan) e).getLeftExpression();
			Expression righte = ((MinorThan) e).getRightExpression();
			exp[0] = lefte;
			exp[1] = righte;
		}else if (e instanceof MinorThanEquals) {
			Expression lefte = ((MinorThanEquals) e).getLeftExpression();
			Expression righte = ((MinorThanEquals) e).getRightExpression();
			exp[0] = lefte;
			exp[1] = righte;
		}
		return exp;
		
		
	}
	
	/**
	 * the method is to judge if a single expression is selection expression or
	 * a join expression. If it is a selection expression, insert it to the expression
	 * list with corresponding fromitem key. Else, if the two expressions are all of 
	 * LongValue, then insert into joinexpr list. Else, if the two tables the left and
	 * right expression refer to are the same, they are regarded as selection expression
	 * and insert into expression list with corresponding fromitem key. If the two table
	 * names are not the same, insert into joinexpr list. 
	 * 
	 * 
	 * @param expr
	 * @param lefte
	 * @param righte
	 */
	public void add(Expression expr, Expression lefte, Expression righte) {
		//the first two if clauses to see if this is a selection expression
		//the final else clause is if the expression is join expression or
		//the values are all of LongValue type.
		
		if ((lefte instanceof LongValue || lefte instanceof SignedExpression) & (righte instanceof Column)) {
			for (FromItem e : getSingleExprs().keySet()) {
				String table = ((Column)righte).getTable().getName();
				if (e.getAlias() != null) {
					if ((e.getAlias().getName().equals(table))) {
						getSingleExprs().get(e).add(expr);
						break;
					}
				} else {
					if (e.toString().equals(table)) {
						getSingleExprs().get(e).add(expr);
						break;
					}
				}
				
			}
				
		}else if ((righte instanceof LongValue || righte instanceof SignedExpression) & (lefte instanceof Column)) {
			for (FromItem e : getSingleExprs().keySet()) {
				String table = ((Column)lefte).getTable().getName();
				if (e.getAlias() != null) {
					if ((e.getAlias().getName().equals(table))) {
						getSingleExprs().get(e).add(expr);
						break;
					}
				} else {
					if (e.toString().equals(table)) {
						getSingleExprs().get(e).add(expr);
						break;
					}
				}
			}								
			 
		}else {
			/**
			 * if the left and right expressions refer to the same table, then
			 * it is regarded as a selection expression. If the table names they
			 * refer to are not the same, then the expression is regarded as join
			 * condition. If the left and right expressions are negative integer
			 * or positive integers, then it is regarded as a join condition.
			 */
			
			if ((righte instanceof SignedExpression || righte instanceof LongValue) & 
					(lefte instanceof SignedExpression || lefte instanceof LongValue)) {
				getJoinCondition().add(expr);
			}else if ((righte instanceof Column) & (lefte instanceof Column)) {
				String leftTable = ((Column)lefte).getTable().getName();
				String rightTable = ((Column)righte).getTable().getName();

				if(leftTable.equals(rightTable)) {
					for (FromItem e : getSingleExprs().keySet()) {
						if (e.getAlias() != null) {
							if ((e.getAlias().getName().equals(leftTable))){
								getSingleExprs().get(e).add(expr);
								break;
							}
						} else {
							if (e.toString().equals(leftTable)) {
								getSingleExprs().get(e).add(expr);
								break;
							}
						}
					}
				}else {
					getJoinCondition().add(expr);
				}
			}
		}
	}
	
	/**
	 * the evaluate method takes the WHERE expression and create a new
	 * ExpressionDeParser instance, which implements visit methods for
	 * seven kinds of expressions. If one single expression matches one
	 * of them, call add method to evaluate if it is a selection expression
	 * or join expression and add them to corresponding hashmap or join
	 * expression list.
	 * 
	 * @param expr
	 * @throws JSQLParserException
	 */
	public void evaluate(Expression expr) throws JSQLParserException {
		
		expressionDeParser = new ExpressionDeParser() {

			@Override
			public void visit(AndExpression andExpression) {
				super.visit(andExpression);

			}

			@Override
			public void visit(EqualsTo equalsTo) {
				super.visit(equalsTo);
				Expression lefte = ((EqualsTo) equalsTo).getLeftExpression();
				Expression righte = ((EqualsTo) equalsTo).getRightExpression();
				
				add(equalsTo, lefte, righte);
				

			}

			@Override
			public void visit(NotEqualsTo notEqualsTo) {
				super.visit(notEqualsTo);
				Expression lefte = ((NotEqualsTo) notEqualsTo).getLeftExpression();
				Expression righte = ((NotEqualsTo) notEqualsTo).getRightExpression();
				
				add(notEqualsTo, lefte, righte);

			}
	    
			@Override
			public void visit(GreaterThan greaterThan) {
				super.visit(greaterThan);
				Expression lefte = ((GreaterThan) greaterThan).getLeftExpression();
				Expression righte = ((GreaterThan) greaterThan).getRightExpression();
				
				add(greaterThan, lefte, righte);

			}
			
			@Override
			public void visit(GreaterThanEquals greaterThanEquals) {
				super.visit(greaterThanEquals);
				Expression lefte = ((GreaterThanEquals) greaterThanEquals).getLeftExpression();
				Expression righte = ((GreaterThanEquals) greaterThanEquals).getRightExpression();
				
				add(greaterThanEquals, lefte, righte);
				
			}
			
			@Override
			public void visit(MinorThan minorThan) {
				super.visit(minorThan);
				Expression lefte = ((MinorThan) minorThan).getLeftExpression();
				Expression righte = ((MinorThan) minorThan).getRightExpression();		
				
				add(minorThan, lefte, righte);

			}
			
			@Override
			public void visit(MinorThanEquals minorThanEquals) {
				super.visit(minorThanEquals);
				Expression lefte = ((MinorThanEquals) minorThanEquals).getLeftExpression();
				Expression righte = ((MinorThanEquals) minorThanEquals).getRightExpression();
				
				add(minorThanEquals, lefte, righte);
				
			}
			
		};
		expr.accept(expressionDeParser);
   
	}


}