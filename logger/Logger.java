package logger;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;


public class Logger {
    static FileOutputStream outputFileName;
    static OutputStreamWriter outputWriter;

    public static void initializer(String fileName) throws IOException {
        outputFileName = new FileOutputStream(fileName);
        outputWriter = new OutputStreamWriter(outputFileName, StandardCharsets.UTF_8);
    }

    public static void abort() {
        try {
            outputWriter.flush();
            outputFileName.close();
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    public static void writeLog(String str) {
        try {
            outputWriter.write(str + '\n');
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}