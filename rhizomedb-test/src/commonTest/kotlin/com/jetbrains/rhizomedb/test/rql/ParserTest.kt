package com.jetbrains.rhizomedb.test.rql

import com.jetbrains.rhizomedb.rql.Parser
import com.jetbrains.rhizomedb.rql.RqlError
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail

class ParserTest {
  @Test
  fun testSingleSelect() {
    assertParser("""  SELECT *  """, "(SELECT *)")
    assertParser("""  SELECT 1  """, "(SELECT (1))")
    assertParser("""  SELECT 'foo'  """, "(SELECT ('foo'))")
    assertParser("""  SELECT column  """, "(SELECT (column))")
    assertParser("""  SELECT 1, 'foo', c1, c2  """, "(SELECT (1 'foo' c1 c2))")
    assertParser("""  SELECT c1 AS Name  """, "(SELECT ((c1 AS Name)))")
    assertParser("""  SELECT c1 AS n1, c2 AS n2, foo  """,
                 "(SELECT ((c1 AS n1) (c2 AS n2) foo))")
    assertParser("""  SELECT 1 as label  """, "(SELECT ((1 AS label)))")

    assertError("1, 2, 3", "Expected SELECT")
    assertError("SELECT foo AS 'name'", "Alias should be an identifier")
    assertError("SELECT 1 + /", "Unexpected token '/'")
    assertError("SELECT a FROM b 1", "Unexpected token '1'")
  }

  @Test
  fun testExpressions() {
    assertParser("""  SELECT 1  """, "(SELECT (1))")
    assertParser("""  SELECT 1 + 2  """, "(SELECT ((+ 1 2)))")
    assertParser("""  SELECT 1 + 2 * 3  """, "(SELECT ((+ 1 (* 2 3))))")
    assertParser("""  SELECT 1 + 2 * 3 + 4  """, "(SELECT ((+ (+ 1 (* 2 3)) 4)))")
    assertParser("""  SELECT a + b + c + d  """, "(SELECT ((+ (+ (+ a b) c) d)))")
    assertParser("""  SELECT a+b-c  """, "(SELECT ((- (+ a b) c)))")
    assertParser("""  SELECT a+b-c, 'foo'  """, "(SELECT ((- (+ a b) c) 'foo'))")
    assertParser("""  SELECT a >= b = d > 0   """, "(SELECT ((= (>= a b) (> d 0))))")
    assertParser("""  SELECT x * -1 = 0   """, "(SELECT ((= (* x (- 1)) 0)))")
    assertParser("""  SELECT (1)   """, "(SELECT (1))")
    assertParser("""  SELECT (((1)))   """, "(SELECT (1))")
    assertParser("""  SELECT (1 + 2)   """, "(SELECT ((+ 1 2)))")
    assertParser("""  SELECT (a + b) * (c - d)   """, "(SELECT ((* (+ a b) (- c d))))")
    assertParser("""  SELECT EMPTY()   """, "(SELECT ((EMPTY )))")
    assertParser("""  SELECT MAX(a)   """, "(SELECT ((MAX a)))")
    assertParser("""  SELECT SUM(a, b, c)   """, "(SELECT ((SUM a b c)))")
    assertParser("""  SELECT SQRT(a) + SQRT(b)   """, "(SELECT ((+ (SQRT a) (SQRT b))))")
    assertParser("""  SELECT Table.field   """, "(SELECT (Table.field))")
    assertParser("""  SELECT com.example.Table.field   """, "(SELECT (com.example.Table.field))")
    assertParser("""  SELECT Table1.field + Table2.field  """, "(SELECT ((+ Table1.field Table2.field)))")
    assertParser("""  SELECT NOT a AND b  """, "(SELECT ((AND (NOT a) b)))")
    assertParser("""  SELECT a OR b AND c OR d  """, "(SELECT ((OR (OR a (AND b c)) d)))")
    assertParser("""  SELECT NOT TEST() """, "(SELECT ((NOT (TEST ))))")
    assertParser("""  SELECT x IN (1, 2, 3)""", "(SELECT ((IN x (1 2 3))))")
    assertParser("""  SELECT x IN ()""", "(SELECT ((IN x ())))")
    assertParser("""  SELECT x IN (SELECT 1)""", "(SELECT ((IN x (SELECT (1)))))")
    assertParser("""  SELECT x IN SELECT 1""", "(SELECT ((IN x (SELECT (1)))))")
    assertParser("""  SELECT x IN SELECT 1""", "(SELECT ((IN x (SELECT (1)))))")
    assertParser("""  SELECT x LIKE 'foo' """, "(SELECT ((LIKE x 'foo')))")
    assertParser("""  SELECT x LIKE 'foo' OR b """, "(SELECT ((OR (LIKE x 'foo') b)))")
    assertParser("""  SELECT X FROM Y WHERE A AND B IN (1, 2)""", "(SELECT (X) (FROM Y) (WHERE (AND A (IN B (1 2)))))")

    assertError("""  SELECT x LIKE a  """, "Expected string after LIKE")
    assertError("""  SELECT a OR b IN c""", "Expecting ( after IN clause")
    assertError("""  SELECT ()""", "Unexpected token ')'")
  }

