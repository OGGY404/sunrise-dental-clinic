package lk.icbt.cis6003.dentalclinic.config;

import jakarta.annotation.PostConstruct;
import lk.icbt.cis6003.dentalclinic.model.ClinicSetting;
import lk.icbt.cis6003.dentalclinic.repository.ClinicSettingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds the clinic-wide settings: the consultation fee, the opening hours, the
 * length of an appointment slot, the clinic name and the currency.
 *
 * DESIGN PATTERN: Singleton.
 *
 * There is exactly one of these objects in the running system. The settings are
 * read from the database once, when the application starts, and kept in memory
 * afterwards. Without this, every bill printed would run another query for the
 * consultation fee, and every booking another two for the opening hours.
 *
 * HOW THE SINGLETON IS ACHIEVED, AND WHY IT IS DONE THIS WAY
 * The textbook Singleton uses a private constructor and a static getInstance()
 * method. That version is a poor fit for a tested application: static state
 * survives between tests, and nothing can be substituted, so the class cannot
 * be tested without a real database behind it.
 *
 * Here the same guarantee comes from Spring instead. A @Component is created
 * once and the same object is given to everything that asks for it, which is
 * what Singleton actually promises. Because the constructor is public and takes
 * the repository as a parameter, the test can build one with a fake repository
 * and check the fallback behaviour. The pattern is kept; the untestable part of
 * the classic implementation is not.
 *
 * CRITICAL EVALUATION (for the report): the cost of caching is staleness. If a
 * manager edits the consultation fee directly in MySQL, this object will not
 * notice until reload() is called or the application restarts. That is accepted
 * because these values change perhaps twice a year, and reload() gives an admin
 * screen a way to refresh them without a restart.
 */
@Component
public class ClinicConfiguration {

    private static final Logger log = LoggerFactory.getLogger(ClinicConfiguration.class);

    // Setting keys, matching the rows inserted by db/data.sql
    private static final String KEY_CLINIC_NAME = "clinic_name";
    private static final String KEY_CLINIC_ADDRESS = "clinic_address";
    private static final String KEY_CLINIC_PHONE = "clinic_phone";
    private static final String KEY_CONSULTATION_FEE = "consultation_fee";
    private static final String KEY_OPENING_TIME = "opening_time";
    private static final String KEY_CLOSING_TIME = "closing_time";
    private static final String KEY_SLOT_MINUTES = "appointment_slot_minutes";
    private static final String KEY_CURRENCY = "currency";

    /**
     * Safe values used when a setting is missing, holds nonsense, or the
     * database cannot be reached. The clinic can still take bookings and print
     * bills on the wrong side of a database problem, which matters more than
     * being exactly right about the fee.
     */
    private static final BigDecimal DEFAULT_CONSULTATION_FEE = new BigDecimal("1500.00");
    private static final LocalTime DEFAULT_OPENING_TIME = LocalTime.of(8, 0);
    private static final LocalTime DEFAULT_CLOSING_TIME = LocalTime.of(18, 0);
    private static final int DEFAULT_SLOT_MINUTES = 30;
    private static final String DEFAULT_CLINIC_NAME = "Sunrise Dental Clinic";
    private static final String DEFAULT_CURRENCY = "LKR";

    private final ClinicSettingRepository settingRepository;

    /** The settings as they were when they were last read. */
    private final Map<String, String> settings = new HashMap<>();

    public ClinicConfiguration(ClinicSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    /**
     * Reads every setting from the database into memory.
     *
     * @PostConstruct means Spring calls this once, straight after the object is
     * built, before anything else can use it.
     *
     * A failure here is logged and swallowed on purpose. If the database is not
     * ready yet, the application should still start and fall back to the safe
     * defaults, rather than refusing to start at all.
     */
    @PostConstruct
    public void load() {
        settings.clear();
        try {
            List<ClinicSetting> rows = settingRepository.findAll();
            for (ClinicSetting row : rows) {
                settings.put(row.getSettingKey(), row.getSettingValue());
            }
            log.info("Loaded {} clinic settings from the database.", settings.size());
        } catch (RuntimeException problem) {
            log.warn("Could not read the clinic settings, so the built-in defaults will be used. Reason: {}",
                    problem.getMessage());
        }
    }

    /** Reads the settings again, for an admin screen that has just changed one. */
    public void reload() {
        load();
    }

    public String getClinicName() {
        return settings.getOrDefault(KEY_CLINIC_NAME, DEFAULT_CLINIC_NAME);
    }

    public String getClinicAddress() {
        return settings.getOrDefault(KEY_CLINIC_ADDRESS, "");
    }

    public String getClinicPhone() {
        return settings.getOrDefault(KEY_CLINIC_PHONE, "");
    }

    public String getCurrency() {
        return settings.getOrDefault(KEY_CURRENCY, DEFAULT_CURRENCY);
    }

    /** Added to every bill, unless the billing rule waives it. */
    public BigDecimal getConsultationFee() {
        return readMoney(KEY_CONSULTATION_FEE, DEFAULT_CONSULTATION_FEE);
    }

    public LocalTime getOpeningTime() {
        return readTime(KEY_OPENING_TIME, DEFAULT_OPENING_TIME);
    }

    public LocalTime getClosingTime() {
        return readTime(KEY_CLOSING_TIME, DEFAULT_CLOSING_TIME);
    }

    /** Appointments must start on a boundary of this many minutes. */
    public int getAppointmentSlotMinutes() {
        return readInt(KEY_SLOT_MINUTES, DEFAULT_SLOT_MINUTES);
    }

    /**
     * True when the clinic is open at this time. Both ends count as open, so an
     * appointment may start exactly at 08:00 or exactly at 18:00.
     */
    public boolean isWithinOpeningHours(LocalTime time) {
        if (time == null) {
            return false;
        }
        return !time.isBefore(getOpeningTime()) && !time.isAfter(getClosingTime());
    }

    /**
     * True when the time starts a proper slot, for example 09:00 or 09:30 but
     * not 09:15. Keeping every booking on the same grid is what makes the daily
     * schedule readable and stops small unusable gaps appearing in the diary.
     */
    public boolean isOnSlotBoundary(LocalTime time) {
        if (time == null) {
            return false;
        }
        int slot = getAppointmentSlotMinutes();
        if (slot <= 0) {
            return true;
        }
        return time.getSecond() == 0 && time.getMinute() % slot == 0;
    }

    // --- reading text safely into the type we need ---------------------------

    private BigDecimal readMoney(String key, BigDecimal fallback) {
        String raw = settings.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException notANumber) {
            log.warn("Setting {} holds \"{}\", which is not a number. Using {} instead.", key, raw, fallback);
            return fallback;
        }
    }

    private int readInt(String key, int fallback) {
        String raw = settings.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException notANumber) {
            log.warn("Setting {} holds \"{}\", which is not a whole number. Using {} instead.", key, raw, fallback);
            return fallback;
        }
    }

    private LocalTime readTime(String key, LocalTime fallback) {
        String raw = settings.get(key);
        if (raw == null) {
            return fallback;
        }
        try {
            return LocalTime.parse(raw.trim());
        } catch (DateTimeParseException notATime) {
            log.warn("Setting {} holds \"{}\", which is not a time. Using {} instead.", key, raw, fallback);
            return fallback;
        }
    }
}
