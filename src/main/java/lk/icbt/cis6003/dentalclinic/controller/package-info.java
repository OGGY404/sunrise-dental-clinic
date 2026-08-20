/**
 * PRESENTATION TIER (tier 1 of 3).
 *
 * <p>Holds the Spring MVC controllers that render the Thymeleaf pages and the REST
 * controllers that expose the system as web services. Classes here only accept input,
 * hand it to the {@code service} tier, and choose which view or response to return.
 * They contain no business rules and never talk to the database directly.</p>
 */
package lk.icbt.cis6003.dentalclinic.controller;
