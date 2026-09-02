package lk.icbt.cis6003.dentalclinic.service.billing;

/**
 * One way of working out what a visit costs (FR4).
 *
 * DESIGN PATTERN: Strategy.
 *
 * The clinic does not charge for every treatment in the same way. A check-up is
 * the consultation, so charging a consultation fee on top would bill the
 * patient twice. Surgery needs extra sterilisation, which costs money. Without
 * this pattern the billing service would hold a growing if-else chain, and
 * every new rule would mean editing a class that already works.
 *
 * With it, each rule is its own small class that can be read and tested on its
 * own, and the billing service never knows which one it is using.
 *
 * CRITICAL EVALUATION (for the report): the cost of Strategy here is
 * indirection. Someone reading BillingService cannot see the arithmetic; they
 * have to open the factory, then the chosen class. With only three rules that
 * is arguably more structure than the problem needs. It earns its place because
 * the price list is the part of a clinic system most likely to change, and
 * because it makes each rule testable in isolation, which a long if-else chain
 * is not.
 */
public interface BillingStrategy {

    /** Short name of this rule, shown on the bill, for example "Surgical". */
    String getName();

    /** Works out the three amounts that make up the bill. */
    BillCharge calculate(BillingContext context);
}
