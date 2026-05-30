package com.example.pulsedistro.repository;

import com.example.pulsedistro.domain.ContentTask;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentTaskRepository extends JpaRepository<ContentTask, String> {
}
