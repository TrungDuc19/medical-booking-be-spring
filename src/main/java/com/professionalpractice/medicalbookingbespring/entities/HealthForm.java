package com.professionalpractice.medicalbookingbespring.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "health-forms")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HealthForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "user_id")
    User user;
    @Column(name = "name_patient", nullable = false)
    String namePatient;
    String email;
    @Column(name = "phone_number", nullable = false)
    String phoneNumber;
    String address;
    @ManyToOne(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumn(name = "shift_id")
    Shift shift;
    @Column(name = "reason", nullable = false)
    String reason;
    @Column(name = "cccd", nullable = false)
    String cccd;
    @Column(name = "bhyt")
    String bhyt;
    @Column(name = "denied_reason")
    String deniedReason;
    Integer status;
    @Column(name = "accepted_number")
    Integer acceptedNumber;
}
