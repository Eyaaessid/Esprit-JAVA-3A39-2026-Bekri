package com.bekri.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("user")
@NoArgsConstructor
@SuperBuilder
public class User extends Utilisateur {}
