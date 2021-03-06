/*
 * Copyright (C) 2018-2020 University of Waterloo.
 *
 * This file is part of Perses.
 *
 * Perses is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 3, or (at your option) any later version.
 *
 * Perses is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Perses; see the file LICENSE.  If not see <http://www.gnu.org/licenses/>.
 */
package org.perses.antlr.pnf;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import org.perses.antlr.ast.*;

import java.util.*;
import java.util.stream.Collectors;

public abstract class AbstractPnfPass {

  public abstract ImmutableRuleDefMap process(ImmutableRuleDefMap grammar);

  protected static List<RuleNameRegistry.RuleNameHandle> getSortedRuleNames(MutableRuleDefMap map) {
    return map.keySet().stream().sorted().collect(Collectors.toList());
  }

  protected static AbstractPersesRuleElement replaceGapWithRuleReference(
      PersesSequenceAst seq, Interval gap, RuleNameRegistry.RuleNameHandle gapReplacement) {
    final ImmutableList.Builder<AbstractPersesRuleElement> builder = ImmutableList.builder();
    for (int i = 0, size = seq.getChildCount(); i < size; ++i) {
      if (i == gap.leftInclusive) {
        builder.add(new PersesRuleReferenceAst(gapReplacement));
      }
      if (gap.leftInclusive <= i && i < gap.rightExclusive) {
        continue;
      }
      builder.add(seq.getChild(i));
    }
    return new PersesSequenceAst(builder.build());
  }

  protected static List<AbstractPersesRuleElement> constructAlternativeBlocksIfNecessary(
      Collection<AbstractPersesRuleElement> alternatives) {
    alternatives = flatternAltBlocks(alternatives);
    final int size = alternatives.size();
    Preconditions.checkArgument(size > 0);
    if (size == 1) {
      return ImmutableList.of(Iterables.getOnlyElement(alternatives));
    }
    final LinkedHashMap<String, AbstractPersesRuleElement> map = new LinkedHashMap<>();
    alternatives.forEach(alt -> map.put(alt.getSourceCode(), alt));
    return map.entrySet().stream()
        .map(Map.Entry::getValue)
        .collect(ImmutableList.toImmutableList());
  }

  protected static void inPlaceFlatternAltBlocks(List<AbstractPersesRuleElement> alternatives) {
    final ArrayList<AbstractPersesRuleElement> result = flatternAltBlocks(alternatives);
    alternatives.clear();
    alternatives.addAll(result);
  }

  protected static ArrayList<AbstractPersesRuleElement> flatternAltBlocks(
      Collection<AbstractPersesRuleElement> alternatives) {
    final ArrayList<AbstractPersesRuleElement> result = new ArrayList<>();
    for (AbstractPersesRuleElement alternative : alternatives) {
      flattern(alternative, result);
    }
    return result;
  }

  private static void flattern(
      AbstractPersesRuleElement element, ArrayList<AbstractPersesRuleElement> result) {
    if (element instanceof PersesAlternativeBlockAst) {
      final PersesAlternativeBlockAst block = (PersesAlternativeBlockAst) element;
      for (AbstractPersesRuleElement blockAlternative : block.getAlternatives()) {
        flattern(blockAlternative, result);
      }
    } else {
      result.add(element);
    }
  }

  protected static int countRulesThatCallsRuleOfName(
      MutableRuleDefMap grammar, RuleNameRegistry.RuleNameHandle ruleName) {
    RuleRefCounterAstVisitor counter = new RuleRefCounterAstVisitor(ruleName);
    counter.preorderGrammar(grammar.entries());
    return counter.getCount();
  }
}
