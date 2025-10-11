import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public class CalendarLogic {

    private final LocalDate lastPeriod;

    public CalendarLogic(LocalDate lastPeriod) {
        this.lastPeriod = lastPeriod;
    }

    /** Predicted next period (simple 28-day rule) */
    public LocalDate nextPeriodDate() {
        return lastPeriod.plusDays(28);
    }

    public LocalDate getLastPeriod() {
        return lastPeriod;
    }

    /** Day number in the current cycle (>=0). If lastPeriod is in the future, returns 0. */
    public long dayOfCycle() {
        long d = ChronoUnit.DAYS.between(lastPeriod, LocalDate.now());
        return Math.max(d, 0);
    }

    /** Rough phase label from day number */
    public String cyclePhase() {
        long day = dayOfCycle();
        if (day <= 5) return "Menstrual Phase 🩸";
        if (day <= 14) return "Follicular Phase 🌱";
        if (day <= 17) return "Ovulation Phase 🌼";
        if (day <= 28) return "Luteal Phase 🌙";
        return "New Cycle Starting Soon 🔄";
    }
}




