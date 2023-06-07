The logic to extract join conditions from the WHERE clause:

First, an instance of class JoinDeparser is created. The class JoinDeparser is used to deparse the join conditions. Then the whole expression from the WHERE clause is fed into the 'evaluate(Expression expr)' for evaluting whether these single expressions in the clause are selection expression or join condition. 

In the 'evaluate' method of JoinDeparser, an ExpressionDeParser instance is initialized and visit method of the seven expressions except LongValue is override. For the visit methods except AndExpression visit, first retrieve the left Expression and the right Expression of the comparison symbol. Then use 'add' method in JoinDeparser class to judge if the left expression and right expression are of Column type, LongValue type or SignedExpression type(for negative integers).

If the left expression and the right expression are all not of type LongValue or SignedExpression, or the two expressions are of Column type and the tables they refer to are not the same, then the single expression is regarded as a join condition. Based on the table name positions in FROM clause, it will extract the maximum position number between the two tables and give the join condition to the hashmap with the FromItem key having the larger position value.

The part to split selection conditions and join conditions is in JoinDeparser.java file with explanation. The part to construct a join tree with explanation is in QueryPlan.java file and corresponding code and comments are in line 188-413.

The Operator class has two child variables:leftChild and rightChild. For JoinOperator, it uses both. For other operators with only one child, I set to use leftChild if other operators have child.