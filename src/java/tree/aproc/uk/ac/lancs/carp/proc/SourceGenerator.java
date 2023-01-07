// -*- c-basic-offset: 4; indent-tabs-mode: nil -*-

/*
 * Copyright 2021,2022, Lancaster University
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * 
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the
 *    distribution.
 * 
 *  * Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * Author: Steven Simpson <https://github.com/simpsonst>
 */

package uk.ac.lancs.carp.proc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.annotation.Annotation;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.processing.Completion;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;
import uk.ac.lancs.carp.deploy.Deploy;
import uk.ac.lancs.carp.deploy.Deploys;
import uk.ac.lancs.carp.map.ExternalName;
import uk.ac.lancs.carp.model.ExpansionContext;
import uk.ac.lancs.carp.model.LoadContext;
import uk.ac.lancs.carp.model.ModuleDefinition;
import uk.ac.lancs.carp.model.QualificationContext;
import uk.ac.lancs.carp.model.QualificationReporter;
import uk.ac.lancs.carp.model.QualifiedDocumentation;
import uk.ac.lancs.carp.model.TextFile;
import uk.ac.lancs.carp.model.Type;
import uk.ac.lancs.carp.model.std.BuiltIns;
import uk.ac.lancs.carp.model.syntax.SourceAssociator;
import uk.ac.lancs.carp.model.syntax.SyntaxTypeFactory;
import uk.ac.lancs.carp.syntax.CommentAssociator;
import uk.ac.lancs.carp.syntax.CommentEliminator;
import uk.ac.lancs.carp.syntax.KeywordRecognizer;
import uk.ac.lancs.carp.syntax.TokenType;
import uk.ac.lancs.carp.syntax.doc.DocTokenType;
import uk.ac.lancs.carp.syntax.doc.Documentation;
import uk.ac.lancs.scc.jardeps.Service;
import uk.ac.lancs.syntax.LL1Grammar;
import uk.ac.lancs.syntax.Lexicon;
import uk.ac.lancs.syntax.Node;
import uk.ac.lancs.syntax.Parser;
import uk.ac.lancs.syntax.TextPosition;
import uk.ac.lancs.syntax.Token;

/**
 * Identifies packages annotated with {@link Deploy}, and generates
 * sufficient RPC code in them to allow client and server code to be
 * written. Only interfaces are generated; implementations are generated
 * at run time.
 *
 * @author simpsons
 */
@Service(Processor.class)
public final class SourceGenerator implements Processor {
    /**
     * The phrase-structure grammar of an IDL
     */
    private static final LL1Grammar<TokenType> syntax;

    /**
     * The lexical grammar of an IDL
     */
    private static final Lexicon<TokenType> lexicon;

