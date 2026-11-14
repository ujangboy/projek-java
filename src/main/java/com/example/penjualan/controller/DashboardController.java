package com.example.penjualan.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.penjualan.repository.BarangRepository;
import com.example.penjualan.repository.PelangganRepository;
import com.example.penjualan.repository.TransaksiRepository;

@Controller
public class DashboardController {

	private final PelangganRepository pelangganRepository;

	private final BarangRepository barangRepository;

	private final TransaksiRepository transaksiRepository;

	public DashboardController(PelangganRepository pelangganRepository, BarangRepository barangRepository,
			TransaksiRepository transaksiRepository) {
		this.pelangganRepository = pelangganRepository;
		this.barangRepository = barangRepository;
		this.transaksiRepository = transaksiRepository;
	}

	@GetMapping("/")
	public String root() {
		return "redirect:/dashboard";
	}

	@GetMapping("/dashboard")
	public String dashboard(Model model) {
		LocalDate today = LocalDate.now();
		LocalDateTime startToday = today.atStartOfDay();
		LocalDateTime startTomorrow = today.plusDays(1).atStartOfDay();
		LocalDateTime startMonth = today.withDayOfMonth(1).atStartOfDay();
		LocalDateTime startNextMonth = today.withDayOfMonth(1).plusMonths(1).atStartOfDay();

		BigDecimal omzetHariIni = zeroIfNull(transaksiRepository.sumTotalBetween(startToday, startTomorrow));
		BigDecimal omzetBulanIni = zeroIfNull(transaksiRepository.sumTotalBetween(startMonth, startNextMonth));
		BigDecimal omzetTotal = zeroIfNull(transaksiRepository.sumTotalAll());

		model.addAttribute("activeMenu", "dashboard");
		model.addAttribute("totalPelanggan", pelangganRepository.count());
		model.addAttribute("totalBarang", barangRepository.count());
		model.addAttribute("totalTransaksi", transaksiRepository.count());
		model.addAttribute("omzetHariIni", omzetHariIni);
		model.addAttribute("omzetBulanIni", omzetBulanIni);
		model.addAttribute("omzetTotal", omzetTotal);
		model.addAttribute("recentTransaksi",
				transaksiRepository.findAllByOrderByTanggalDescIdTransaksiDesc(PageRequest.of(0, 5)).getContent());
		model.addAttribute("lowStock", barangRepository.findLowStock());
		return "dashboard/index";
	}

	private BigDecimal zeroIfNull(BigDecimal value) {
		return value == null ? BigDecimal.ZERO : value;
	}
}
