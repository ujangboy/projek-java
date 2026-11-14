package com.example.penjualan.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "pelanggan")
public class Pelanggan {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_pelanggan")
	private Long idPelanggan;

	@NotBlank(message = "Nama pelanggan wajib diisi.")
	@Size(max = 100, message = "Nama pelanggan maksimal 100 karakter.")
	@Column(nullable = false, length = 100)
	private String nama;

	@Size(max = 255, message = "Alamat maksimal 255 karakter.")
	@Column(length = 255)
	private String alamat;

	@Size(max = 20, message = "Nomor telepon maksimal 20 karakter.")
	@Column(name = "no_telepon", length = 20)
	private String noTelepon;

	@Email(message = "Format email tidak valid.")
	@Size(max = 100, message = "Email maksimal 100 karakter.")
	@Column(length = 100)
	private String email;

	@Column(name = "created_at", nullable = false, insertable = false, updatable = false)
	private LocalDateTime createdAt;

	public Long getIdPelanggan() {
		return idPelanggan;
	}

	public void setIdPelanggan(Long idPelanggan) {
		this.idPelanggan = idPelanggan;
	}

	public String getNama() {
		return nama;
	}

	public void setNama(String nama) {
		this.nama = nama;
	}

	public String getAlamat() {
		return alamat;
	}

	public void setAlamat(String alamat) {
		this.alamat = alamat;
	}

	public String getNoTelepon() {
		return noTelepon;
	}

	public void setNoTelepon(String noTelepon) {
		this.noTelepon = noTelepon;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public LocalDateTime getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(LocalDateTime createdAt) {
		this.createdAt = createdAt;
	}
}
