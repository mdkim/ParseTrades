import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

class ParseTrades {

	public final static String
        PATH_IN = "./Archive/TD 2020-010-01 to 2021-01-14p.csv",
        PATH_OUT = "./out.txt";
    // true = print to writer, false = print to System.out
    public final static boolean IS_WRITER_OUT = false;        
    
    private final static int
        DATE = 0, TRANSACTION_ID = 1, DESCRIPTION = 2, QUANTITY = 3, SYMBOL = 4,
        PRICE = 5, COMMISSION = 6, AMOUNT = 7, REG_FEE = 8;
        //SHORT_TERM_RDM_FEE = 9, FUND_REDEMPTION_FEE = 10, DEFERRED_SALES_CHARGE = 11;

    private final static SimpleDateFormat SDF = new SimpleDateFormat("m/d/Y");

    public static void main(String[] args) {
        try {
            new ParseTrades().go();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    private void go() throws IOException {
        Path pathIn = Paths.get(PATH_IN);
        Path pathOut = Paths.get(PATH_OUT);

        Map<String, SymbolTally> tallies = new LinkedHashMap<>();
        try (
            BufferedReader reader = Files.newBufferedReader(pathIn);
            BufferedWriter writer = Files.newBufferedWriter(pathOut)
        ) {
            SimplePrinter out = new SimplePrinter(writer, IS_WRITER_OUT);
            out.println();
            
            String line = reader.readLine();
			//String[] fieldNames = line.split(",", -1);

            lineLoop:
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split(",", -1);
                if (fields.length < REG_FEE + 1) break lineLoop;

                @SuppressWarnings("unused")
                BigDecimal commission = BigDecimal.ZERO, regFee = BigDecimal.ZERO;
                // Date date; String transId; Big Decimal price;

                boolean isMandatoryExchangeStart = true;
                String mandatoryExchangeStartSymbol = null;

                SymbolTally tally;
                String symbol = null, desc = null;
                int quantity = 0;
                BigDecimal amount = null;
                for (int i=0; i < fields.length; i++) {
                    switch (i) {
                    case DATE:
                        //date = parseDate(fields[i]);
                        break;
                    case TRANSACTION_ID:
                        //transId = fields[i];
                        break;
                    case SYMBOL:
                        symbol = fields[i];
                        break;
                    case DESCRIPTION:
                        desc = fields[i];
                        if (!(desc.startsWith("Bought ")
                            || desc.startsWith("Sold ")
                            || desc.startsWith("MANDATORY - EXCHANGE ")
                        )) {
                            continue lineLoop;
                        }
                        break;
                    case QUANTITY:
                        quantity = Integer.valueOf(fields[i]);
                        break;
                    case PRICE:
                        //price = new BigDecimal(fields[i]);
                        break;
                    case COMMISSION:
                        commission = getBlankableBigDecimal(fields[i]);
                        break;
                    case AMOUNT:
                        amount = new BigDecimal(fields[i]);
                        break;
                    case REG_FEE:
                        regFee = getBlankableBigDecimal(fields[i]);
                        break;
                    }
                }

                tally = tallies.get(symbol);
                if (tally == null) tally = new SymbolTally(symbol);
                
                String message = tally.trade(quantity, amount);
                tallies.put(symbol, tally);
                out.println(message);

                // Mandatory Exchange:
                if (desc.startsWith("MANDATORY - EXCHANGE ")) {
                    mandatoryExchangeStartSymbol = doMandatoryExchange(
                        symbol, mandatoryExchangeStartSymbol, quantity,
                        tallies, isMandatoryExchangeStart);
                    isMandatoryExchangeStart = !isMandatoryExchangeStart;
                }
            }

            // OUTPUT:
            out.println();
            out.println("SYMBOL\tSHARES @ AVG. PRICE\t NET CASH");
            for (Map.Entry<String, SymbolTally> tallyEntry : tallies.entrySet()) {
                SymbolTally tally = tallyEntry.getValue();
                out.println(tally.toString());
            }                
        }
    }

    private static BigDecimal getBlankableBigDecimal(String field) {
        BigDecimal value;
        if (field.length() > 0) {
            value = new BigDecimal(field);
        } else {
            value = BigDecimal.ZERO;
        }
        return value;
    }

    // MANDATORY - EXCHANGE (SPAQ),10,SPAQ
    // MANDATORY - EXCHANGE (FSR),10,FSR
    private String doMandatoryExchange(String symbol, String startSymbol, int quantity,
        Map<String, SymbolTally> tallies, boolean isMandatoryExchangeStart) {

        if (isMandatoryExchangeStart) {
            return symbol;
        }
        SymbolTally tally = tallies.get(startSymbol);
        tally.exchange(symbol, quantity);
        tallies.put(symbol, tally);
        tallies.remove(startSymbol);
        return null;
    }

    @SuppressWarnings("unused")
    private static Date parseDate(String txt) {
        Date date;
        try {
            date = SDF.parse(txt);
        } catch(ParseException e) {
            throw new RuntimeException(e);
        }
        return date;
    }
}
