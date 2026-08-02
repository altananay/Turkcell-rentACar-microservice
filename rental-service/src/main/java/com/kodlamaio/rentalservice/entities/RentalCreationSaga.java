package com.kodlamaio.rentalservice.entities;

import com.kodlamaio.rentalservice.entities.enums.SagaStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "rental_creation_sagas")
public class RentalCreationSaga {
    @Id
    private UUID id;

    @Version
    private Long version;

    private UUID rentalId;
    private UUID carId;
    private double dailyPrice;
    private int rentedForDays;
    private double price;

    private String cardNumber;
    private String cardHolder;
    private int cardExpirationYear;
    private int cardExpirationMonth;
    // NOTE: storing CVV post-authorization is a real PCI-DSS violation in production. Acceptable
    // here only because this is a FakePosServiceAdapter simulation with test card data, and it is
    // required for a crash during the STARTED phase to be automatically resumable.
    private String cardCvv;

    @Enumerated(EnumType.STRING)
    private SagaStatus status;

    private String failureReason;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
