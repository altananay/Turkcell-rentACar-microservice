package com.kodlamaio.rentalservice.business.dto.requests;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateRentalRequest {
    @NotNull
    private UUID id;
    @NotNull
    private UUID carId;
    @Min(1)
    private double dailyPrice;
    @Min(1)
    private int rentedForDays;
}
