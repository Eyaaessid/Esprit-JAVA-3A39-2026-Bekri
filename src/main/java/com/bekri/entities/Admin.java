package com.bekri.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("admin")
@NoArgsConstructor
@SuperBuilder
public class Admin extends Utilisateur {}
