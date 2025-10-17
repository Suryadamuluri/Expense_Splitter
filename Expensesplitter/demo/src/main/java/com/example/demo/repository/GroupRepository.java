package com.example.demo.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.model.Group;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    boolean existsByNameIgnoreCase(String name);
    Optional<Group> findByNameIgnoreCase(String name);
}