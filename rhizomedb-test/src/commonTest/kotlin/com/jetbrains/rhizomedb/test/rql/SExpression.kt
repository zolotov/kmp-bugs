package com.jetbrains.rhizomedb.test.rql

import com.jetbrains.rhizomedb.rql.*

fun Query.sexpr(): String {
  return buildString {
    append("(SELECT ")
    append(select.sexpr())
    if (from.isNotEmpty()) {
      val sources = from.joinToString(" ") { it.sexpr() }
      append(" (FROM $sources)")
    }
    where?.let { append(" (WHERE ${it.sexpr()})") }
    groupBy?.let { append(" ${it.sexpr()}") }
    orderBy?.let { append(" ${it.sexpr()}") }
    limit?.let { append(" ${it.sexpr()}") }
    append(")")
  }
}

fun Column.sexpr(): String {
  return if (alias != null) {
    "(${expression.sexpr()} AS ${alias})"
  }
  else {
    expression.sexpr()
  }
}

fun ColumnSelection.sexpr(): String {
  return when (val e = this) {
    is ColumnSelection.Wildcard -> {
      "*"
    }
    is ColumnSelection.Columns -> {
      val items = e.columns.joinToString(" ") { it.sexpr() }
      "($items)"
    }
  }
}

fun Expression.sexpr(): String {
  return when (this) {
    is Expression.Literal -> value.lexeme
    is Expression.Variable -> name.lexeme
    is Expression.UnaryOp -> "(${operator.lexeme} ${operand.sexpr()})"
    is Expression.BinaryOp -> "(${operator.lexeme} ${left.sexpr()} ${right.sexpr()})"
    is Expression.InOp -> "(IN ${left.sexpr()} ${right.sexpr()})"
    is Expression.FunctionCall -> {
      val params = parameters.joinToString(" ") { it.sexpr() }
      "(${expression.sexpr()} $params)"
    }
  }
}

fun List<Expression>.sexpr(): String {
  return "(" + joinToString(" ") { it.sexpr() } + ")"
}

fun InExpression.sexpr(): String {
  return when (this) {
    is InExpression.Tuple -> items.sexpr()
    is InExpression.Select -> {
      query.sexpr()
    }
  }
}

fun FromSource.sexpr(): String {
  return when (this) {
    is FromSource.Table -> this.name.lexeme
    is FromSource.Join -> {
      "(JOIN ${left.sexpr()} ${right.lexeme} ${on.sexpr()})"
    }
  }
}

fun GroupBy.sexpr(): String {
  return buildString {
    append("(GROUP_BY ")
    append(expressions.sexpr())
    having?.let { append(" (HAVING ${it.sexpr()})") }
    append(")")
  }
}

fun OrderBy.sexpr(): String {
  val termList = terms.joinToString(" ") { it.sexpr() }
  return "(ORDER_BY $termList)"
}

fun OrderTerm.sexpr(): String {
  val direction = if (ascending) "ASC" else "DESC"
  return "($direction ${expression.sexpr()})"
}

fun Limit.sexpr(): String {
  val offsetPart = offset?.let { " ${it.sexpr()}" } ?: ""
  return "(LIMIT ${limit.sexpr()}$offsetPart)"
}