package io.github.nullptr.tools.annotations.processor.builder;

import io.github.nullptr.tools.annotations.BuilderArgumentGenerator;
import io.github.nullptr.tools.annotations.processor.AnnotationProcessorHelper;
import io.github.nullptr.tools.annotations.processor.ImportScanner;
import io.github.nullptr.tools.string.StringHelper;
import io.github.nullptr.tools.types.Pair;

import javax.lang.model.element.Element;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class BuilderAnnotationWriter {

    private final Element element;
    private final String className;
    private final JavaFileObject file;
    private final Map<Element, BuilderArgumentGenerator> fields;
    private final Map<Element, Pair<String, String>> fieldsType;

    public BuilderAnnotationWriter(Element element, String className, JavaFileObject file) {
        this.element = element;
        this.className = className;
        this.file = file;
        this.fieldsType = new HashMap<>();
        this.fields = AnnotationProcessorHelper.getAnnotations(element, BuilderArgumentGenerator.class);
        
        for (final Map.Entry<Element, BuilderArgumentGenerator> entry : this.fields.entrySet()) {
            final Element field = entry.getKey();
            final Pair<String, String> type = AnnotationProcessorHelper.getType(field);
            
            this.fieldsType.put(field, type);
        }
    }

    public void write() throws IOException {
        final String packageName = this.element.getEnclosingElement().toString();

        try (final PrintWriter writer = new PrintWriter(this.file.openWriter())) {
            writer.println("package " + packageName + ";");
            writer.println();

            final ImportScanner scanner = new ImportScanner();
            scanner.scan(this.element);

            writer.println("import java.util.function.Supplier;");
            writer.println("import io.github.nullptr.tools.builder.IBuilder;");
            writer.println("import io.github.nullptr.tools.builder.BuilderArgument;");
            writer.println();

            scanner.getImports().forEach(name -> writer.println("import " + name + ";"));
            writer.println();

            writer.println("public class " + this.className + " implements IBuilder<" + this.element.getSimpleName() + "> {");
            writer.println();

            for (final Element field : this.fields.keySet()) {
                final Pair<String, String> type = this.fieldsType.get(field);

                writer.println("    private final BuilderArgument<" + type.getRight() + "> " + field.getSimpleName() + ";");
            }

            writer.println();

            writer.println("    public " + this.className + "() {");
            for (final Map.Entry<Element, BuilderArgumentGenerator> entry : this.fields.entrySet()) {
                final Element field = entry.getKey();
                final BuilderArgumentGenerator generator = entry.getValue();
                final Pair<String, String> type = this.fieldsType.get(field);

                writer.print("        this." + field.getSimpleName() + " = new BuilderArgument<" + type.getRight() + ">(\"" + generator.name() + "\")");
                writer.println("." + (generator.required() ? "required()" : "optional()") + ";");
            }
            writer.println("    }");
            writer.println();

            for (final Map.Entry<Element, BuilderArgumentGenerator> entry : this.fields.entrySet()) {
                final Element field = entry.getKey();
                final Pair<String, String> type = this.fieldsType.get(field);
                final String capitalized = StringHelper.capitalize(field.getSimpleName().toString());

                writer.println("    public " + this.className + " with" + capitalized + "(Supplier<" + type.getRight() + "> " + field.getSimpleName() + ") {");
                writer.println("        this." + field.getSimpleName() + ".set(" + field.getSimpleName() + ");");
                writer.println("        return this;");
                writer.println("    }");
                writer.println();
            }

            writer.println("    @Override");
            writer.println("    public " + this.element.getSimpleName() + " build() {");
            writer.println("        return new " + this.element.getSimpleName() + "(");
            for (final Map.Entry<Element, BuilderArgumentGenerator> entry : this.fields.entrySet()) {
                final Element field = entry.getKey();
                final boolean shouldAddComma = entry.getKey() != this.fields.keySet().toArray()[this.fields.size() - 1];

                writer.println("            this." + field.getSimpleName() + ".get()" + (shouldAddComma ? "," : ""));
            }
            writer.println("        );");
            writer.println("    }");

            writer.println("}");
        }
    }
}
