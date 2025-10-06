package com.jetbrains.rhizomedb.test.rql

import com.jetbrains.rhizomedb.rql.Lexer
import com.jetbrains.rhizomedb.rql.Token
import com.jetbrains.rhizomedb.rql.TokenType
import kotlin.test.Test
import kotlin.test.assertEquals
import com.jetbrains.rhizomedb.rql.TokenType.*

class LexerTest {
  @Test
  fun testNumericLiterals() {
    assertLexer("123", T(INTEGER, "123", 123))
    assertLexer("12.34", T(DOUBLE, "12.34", 12.34))
    assertLexer("12.34f", T(FLOAT, "12.34f", 12.34f))
    assertLexer("-12.34", T(MINUS, "-"), T(DOUBLE, "12.34", 12.34))
    assertLexer("-500", T(MINUS, "-"), T(INTEGER, "500", 500))
  }

  @Test
  fun testStrings() {
    assertLexer("'some string'", T(STRING, "'some string'", "some string"))
    assertLexer("""'some \n string'""", T(STRING, """'some \n string'""", "some \n string"))
    assertLexer("""'some \'string\' '""", T(STRING, """'some \'string\' '""", "some 'string' "))
    assertLexer("""'some "string" '""", T(STRING, """'some "string" '""", "some \"string\" "))
    assertLexer(
      "SELECT \n 'some \n string' \n FROM",
      T(SELECT, "SELECT"), T(STRING, "'some \n string'", "some \n string"), T(FROM, "FROM"))
  }

  @Test
  fun testArithmeticOperators() {
    assertLexer("1 - 2", T(INTEGER, "1", 1), T(MINUS, "-"), T(INTEGER, "2", 2))
    assertLexer("1 + 2", T(INTEGER, "1", 1), T(PLUS, "+"), T(INTEGER, "2", 2))
    assertLexer("1 * 2", T(INTEGER, "1", 1), T(STAR, "*"), T(INTEGER, "2", 2))
    assertLexer("1 / 2", T(INTEGER, "1", 1), T(SLASH, "/"), T(INTEGER, "2", 2))
    assertLexer("1 % 2", T(INTEGER, "1", 1), T(PERCENT, "%"), T(INTEGER, "2", 2))
    assertLexer("(1 % 2)", T(LPAREN, "("), T(INTEGER, "1", 1), T(PERCENT, "%"), T(INTEGER, "2", 2), T(RPAREN, ")"))
  }

  @Test
  fun testComparisionOperators() {
    assertLexer("foo = bar", T(IDENTIFIER, "foo"), T(EQUAL, "="), T(IDENTIFIER, "bar"))
    assertLexer("foo > bar", T(IDENTIFIER, "foo"), T(GREATER, ">"), T(IDENTIFIER, "bar"))
    assertLexer("foo < bar", T(IDENTIFIER, "foo"), T(LESS, "<"), T(IDENTIFIER, "bar"))
    assertLexer("foo >= bar", T(IDENTIFIER, "foo"), T(GREATER_EQUAL, ">="), T(IDENTIFIER, "bar"))
    assertLexer("foo <= bar", T(IDENTIFIER, "foo"), T(LESS_EQUAL, "<="), T(IDENTIFIER, "bar"))
    assertLexer("foo != bar", T(IDENTIFIER, "foo"), T(NOT_EQUAL, "!="), T(IDENTIFIER, "bar"))
    assertLexer("foo <> bar", T(IDENTIFIER, "foo"), T(NOT_EQUAL, "<>"), T(IDENTIFIER, "bar"))
  }

  @Test
  fun testLogicalOperators() {
    assertLexer("NOT b", T(NOT, "NOT"), T(IDENTIFIER, "b"))
    assertLexer("a AND b", T(IDENTIFIER, "a"), T(AND, "AND"), T(IDENTIFIER, "b"))
    assertLexer("a OR b", T(IDENTIFIER, "a"), T(OR, "OR"), T(IDENTIFIER, "b"))
    assertLexer("a IN b", T(IDENTIFIER, "a"), T(IN, "IN"), T(IDENTIFIER, "b"))
    assertLexer("a LIKE b", T(IDENTIFIER, "a"), T(LIKE, "LIKE"), T(IDENTIFIER, "b"))
  }

