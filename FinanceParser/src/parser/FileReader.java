package parser;
import java.io.*;
import java.util.*;
// FileReader — reads input.txt (legacy plain-text mode)
// For real SMS use SmsFileReader instead
public class FileReader {
    public static List<String> readLines(String filePath) {
        List<String> lines=new ArrayList<>();
        try (
                InputStream is=FileReader.class.getClassLoader().getResourceAsStream(filePath);
                BufferedReader reader=new BufferedReader(new InputStreamReader(is))
        ) {
            String line;
            while ((line=reader.readLine())!=null) {
                line=line.trim();
                if (!line.isEmpty() && !line.startsWith("#"))
                    lines.add(line);
            }
        } catch (IOException e) {
            System.err.println("ERROR: Could not read file: "+filePath);
            e.printStackTrace();
        }
        System.out.println("[FileReader] Read "+lines.size()+" lines.");
        return lines;
    }
}