  @Test
  fun testSelectFrom() {
    assertParser("""  SELECT * FROM Table""", "(SELECT * (FROM Table))")
    assertParser("""  SELECT c1, c2 FROM Table""", "(SELECT (c1 c2) (FROM Table))")
  }

  @Test
  fun testSelectWhere() {
    assertParser("""  SELECT * FROM Table WHERE true""", "(SELECT * (FROM Table) (WHERE true))")
    assertParser("""  SELECT * WHERE true""", "(SELECT * (WHERE true))")
    assertParser("""  SELECT * WHERE a > 0 AND a < 100""", "(SELECT * (WHERE (AND (> a 0) (< a 100))))")
  }

  @Test
  fun testSelectGroupBy() {
    assertParser("""  SELECT * FROM Table WHERE true GROUP BY foo HAVING bar > 1""",
                 "(SELECT * (FROM Table) (WHERE true) (GROUP_BY (foo) (HAVING (> bar 1))))")
    assertParser("""  SELECT * GROUP BY a, b, c""",
                 "(SELECT * (GROUP_BY (a b c)))")

    assertError("""  SELECT * GROUP a, b, c""", "Expected GROUP BY")
  }

  @Test
  fun testOrderBy() {
    assertParser("""  SELECT * ORDER BY a""", "(SELECT * (ORDER_BY (ASC a)))")
    assertParser("""  SELECT * ORDER BY a ASC""", "(SELECT * (ORDER_BY (ASC a)))")
    assertParser("""  SELECT * ORDER BY a DESC""", "(SELECT * (ORDER_BY (DESC a)))")
    assertParser("""  SELECT * ORDER BY a, b, c""", "(SELECT * (ORDER_BY (ASC a) (ASC b) (ASC c)))")
    assertParser("""  SELECT * ORDER BY a, b DESC, c""", "(SELECT * (ORDER_BY (ASC a) (DESC b) (ASC c)))")

    assertError("""  SELECT * ORDER a""", "Expected ORDER BY")
  }

  @Test
  fun testLimit() {
    assertParser("""  SELECT * LIMIT 100""", "(SELECT * (LIMIT 100))")
    assertParser("""  SELECT * LIMIT 100 + 10""", "(SELECT * (LIMIT (+ 100 10)))")
    assertParser("""  SELECT * LIMIT 100 OFFSET 10 + 10""", "(SELECT * (LIMIT 100 (+ 10 10)))")

    assertError("""  SELECT * OFFSET 10""", "Unexpected token 'OFFSET'")
  }

  @Test
  fun testJoin() {
    assertParser("""  SELECT * FROM a JOIN b ON a.x = b.y""",
                 "(SELECT * (FROM (JOIN a b (= a.x b.y))))")
    assertParser("""  SELECT * FROM a, b, c""",
                 "(SELECT * (FROM a b c))")
    assertParser("""  SELECT * FROM a JOIN b ON a.x = b.y, other""",
                 "(SELECT * (FROM (JOIN a b (= a.x b.y)) other))")
    assertParser("""  SELECT * FROM a JOIN b ON a.x = b.y JOIN c ON b.y = c.z""",
                 "(SELECT * (FROM (JOIN (JOIN a b (= a.x b.y)) c (= b.y c.z))))")
  }

  private fun assertParser(code: String, expected: String) {
    val ast = Parser(code).parse()
    assertEquals(expected, ast.sexpr())
  }

  private fun assertError(code: String, expectedMessage: String) {
    try {
      val ast = Parser(code).parse()
      fail("Parsing was supposed to fail: ${ast.sexpr()}")
    } catch (e: RqlError) {
      assertEquals("Parse error: $expectedMessage", e.msg)
    }
  }
}