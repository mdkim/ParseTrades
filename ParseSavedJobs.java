import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class ParseSavedJobs {

    private static Pattern PATT_CITY = Pattern.compile("^(.*), (CA, US|California, United States)$");
    private static Pattern PATT_NUM_APPLICANTS = Pattern.compile("^(\\d+) (hours?|days?|weeks?|months?) ago (\\d+) applicants?$");
    private static Pattern PATT_EARLY_LATE = Pattern.compile("^(Be an early applicant|This job may close soon)$");
    private static Pattern PATT_ALUMNI = Pattern.compile("^(Stanford University|FabFitFun|Age of Learning)");
    private static Pattern PATT_NUM_ALUMNI = Pattern.compile("^(\\d+)( company)? alum(ni)?$");

    private static String JOB_TITLE = "jobTitle";
    private static String COMPANY_NAME = "companyName";
    private static String CITY = "city";
    private static String NUM_APPLICANTS = "numApplicants";
    private static String EARLY_LATE = "earlyLate";
    private static String ALUMNI = "alumni";
    private static String NUM_ALUMNI = "numAlumni";
    
    public static void main(String[] args) {
        new ParseSavedJobs().go();
    }

    void go() {
        Path path = Paths.get("./saved jobs.txt");
        Path pathOut = Paths.get("./temp out.csv");
        String line = null;
        Matcher m = null;
        Map<String, String> lineMap = new HashMap<>();
        try (
            BufferedReader reader = Files.newBufferedReader(path);
            BufferedWriter writer = Files.newBufferedWriter(pathOut)
        ) {
            printHeaders(writer, new String[]{
                "Closed", COMPANY_NAME, JOB_TITLE, CITY, NUM_APPLICANTS, EARLY_LATE, ALUMNI, NUM_ALUMNI
            });

            boolean isAfterEasyApply = false;
            int i=0;
            int lineNumber=-1;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                if (line.equals("")) continue; // while loop

                if (!isAfterEasyApply && line.equals("Easy Apply")) {
                    isAfterEasyApply = true;
                    continue;
                }

                switch (i) {
                case 0:
                    lineMap.put(JOB_TITLE, line);
                    break;
                case 1:
                    lineMap.put(COMPANY_NAME, line);
                    break;
                case 2:
                    if (line.indexOf("Los Angeles") >= 0) {
                        lineMap.put(CITY, line);
                        break;
                    } else {
                        m = PATT_CITY.matcher(line);
                        if (m.find()) {
                            lineMap.put(CITY, m.group(1)); // city only
                            break;
                        }
                    }
                    // else fall through
                case 3:
                    m = PATT_NUM_APPLICANTS.matcher(line);
                    if (m.find()) {
                        lineMap.put(NUM_APPLICANTS, m.group(1) + " " + m.group(2));
                        break;
                    }
                    // else fall through
                case 4:
                    m = PATT_EARLY_LATE.matcher(line);
                    if (m.find()) {
                        lineMap.put(EARLY_LATE, m.group(1));
                        break;
                    }
                    // else fall through
                case 5:
                    m = PATT_ALUMNI.matcher(line);
                    if (m.find()) {
                        lineMap.put(ALUMNI, m.group(1));
                        break;
                    }
                    // else fall through
                case 6:
                    m = PATT_NUM_ALUMNI.matcher(line);
                    if (m.find()) {
                        lineMap.put(NUM_ALUMNI, m.group(1));
                        break;
                    }
                    // else fall through
                case 7:
                    if (line.endsWith("Apply") || line.startsWith("No longer accepting applications")) {
                        if (!line.endsWith("Apply")) {
                            print(writer, line); // No longer accepting applications ...
                        } else {
                            print(writer, null);
                        }
                        print(writer, lineMap.get(COMPANY_NAME));
                        print(writer, lineMap.get(JOB_TITLE));
                        print(writer, lineMap.get(CITY));
                        print(writer, lineMap.get(NUM_APPLICANTS));
                        print(writer, lineMap.get(EARLY_LATE));
                        print(writer, lineMap.get(ALUMNI));
                        print(writer, lineMap.get(NUM_ALUMNI));
                        println(writer);
                        i=0;
                        isAfterEasyApply = false;
                        lineMap.clear();
                        continue; // while looop
                    }
                default:
                    debugMatcher("line " + lineNumber + ": case 8 invalid", line, m);
                    throw new RuntimeException("case 8 invalid");
                }
                i++;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (IllegalStateException e) {
            debugMatcher(null, line, m);
            throw new RuntimeException(e);
        }
        System.out.println("DONE");
    }

    private static void print(BufferedWriter writer, String text) throws IOException {
        if (text != null) writer.write('"' + text + '"');
        writer.write(",");

        //System.out.println(text);
    }

    private static void println(BufferedWriter writer) throws IOException {
        writer.write('\n');
    }

    private static void printHeaders(BufferedWriter writer, String[] text) throws IOException {
        for (String header : text) {
            writer.write(header + ',');
        }
        writer.write('\n');
    }

    private static void debugMatcher(String msg, String line, Matcher m) {
        if (msg != null) System.err.println(msg);
        System.err.println("line=" + line);
        System.err.println("matcher=" + m.pattern().pattern());
    }
}
