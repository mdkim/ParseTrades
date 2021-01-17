import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * regFee (0.01) and commission (6.95) are unused, since included in the
 * 'amount'
 */
class SymbolTally {
    private String symbol;
    private int netShares = 0;
    private BigDecimal netAmount = BigDecimal.ZERO;
    private BigDecimal netCash = BigDecimal.ZERO;

    public SymbolTally(String symbol) {
        this.symbol = symbol;
    }

    @SuppressWarnings("unused")
    private SymbolTally() {}

    public void exchange(String symbol, int quantity) {
        this.symbol = symbol;
        this.netShares = quantity; // assumes exchanges are all-or-nothing
    }

    public String trade(int quantity, BigDecimal amount) {
        //if (amount.signum() == 0) return; // ignore regFee and commission

        if (amount.signum() == -1) { // buy
            netShares += quantity;
            netAmount = netAmount.add(amount);
            netCash = netCash.add(amount); // adding a negative amount
        } else { // sell
            netShares -= quantity;
            netAmount = netAmount.subtract(amount);
            netCash = netCash.add(amount);
        }

        /*
         * build log message
         */
        String buy_sell = (amount.signum() == -1) ? "Bought " : "Sold ";
        if (amount.signum() == 0) buy_sell = "\tMANDATORY EXCHANGE: \n";

        StringBuilder sb = new StringBuilder();
        sb.append(buy_sell);
        sb.append(quantity);
        sb.append(" shares ");
        sb.append(symbol);
        sb.append(" @ ");

        // 'amount' is rounded in the CSV
        BigDecimal avgPrice = amount.abs().divide(
            BigDecimal.valueOf(quantity), 4, RoundingMode.HALF_UP
        ).stripTrailingZeros();
        if (avgPrice.scale() < 0) avgPrice = avgPrice.setScale(0);
        sb.append(avgPrice);

        String pad = String.format("%" + (35 - sb.length()) + "s", "");
        sb.append(pad);

        sb.append("Net Cash " + netCash + " for " + symbol);

        return sb.toString();
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

        sb.append("Net Cash ");
        sb.append(netCash.signum() == -1 ? "(" : "");
        sb.append(netCash);
        sb.append(netCash.signum() == -1 ? ")" : "");
        sb.append(" for ");
        sb.append(symbol);
        
        return symbol + "\t" + sb.toString();
    }
}
