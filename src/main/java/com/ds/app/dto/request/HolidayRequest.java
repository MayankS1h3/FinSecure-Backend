package com.ds.app.dto.request;

import com.ds.app.enums.HolidayType;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HolidayRequest {

    @NotNull(message = "Date is required")
    @FutureOrPresent(message = "Holiday date cannot be in the past")
    private LocalDate date;

    @NotBlank(message = "Holiday name is required")
    private String name;

    @NotNull(message = "Holiday type is required")
    private HolidayType type;
}