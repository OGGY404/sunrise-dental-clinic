/**
 * DATA TRANSFER OBJECTS - the shapes of data moving in and out of the system.
 *
 * <p>Form-backing objects and API request/response bodies carry the validation
 * annotations (@NotBlank, @Pattern, @Future). Keeping them separate from the entities
 * means a bad web request can never write straight into a database row.</p>
 */
package lk.icbt.cis6003.dentalclinic.dto;