  @Test
  fun testKeywords() {
    assertLexer("SELECT x ", T(SELECT, "SELECT"), T(IDENTIFIER, "x"))
    assertLexer("GROUP BY x ", T(GROUP, "GROUP"), T(BY, "BY"), T(IDENTIFIER, "x"))
    assertLexer("SELECT x HAVING", T(SELECT, "SELECT"), T(IDENTIFIER, "x"), T(HAVING, "HAVING"))
    assertLexer("ORDER BY x ", T(ORDER, "ORDER"), T(BY, "BY"), T(IDENTIFIER, "x"))
    assertLexer("LIMIT x ", T(LIMIT, "LIMIT"), T(IDENTIFIER, "x"))
    assertLexer("TRUE FALSE NULL", T(TRUE, "TRUE", true), T(FALSE, "FALSE", false), T(NULL, "NULL", null))
  }

  @Test
  fun testComments() {
    assertLexer(
      """100
            - --This is a comment should be ignored +-/*
            200""",
      T(INTEGER, "100", 100), T(MINUS, "-"), T(INTEGER, "200", 200)
    )

    assertLexer(
      """20
            /* This is a comment */
            -
            /* This is a also a comment */
            40""",
      T(INTEGER, "20", 20), T(MINUS, "-"), T(INTEGER, "40", 40)
    )

    assertLexer(
      """/* This
            Is
            Multi-line */
            123""",
      T(INTEGER, "123", 123)
    )

    assertLexer(
      """123 -- Line comment""",
      T(INTEGER, "123", 123)
    )

    assertLexer(
      """123 /* Line comment""",
      T(INTEGER, "123", 123)
    )

    assertLexer(
      """1/**/2""",
      T(INTEGER, "1", 1), T(INTEGER, "2", 2)
    )

    assertLexer(
      """1/*""",
      T(INTEGER, "1", 1)
    )
  }

  @Test
  fun testIdentifiers() {
    assertLexer("foo", T(IDENTIFIER, "foo"))
    assertLexer("foo bar", T(IDENTIFIER, "foo"), T(IDENTIFIER, "bar"))
    assertLexer("Foo + Bar", T(IDENTIFIER, "Foo"), T(PLUS, "+"), T(IDENTIFIER, "Bar"))
    assertLexer("one_id + two_id", T(IDENTIFIER, "one_id"), T(PLUS, "+"), T(IDENTIFIER, "two_id"))
    assertLexer("_one + _two", T(IDENTIFIER, "_one"), T(PLUS, "+"), T(IDENTIFIER, "_two"))
    assertLexer("123andme", T(INTEGER, "123", 123), T(IDENTIFIER, "andme"))
    assertLexer("123.456Fandme", T(FLOAT, "123.456F", 123.456f), T(IDENTIFIER, "andme"))
    assertLexer("123.456dandme", T(DOUBLE, "123.456d", 123.456), T(IDENTIFIER, "andme"))
    assertLexer("select foo", T(SELECT, "select"), T(IDENTIFIER, "foo"))
    assertLexer("SELECT foo", T(SELECT, "SELECT"), T(IDENTIFIER, "foo"))
    assertLexer("SelecT foo", T(SELECT, "SelecT"), T(IDENTIFIER, "foo"))
    assertLexer("not b", T(NOT, "not"), T(IDENTIFIER, "b"))
    assertLexer("a and b", T(IDENTIFIER, "a"), T(AND, "and"), T(IDENTIFIER, "b"))
    assertLexer("a or b", T(IDENTIFIER, "a"), T(OR, "or"), T(IDENTIFIER, "b"))
    assertLexer("a in b", T(IDENTIFIER, "a"), T(IN, "in"), T(IDENTIFIER, "b"))
    assertLexer("a like b", T(IDENTIFIER, "a"), T(LIKE, "like"), T(IDENTIFIER, "b"))
  }

