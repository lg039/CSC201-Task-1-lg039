import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CSC201 DSA Task 1 -- XML Checker
 * Solution One
 *
 * Student name :
 * Student ID   :
 *
 * Data-structure combination used by this solution:
 * array-based stack + array. a stack tracks currently open tags for
 * nesting/matching, a plain array holds attribute names seen in the tag
 * being parsed for duplicate checks.
 */
public class XmlCheckerSolutionOne {

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

    // tag name plus the line it opened on
    private static final class TagFrame {
        final String name;
        final int line;
        TagFrame(String name, int line) {
            this.name = name;
            this.line = line;
        }
    }

    // stack of open tags, array-backed, doubles when full
    private static final class ArrayStack {
        private TagFrame[] data = new TagFrame[16];
        private int top = -1;

        boolean isEmpty() {
            return top == -1;
        }

        void push(TagFrame frame) {
            if (top + 1 == data.length) {
                data = Arrays.copyOf(data, data.length * 2);
            }
            data[++top] = frame;
        }

        TagFrame pop() {
            TagFrame frame = data[top];
            data[top--] = null;
            return frame;
        }

        TagFrame peek() {
            return data[top];
        }
    }

    // valid character in a tag or attribute name
    private static boolean isNameChar(char c) {
        return !Character.isWhitespace(c) && c != '<' && c != '>' && c != '/'
                && c != '=' && c != '"' && c != '\'';
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

        ArrayStack stack = new ArrayStack();
        boolean rootClosed = false;

        int i = 0;
        while (i < len) {
            if (text[i] != '<') {
                i++;
                continue;
            }

            // skip xml declarations and comments, assumed correct
            if (i + 1 < len && text[i + 1] == '?') {
                int j = i + 2;
                while (j + 1 < len && !(text[j] == '?' && text[j + 1] == '>')) j++;
                i = j + 2;
                continue;
            }
            if (i + 3 < len && text[i + 1] == '!' && text[i + 2] == '-' && text[i + 3] == '-') {
                int j = i + 4;
                while (j + 2 < len && !(text[j] == '-' && text[j + 1] == '-' && text[j + 2] == '>')) j++;
                i = j + 3;
                continue;
            }

            int tagLine = lineOf[i];
            int j = i + 1;
            boolean closing = j < len && text[j] == '/';
            if (closing) j++;

            int nameStart = j;
            while (j < len && isNameChar(text[j])) j++;
            String name = new String(text, nameStart, j - nameStart);

            if (closing) {
                while (j < len && text[j] != '>') j++;
                i = j + 1;
                if (stack.isEmpty()) {
                    return Result.fail(tagLine, ErrorCode.MISMATCHED_TAG);
                }
                TagFrame top = stack.pop();
                if (!top.name.equals(name)) {
                    return Result.fail(tagLine, ErrorCode.MISMATCHED_TAG);
                }
                if (stack.isEmpty()) rootClosed = true;
                continue;
            }

            if (stack.isEmpty() && rootClosed) {
                return Result.fail(tagLine, ErrorCode.MULTIPLE_ROOTS);
            }

            boolean selfClosing = false;
            while (true) {
                while (j < len && Character.isWhitespace(text[j])) j++;
                if (j + 1 < len && text[j] == '/' && text[j + 1] == '>') {
                    selfClosing = true;
                    j += 2;
                    break;
                }
                if (j < len && text[j] == '>') {
                    j++;
                    break;
                }
                while (j < len && isNameChar(text[j])) j++;
                while (j < len && Character.isWhitespace(text[j])) j++;
                if (j < len && text[j] == '=') j++;
                while (j < len && Character.isWhitespace(text[j])) j++;

                if (j >= len || (text[j] != '"' && text[j] != '\'')) {
                    int errLine = j < len ? lineOf[j] : lineOf[len - 1];
                    return Result.fail(errLine, ErrorCode.UNQUOTED_ATTRIBUTE);
                }
                char quote = text[j];
                int valueLine = lineOf[j];
                j++;
                while (j < len && text[j] != quote) j++;
                // must close with the same quote it opened with
                if (j >= len) {
                    return Result.fail(valueLine, ErrorCode.UNQUOTED_ATTRIBUTE);
                }
                j++;
            }
            i = j;

            if (selfClosing) {
                if (stack.isEmpty()) rootClosed = true;
            } else {
                stack.push(new TagFrame(name, tagLine));
            }
        }

        if (!stack.isEmpty()) {
            return Result.fail(stack.peek().line, ErrorCode.UNCLOSED_TAG);
        }
        return Result.ok();
    }

}
