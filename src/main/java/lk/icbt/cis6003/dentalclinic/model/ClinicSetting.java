package lk.icbt.cis6003.dentalclinic.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * One clinic-wide setting, stored as a key and a value.
 *
 * The consultation fee, the opening hours and the currency live here instead of
 * being written into the Java code. That way the clinic manager can change the
 * fee without a developer rebuilding and redeploying the whole program.
 *
 * The key is the primary key itself, so a setting can only exist once. Saving
 * the same key again updates the row rather than adding a second one.
 */
@Entity
@Table(name = "clinic_settings")
public class ClinicSetting {

    @Id
    @Column(name = "setting_key", nullable = false, length = 60)
    private String settingKey;

    /**
     * Kept as text on purpose. One table then holds a fee (1500.00), a time
     * (08:00:00) and a name (Sunrise Dental Clinic) without needing a separate
     * column for each type. The code converts the text where it is used.
     */
    @Column(name = "setting_value", nullable = false, length = 255)
    private String settingValue;

    @Column(name = "description", length = 255)
    private String description;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public ClinicSetting() {
    }

    public ClinicSetting(String settingKey, String settingValue, String description) {
        this.settingKey = settingKey;
        this.settingValue = settingValue;
        this.description = description;
    }

    public String getSettingKey() {
        return settingKey;
    }

    public void setSettingKey(String settingKey) {
        this.settingKey = settingKey;
    }

    public String getSettingValue() {
        return settingValue;
    }

    public void setSettingValue(String settingValue) {
        this.settingValue = settingValue;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClinicSetting)) {
            return false;
        }
        return Objects.equals(settingKey, ((ClinicSetting) other).settingKey);
    }

    @Override
    public int hashCode() {
        return Objects.hash(settingKey);
    }

    @Override
    public String toString() {
        return "ClinicSetting{" + settingKey + "=" + settingValue + "}";
    }
}