  @Test
  fun testErrors() {
    assertError(""" SELECT '  """, "8:Unterminated string")
    assertError(""" SELECT '\x'  """, "8:Unknown string escape '\\x'")
    assertError(""" SELECT * WHERE X !- 1""", "18:Unexpected character '-'")
    assertError(""" SELECT * WHERE X = 1 & Y = 2""", "22:Unexpected character '&'")
    assertError(""" SELECT a !+ 1 $#@ WHERE y = '\f' AND z = ' """,
                "10:Unexpected character '+'",
                "15:Unexpected character '$'",
                "16:Unexpected character '#'",
                "17:Unexpected character '@'",
                "29:Unknown string escape '\\f'",
                "42:Unterminated string")
  }

  @Test
  fun testFullQueries() {
    assertLexer("SELECT field FROM Document", T(SELECT, "SELECT"), T(IDENTIFIER, "field"), T(FROM, "FROM"), T(IDENTIFIER, "Document"))

    assertLexer("select field1, field2 from document",
                T(SELECT, "select"), T(IDENTIFIER, "field1"), T(COMMA, ","), T(IDENTIFIER, "field2"),
                T(FROM, "from"), T(IDENTIFIER, "document"))

    assertLexer("""select
        field1 AS name1,
        field2 as name2
        from Document""",
                T(SELECT, "select"),
                T(IDENTIFIER, "field1"), T(AS, "AS"), T(IDENTIFIER, "name1"), T(COMMA, ","),
                T(IDENTIFIER, "field2"), T(AS, "as"), T(IDENTIFIER, "name2"),
                T(FROM, "from"), T(IDENTIFIER, "Document"))

    assertLexer("""select
        field1 > 0
        AS isBigger,
        field2 + field3
        AS theSum,
        AVG(field4) < 0
        AS average,
        from Document JOIN Editor
        ON Document.id
        where (theSum > 1)
        AND field1""",
                T(SELECT, "select"),
                T(IDENTIFIER, "field1"), T(GREATER, ">"), T(INTEGER, "0", 0),
                T(AS, "AS"), T(IDENTIFIER, "isBigger"), T(COMMA, ","),
                T(IDENTIFIER, "field2"), T(PLUS, "+"), T(IDENTIFIER, "field3"),
                T(AS, "AS"), T(IDENTIFIER, "theSum"), T(COMMA, ","),
                T(IDENTIFIER, "AVG"), T(LPAREN, "("), T(IDENTIFIER, "field4"), T(RPAREN, ")"), T(LESS, "<"), T(INTEGER, "0", 0),
                T(AS, "AS"), T(IDENTIFIER, "average"), T(COMMA, ","),
                T(FROM, "from"), T(IDENTIFIER, "Document"), T(JOIN, "JOIN"), T(IDENTIFIER, "Editor"),
                T(ON, "ON"), T(IDENTIFIER, "Document.id"),
                T(WHERE, "where"), T(LPAREN, "("), T(IDENTIFIER, "theSum"), T(GREATER, ">"), T(INTEGER, "1", 1), T(RPAREN, ")"),
                T(AND, "AND"), T(IDENTIFIER, "field1")
    )
  }

  private data class T(val type: TokenType, val lexeme: String, val literal: Any? = null)

  private fun assertLexer(code: String, vararg expectedTokens: T) {
    val lexer = Lexer(code)
    var currentToken = lexer.scanToken()
    val tokens = mutableListOf<Token>()
    while (currentToken.type != EOF) {
      tokens.add(currentToken)
      currentToken = lexer.scanToken()
    }
    for ((token, expected) in tokens.zip(expectedTokens)) {
      assertEquals(expected.type, token.type)
      assertEquals(expected.lexeme, token.lexeme)
      assertEquals(expected.literal, token.literal)
    }
  }

  private fun assertError(code: String, vararg expectedErrors: String) {
    val lexer = Lexer(code)
    var currentToken = lexer.scanToken()
    val errors = mutableListOf<Token>()
    while (currentToken.type != EOF) {
      if (currentToken.type == ERROR) errors.add(currentToken)
      currentToken = lexer.scanToken()
    }
    assertEquals(expectedErrors.toList(), errors.map { "${it.startOffset}:${it.literal}" })
  }
}
