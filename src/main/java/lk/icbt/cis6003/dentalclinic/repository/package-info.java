/**
 * DATA ACCESS TIER (tier 3 of 3).
 *
 * <p>The only package in the system that talks to MySQL. It uses the Repository (DAO)
 * pattern: Spring Data JPA interfaces for ordinary queries, and JdbcTemplate calls for
 * the stored procedures and functions defined in the database schema.</p>
 *
 * <p>Because every database call goes through this tier, the business logic can be
 * tested without a real database by replacing these interfaces with Mockito mocks.</p>
 */
package lk.icbt.cis6003.dentalclinic.repository;
