package com.std.ec.PrimerSpring.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.std.ec.PrimerSpring.model.Reporte;

public interface ReporteRepository extends JpaRepository<Reporte, Integer> {

    List<Reporte> findTop8ByOrderByIdDesc();
}