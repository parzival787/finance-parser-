package parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

// SmsFileReader — reads sms_inbox.txt where SMS messages are separated by blank lines
//
// FIX APPLIED: Using fully-qualified java.io.FileReader instead of just FileReader
// This avoids the name collision with the custom parser.FileReader class in this project.
// The custom parser.FileReader uses getResourceAsStream() and accepts NO constructor args.
// java.io.FileReader accepts a String file path — which is what we need here.

public class SmsFileReader {

    /**
     * Reads all SMS messages from the given file path.
     * Each SMS is separated by a blank line.
     * Multi-line messages (Axis Bank format) keep their internal line breaks.
     *
     * @param filePath  absolute or relative path to sms_inbox.txt
     * @return  List of SMS strings, one element per message
     */
    public static List<String> readAllSms(String filePath) {
        List<String> smsList  = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        // KEY FIX: java.io.FileReader (fully-qualified) avoids clash with parser.FileReader
        try (BufferedReader br = new BufferedReader(new java.io.FileReader(filePath))) {

            String line;
            while ((line = br.readLine()) != null) {

                // Skip comment lines starting with #
                if (line.trim().startsWith("#")) continue;

                if (line.trim().isEmpty()) {
                    // Blank line = end of one complete SMS message
                    if (current.length() > 0) {
                        smsList.add(current.toString().trim());
                        current = new StringBuilder();
                    }
                } else {
                    // Append this line to the current SMS block
                    if (current.length() > 0) current.append("\n");
                    current.append(line);
                }
            }

            // Capture last message if file doesn't end with a blank line
            if (current.length() > 0) {
                smsList.add(current.toString().trim());
            }

        } catch (IOException e) {
            System.err.println("[SmsFileReader] ERROR: Cannot read file: " + filePath);
            System.err.println("  → Make sure the file exists at: " + filePath);
            System.err.println("  → If using a relative path, run from the project root directory");
            e.printStackTrace();
        }

        System.out.println("[SmsFileReader] Read " + smsList.size() + " SMS messages from: " + filePath);
        return smsList;
    }
}
