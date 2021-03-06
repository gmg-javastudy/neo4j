/*
 * Copyright (c) 2002-2019 "Neo4j,"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.neo4j.cypher.internal.v4_0.ast.semantics

import org.neo4j.cypher.internal.v4_0.expressions.Subtract
import org.neo4j.cypher.internal.v4_0.util.DummyPosition
import org.neo4j.cypher.internal.v4_0.util.symbols._

class SubtractTest extends InfixExpressionTestBase(Subtract(_, _)(DummyPosition(0))) {

  // Infix specializations:
  // 1 - 1 => 0
  // 1 - 1.1 => -0.1
  // 1.1 - 1 => 0.1
  // 1.1 - 1.1 => 0.0

  test("shouldHandleAllSpecializations") {
    testValidTypes(CTInteger, CTInteger)(CTInteger)
    testValidTypes(CTInteger, CTFloat)(CTFloat)
    testValidTypes(CTFloat, CTInteger)(CTFloat)
    testValidTypes(CTFloat, CTFloat)(CTFloat)
    testValidTypes(CTDuration, CTDuration)(CTDuration)
    testValidTypes(CTDate, CTDuration)(CTDate)
    testValidTypes(CTTime, CTDuration)(CTTime)
    testValidTypes(CTLocalTime, CTDuration)(CTLocalTime)
    testValidTypes(CTDateTime, CTDuration)(CTDateTime)
    testValidTypes(CTLocalDateTime, CTDuration)(CTLocalDateTime)
  }

  test("shouldHandleCombinedSpecializations") {
    testValidTypes(CTFloat | CTInteger, CTFloat | CTInteger)(CTFloat | CTInteger)
  }

  test("shouldFailTypeCheckForIncompatibleArguments") {
    testInvalidApplication(CTInteger, CTBoolean)(
      "Type mismatch: expected Float or Integer but was Boolean"
    )
    testInvalidApplication(CTBoolean, CTInteger)(
      "Type mismatch: expected Float, Integer, Duration, Date, Time, LocalTime, LocalDateTime or DateTime but was Boolean"
    )
  }
}
