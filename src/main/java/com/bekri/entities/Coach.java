package com.bekri.entities;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@DiscriminatorValue("coach")
@NoArgsConstructor
@SuperBuilder
public class Coach extends Utilisateur {}
