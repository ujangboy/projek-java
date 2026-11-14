package com.example.penjualan.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.penjualan.dto.TransaksiForm;
import com.example.penjualan.dto.TransaksiForm.ItemForm;
import com.example.penjualan.entity.Barang;
import com.example.penjualan.entity.DetailTransaksi;
import com.example.penjualan.entity.Pelanggan;
import com.example.penjualan.entity.Transaksi;
import com.example.penjualan.entity.User;
import com.example.penjualan.repository.BarangRepository;
import com.example.penjualan.repository.PelangganRepository;
import com.example.penjualan.repository.TransaksiRepository;
import com.example.penjualan.repository.UserRepository;

@Service
public class TransaksiService {

	private final TransaksiRepository transaksiRepository;

	private final PelangganRepository pelangganRepository;

	private final BarangRepository barangRepository;

	private final UserRepository userRepository;

	public TransaksiService(TransaksiRepository transaksiRepository, PelangganRepository pelangganRepository,
			BarangRepository barangRepository, UserRepository userRepository) {
		this.transaksiRepository = transaksiRepository;
		this.pelangganRepository = pelangganRepository;
		this.barangRepository = barangRepository;
		this.userRepository = userRepository;
	}

	@Transactional
	public Transaksi create(TransaksiForm form, String username) {
		if (form.getIdPelanggan() == null) {
			throw new TransaksiException("Pelanggan wajib dipilih.");
		}
		Pelanggan pelanggan = pelangganRepository.findById(form.getIdPelanggan())
				.orElseThrow(() -> new TransaksiException("Pelanggan tidak ditemukan."));

		if (form.getItems() == null || form.getItems().isEmpty()) {
			throw new TransaksiException("Minimal satu barang harus dipilih.");
		}

		Map<Long, Integer> quantities = new LinkedHashMap<>();
		for (ItemForm item : form.getItems()) {
			if (item.getIdBarang() == null) {
				throw new TransaksiException("Barang wajib dipilih.");
			}
			if (item.getJumlah() == null || item.getJumlah() < 1) {
				throw new TransaksiException("Jumlah barang minimal 1.");
			}
			try {
				quantities.merge(item.getIdBarang(), item.getJumlah(), Math::addExact);
			}
			catch (ArithmeticException ex) {
				throw new TransaksiException("Jumlah barang terlalu besar.");
			}
		}

		User user = userRepository.findByUsername(username)
				.orElseThrow(() -> new IllegalStateException("User tidak ditemukan: " + username));

		List<Barang> locked = barangRepository.findAllForUpdate(quantities.keySet());
		Map<Long, Barang> barangById = locked.stream()
				.collect(Collectors.toMap(Barang::getIdBarang, Function.identity()));
		if (barangById.size() != quantities.size()) {
			throw new TransaksiException("Barang tidak ditemukan.");
		}

		Transaksi transaksi = new Transaksi();
		transaksi.setPelanggan(pelanggan);
		transaksi.setUser(user);
		transaksi.setTanggal(LocalDateTime.now());

		BigDecimal total = BigDecimal.ZERO;
		for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
			Barang barang = barangById.get(entry.getKey());
			int jumlah = entry.getValue();
			if (barang.getStok() < jumlah) {
				throw new TransaksiException("Stok barang '" + barang.getNamaBarang()
						+ "' tidak mencukupi. Tersedia: " + barang.getStok() + ", diminta: " + jumlah + ".");
			}

			barang.setStok(barang.getStok() - jumlah);

			DetailTransaksi detail = new DetailTransaksi();
			detail.setBarang(barang);
			detail.setJumlah(jumlah);
			detail.setHargaSatuan(barang.getHarga());
			detail.setSubtotal(barang.getHarga().multiply(BigDecimal.valueOf(jumlah)));
			transaksi.addDetail(detail);
			total = total.add(detail.getSubtotal());
		}
		transaksi.setTotal(total);

		return transaksiRepository.save(transaksi);
	}
}
