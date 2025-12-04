package org.synyx.urlaubsverwaltung.absence.web;

import de.focus_shift.launchpad.api.HasLaunchpad;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.propertyeditors.CustomCollectionEditor;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.synyx.urlaubsverwaltung.absence.AbsenceService;
import org.synyx.urlaubsverwaltung.absence.DateRange;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationType;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationTypeColor;
import org.synyx.urlaubsverwaltung.application.vacationtype.VacationTypeService;
import org.synyx.urlaubsverwaltung.department.Department;
import org.synyx.urlaubsverwaltung.department.DepartmentService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.PersonService;
import org.synyx.urlaubsverwaltung.publicholiday.PublicHolidaysService;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeService;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Year;
import java.time.temporal.TemporalAdjuster;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

import static java.lang.Integer.parseInt;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toCollection;
import static org.springframework.util.StringUtils.hasText;
import org.synyx.urlaubsverwaltung.application.application.ApplicationForLeave;
import org.synyx.urlaubsverwaltung.application.application.ApplicationService;
import org.synyx.urlaubsverwaltung.application.application.ApplicationStatus;
import org.synyx.urlaubsverwaltung.overview.GlobalOverviewApplicationDto;
import org.synyx.urlaubsverwaltung.overview.OverviewVacationTypDto;
import static org.synyx.urlaubsverwaltung.person.Role.BOSS;
import static org.synyx.urlaubsverwaltung.person.Role.DEPARTMENT_HEAD;
import static org.synyx.urlaubsverwaltung.person.Role.INACTIVE;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.SECOND_STAGE_AUTHORITY;
import org.synyx.urlaubsverwaltung.workingtime.WorkDaysCountService;

@RequestMapping("/web/global-absences2")
@Controller
public class GlobalAbsenceOverview2ViewController implements HasLaunchpad {

    private static final VacationTypeColor ANONYMIZED_ABSENCE_COLOR = VacationTypeColor.YELLOW;

    private final PersonService personService;
    private final DepartmentService departmentService;
    private final MessageSource messageSource;
    private final Clock clock;
    private final PublicHolidaysService publicHolidaysService;
    private final AbsenceService absenceService;
    private final WorkingTimeService workingTimeService;
    private final VacationTypeService vacationTypeService;
    private final ApplicationService applicationService;
    private final WorkDaysCountService workDaysCountService;

    @Autowired
    public GlobalAbsenceOverview2ViewController(
        PersonService personService, DepartmentService departmentService,
        MessageSource messageSource, Clock clock,
        PublicHolidaysService publicHolidaysService,
        AbsenceService absenceService, WorkingTimeService workingTimeService,
        VacationTypeService vacationTypeService,
        ApplicationService applicationService,
        WorkDaysCountService workDaysCountService
    ) {
        this.personService = personService;
        this.departmentService = departmentService;
        this.messageSource = messageSource;
        this.clock = clock;
        this.publicHolidaysService = publicHolidaysService;
        this.absenceService = absenceService;
        this.workingTimeService = workingTimeService;
        this.workDaysCountService = workDaysCountService;
        this.vacationTypeService = vacationTypeService;
        this.applicationService = applicationService;
    }

    @InitBinder
    public void initBinder(WebDataBinder binder) {
        binder.registerCustomEditor(List.class, new CustomCollectionEditor(List.class));
    }

    @GetMapping
    public String absenceOverview(
        @RequestParam(required = false) Integer year,
        @RequestParam(required = false) String month,
        @RequestParam(name = "department", required = false, defaultValue = "") List<String> rawSelectedDepartments,
        Model model, Locale locale) {

        final Person signedInUser = personService.getSignedInUser();

        final List<Person> overviewPersons;
        if (departmentService.getNumberOfDepartments() > 0) {

            final List<Department> visibleDepartments = departmentService.getAllDepartments();
            model.addAttribute("visibleDepartments", visibleDepartments);

            if (visibleDepartments.isEmpty()) {
                overviewPersons = List.of(signedInUser);
            } else {
                final List<String> selectedDepartmentNames = getSelectedDepartmentNames(rawSelectedDepartments);
                
                if (selectedDepartmentNames.isEmpty()) {
                    selectedDepartmentNames.addAll(visibleDepartments.stream().map(Department::getName).toList());
                }
                
                model.addAttribute("selectedDepartments", selectedDepartmentNames);
                
                overviewPersons = visibleDepartments.stream()
                    .filter(department -> selectedDepartmentNames.contains(department.getName()))
                    .map(Department::getMembers)
                    .flatMap(List::stream)
                    .filter(member -> !member.hasRole(INACTIVE))
                    .distinct()
                    .sorted(comparing(Person::getFirstName))
                    .toList();
            }
        } else {
            overviewPersons = personService.getActivePersons();
        }

        final LocalDate startDate = getStartDate(year, month);
        final LocalDate endDate = getEndDate(year, month);

        model.addAttribute("currentYear", Year.now(clock).getValue());
        model.addAttribute("selectedYear", startDate.getYear());

        final String selectedMonth = getSelectedMonth(month, startDate);
        model.addAttribute("selectedMonth", selectedMonth);
        
        final DateRange dateRange = new DateRange(startDate, endDate); 
        final List<GlobalOverviewApplicationDto> applications = getApplications(overviewPersons, dateRange, locale);
        model.addAttribute("applications", applications);

        return "absences/global-absences-overview2";
    }
    
    private List<String> getSelectedDepartmentNames(List<String> rawSelectedDepartments) {
        final List<String> preparedSelectedDepartments = rawSelectedDepartments.stream().filter(StringUtils::hasText).toList();
        return preparedSelectedDepartments.isEmpty() ? new ArrayList<>() : preparedSelectedDepartments;
    }
    
