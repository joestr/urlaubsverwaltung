package org.synyx.urlaubsverwaltung.application.statistics;

import java.math.BigDecimal;
import java.util.Map;

import static java.math.BigDecimal.ZERO;

public final class ApplicationForLeaveStatisticsDto {

    private final Long id;
    private final String firstName;
    private final String lastName;
    private final String niceName;
    private final String initials;
    private final String gravatarURL;

    private final String personnelNumber;

    private final BigDecimal totalAllowedVacationDays;
    private final Map<ApplicationForLeaveStatisticsVacationTypeDto, BigDecimal> allowedVacationDays;

    private final BigDecimal totalWaitingVacationDays;
    private final Map<ApplicationForLeaveStatisticsVacationTypeDto, BigDecimal> waitingVacationDays;

    ApplicationForLeaveStatisticsDto(
        Long id, String firstName, String lastName, String niceName, String initials, String gravatarURL, String personnelNumber,
        BigDecimal totalAllowedVacationDays, Map<ApplicationForLeaveStatisticsVacationTypeDto, BigDecimal> allowedVacationDays,
        BigDecimal totalWaitingVacationDays, Map<ApplicationForLeaveStatisticsVacationTypeDto, BigDecimal> waitingVacationDays,
        BigDecimal leftVacationDaysForPeriod, BigDecimal remainingLeftVacationDaysForPeriod, BigDecimal leftVacationDays,
        BigDecimal remainingLeftVacationDays, String leftOvertime, String leftOvertimeForPeriod
    ) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.niceName = niceName;
        this.initials = initials;
        this.gravatarURL = gravatarURL;
        this.personnelNumber = personnelNumber;
        this.totalAllowedVacationDays = totalAllowedVacationDays;
        this.allowedVacationDays = allowedVacationDays;
        this.totalWaitingVacationDays = totalWaitingVacationDays;
        this.waitingVacationDays = waitingVacationDays;
    }

    public Long getId() {
        return id;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getNiceName() {
        return niceName;
    }

    public String getInitials() {
        return initials;
    }

    public String getGravatarURL() {
        return gravatarURL;
    }

    public String getPersonnelNumber() {
        return personnelNumber;
    }

    public BigDecimal getTotalAllowedVacationDays() {
        return totalAllowedVacationDays;
    }

    public Map<ApplicationForLeaveStatisticsVacationTypeDto, BigDecimal> getAllowedVacationDays() {
        return allowedVacationDays;
    }

    public BigDecimal getTotalWaitingVacationDays() {
        return totalWaitingVacationDays;
    }

    public Map<ApplicationForLeaveStatisticsVacationTypeDto, BigDecimal> getWaitingVacationDays() {
        return waitingVacationDays;
    }

    public boolean hasVacationType(ApplicationForLeaveStatisticsVacationTypeDto type) {
        return waitingVacationDays.containsKey(type) || allowedVacationDays.containsKey(type);
    }

    public BigDecimal getWaitingVacationDays(ApplicationForLeaveStatisticsVacationTypeDto type) {
        return waitingVacationDays.getOrDefault(type, ZERO);
    }

    public BigDecimal getAllowedVacationDays(ApplicationForLeaveStatisticsVacationTypeDto type) {
        return allowedVacationDays.getOrDefault(type, ZERO);
    }
}
