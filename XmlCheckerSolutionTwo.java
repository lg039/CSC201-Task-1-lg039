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
 *
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

    /**
     * TODO: implement your second checking solution here.
     *
     * @param lines the file content, one entry per line (line i of the file is
     *              lines.get(i - 1), so line numbers for error reporting are
     *              index + 1)
     * @return Result.ok() if well-formed, otherwise Result.fail(line, code)
     *         for the FIRST violation in reading order.
     */
    static Result check(List<String> lines) {
        // ---------------- YOUR CODE STARTS HERE ----------------
        throw new UnsupportedOperationException("check() not implemented yet");
        // ----------------- YOUR CODE ENDS HERE -----------------
    }

}