    private List<GlobalOverviewApplicationDto> getApplications(List<Person> person, DateRange dateRange, Locale locale) {
        var applications = applicationService.getApplicationsForACertainPeriodAndStatus(dateRange.startDate(), dateRange.endDate(), person, List.of(ApplicationStatus.ALLOWED))
                .stream()
                .map(application -> new ApplicationForLeave(application, workDaysCountService))
                .sorted(comparing(ApplicationForLeave::getStartDate))
                .map(applicationForLeave -> overviewApplicationDto(applicationForLeave, locale))
                .toList();
        
        return applications;
    }
    
    private GlobalOverviewApplicationDto overviewApplicationDto(ApplicationForLeave applicationForLeave, Locale locale) {
        final GlobalOverviewApplicationDto dto = new GlobalOverviewApplicationDto();
        dto.setId(applicationForLeave.getId());
        dto.setPersonFullName(applicationForLeave.getPerson().getNiceName());
        dto.setStatus(applicationForLeave.getStatus());
        dto.setVacationType(overviewVacationTypDto(applicationForLeave.getVacationType(), locale));
        dto.setApplicationDate(applicationForLeave.getApplicationDate());
        dto.setStartDate(applicationForLeave.getStartDate());
        dto.setEndDate(applicationForLeave.getEndDate());
        dto.setStartTime(applicationForLeave.getStartTime());
        dto.setEndTime(applicationForLeave.getEndTime());
        dto.setStartDateWithTime(applicationForLeave.getStartDateWithTime());
        dto.setEndDateWithTime(applicationForLeave.getEndDateWithTime());
        dto.setDayLength(applicationForLeave.getDayLength());
        dto.setWorkDays(applicationForLeave.getWorkDays());
        dto.setPersonId(applicationForLeave.getPerson().getId());
        dto.setHours(applicationForLeave.getHours());
        dto.setWeekDayOfStartDate(applicationForLeave.getWeekDayOfStartDate());
        dto.setWeekDayOfEndDate(applicationForLeave.getWeekDayOfEndDate());
        dto.setEditedDate(applicationForLeave.getEditedDate());
        dto.setCancelDate(applicationForLeave.getCancelDate());
        return dto;
    }
    
    private OverviewVacationTypDto overviewVacationTypDto(VacationType<?> vacationType, Locale locale) {
        return new OverviewVacationTypDto(vacationType.getLabel(locale), vacationType.getCategory(), vacationType.getColor());
    }



    private String getSelectedMonth(String month, LocalDate startDate) {
        String selectedMonth = "";
        if (month == null) {
            selectedMonth = String.valueOf(startDate.getMonthValue());
        } else if (hasText(month)) {
            selectedMonth = month;
        }
        return selectedMonth;
    }

    private String getMonthText(LocalDate date, Locale locale) {
        return messageSource.getMessage(getMonthMessageCode(date), new Object[]{}, locale);
    }

    private String getMonthMessageCode(LocalDate localDate) {
        return switch (localDate.getMonthValue()) {
            case 1 -> "month.january";
            case 2 -> "month.february";
            case 3 -> "month.march";
            case 4 -> "month.april";
            case 5 -> "month.may";
            case 6 -> "month.june";
            case 7 -> "month.july";
            case 8 -> "month.august";
            case 9 -> "month.september";
            case 10 -> "month.october";
            case 11 -> "month.november";
            case 12 -> "month.december";
            default ->
                throw new IllegalStateException("month value not in range of 1 to 12 cannot be mapped to a message key.");
        };
    }

    private LocalDate getStartDate(Integer year, String month) {
        return getStartOrEndDate(year, month, TemporalAdjusters::firstDayOfYear, TemporalAdjusters::firstDayOfMonth);
    }

    private LocalDate getEndDate(Integer year, String month) {
        return getStartOrEndDate(year, month, TemporalAdjusters::lastDayOfYear, TemporalAdjusters::lastDayOfMonth);
    }

    private LocalDate getStartOrEndDate(Integer year, String month, Supplier<TemporalAdjuster> firstOrLastOfYearSupplier,
                                        Supplier<TemporalAdjuster> firstOrLastOfMonthSupplier) {

        final LocalDate now = LocalDate.now(clock);

        if (year != null) {
            if (hasText(month)) {
                return now.withYear(year).withMonth(parseInt(month)).with(firstOrLastOfMonthSupplier.get());
            }
            if ("".equals(month)) {
                return now.withYear(year).with(firstOrLastOfYearSupplier.get());
            }
            return now.withYear(year).with(firstOrLastOfMonthSupplier.get());
        }

        if (hasText(month)) {
            return now.withMonth(parseInt(month)).with(firstOrLastOfMonthSupplier.get());
        }
        return now.with(firstOrLastOfMonthSupplier.get());
    }

    private List<Person> getActiveMembersOfPerson(final Person person) {

        if (person.hasRole(BOSS) || person.hasRole(OFFICE)) {
            return personService.getActivePersons();
        }

        final List<Person> relevantPersons = new ArrayList<>();
        if (person.hasRole(DEPARTMENT_HEAD)) {
            departmentService.getMembersForDepartmentHead(person).stream()
                .filter(member -> !member.hasRole(INACTIVE))
                .collect(toCollection(() -> relevantPersons));
        }

        if (person.hasRole(SECOND_STAGE_AUTHORITY)) {
            departmentService.getMembersForSecondStageAuthority(person).stream()
                .filter(member -> !member.hasRole(INACTIVE))
                .collect(toCollection(() -> relevantPersons));
        }

        return relevantPersons.stream()
            .distinct()
            .toList();
    }
}
