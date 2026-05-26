package com.ds.app;

import com.ds.app.entity.*;
import com.ds.app.enums.*;
import com.ds.app.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final IEmployeeRepository employeeRepo;
    private final ISystemConfigurationRepository systemConfigurationRepo;
    private final IAttendanceRepository attendanceRepo;
    private final ILeaveBalanceRepository leaveBalanceRepo;
    private final ILeaveRepository leaveRepo;
    private final IRegularizationRequestRepository regularizationRepo;
    private final IHolidayRepository holidayRepo;
    private final ITimesheetRepository timesheetRepo;
    private final ITimesheetEntryRepository timesheetEntryRepo;
    private final ICompanyRepository companyRepo;
    private final IDepartmentRepository departmentRepo;
    private final IProjectRepository projectRepo;
    private final IEmployeeProjectRepository employeeProjectRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        String defaultPass = passwordEncoder.encode("123");
        boolean demoMarkerMissing = employeeRepo.findByUsername("shruti_hr").isEmpty();

        seedSystemConfigurations();
        ensureAdminUser(defaultPass);

        if (!demoMarkerMissing) {
            log.info("Database already populated. Skipping demo data seeder.");
            return;
        }

        long employeeCount = employeeRepo.count();
        if (employeeCount > 1) {
            log.warn("Employees already exist but demo marker user is missing. Skipping demo data seeder.");
            return;
        }

        log.info("Starting demo data generation (25 employees)...");

        seedHolidays();

        // ── Step 1: Companies ──────────────────────────────────────────
        Company finSecure = companyRepo.save(Company.builder()
                .name("FinSecure Pvt. Ltd.").type(CompanyType.INTERNAL)
                .code("FINSEC-001").isActive(true).build());

        Company icici = companyRepo.save(Company.builder()
                .name("ICICI Bank").type(CompanyType.CLIENT)
                .code("ICICI-001").isActive(true).build());

        Company hdfc = companyRepo.save(Company.builder()
                .name("HDFC Bank").type(CompanyType.CLIENT)
                .code("HDFC-001").isActive(true).build());

        Company infosys = companyRepo.save(Company.builder()
                .name("Infosys Ltd.").type(CompanyType.CLIENT)
                .code("INFY-001").isActive(true).build());

        Company tata = companyRepo.save(Company.builder()
                .name("Tata Consultancy Services").type(CompanyType.CLIENT)
                .code("TCS-001").isActive(true).build());

        Company bajaj = companyRepo.save(Company.builder()
                .name("Bajaj Finserv").type(CompanyType.CLIENT)
                .code("BJFIN-001").isActive(true).build());

        // ── Step 2: Departments ────────────────────────────────────────
        Department engineering = departmentRepo.save(Department.builder()
                .name("Engineering").code("ENG").isActive(true).company(finSecure).build());

        Department hrDept = departmentRepo.save(Department.builder()
                .name("Human Resources").code("HR").isActive(true).company(finSecure).build());

        Department financeDept = departmentRepo.save(Department.builder()
                .name("Finance").code("FIN").isActive(true).company(finSecure).build());

        Department qaDept = departmentRepo.save(Department.builder()
                .name("Quality Assurance").code("QA").isActive(true).company(finSecure).build());

        Department devOpsDept = departmentRepo.save(Department.builder()
                .name("DevOps").code("DEVOPS").isActive(true).company(finSecure).build());

        // ── Step 3: Projects ───────────────────────────────────────────
        Project hrmsCore = projectRepo.save(Project.builder()
                .projectName("HRMS Core").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2025, 1, 1)).company(finSecure).build());

        Project finSecurePortal = projectRepo.save(Project.builder()
                .projectName("FinSecure Employee Portal").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2025, 4, 1)).company(finSecure).build());

        Project iciciPayroll = projectRepo.save(Project.builder()
                .projectName("ICICI Payroll Integration").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2025, 6, 1)).company(icici).build());

        Project iciciMobile = projectRepo.save(Project.builder()
                .projectName("ICICI Mobile Banking Upgrade").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2025, 9, 1)).company(icici).build());

        Project hdfcReporting = projectRepo.save(Project.builder()
                .projectName("HDFC Financial Reporting").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 1)).company(hdfc).build());

        Project hdfcCompliance = projectRepo.save(Project.builder()
                .projectName("HDFC Compliance Automation").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2025, 11, 1)).company(hdfc).build());

        Project hdfcDataMigration = projectRepo.save(Project.builder()
                .projectName("HDFC Core Banking Data Migration").status(ProjectStatus.COMPLETED)
                .startDate(LocalDate.of(2025, 3, 1)).endDate(LocalDate.of(2025, 10, 31))
                .company(hdfc).build());

        Project infosysERP = projectRepo.save(Project.builder()
                .projectName("Infosys ERP Modernisation").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2025, 7, 1)).company(infosys).build());

        Project infosysAnalytics = projectRepo.save(Project.builder()
                .projectName("Infosys HR Analytics Dashboard").status(ProjectStatus.ON_HOLD)
                .startDate(LocalDate.of(2025, 10, 1)).company(infosys).build());

        Project tcsLeaveSystem = projectRepo.save(Project.builder()
                .projectName("TCS Leave Management System").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 2, 1)).company(tata).build());

        Project tcsAttendance = projectRepo.save(Project.builder()
                .projectName("TCS Attendance Tracking").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2025, 12, 1)).company(tata).build());

        Project bajajLending = projectRepo.save(Project.builder()
                .projectName("Bajaj Lending Platform Integration").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2026, 1, 15)).company(bajaj).build());

        Project bajajInsurance = projectRepo.save(Project.builder()
                .projectName("Bajaj Insurance Claims Portal").status(ProjectStatus.ACTIVE)
                .startDate(LocalDate.of(2025, 8, 1)).company(bajaj).build());

        // ── Step 4: Employees (25 total incl. admin) ───────────────────
        ensureAdminUser(defaultPass);

        // HR (1)
        Employee shruti = createEmployee("Shruti", "Mehra", "shruti@gmail.com",
                "shruti_hr", defaultPass, UserRole.HR, null, hrDept);

        // Managers (4 — two per engineering sub-team, one QA lead, one DevOps lead)
        Employee mgr1 = createEmployee("Manish", "Sharma", "manish@gmail.com",
                "manish_mngr", defaultPass, UserRole.MANAGER, shruti, engineering);
        Employee mgr2 = createEmployee("Dev", "Kumar", "dev@gmail.com",
                "dev_mngr", defaultPass, UserRole.MANAGER, shruti, engineering);
        Employee mgr3 = createEmployee("Priya", "Nair", "priya@gmail.com",
                "priya_mngr", defaultPass, UserRole.MANAGER, shruti, qaDept);
        Employee mgr4 = createEmployee("Rohit", "Joshi", "rohit@gmail.com",
                "rohit_mngr", defaultPass, UserRole.MANAGER, shruti, devOpsDept);

        // Engineering — under mgr1 (5 devs)
        Employee emp1 = createEmployee("Mayank",  "Sharma",   "mayank@gmail.com",  "mayank",  defaultPass, UserRole.EMPLOYEE, mgr1, engineering);
        Employee emp2 = createEmployee("Harsh",   "Sharma",   "harsh@gmail.com",   "harsh",   defaultPass, UserRole.EMPLOYEE, mgr1, engineering);
        Employee emp3 = createEmployee("Neha",    "Patel",    "neha@gmail.com",    "neha",    defaultPass, UserRole.EMPLOYEE, mgr1, engineering);
        Employee emp4 = createEmployee("Arjun",   "Verma",    "arjun@gmail.com",   "arjun",   defaultPass, UserRole.EMPLOYEE, mgr1, engineering);
        Employee emp5 = createEmployee("Pooja",   "Mishra",   "pooja@gmail.com",   "pooja",   defaultPass, UserRole.EMPLOYEE, mgr1, engineering);

        // Engineering — under mgr2 (5 devs)
        Employee emp6  = createEmployee("Viresh",  "Raghav",  "viresh@gmail.com",  "viresh",  defaultPass, UserRole.EMPLOYEE, mgr2, engineering);
        Employee emp7  = createEmployee("Tanya",   "Singh",   "tanya@gmail.com",   "tanya",   defaultPass, UserRole.EMPLOYEE, mgr2, financeDept);
        Employee emp8  = createEmployee("Karan",   "Mehta",   "karan@gmail.com",   "karan",   defaultPass, UserRole.EMPLOYEE, mgr2, engineering);
        Employee emp9  = createEmployee("Sonal",   "Gupta",   "sonal@gmail.com",   "sonal",   defaultPass, UserRole.EMPLOYEE, mgr2, engineering);
        Employee emp10 = createEmployee("Aditya",  "Rao",     "aditya@gmail.com",  "aditya",  defaultPass, UserRole.EMPLOYEE, mgr2, engineering);

        // QA — under mgr3 (5 testers)
        Employee emp11 = createEmployee("Ritika",  "Bose",    "ritika@gmail.com",  "ritika",  defaultPass, UserRole.EMPLOYEE, mgr3, qaDept);
        Employee emp12 = createEmployee("Suresh",  "Iyer",    "suresh@gmail.com",  "suresh",  defaultPass, UserRole.EMPLOYEE, mgr3, qaDept);
        Employee emp13 = createEmployee("Divya",   "Tiwari",  "divya@gmail.com",   "divya",   defaultPass, UserRole.EMPLOYEE, mgr3, qaDept);
        Employee emp14 = createEmployee("Ankur",   "Das",     "ankur@gmail.com",   "ankur",   defaultPass, UserRole.EMPLOYEE, mgr3, qaDept);
        Employee emp15 = createEmployee("Meena",   "Shah",    "meena@gmail.com",   "meena",   defaultPass, UserRole.EMPLOYEE, mgr3, qaDept);

        // DevOps — under mgr4 (4 engineers)
        Employee emp16 = createEmployee("Vivek",   "Pandey",  "vivek@gmail.com",   "vivek",   defaultPass, UserRole.EMPLOYEE, mgr4, devOpsDept);
        Employee emp17 = createEmployee("Shreya",  "Kapoor",  "shreya@gmail.com",  "shreya",  defaultPass, UserRole.EMPLOYEE, mgr4, devOpsDept);
        Employee emp18 = createEmployee("Nikhil",  "Desai",   "nikhil@gmail.com",  "nikhil",  defaultPass, UserRole.EMPLOYEE, mgr4, devOpsDept);
        Employee emp19 = createEmployee("Anjali",  "Reddy",   "anjali@gmail.com",  "anjali",  defaultPass, UserRole.EMPLOYEE, mgr4, devOpsDept);

        // Finance dept staff (under mgr2 since they interact with fintech projects)
        Employee emp20 = createEmployee("Sameer",  "Kulkarni","sameer@gmail.com",  "sameer",  defaultPass, UserRole.EMPLOYEE, mgr2, financeDept);

        List<Employee> allStaff = List.of(
                shruti, mgr1, mgr2, mgr3, mgr4,
                emp1, emp2, emp3, emp4, emp5,
                emp6, emp7, emp8, emp9, emp10,
                emp11, emp12, emp13, emp14, emp15,
                emp16, emp17, emp18, emp19, emp20
        );

        // ── Step 5: EmployeeProject assignments ────────────────────────

        // mgr1 leads
        seedEmployeeProject(mgr1, hrmsCore,        "LEAD", LocalDate.of(2025, 1,  1));
        seedEmployeeProject(mgr1, iciciPayroll,    "LEAD", LocalDate.of(2025, 6,  1));
        seedEmployeeProject(mgr1, hdfcCompliance,  "LEAD", LocalDate.of(2025, 11, 1));

        // mgr2 leads
        seedEmployeeProject(mgr2, hdfcReporting,   "LEAD", LocalDate.of(2026, 1,  1));
        seedEmployeeProject(mgr2, tcsAttendance,   "LEAD", LocalDate.of(2025, 12, 1));
        seedEmployeeProject(mgr2, bajajLending,    "LEAD", LocalDate.of(2026, 1, 15));

        // mgr3 leads
        seedEmployeeProject(mgr3, tcsLeaveSystem,  "LEAD", LocalDate.of(2026, 2,  1));
        seedEmployeeProject(mgr3, infosysAnalytics,"LEAD", LocalDate.of(2025, 10, 1));

        // mgr4 leads
        seedEmployeeProject(mgr4, finSecurePortal, "LEAD", LocalDate.of(2025, 4,  1));
        seedEmployeeProject(mgr4, infosysERP,      "LEAD", LocalDate.of(2025, 7,  1));

        // Engineering team (mgr1)
        seedEmployeeProject(emp1,  hrmsCore,         "DEVELOPER", LocalDate.of(2025, 1,  1));
        seedEmployeeProject(emp1,  iciciPayroll,     "DEVELOPER", LocalDate.of(2025, 6,  1));
        seedEmployeeProject(emp1,  iciciMobile,      "DEVELOPER", LocalDate.of(2025, 9,  1));

        seedEmployeeProject(emp2,  hrmsCore,         "DEVELOPER", LocalDate.of(2025, 1,  1));
        seedEmployeeProject(emp2,  finSecurePortal,  "DEVELOPER", LocalDate.of(2025, 4,  1));
        seedEmployeeProject(emp2,  infosysERP,       "DEVELOPER", LocalDate.of(2025, 7,  1));

        seedEmployeeProject(emp3,  hrmsCore,         "DEVELOPER", LocalDate.of(2025, 1,  1));
        seedEmployeeProject(emp3,  hdfcCompliance,   "DEVELOPER", LocalDate.of(2025, 11, 1));

        seedEmployeeProject(emp4,  iciciPayroll,     "DEVELOPER", LocalDate.of(2025, 6,  1));
        seedEmployeeProject(emp4,  bajajInsurance,   "DEVELOPER", LocalDate.of(2025, 8,  1));

        seedEmployeeProject(emp5,  hrmsCore,         "DEVELOPER", LocalDate.of(2025, 1,  1));
        seedEmployeeProject(emp5,  hdfcDataMigration,"DEVELOPER", LocalDate.of(2025, 3,  1));

        // Engineering team (mgr2)
        seedEmployeeProject(emp6,  hdfcReporting,   "DEVELOPER", LocalDate.of(2026, 1,  1));
        seedEmployeeProject(emp6,  tcsLeaveSystem,  "DEVELOPER", LocalDate.of(2026, 2,  1));
        seedEmployeeProject(emp6,  bajajInsurance,  "DEVELOPER", LocalDate.of(2025, 8,  1));

        seedEmployeeProject(emp7,  bajajLending,    "DEVELOPER", LocalDate.of(2026, 1, 15));
        seedEmployeeProject(emp7,  hdfcCompliance,  "DEVELOPER", LocalDate.of(2025, 11, 1));
        seedEmployeeProject(emp7,  infosysAnalytics,"DEVELOPER", LocalDate.of(2025, 10, 1));

        seedEmployeeProject(emp8,  tcsAttendance,   "DEVELOPER", LocalDate.of(2025, 12, 1));
        seedEmployeeProject(emp8,  hdfcReporting,   "DEVELOPER", LocalDate.of(2026, 1,  1));

        seedEmployeeProject(emp9,  bajajLending,    "DEVELOPER", LocalDate.of(2026, 1, 15));
        seedEmployeeProject(emp9,  iciciMobile,     "DEVELOPER", LocalDate.of(2025, 9,  1));

        seedEmployeeProject(emp10, tcsAttendance,   "DEVELOPER", LocalDate.of(2025, 12, 1));
        seedEmployeeProject(emp10, infosysERP,      "DEVELOPER", LocalDate.of(2025, 7,  1));

        // QA team
        seedEmployeeProject(emp11, hrmsCore,        "QA",        LocalDate.of(2025, 1,  1));
        seedEmployeeProject(emp11, iciciPayroll,    "QA",        LocalDate.of(2025, 6,  1));

        seedEmployeeProject(emp12, hdfcCompliance,  "QA",        LocalDate.of(2025, 11, 1));
        seedEmployeeProject(emp12, tcsLeaveSystem,  "QA",        LocalDate.of(2026, 2,  1));

        seedEmployeeProject(emp13, bajajInsurance,  "QA",        LocalDate.of(2025, 8,  1));
        seedEmployeeProject(emp13, infosysAnalytics,"QA",        LocalDate.of(2025, 10, 1));

        seedEmployeeProject(emp14, iciciMobile,     "QA",        LocalDate.of(2025, 9,  1));
        seedEmployeeProject(emp14, hdfcReporting,   "QA",        LocalDate.of(2026, 1,  1));

        seedEmployeeProject(emp15, finSecurePortal, "QA",        LocalDate.of(2025, 4,  1));
        seedEmployeeProject(emp15, tcsAttendance,   "QA",        LocalDate.of(2025, 12, 1));

        // DevOps team
        seedEmployeeProject(emp16, hrmsCore,        "DEVOPS",    LocalDate.of(2025, 1,  1));
        seedEmployeeProject(emp16, infosysERP,      "DEVOPS",    LocalDate.of(2025, 7,  1));

        seedEmployeeProject(emp17, hdfcDataMigration,"DEVOPS",   LocalDate.of(2025, 3,  1));
        seedEmployeeProject(emp17, bajajLending,    "DEVOPS",    LocalDate.of(2026, 1, 15));

        seedEmployeeProject(emp18, tcsAttendance,   "DEVOPS",    LocalDate.of(2025, 12, 1));
        seedEmployeeProject(emp18, iciciPayroll,    "DEVOPS",    LocalDate.of(2025, 6,  1));

        seedEmployeeProject(emp19, hdfcCompliance,  "DEVOPS",    LocalDate.of(2025, 11, 1));
        seedEmployeeProject(emp19, finSecurePortal, "DEVOPS",    LocalDate.of(2025, 4,  1));

        // Finance staff
        seedEmployeeProject(emp20, bajajInsurance,  "ANALYST",   LocalDate.of(2025, 8,  1));
        seedEmployeeProject(emp20, hdfcReporting,   "ANALYST",   LocalDate.of(2026, 1,  1));

        // ── Step 6: Leave Balances + Daily Records ─────────────────────
        Map<Long, LeaveBalance> employeeBalances = initializeLeaveBalances(allStaff, LocalDate.now().getYear());

        seedDailyRecords(allStaff, employeeBalances, LocalDate.of(2026, 1, 1), hrmsCore);

        // Spread future pending leaves across more employees for load testing
        seedFuturePendingLeaves(List.of(emp1, emp3, emp5, emp7, emp9, emp11, emp13, emp15, emp17, emp19));

        leaveBalanceRepo.saveAll(employeeBalances.values());

        log.info("Demo data seeding complete! 25 employees created. " +
                "Use LeaveLoadTest to simulate concurrent leave applications.");
    }

    // ─────────────────────────────────────────────────────────────────
    //  Helpers (unchanged from original)
    // ─────────────────────────────────────────────────────────────────

    private void seedEmployeeProject(Employee employee, Project project, String role, LocalDate joinedAt) {
        employeeProjectRepo.save(EmployeeProject.builder()
                .employee(employee).project(project).role(role).joinedAt(joinedAt).build());
    }

    private void seedSystemConfigurations() {
        for (DefaultConfig defaultConfig : DefaultConfig.values()) {
            if (systemConfigurationRepo.findById(defaultConfig.getKey()).isPresent()) continue;
            systemConfigurationRepo.save(SystemConfiguration.builder()
                    .configKey(defaultConfig.getKey())
                    .configValue(defaultConfig.getDefaultValue())
                    .build());
        }
    }

    private Employee ensureAdminUser(String defaultPass) {
        return employeeRepo.findByUsername("admin")
                .orElseGet(() -> createEmployee(
                        "System", "Admin", "admin@finsecure.com",
                        "admin", defaultPass, UserRole.ADMIN, null, null));
    }

    private void seedHolidays() {
        List<Holiday> holidays = List.of(
                Holiday.builder().date(LocalDate.of(2026, 1, 26)).name("Republic Day").type(HolidayType.NATIONAL).build(),
                Holiday.builder().date(LocalDate.of(2026, 3, 3)).name("Holi").type(HolidayType.NATIONAL).build(),
                Holiday.builder().date(LocalDate.of(2026, 3, 30)).name("Company Foundation Day").type(HolidayType.RELIGIOUS).build()
        );
        holidayRepo.saveAll(holidays);
    }

    private Employee createEmployee(String first, String last, String email, String username,
                                    String pass, UserRole role, Employee manager, Department department) {
        Employee emp = new Employee();
        emp.setUsername(username); emp.setPassword(pass); emp.setRole(role);
        emp.setFailedLoginAttemptsCount(0); emp.setIsAccountLocked(false);
        emp.setFirstName(first); emp.setLastName(last); emp.setEmail(email);
        emp.setManager(manager); emp.setDepartment(department);
        return employeeRepo.save(emp);
    }

    private Map<Long, LeaveBalance> initializeLeaveBalances(List<Employee> employees, int year) {
        Map<Long, LeaveBalance> map = new HashMap<>();
        for (Employee emp : employees) {
            LeaveBalance balance = LeaveBalance.builder()
                    .employee(emp).year(year)
                    .casualLeaveBalance(8).sickLeaveBalance(10)
                    .earnedLeaveBalance(BigDecimal.valueOf(12.5))
                    .casualLeavesConsumed(0).sickLeavesConsumed(0)
                    .build();
            map.put(emp.getUserId(), balance);
        }
        return map;
    }

    private void seedDailyRecords(List<Employee> employees, Map<Long, LeaveBalance> balancesMap,
                                  LocalDate startDate, Project defaultProject) {
        LocalDate endDate = LocalDate.now().minusDays(1);
        Random random = new Random();

        List<Attendance> attendanceBatch = new ArrayList<>();
        List<Leave> leaveBatch = new ArrayList<>();
        List<RegularizationRequest> regBatch = new ArrayList<>();
        List<TimesheetEntry> entryBatch = new ArrayList<>();
        Map<String, Timesheet> timesheetMap = new HashMap<>();

        List<LocalDate> holidayDates = holidayRepo.findAll().stream().map(Holiday::getDate).toList();
        LocalDate currentWeekStart = LocalDate.now().with(DayOfWeek.MONDAY);
        LocalDate currentWeekEnd   = LocalDate.now().with(DayOfWeek.SUNDAY);

        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            if (date.getDayOfWeek() == DayOfWeek.SATURDAY || date.getDayOfWeek() == DayOfWeek.SUNDAY
                    || holidayDates.contains(date)) {
                continue;
            }

            for (Employee emp : employees) {

                boolean isCurrentWeek = !date.isBefore(currentWeekStart) && !date.isAfter(currentWeekEnd);

                // ── INJECT DISCREPANCIES FOR CURRENT MONTH ────────────
                if (date.getMonthValue() == LocalDate.now().getMonthValue()
                        && date.getYear() == LocalDate.now().getYear()) {
                    int day = date.getDayOfMonth();
                    AttendanceStatus status;
                    int attMins;
                    int tsMins;

                    if (day % 3 == 1) {
                        status = AttendanceStatus.ABSENT;
                        attMins = 0;
                        tsMins  = 480;
                    } else if (day % 3 == 2) {
                        status  = AttendanceStatus.PRESENT;
                        attMins = 540;
                        tsMins  = 300;
                    } else {
                        status  = AttendanceStatus.PRESENT;
                        attMins = 480;
                        tsMins  = 480;
                    }

                    attendanceBatch.add(createAttendance(emp, date, status,
                            status == AttendanceStatus.PRESENT ? LocalTime.of(9, 0) : null,
                            status == AttendanceStatus.PRESENT ? LocalTime.of(18, 0) : null,
                            attMins, false));

                    if (tsMins > 0 && !isCurrentWeek) {
                        Timesheet ts = getOrCreateTimesheet(emp, date, timesheetMap);
                        entryBatch.add(TimesheetEntry.builder()
                                .employee(emp).date(date)
                                .taskDescription("Current Month Testing")
                                .totalMinutesWorked(tsMins)
                                .projectId(defaultProject.getProjectId())
                                .projectName(defaultProject.getProjectName())
                                .build());
                        ts.setTotalMonthlyMinutes(ts.getTotalMonthlyMinutes() + tsMins);
                    }
                    continue;
                }

                // ── NORMAL HISTORY ─────────────────────────────────────
                int chance = random.nextInt(100);
                int minsWorked = 0;
                boolean isPresent = false;

                if (chance < 80) {
                    minsWorked = 540; isPresent = true;
                    attendanceBatch.add(createAttendance(emp, date, AttendanceStatus.PRESENT,
                            LocalTime.of(9, random.nextInt(15)),
                            LocalTime.of(18, random.nextInt(30)), minsWorked, false));

                } else if (chance < 85) {
                    minsWorked = 450; isPresent = true;
                    attendanceBatch.add(createAttendance(emp, date, AttendanceStatus.PRESENT,
                            LocalTime.of(10, 30), LocalTime.of(18, 0), minsWorked, true));

                } else if (chance < 90) {
                    attendanceBatch.add(createAttendance(emp, date, AttendanceStatus.MISS_SWIPE,
                            LocalTime.of(9, 0), null, 0, false));
                    regBatch.add(RegularizationRequest.builder()
                            .employee(emp).date(date)
                            .reason("Forgot to punch out")
                            .punchInTime(LocalTime.of(9, 0))
                            .punchOutTime(LocalTime.of(18, 0))
                            .status(RegularizationRequestStatus.PENDING)
                            .build());

                } else {
                    LeaveType type = random.nextBoolean() ? LeaveType.SICK : LeaveType.CASUAL;
                    int leaveStateChance = random.nextInt(100);
                    LeaveStatus leaveStatus;
                    boolean managerActed = false;
                    String rejectionReason = null;

                    if (leaveStateChance < 40)      { leaveStatus = LeaveStatus.APPROVED;             managerActed = true; }
                    else if (leaveStateChance < 55) { leaveStatus = LeaveStatus.REJECTED;             managerActed = true; rejectionReason = "Not enough coverage."; }
                    else if (leaveStateChance < 70) { leaveStatus = LeaveStatus.EXPIRED; }
                    else if (leaveStateChance < 80) { leaveStatus = LeaveStatus.WITHDRAWN; }
                    else if (leaveStateChance < 90) { leaveStatus = LeaveStatus.CANCELLED;            managerActed = true; }
                    else                            { leaveStatus = LeaveStatus.CANCELLATION_PENDING; managerActed = true; }

                    attendanceBatch.add(createAttendance(emp, date, AttendanceStatus.ABSENT,
                            null, null, 0, false));

                    leaveBatch.add(Leave.builder()
                            .employee(emp).startDate(date).endDate(date).totalDays(1)
                            .reasonForLeave(type == LeaveType.SICK ? "Fever" : "Personal Errand")
                            .leaveType(type).status(leaveStatus)
                            .approvedBy(managerActed ? emp.getManager() : null)
                            .approvalDate(managerActed ? date.minusDays(1) : null)
                            .rejectionReason(rejectionReason)
                            .build());

                    if (leaveStatus == LeaveStatus.APPROVED || leaveStatus == LeaveStatus.CANCELLATION_PENDING) {
                        LeaveBalance b = balancesMap.get(emp.getUserId());
                        if (type == LeaveType.SICK) {
                            b.setSickLeaveBalance(b.getSickLeaveBalance() - 1);
                            b.setSickLeavesConsumed(b.getSickLeavesConsumed() + 1);
                        } else {
                            b.setCasualLeaveBalance(b.getCasualLeaveBalance() - 1);
                            b.setCasualLeavesConsumed(b.getCasualLeavesConsumed() + 1);
                        }
                    }
                }

                if (isPresent && !isCurrentWeek) {
                    Timesheet ts = getOrCreateTimesheet(emp, date, timesheetMap);
                    entryBatch.add(TimesheetEntry.builder()
                            .employee(emp).date(date)
                            .taskDescription("Routine tasks")
                            .totalMinutesWorked(minsWorked)
                            .projectId(defaultProject.getProjectId())
                            .projectName(defaultProject.getProjectName())
                            .build());
                    ts.setTotalMonthlyMinutes(ts.getTotalMonthlyMinutes() + minsWorked);
                }
            }
        }

        processAndApproveTimesheets(timesheetMap);

        attendanceRepo.saveAll(attendanceBatch);
        leaveRepo.saveAll(leaveBatch);
        regularizationRepo.saveAll(regBatch);
        timesheetEntryRepo.saveAll(entryBatch);
        timesheetRepo.saveAll(timesheetMap.values());
    }

    private Timesheet getOrCreateTimesheet(Employee emp, LocalDate date, Map<String, Timesheet> timesheetMap) {
        String key = emp.getUserId() + "-" + date.getYear() + "-" + date.getMonthValue();
        if (!timesheetMap.containsKey(key)) {
            Timesheet ts = Timesheet.builder()
                    .employee(emp).year(date.getYear()).month(date.getMonthValue())
                    .status(TimesheetStatus.DRAFT).totalMonthlyMinutes(0).build();
            timesheetMap.put(key, ts);
        }
        return timesheetMap.get(key);
    }

    private void processAndApproveTimesheets(Map<String, Timesheet> timesheetMap) {
        LocalDate currentMonthStart = LocalDate.now().withDayOfMonth(1);
        for (Timesheet ts : timesheetMap.values()) {
            LocalDate tsDate = LocalDate.of(ts.getYear(), ts.getMonth(), 1);
            if (tsDate.isBefore(currentMonthStart)) {
                ts.setStatus(TimesheetStatus.APPROVED);
                ts.setSubmittedAt(tsDate.plusMonths(1).minusDays(1).atTime(18, 0));
                ts.setApprovedBy(ts.getEmployee().getManager());
                ts.setApprovalDate(tsDate.plusMonths(1));
            }
        }
    }

    private void seedFuturePendingLeaves(List<Employee> employeesToRequestLeave) {
        LocalDate nextWeek = LocalDate.now().plusDays(5);
        List<Leave> futureLeaves = new ArrayList<>();
        for (Employee emp : employeesToRequestLeave) {
            futureLeaves.add(Leave.builder()
                    .employee(emp).startDate(nextWeek).endDate(nextWeek).totalDays(1)
                    .reasonForLeave("Doctor appointment").leaveType(LeaveType.SICK)
                    .status(LeaveStatus.PENDING).build());
        }
        leaveRepo.saveAll(futureLeaves);
    }

    private Attendance createAttendance(Employee emp, LocalDate date, AttendanceStatus status,
                                        LocalTime in, LocalTime out, int mins, boolean isLate) {
        return Attendance.builder()
                .employee(emp).date(date).status(status)
                .punchInTime(in).punchOutTime(out)
                .totalMinutesWorked(mins).isLate(isLate).isRegularized(false).build();
    }
}