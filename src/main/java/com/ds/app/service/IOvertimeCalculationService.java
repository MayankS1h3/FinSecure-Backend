package com.ds.app.service;

import com.ds.app.dto.response.OvertimeResponse;
import com.ds.app.entity.OvertimePolicy;
import com.ds.app.entity.TimesheetEntry;

public interface IOvertimeCalculationService {

    OvertimeResponse calculateDailyOvertime(TimesheetEntry entry, OvertimePolicy policy, boolean isHoliday);
}