package org.perses;

import com.google.common.collect.ImmutableList;
import org.antlr.v4.runtime.Token;
import org.perses.antlr.ParseTreeWithParser;
import org.perses.grammar.AbstractParserFacade;
import org.perses.grammar.ParserFacadeFactory;
import org.perses.program.SourceFile;

import java.io.File;
import java.io.IOException;

import static com.google.common.base.Preconditions.checkArgument;

public class TokenCounterMain {

  public static void main(String[] args) throws IOException {
    checkArgument(args.length == 1, "Usage: <main> <source file>");
    final File file = new File(args[0]);
    checkArgument(file.exists(), "The source file does not exist. %s", file);
    checkArgument(file.isFile(), "The source file is not a regular file. %s", file);
    final SourceFile sourceFile = new SourceFile(file);
    final AbstractParserFacade parserFacade =
        ParserFacadeFactory.createForPnfC().createParserFacade(sourceFile.getLanguageKind());
    final ImmutableList<Token> tokens = parserFacade.parseIntoTokens(sourceFile.getFile());
    int count = 0;
    for (Token token: tokens) {
      if (token.getChannel() == Token.DEFAULT_CHANNEL) {
        ++count;
      }
    }
    System.out.println();
    System.out.printf("The number of tokens in file '%s' is:\n", file);
    System.out.println(count);
  }
}
