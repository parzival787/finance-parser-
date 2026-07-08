package parser;
import java.io.*;
import java.util.*;
// SmsXmlConverter — reads Android SMS backup XML, extracts bank SMS, writes sms_inbox.txt
// Usage: SmsXmlConverter.convert("Downloads/backup.xml","resources/sms_inbox.txt");
public class SmsXmlConverter {
    private static final String[] BANK_KEYWORDS={
            "SBI","HDFC","ICICI","Axis Bank","Kotak","PNB","Bank of Baroda","Canara",
            "debited","credited","A/c no","Avl Bal","UPI","INR"
    };
    public static void convert(String xmlPath, String outputPath) throws Exception {
        List<String> smsList=new ArrayList<>();
        try (BufferedReader br=new BufferedReader(new java.io.FileReader(xmlPath))) {
            String line;
            while ((line=br.readLine())!=null) {
                if (line.contains("<sms ")&&line.contains("body=")) {
                    String body=extractBody(line);
                    if (body!=null&&isBankSms(body))
                        smsList.add(body.replace("\n"," ").trim());
                }
            }
        }
        try (PrintWriter pw=new PrintWriter(new FileWriter(outputPath))) {
            pw.println("# Bank SMS Inbox — auto-generated"); pw.println();
            for (String sms:smsList) { pw.println(sms); pw.println(); }
        }
        System.out.println("[SmsXmlConverter] Extracted "+smsList.size()+" bank SMS → "+outputPath);
    }
    private static String extractBody(String xmlLine) {
        int s=xmlLine.indexOf("body="); if (s<0) return null; s+=6;
        int e=xmlLine.indexOf("",s);   if (e<0) return null;
        return xmlLine.substring(s,e).replace("&amp;","&").replace("&lt;","<").replace("&gt;",">");
    }
    private static boolean isBankSms(String body) {
        String u=body.toUpperCase();
        for (String kw:BANK_KEYWORDS) if (u.contains(kw.toUpperCase())) return true;
        return false;
    }
    public static void main(String[] args) throws Exception {
        if (args.length<2) { System.out.println("Usage: SmsXmlConverter <backup.xml> <sms_inbox.txt>"); return; }
        convert(args[0],args[1]);
    }
}
