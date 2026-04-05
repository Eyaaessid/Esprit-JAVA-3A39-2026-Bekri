package com.bekri.repositories;

import com.bekri.entities.Coach;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoachRepository extends JpaRepository<Coach, Integer> {}
