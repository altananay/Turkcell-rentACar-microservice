package com.kodlamaio.inventoryservice.business.dto.responses.create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateModelRequest {
    @NotBlank
    @NotNull
    private UUID brandId;
    @NotBlank
    @Size(min = 2, max = 20)
    private String name;
}