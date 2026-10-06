package com.example.spabooking.staff.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class UpdateWorkingHoursRequest {

    @NotEmpty(message = "Working hours list cannot be empty")
    @Valid
    private List<StaffWorkingHoursDto> workingHours;

    public UpdateWorkingHoursRequest() {}

    public UpdateWorkingHoursRequest(List<StaffWorkingHoursDto> workingHours) {
        this.workingHours = workingHours;
    }

    public List<StaffWorkingHoursDto> getWorkingHours() {
        return workingHours;
    }

    public void setWorkingHours(List<StaffWorkingHoursDto> workingHours) {
        this.workingHours = workingHours;
    }
}
