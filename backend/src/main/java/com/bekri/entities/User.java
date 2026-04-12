package com.bekri.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("user")
@NoArgsConstructor
public class User extends Utilisateur {
}

