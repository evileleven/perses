def extract_grammar_name(grammar_file):
    if not grammar_file.endswith(".g4"):
        fail("The grammar file name has to end with '.g4'.")
    grammar_file = grammar_file[:-3]
    last_index = grammar_file.rfind("/")
    if last_index == -1:
        return grammar_file
    else:
        fail("The grammar file must be in the current package.")

def antlr_merge_grammar(name, lexer_grammar, parser_grammar, target_grammar):
    grammar_name = extract_grammar_name(target_grammar)
    combiner = "//antlropt/org/perses/antlr/util:combine_lexer_parser_bin"
    args = [
        "$(location %s)" % combiner,
        "--lexer-grammar $(location %s)" % lexer_grammar,
        "--parser-grammar $(location %s)" % parser_grammar,
        "--target-grammar $(location %s)" % target_grammar,
    ]

    native.genrule(
        name = name,
        outs = [target_grammar],
        srcs = [lexer_grammar, parser_grammar],
        cmd = " ".join(args),
        tools = [combiner],
    )

def antlr_codegen(
        name,
        parser_grammar_file,
        parser_java_file_name,
        java_pkg_name,
        lexer_java_file_name,
        lexer_grammar_file = None,
        aux_java_files = None):
    genrule_name = "%s_gen" % name

    current_pkg_name = native.package_name()
    java_pkg_folder = java_pkg_name.replace(".", "/")
    if not current_pkg_name.endswith(java_pkg_folder):
        fail("The current pkg name '%s' should end with %s" % (current_pkg_name, java_pkg_folder))

    commands = [
        "GRAMMAR_FILE=$(location %s)" % parser_grammar_file,
        "GRAMMAR_FILE_NAME=$$(basename $${GRAMMAR_FILE})",
        "TMP=$$(mktemp -d)",
        "cp $${GRAMMAR_FILE} $${TMP}",
    ]
    common_antlr_args = [
        "\"$(location //src/org/perses/grammar:antlr_tool)\"",
        "-no-listener",
        "-no-visitor",
        "-lib \"$${TMP}\"", # For antlr to locate the tokens files generated from the lexer grammar.
        "-package %s" % java_pkg_name,
    ]
    if lexer_grammar_file:
        commands.append("LEXER_GRAMMAR_FILE=$(location %s)" % lexer_grammar_file)
        commands.append("LEXER_GRAMMAR_FILE_NAME=$$(basename $${LEXER_GRAMMAR_FILE})")
        commands.append("cp $${LEXER_GRAMMAR_FILE} $${TMP}")

        antlr_args_for_lexer = common_antlr_args[:]  + ["\"$${TMP}/$${LEXER_GRAMMAR_FILE_NAME}\""]
        commands.append(" ".join(antlr_args_for_lexer))

    antlr_args_for_parser = common_antlr_args[:] + ["\"$${TMP}/$${GRAMMAR_FILE_NAME}\""]
    commands.append(" ".join(antlr_args_for_parser))
    commands.append("cp \"$${TMP}/%s\" $(location %s)" % (parser_java_file_name, parser_java_file_name))
    commands.append("cp \"$${TMP}/%s\" $(location %s)" % (lexer_java_file_name, lexer_java_file_name))

    grammar_files = [parser_grammar_file] + ([lexer_grammar_file] if lexer_grammar_file else [])

    native.genrule(
        name = genrule_name,
        outs = [lexer_java_file_name, parser_java_file_name],
        srcs = grammar_files,
        cmd = " ; \\\n".join(commands),
        tools = ["//src/org/perses/grammar:antlr_tool"],
    )
    aux_java_files = aux_java_files or []
    native.java_library(
        name = name,
        srcs = [
            lexer_java_file_name,
            parser_java_file_name,
        ] + aux_java_files,
        resources = grammar_files,
        deps = ["@maven//:org_antlr_antlr4_runtime"],
    )
