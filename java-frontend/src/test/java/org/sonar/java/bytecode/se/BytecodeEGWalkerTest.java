/*
 * SonarQube Java
 * Copyright (C) 2012-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.java.bytecode.se;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.sonar.java.ast.parser.JavaParser;
import org.sonar.java.bytecode.loader.SquidClassLoader;
import org.sonar.java.resolve.SemanticModel;
import org.sonar.java.se.ProgramState;
import org.sonar.java.se.checks.DivisionByZeroCheck;
import org.sonar.java.se.constraint.BooleanConstraint;
import org.sonar.java.se.constraint.ObjectConstraint;
import org.sonar.java.se.symbolicvalues.SymbolicValue;
import org.sonar.java.se.xproc.BehaviorCache;
import org.sonar.java.se.xproc.HappyPathYield;
import org.sonar.java.se.xproc.MethodBehavior;
import org.sonar.plugins.java.api.semantic.Symbol;
import org.sonar.plugins.java.api.tree.ClassTree;
import org.sonar.plugins.java.api.tree.CompilationUnitTree;
import org.sonar.plugins.java.api.tree.MethodTree;

import java.io.File;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static org.fest.assertions.Assertions.assertThat;

public class BytecodeEGWalkerTest {

  @Test
  public void generateMethodBehavior() throws Exception {
    BytecodeEGWalker bytecodeEGWalker = new BytecodeEGWalker(new BehaviorCache(null));
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    File file = new File("src/test/java/org/sonar/java/bytecode/se/BytecodeEGWalkerTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    Symbol.MethodSymbol symbol = ((MethodTree) ((ClassTree) ((ClassTree) tree.types().get(0)).members().get(1)).members().get(0)).symbol();
    MethodBehavior methodBehavior = bytecodeEGWalker.getMethodBehavior(symbol, squidClassLoader);
    assertThat(methodBehavior.yields()).hasSize(2);

    SymbolicValue svThis = new SymbolicValue();
    SymbolicValue svFirstArg = new SymbolicValue();
    SymbolicValue svsecondArg = new SymbolicValue();
    SymbolicValue svResult = new SymbolicValue();
    List<SymbolicValue> invocationArguments = Lists.newArrayList(svThis, svFirstArg, svsecondArg);
    List<ObjectConstraint> collect = methodBehavior.yields().stream().map(my -> {

      Collection<ProgramState> ps = my.statesAfterInvocation(invocationArguments, Lists.newArrayList(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
      assertThat(ps).hasSize(1);
      ProgramState next = ps.iterator().next();
      return next.getConstraint(svResult, ObjectConstraint.class);
    })
      .collect(Collectors.toList());
    assertThat(collect).hasSize(2).containsOnly(ObjectConstraint.NOT_NULL, ObjectConstraint.NULL);

    List<HappyPathYield> nullConstraintOnResult =
      methodBehavior.happyPathYields().filter(my -> ObjectConstraint.NULL.equals(my.resultConstraint().get(ObjectConstraint.class))).collect(Collectors.toList());
    assertThat(nullConstraintOnResult).hasSize(1);
    HappyPathYield nullConstraintResult = nullConstraintOnResult.get(0);
    Collection<ProgramState> ps = nullConstraintResult.statesAfterInvocation(invocationArguments, Lists.newArrayList(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
    assertThat(ps).hasSize(1);
    ObjectConstraint constraint = ps.iterator().next().getConstraint(svsecondArg, ObjectConstraint.class);
    assertThat(constraint).isSameAs(ObjectConstraint.NULL);


  }

  static class InnerClass {
    Object fun(boolean a, Object b) {
      if (b == null) {
        return null;
      }
      return "";
    }

    Object fun2(boolean a) {
      if (a) {
        return null;
      }
      return "";
    }
  }


  @Test
  public void verify_behavior_of_fun2_method() throws Exception {
    BytecodeEGWalker bytecodeEGWalker = new BytecodeEGWalker(new BehaviorCache(null));
    SquidClassLoader squidClassLoader = new SquidClassLoader(Lists.newArrayList(new File("target/test-classes"), new File("target/classes")));
    File file = new File("src/test/java/org/sonar/java/bytecode/se/BytecodeEGWalkerTest.java");
    CompilationUnitTree tree = (CompilationUnitTree) JavaParser.createParser().parse(file);
    SemanticModel.createFor(tree, squidClassLoader);
    Symbol.MethodSymbol symbol = ((MethodTree) ((ClassTree) ((ClassTree) tree.types().get(0)).members().get(1)).members().get(1)).symbol();
    MethodBehavior methodBehavior = bytecodeEGWalker.getMethodBehavior(symbol, squidClassLoader);
    assertThat(methodBehavior.yields()).hasSize(2);

    SymbolicValue svThis = new SymbolicValue();
    SymbolicValue svFirstArg = new SymbolicValue();
    SymbolicValue svResult = new SymbolicValue();
    List<SymbolicValue> invocationArguments = Lists.newArrayList(svThis, svFirstArg);
    List<HappyPathYield> oneYield =
      methodBehavior.happyPathYields().filter(my -> ObjectConstraint.NULL.equals(my.resultConstraint().get(ObjectConstraint.class))).collect(Collectors.toList());

    assertThat(oneYield).hasSize(1);
    HappyPathYield yield = oneYield.get(0);
    Collection<ProgramState> pss = yield.statesAfterInvocation(invocationArguments, Lists.newArrayList(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
    assertThat(pss).hasSize(1);
    ProgramState ps = pss.iterator().next();
    assertThat(ps.getConstraint(svFirstArg, ObjectConstraint.class)).isNull();
    assertThat(ps.getConstraint(svFirstArg, BooleanConstraint.class)).isSameAs(BooleanConstraint.TRUE);
    assertThat(ps.getConstraint(svFirstArg, DivisionByZeroCheck.ZeroConstraint.class)).isSameAs(DivisionByZeroCheck.ZeroConstraint.NON_ZERO);

    oneYield =
      methodBehavior.happyPathYields().filter(my -> ObjectConstraint.NOT_NULL.equals(my.resultConstraint().get(ObjectConstraint.class))).collect(Collectors.toList());

    assertThat(oneYield).hasSize(1);
    yield = oneYield.get(0);
    pss = yield.statesAfterInvocation(invocationArguments, Lists.newArrayList(), ProgramState.EMPTY_STATE, () -> svResult).collect(Collectors.toList());
    assertThat(pss).hasSize(1);
    ps = pss.iterator().next();
    assertThat(ps.getConstraint(svFirstArg, ObjectConstraint.class)).isNull();
    assertThat(ps.getConstraint(svFirstArg, BooleanConstraint.class)).isSameAs(BooleanConstraint.FALSE);
    assertThat(ps.getConstraint(svFirstArg, DivisionByZeroCheck.ZeroConstraint.class)).isSameAs(DivisionByZeroCheck.ZeroConstraint.ZERO);

  }
}