package com.bekri.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("admin")
@NoArgsConstructor
public class Admin extends Utilisateur {
}

