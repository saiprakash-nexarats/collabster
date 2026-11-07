package com.collabsphere_first.career.repository;

import com.collabsphere_first.career.entity.CareerJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CareerJobRepository extends JpaRepository<CareerJob, UUID> {
    List<CareerJob> findByStateOrderByCreatedAtDesc(String state);
    List<CareerJob> findByEmployerIdOrderByCreatedAtDesc(Long employerId);
}
