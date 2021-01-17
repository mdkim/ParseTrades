import java.io.IOException;
import java.io.Writer;

class SimplePrinter {
    Writer writer;
    boolean isWriterOut;

    SimplePrinter(Writer writer, boolean isWriterOut) {
        this.writer = writer;
    }
    void print(String text) throws IOException {
        if (isWriterOut) {
            writer.write(text);
        } else {
            System.out.print(text);
        }
    }
    void println(String text) throws IOException {
        if (isWriterOut) {
            writer.write(text);
            writer.write("\n");
        } else {
            System.out.println(text);
        }
    }
    void println() throws IOException {
        if (isWriterOut) {
            writer.write("\n");
        } else {
            System.out.println();
        }
    }
}