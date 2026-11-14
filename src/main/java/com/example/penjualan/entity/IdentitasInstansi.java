package com.example.penjualan.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "identitas_instansi")
public class IdentitasInstansi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_instansi")
	private Long idInstansi;

	@Column(name = "nama_instansi", nullable = false, length = 150)
	private String namaInstansi;

	@Column(nullable = false, length = 255)
	private String alamat;

	@Column(length = 100)
	private String email;

	@Column(name = "no_telepon", length = 30)
	private String noTelepon;

	@Column(length = 255)
	private String logo;

	@Column(name = "updated_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime updatedAt;

	public Long getIdInstansi() {
		return idInstansi;
	}

	public void setIdInstansi(Long idInstansi) {
		this.idInstansi = idInstansi;
	}

	public String getNamaInstansi() {
		return namaInstansi;
	}

	public void setNamaInstansi(String namaInstansi) {
		this.namaInstansi = namaInstansi;
	}

	public String getAlamat() {
		return alamat;
	}

	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getNoTelepon() {
		return noTelepon;
	}

	public void setNoTelepon(String noTelepon) {
		this.noTelepon = noTelepon;
	}

	public String getLogo() {
		return logo;
	}

	public void setLogo(String logo) {
		this.logo = logo;
	}

	public LocalDateTime getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(LocalDateTime updatedAt) {
		this.updatedAt = updatedAt;
	}
}
