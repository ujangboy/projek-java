package com.example.penjualan.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.penjualan.entity.Pelanggan;

public interface PelangganRepository extends JpaRepository<Pelanggan, Long> {

	Page<Pelanggan> findByNamaContainingIgnoreCase(String nama, Pageable pageable);
}
