import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * regFee (0.01) and commission (6.95) are unused, since included in the
 * 'amount'
 */
class SymbolTally {
    private String symbol, startSymbol;
    private int netShares = 0;
    private BigDecimal netAmount = BigDecimal.ZERO;
    private BigDecimal netCash = BigDecimal.ZERO;

    public SymbolTally(String symbol) {
        this.symbol = symbol;
    }

    @SuppressWarnings("unused")
    private SymbolTally() {}

    public void exchange(String symbol, String startSymbol, int quantity) {
        this.symbol = symbol;
        this.startSymbol = startSymbol;
        this.netShares = quantity; // assumes exchanges are all-or-nothing
    }

    public String trade(int quantity, BigDecimal amount) {
        int amountSignum = amount.signum();

        if (amountSignum == -1) { // buy
            netShares += quantity;
            netAmount = netAmount.add(amount);
            netCash = netCash.add(amount); // adding a negative amount
        } else if (amountSignum == 1) { // sell
            netShares -= quantity;
            netAmount = netAmount.subtract(amount);
            netCash = netCash.add(amount);
        } else {
            amount = this.netAmount;
            if (this.startSymbol == null) return null;
        }

        /*
         * build log message
         */
        String prepend = "";
        String buy_sell = (amountSignum == -1) ? "Bought " : "Sold ";
        if (amountSignum == 0) {
            prepend = "\tMANDATORY EXCHANGE: Exchanged " + startSymbol + " for " + symbol + "\n";
            buy_sell = "Received ";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(prepend);
        sb.append(buy_sell);
        sb.append(quantity);
        sb.append(" shares ");
        sb.append(symbol);
        sb.append(" @ ");

        // TODO: avgPrice is wrong for FSR, investigate
        // 'amount' is rounded in the CSV
        BigDecimal avgPrice = amount.abs().divide(
            BigDecimal.valueOf(quantity), 4, RoundingMode.HALF_UP
        ).stripTrailingZeros();
        if (avgPrice.scale() < 0) avgPrice = avgPrice.setScale(0);
        sb.append(avgPrice);

        String pad = String.format("%" + (35 - sb.length() + prepend.length()) + "s", "");
        sb.append(pad);

        appendNetCash(sb);
         
        return sb.toString();
    }

    private void appendNetCash(StringBuilder sb) {
        sb.append("Net Cash ");
         sb.append(netCash.signum() == -1 ? "(" : "");
         sb.append(netCash);
         sb.append(netCash.signum() == -1 ? ")" : "");
         sb.append(" for ");
         sb.append(symbol);
    }

    //show total-amount / total-shares = avg. price per share
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        if (netShares != 0) {
            sb.append(netShares);
            sb.append(" @ ");
            sb.append(netAmount.divide(BigDecimal.valueOf(netShares), 4, RoundingMode.HALF_UP));
            sb.append(" = ");
            sb.append(netAmount);
        } else {
            sb.append("0 @ * = ");
            sb.append(netAmount);
        }

        String pad = String.format("%" + (25 - sb.length()) + "s", "");
        sb.append(pad);

        appendNetCash(sb);

        return symbol + "\t" + sb.toString();
    }
}
