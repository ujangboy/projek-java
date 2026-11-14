package com.example.penjualan.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "transaksi")
public class Transaksi {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "id_transaksi")
	private Long idTransaksi;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_pelanggan", nullable = false)
	private Pelanggan pelanggan;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "id_user", nullable = false)
	private User user;

	@Column(nullable = false)
	private LocalDateTime tanggal;

	@Column(nullable = false, precision = 15, scale = 2)
	private BigDecimal total = BigDecimal.ZERO;

	@OneToMany(mappedBy = "transaksi", cascade = CascadeType.ALL, orphanRemoval = true)
	private List<DetailTransaksi> details = new ArrayList<>();

	public String getNomorTransaksi() {
		return "TRX-" + tanggal.format(DateTimeFormatter.BASIC_ISO_DATE)
				+ "-" + String.format("%04d", idTransaksi);
	}

	public void addDetail(DetailTransaksi detail) {
		detail.setTransaksi(this);
		details.add(detail);
	}

	public Long getIdTransaksi() {
		return idTransaksi;
	}

	public void setIdTransaksi(Long idTransaksi) {
		this.idTransaksi = idTransaksi;
	}

	public Pelanggan getPelanggan() {
		return pelanggan;
	}

	public void setPelanggan(Pelanggan pelanggan) {
		this.pelanggan = pelanggan;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public LocalDateTime getTanggal() {
		return tanggal;
	}

	public void setTanggal(LocalDateTime tanggal) {
		this.tanggal = tanggal;
	}

	public BigDecimal getTotal() {
		return total;
	}

	public void setTotal(BigDecimal total) {
		this.total = total;
	}

	public List<DetailTransaksi> getDetails() {
		return details;
	}

	public void setDetails(List<DetailTransaksi> details) {
		this.details = details;
	}
}
