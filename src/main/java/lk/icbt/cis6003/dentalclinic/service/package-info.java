/**
 * BUSINESS LOGIC TIER (tier 2 of 3).
 *
 * <p>Holds the rules of the clinic: booking an appointment, preventing double bookings,
 * calculating a bill, and sending notifications. Several small classes live here rather
 * than one large class, so each one has a single responsibility.</p>
 *
 * <p>Most of the design patterns for Task B sit in this tier - Strategy for the billing
 * rules, Factory for creating them, and Observer for notifications.</p>
 */
package lk.icbt.cis6003.dentalclinic.service;
