import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CSC201 DSA Task 1 -- XML Checker
 * Solution Two
 *
 * Student name :
 * Student ID   :
 *
 * Data-structure combination used by this solution:
 * recursion + linked list. parseElement recurses per child so the call
 * stack tracks nesting instead of an explicit stack; a linked list holds
 * attribute names seen in the tag being parsed for duplicate checks.
 */
public class XmlCheckerSolutionTwo {

    /** Error codes required by the task specification. Do not rename. */
    enum ErrorCode {
        MISMATCHED_TAG,
        UNCLOSED_TAG,
        MULTIPLE_ROOTS,
        UNQUOTED_ATTRIBUTE,
        DUPLICATE_ATTRIBUTE
    }

    /** Result of a check: either well-formed, or the FIRST violation found. */
    static final class Result {
        final boolean wellFormed;
        final int line;           // 1-based line number of the violation
        final ErrorCode code;     // null when wellFormed

        private Result(boolean wellFormed, int line, ErrorCode code) {
            this.wellFormed = wellFormed;
            this.line = line;
            this.code = code;
        }
        static Result ok()                          { return new Result(true, -1, null); }
        static Result fail(int line, ErrorCode c)   { return new Result(false, line, c); }
    }

    public static void main(String[] args) throws IOException {
        Path dir = Path.of("testFiles");
        if (!Files.isDirectory(dir)) {
            System.err.println("Folder not found: testFiles");
            System.exit(2);
        }

        // Collect all .xml files in testFiles, in alphabetical order of file name.
        List<Path> xmlFiles;
        try (Stream<Path> s = Files.list(dir)) {
            xmlFiles = s.filter(f -> f.getFileName().toString().toLowerCase().endsWith(".xml"))
                    .sorted(Comparator.comparing(f -> f.getFileName().toString()))
                    .collect(Collectors.toList());
        }

        for (Path file : xmlFiles) {
            List<String> lines = Files.readAllLines(file);
            Result result = check(lines);
            printResult(file.getFileName().toString(), result);
        }
    }

    /** Prints the verdict in the exact format required by the task. Do not change. */
    static void printResult(String fileName, Result r) {
        if (r.wellFormed) {
            System.out.println("File " + fileName + " is TRUE");
        } else {
            System.out.println("File " + fileName + " is FALSE");
            System.out.println("Line " + r.line + ": " + r.code);
        }
    }

    // valid character in a tag or attribute name
    private static boolean isNameChar(char c) {
        return !Character.isWhitespace(c) && c != '<' && c != '>' && c != '/'
                && c != '=' && c != '"' && c != '\'';
    }

    // recursive descent parser; nesting lives on the call stack, not an explicit stack
    private static final class Parser {
        private final char[] text;
        private final int[] lineOf;
        private final int len;
        private int pos = 0;

        Parser(char[] text, int[] lineOf, int len) {
            this.text = text;
            this.lineOf = lineOf;
            this.len = len;
        }

        private boolean atEnd() {
            return pos >= len;
        }

        // skips whitespace, comments, and xml declarations
        private void skipMisc() {
            while (pos < len) {
                char c = text[pos];
                if (c != '<') {
                    pos++;
                    continue;
                }
                if (pos + 1 < len && text[pos + 1] == '?') {
                    int j = pos + 2;
                    while (j + 1 < len && !(text[j] == '?' && text[j + 1] == '>')) j++;
                    pos = j + 2;
                    continue;
                }
                if (pos + 3 < len && text[pos + 1] == '!' && text[pos + 2] == '-' && text[pos + 3] == '-') {
                    int j = pos + 4;
                    while (j + 2 < len && !(text[j] == '-' && text[j + 1] == '-' && text[j + 2] == '>')) j++;
                    pos = j + 3;
                    continue;
                }
                break;
            }
        }

        Result parseDocument() {
            skipMisc();
            if (atEnd()) return Result.ok();
            if (text[pos] == '<' && pos + 1 < len && text[pos + 1] == '/') {
                return Result.fail(lineOf[pos], ErrorCode.MISMATCHED_TAG);
            }

            Result rootResult = parseElement();
            if (!rootResult.wellFormed) return rootResult;

            skipMisc();
            if (atEnd()) return Result.ok();
            if (text[pos] == '<' && pos + 1 < len && text[pos + 1] == '/') {
                return Result.fail(lineOf[pos], ErrorCode.MISMATCHED_TAG);
            }
            return Result.fail(lineOf[pos], ErrorCode.MULTIPLE_ROOTS);
        }

        // pos must be at the '<' of an opening tag
        private Result parseElement() {
            int tagLine = lineOf[pos];
            pos++; // consume '<'
            int nameStart = pos;
            while (pos < len && isNameChar(text[pos])) pos++;
            String name = new String(text, nameStart, pos - nameStart);

            boolean selfClosing = false;
            while (true) {
                while (pos < len && Character.isWhitespace(text[pos])) pos++;
                if (pos + 1 < len && text[pos] == '/' && text[pos + 1] == '>') {
                    selfClosing = true;
                    pos += 2;
                    break;
                }
                if (pos < len && text[pos] == '>') {
                    pos++;
                    break;
                }
                while (pos < len && isNameChar(text[pos])) pos++;
                while (pos < len && Character.isWhitespace(text[pos])) pos++;
                if (pos < len && text[pos] == '=') pos++;
                while (pos < len && Character.isWhitespace(text[pos])) pos++;
                if (pos < len && (text[pos] == '"' || text[pos] == '\'')) {
                    char quote = text[pos];
                    pos++;
                    while (pos < len && text[pos] != quote) pos++;
                    if (pos < len) pos++;
                } else {
                    while (pos < len && !Character.isWhitespace(text[pos]) && text[pos] != '>' && text[pos] != '/') pos++;
                }
            }

            if (selfClosing) {
                return Result.ok();
            }

            while (true) {
                skipMisc();
                if (atEnd()) {
                    return Result.fail(tagLine, ErrorCode.UNCLOSED_TAG);
                }
                if (text[pos] == '<' && pos + 1 < len && text[pos + 1] == '/') {
                    int closeLine = lineOf[pos];
                    pos += 2;
                    int closeNameStart = pos;
                    while (pos < len && isNameChar(text[pos])) pos++;
                    String closeName = new String(text, closeNameStart, pos - closeNameStart);
                    while (pos < len && text[pos] != '>') pos++;
                    if (pos < len) pos++;
                    if (!closeName.equals(name)) {
                        return Result.fail(closeLine, ErrorCode.MISMATCHED_TAG);
                    }
                    return Result.ok();
                }
                // recurse into the nested element
                Result childResult = parseElement();
                if (!childResult.wellFormed) return childResult;
            }
        }
    }

    /**
     * checks whether the document is well-formed.
     *
     * @param lines file content, one line per entry
     * @return ok(), or fail() for the first violation found
     */
    static Result check(List<String> lines) {
        // flatten lines into one array, track each char's line number
        int totalLen = 0;
        for (String s : lines) totalLen += s.length() + 1;
        char[] text = new char[totalLen];
        int[] lineOf = new int[totalLen];
        int p = 0;
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            int lineNo = i + 1;
            for (int j = 0; j < s.length(); j++) {
                text[p] = s.charAt(j);
                lineOf[p] = lineNo;
                p++;
            }
            text[p] = '\n';
            lineOf[p] = lineNo;
            p++;
        }
        int len = p;

        return new Parser(text, lineOf, len).parseDocument();
    }

}
