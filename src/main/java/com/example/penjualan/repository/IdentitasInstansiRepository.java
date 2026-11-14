package com.example.penjualan.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.penjualan.entity.IdentitasInstansi;

public interface IdentitasInstansiRepository extends JpaRepository<IdentitasInstansi, Long> {

	Optional<IdentitasInstansi> findFirstByOrderByIdInstansiAsc();
}