    static {
        try {
            syntax = new LL1Grammar<>(TokenType.class);
            lexicon = new Lexicon<>(TokenType.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new AssertionError("grammar defined incorrectly", ex);
        }
    }

    private static final LL1Grammar<DocTokenType> docSyntax;

    private static final Lexicon<DocTokenType> docLexicon;

    static {
        try {
            docSyntax = new LL1Grammar<>(DocTokenType.class);
            docLexicon = new Lexicon<>(DocTokenType.class);
        } catch (IllegalAccessException | NoSuchFieldException ex) {
            throw new AssertionError("doc grammar defined incorrectly", ex);
        }
    }

    private final Set<Class<? extends Annotation>> TYPES =
        Set.copyOf(Arrays.asList(Deploys.class, Deploy.class));

    /**
     * Get a resource containing the IDL for a module.
     * 
     * @param moduleName the module name
     * 
     * @param filer the provider of access to an abstract file system
     * 
     * @return an object to read the file
     * 
     * @throws IOException if the file cannot be opened
     * 
     * @throws FilerException if the same pathname has already been
     * opened for writing, if the source module cannot be determined, or
     * if the target module is not writable, or if an explicit target
     * module is specified and the location does not support it
     * 
     * @throws IllegalArgumentException for an unsupported location
     */
    private static FileObject getModuleSource(ExternalName moduleName,
                                              Filer filer)
        throws IOException {
        try {
            return filer.getResource(StandardLocation.SOURCE_PATH,
                                     moduleName.asJavaPackageName(),
                                     ExternalName.JAVA_IDL_LEAF_NAME);
        } catch (FileNotFoundException ex) {
            return filer.getResource(StandardLocation.CLASS_PATH,
                                     moduleName.asJavaPackageName(),
                                     ExternalName.JAVA_IDL_LEAF_NAME);
        }
    }

    /**
     * Get a resource containing the imported properties for a module.
     * 
     * @param moduleName the module name
     * 
     * @param filer the provider of access to an abstract file system
     * 
     * @return an object to read the file
     * 
     * @throws IOException if the file cannot be opened
     * 
     * @throws FilerException if the same pathname has already been
     * opened for writing, if the source module cannot be determined, or
     * if the target module is not writable, or if an explicit target
     * module is specified and the location does not support it
     * 
     * @throws IllegalArgumentException for an unsupported location
     */
    private FileObject getImportedProperties(ExternalName moduleName,
                                             Filer filer)
        throws IOException {
        return filer.getResource(StandardLocation.CLASS_PATH,
                                 moduleName.asJavaPackageName(),
                                 ExternalName.JAVA_PROPERTIES_LEAF_NAME);
    }

    /**
     * Create a resource to output the properties of a module.
     * 
     * @param moduleName the module name
     * 
     * @param filer the provider of access to an abstract file system
     * 
     * @return an object to write the file
     * 
     * @throws IOException if the file cannot be opened
     * 
     * @throws FilerException if the same pathname has already been
     * opened for writing, if the source module cannot be determined, or
     * if the target module is not writable, or if an explicit target
     * module is specified and the location does not support it
     * 
     * @throws IllegalArgumentException for an unsupported location
     */
    private FileObject getExportedProperties(ExternalName moduleName,
                                             Filer filer)
        throws IOException {
        return filer.createResource(StandardLocation.CLASS_OUTPUT,
                                    moduleName.asJavaPackageName(),
                                    ExternalName.JAVA_PROPERTIES_LEAF_NAME);
    }

    @Override
    public Set<String> getSupportedOptions() {
        return Collections.emptySet();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        return Collections.singleton("*");
        // return Collections.singleton(Deploy.class.getName());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public Iterable<? extends Completion>
        getCompletions(Element element, AnnotationMirror annotation,
                       ExecutableElement member, String userText) {
        return Collections.emptySet();
    }

    @Override
    public void init(ProcessingEnvironment processingEnv) {
        this.env = processingEnv;
        this.messager = this.env.getMessager();
        this.types = this.env.getTypeUtils();
        this.elements = this.env.getElementUtils();
        this.filer = this.env.getFiler();
    }

    private ProcessingEnvironment env;

    private Messager messager;

    @SuppressWarnings("unused")
    private Types types;

    private Elements elements;

    private Filer filer;

    /**
     * Maps module names to Java packages (or rather, the program
     * elements that specified the mapping), as specified by applied
     * {@link Deploy} annotations. Call
     * {@link #getModulePackage(ExternalName)} to map a module name to a
     * package.
     */
    private final Map<ExternalName, Collection<Element>> applications =
        new HashMap<>();

    private final Map<ExternalName,
                      Collection<PackageElement>> packageApplications =
                          new HashMap<>();

    /**
     * Get the Java package that a module has been mapped to. Do not
     * call on a module that has been erroneously applied to more than
     * one Java package.
     * 
     * @param moduleName the module name (no!)
     * 
     * @return the package the module has been mapped to; or
     * {@code null} if no mapping is available
     */
    private PackageElement getModulePackage(ExternalName moduleName) {
        Collection<PackageElement> res = packageApplications.get(moduleName);
        if (res == null) return null;
        assert res.size() == 1;
        return res.iterator().next();
    }

    /**
     * Maps module names to their definitions, whether parsed from IDL
     * or loaded from imported properties.
     */
    private final Map<ExternalName, ModuleDefinition> moduleDefs =
        new HashMap<>();

    /**
     * Identifies modules that have been processed for references to
     * other types.
     */
    private final Collection<ExternalName> processedModules = new HashSet<>();

    /**
     * Records types that were referenced but not found. This might
     * happen because no module definition was found (whether from
     * source or class path), or because the module definition was
     * loaded but did not contain the named type.
     */
    private final Collection<ExternalName> missingTypes = new HashSet<>();

    /**
     * Records modules whose types were referenced but which modules
     * where not found. That is, the module itself was not defined.
     */
    private final Collection<ExternalName> missingMods = new HashSet<>();

    /**
     * Maps referenced types to the set of types that directly refer to
     * them.
     */
    private final Map<ExternalName, Collection<ExternalName>> references =
        new HashMap<>();

    /**
     * Get the model for a given type. {@link #missingMods},
     * {@link #missingTypes} and {@link #moduleDefs} are updated,
     * according to what was found. If a type is already in
     * {@link #missingTypes}, the method immediately returns
     * {@code null}.
     * 
     * @param typeName the type's name
     * 
     * @return the type's model; or {@code null} if not found
     */
    private Type getModel(ExternalName typeName) {
        /* Stop now if we've already determined that this type has not
         * been defined. */
        if (missingTypes.contains(typeName)) return null;

        /* Ensure we have the module definition loaded. Stop now if
         * we've already determined that the whole module is missing.
         * Use the loaded module definition if we've got it. Otherwise,
         * try to load the definition. */
        ExternalName moduleName = typeName.getParent();

        /* The module name will be null if the type name is a leaf. This
         * might happen if a type name is passed that failed to
         * qualify. */
        if (moduleName == null) return null;

        if (missingMods.contains(moduleName)) return null;
        ModuleDefinition defn = moduleDefs.get(moduleName);
        if (defn == null) {
            /* Try to load the module containing the missing class. */
            try {
                FileObject classFile = getImportedProperties(moduleName, filer);
                Properties props = new Properties();
                try (InputStream in = classFile.openInputStream()) {
                    props.loadFromXML(in);
                } catch (InvalidPropertiesFormatException ex) {
                    try (Reader in = classFile.openReader(false)) {
                        props.load(in);
                    }
                }
                LoadContext loadCtxt =
                    () -> SourceGenerator.class.getClassLoader();
                defn = ModuleDefinition
                    .load("module.", props,
                          SourceGenerator.class.getClassLoader(), loadCtxt);
                moduleDefs.put(moduleName, defn);

                /* Also load the mapping from this module to Java. */
                String pkgName = props.getProperty("module.java-package");
                PackageElement pkg = elements.getPackageElement(pkgName);
                packageApplications.merge(moduleName,
                                          Collections.singleton(pkg),
                                          SourceGenerator::merger);
            } catch (IOException ex) {
                /* Prevent us from trying to load this module or look up
                 * this type again. */
                missingMods.add(moduleName);
                missingTypes.add(typeName);
                return null;
            }
        }

        /* Get the sought definition. If not present, record that we
         * tried, so we don't try again. */
        Type result = defn.types.get(typeName);
        if (result == null) missingTypes.add(typeName);
        return result;
    }

    /**
     * Maps grammatical elements to module names.
     */
    private final Map<Object, ExternalName> elemMods = new WeakHashMap<>();

    /**
     * Maps grammatical elements to start positions in source.
     */
    private final Map<Object, TextPosition> elemStarts = new WeakHashMap<>();

    /**
     * Maps grammatical elements to end positions in source.
     */
    private final Map<Object, TextPosition> elemEnds = new WeakHashMap<>();

    /**
     * Maps grammatical elements to documentation comments.
     */
    private final Map<Object, QualifiedDocumentation> elemComments =
        new WeakHashMap<>();

    /**
     * Maps grammatical elements to documentation comments.
     */
    private final Map<Token<TokenType>, Token<TokenType>> docCommentText =
        new WeakHashMap<>();

    private final Collection<ExternalName> brokenModules = new HashSet<>();

    private void loadSourceDefinition(ExternalName moduleName,
                                      Element definingElement) {
        /* Record that this class and package attempted to apply this
         * module. */
        applications.merge(moduleName, Collections.singleton(definingElement),
                           SourceGenerator::merger);
        PackageElement javaPackage = elements.getPackageOf(definingElement);
        packageApplications.merge(moduleName,
                                  Collections.singleton(javaPackage),
                                  SourceGenerator::merger);

        /* A module cannot be applied to two distinct packages. Do
         * nothing more for now, and report errors during the final
         * phase. This also means we don't try to parseDefinition the
         * source twice. */
        if (packageApplications.get(moduleName).size() > 1) return;

        /* Don't re-parse an IDL. */
        if (moduleDefs.containsKey(moduleName)) return;

        /* Open and parse the file. */
        Node<TokenType> tree;
        try {
            FileObject srcFile = getModuleSource(moduleName, filer);
            Parser<TokenType> parser = syntax.newParser(TokenType.DECLS);
            KeywordRecognizer keyRec =
                new KeywordRecognizer(parser, KeywordRecognizer
                    .mapUpdater(docCommentText, true));
            CommentAssociator comAss =
                new CommentAssociator(keyRec, docCommentText);
            CommentEliminator comElim = new CommentEliminator(comAss);
            try (Reader in = srcFile.openReader(false)) {
                lexicon.tokenize(in, comElim);
            }
            tree = parser.root();

            Token<TokenType> fault = parser.fault();
            if (fault != null) {
                messager.printMessage(
                                      Kind.ERROR, "syntax error in "
                                          + moduleName + " at " + fault,
                                      definingElement);
                brokenModules.add(moduleName);
                return;
            }
        } catch (IllegalArgumentException ex) {
            messager.printMessage(Kind.ERROR, "problem fetching resource: "
                + ex.getMessage(), definingElement);
            return;
        } catch (IOException ex) {
            messager.printMessage(Kind.ERROR,
                                  "error accessing IDL " + moduleName + ": "
                                      + ex.getClass().getCanonicalName() + ": "
                                      + ex.getMessage(),
                                  definingElement);
            return;
        }

        /* Perform left association on the tree. */
        tree = TokenType.postprocess(tree);

        /* Specify how to store documentation comments with their type
         * definitions. */
        Map<Object, Documentation> newDocs = new IdentityHashMap<>();
        SourceAssociator commenter = (obj, start) -> {
            elemMods.put(obj, moduleName);
            elemStarts.put(obj, start.start);
            var odc = Stream.concat(Stream.of(start), start.children().stream())
                .map(docCommentText::get).filter(x -> x != null).findFirst();
            if (odc.isEmpty()) return;
            Token<TokenType> dc = odc.get();

            /* Convert the document comment token to text, stripping out
             * leading characters. */
            String dcText = CommentAssociator.extractDocComment(dc);

            /* Parse the text as a documentation comment. */
            Parser<DocTokenType> parser =
                docSyntax.newParser(DocTokenType.DOCUMENT);
            try (Reader in = new StringReader(dcText)) {
                docLexicon.tokenize(in, parser);
            } catch (IOException ex) {
                throw new AssertionError("unreachable", ex);
            }

            /* Check for failure. */
            Token<DocTokenType> fault = parser.fault();
            if (fault != null) {
                /* Work out where the error is, as our tokens have
                 * line/column numbers based on the extracted comment
                 * content. */
                TextPosition localStart = fault.start;
                TextPosition fileStart =
                    TextPosition.of(localStart.line + dc.start.line - 1,
                                    localStart.column + dc.start.column - 1);

                /* Pass the fault on as a note. It just means
                 * documentation won't be available for this element. It
                 * should not stop code generation, etc. */
                messager.printMessage(Kind.NOTE,
                                      "expected " + parser.expected() + " at "
                                          + fileStart + "; got " + fault.type,
                                      definingElement);
            } else {
                /* Post-process the documentation tokens to overcome the
                 * limitations of LL1 grammars. Convert to a
                 * documentation structure, and store with the
                 * element. */
                var pp = DocTokenType.postprocess(parser.root());
                Documentation doc = Documentation.parse(pp);
                // elemComments.put(obj, doc);
                newDocs.put(obj, doc);
            }
        };

        /* Translate the syntax tree into type structures. */
        ModuleDefinition defn = SyntaxTypeFactory
            .parseDefinition(tree, commenter,
                             SourceGenerator.class.getClassLoader());

        /* Remember how to resolve local names referenced in each
         * documentation comment. */
        QualificationContext qctxt = BuiltIns.wrap(defn
            .getQualificationContext(moduleName, converge(),
                                     unqualifiedReporter(moduleName,
                                                         definingElement)));
        for (var entry : newDocs.entrySet()) {
            QualifiedDocumentation qd =
                new QualifiedDocumentation(entry.getValue(), qctxt);
            elemComments.put(entry.getKey(), qd);
        }

        if (false) for (var entry : elemComments.entrySet()) {
            System.err.printf("%s: %s%n", entry.getKey(), entry.getValue());
        }

        /* Resolve unqualified names in the module against the module
         * name and imports. */
        defn = defn.qualify(moduleName, qctxt);

        /* Index the module by its name. */
        moduleDefs.put(moduleName, defn);
    }

    private QualificationReporter unqualifiedReporter(ExternalName moduleName,
                                                      Element definingElement) {
        return (name, line, column) -> {
            messager.printMessage(Kind.ERROR,
                                  "unknown name " + name + " in module "
                                      + moduleName + ", line " + line
                                      + ", column " + column,
                                  definingElement);
            brokenModules.add(moduleName);
        };
    }

    private BiConsumer<Object, Object> converge() {
        return converge(elemMods).andThen(converge(elemStarts))
            .andThen(converge(elemEnds)).andThen(converge(elemComments));
    }

    private static <K, V> BiConsumer<K, K> converge(Map<K, V> map) {
        return (to, from) -> map.put(to, map.get(from));
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations,
                           RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            /* TODO: Generate errors for multiply-applied modules. */

            /* TODO: Generate errors for missing types. */
            return false;
        }

        /* Get a mapping from IDL module names to the packages they are
         * to be deployed in. */
        Collection<ExternalName> sourceModules = new HashSet<>();
        for (Element elem : roundEnv.getElementsAnnotatedWithAny(TYPES)) {
            /* Parse each of the names as an ExternalName, and apply an
             * error to the approriate element if it doesn't
             * parseDefinition. */
            final List<Deploy> list;
            {
                Deploy single = elem.getAnnotation(Deploy.class);
                if (single == null) {
                    Deploys group = elem.getAnnotation(Deploys.class);
                    if (group == null) {
                        list = Collections.emptyList();
                    } else {
                        list = Arrays.asList(group.value());
                    }
                } else {
                    list = Collections.singletonList(single);
                }
            }

            for (Deploy item : list) {
                final ExternalName name;
                try {
                    name = ExternalName.parse(item.value());
                } catch (IllegalArgumentException ex) {
                    /* TODO: Can we apply the message to the annotation
                     * itself, instead of the class or package? */
                    messager.printMessage(Kind.ERROR,
                                          "bad name: " + ex.getMessage(), elem);
                    continue;
                }

                /* Load the module's source. Record this as a source
                 * module, as we need to generate Java source from it
                 * later. */
                loadSourceDefinition(name, elem);
                sourceModules.add(name);
            }
        }

        if (!brokenModules.isEmpty()) return false;

        /* Go through all modules, looking for referenced types. For
         * each referenced type, ensure the defining module is loaded.
         * Also keep track of which */
        BiConsumer<ExternalName, ExternalName> refMerger =
            (referee, referrent) -> {
                /* Record which types refer to other types. */
                references.merge(referee, Collections.singleton(referrent),
                                 SourceGenerator::merger);

                /* Ensure that the defining module has been loaded. */
                getModel(referee);
            };
        while (true) {
            /* Pick any loaded module that hasn't been loaded yet. */
            Optional<Map.Entry<ExternalName, ModuleDefinition>> any = moduleDefs
                .entrySet().stream()
                .filter(e -> !processedModules.contains(e.getKey())).findAny();
            if (any.isEmpty()) break;
            ModuleDefinition module = any.get().getValue();

            /* Go through the types defined by this module, and ensure
             * that modules containing referenced types are loaded. */
            module.gatherReferences(refMerger);

            /* Mark this process as done. */
            processedModules.add(any.get().getKey());
        }

        for (ExternalName moduleName : sourceModules) {
            ExpansionContext ctxt = new ExpansionContext() {
                @Override
                public Type getModel(ExternalName idlName) {
                    return SourceGenerator.this.getModel(idlName);
                }

                @Override
                public String getTarget(ExternalName moduleName) {
                    PackageElement pkg = getModulePackage(moduleName);
                    return pkg.toString();
                }

                @Override
                public QualifiedDocumentation getDocs(Object element) {
                    return elemComments.get(element);
                }

                @Override
                public void reportBadReference(TextPosition pos, String text,
                                               String detail) {
                    PackageElement pkgElem = getModulePackage(moduleName);
                    messager.printMessage(Kind.WARNING,
                                          detail + " at " + pos + " in module "
                                              + moduleName + ": " + text,
                                          pkgElem);
                }
            };
            ctxt = BuiltIns.wrap(ctxt);
            ModuleDefinition module = moduleDefs.get(moduleName);
            if (module == null) continue;
            PackageElement pkg = getModulePackage(moduleName);

            /* Generate user-facing classes. */
            for (Map.Entry<ExternalName, Type> entry : module.types
                .entrySet()) {
                ExternalName typeName = entry.getKey();
                Type model = entry.getValue();
                if (!model.mustBeDefinedInJava()) continue;
                String sourceName =
                    pkg.toString() + '.' + typeName.getLeaf().asJavaClassName();
                try {
                    JavaFileObject srcFile = filer.createSourceFile(sourceName);
                    try (Writer fout = srcFile.openWriter();
                         TextFile out = new TextFile(fout, 4, true)) {
                        model.defineJavaType(out, typeName, ctxt);
                    }
                } catch (IOException e) {
                    messager.printMessage(Kind.ERROR,
                                          "Failed to write IDL type " + typeName
                                              + " into " + sourceName);
                }
            }

            /* Generate properties files for runtime and later
             * compilation. */
            try {
                Properties props = new Properties();
                module.describe("module.", props);
                props.setProperty("module.java-package", pkg.toString());
                FileObject propsFile = getExportedProperties(moduleName, filer);
                try (OutputStream out = propsFile.openOutputStream()) {
                    props.storeToXML(out,
                                     "CARP properties for module " + moduleName,
                                     StandardCharsets.UTF_8);
                }
            } catch (IOException e) {
                messager.printMessage(Kind.ERROR,
                                      "Failed to write IDL properties "
                                          + moduleName);
            }
        }

        return false;
    }

    /**
     * Merge two collections. This is intended for use with
     * {@link Map#merge(Object, Object, java.util.function.BiFunction)}.
     * 
     * @param a the existing collection
     * 
     * @param b the additional collection
     * 
     * @return the union of the two input collections
     */
    private static <T> Collection<T> merger(Collection<T> a, Collection<T> b) {
        return Stream.concat(a.stream(), b.stream())
            .collect(Collectors.toSet());
    }
}
