package com.example.penjualan.controller;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import com.example.penjualan.entity.DetailTransaksi;
import com.example.penjualan.entity.IdentitasInstansi;
import com.example.penjualan.entity.Transaksi;
import com.example.penjualan.repository.DetailTransaksiRepository;
import com.example.penjualan.repository.IdentitasInstansiRepository;
import com.example.penjualan.repository.PelangganRepository;
import com.example.penjualan.repository.TransaksiRepository;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.pdf.JRPdfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;

@Controller
@RequestMapping("/laporan")
public class LaporanController {

	private static final DateTimeFormatter PERIODE_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy");
	private static final DateTimeFormatter DT_FMT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

	private final TransaksiRepository transaksiRepository;
	private final DetailTransaksiRepository detailTransaksiRepository;
	private final PelangganRepository pelangganRepository;
	private final IdentitasInstansiRepository identitasInstansiRepository;

	private final Map<String, JasperReport> reportCache = new ConcurrentHashMap<>();

	public LaporanController(TransaksiRepository transaksiRepository, DetailTransaksiRepository detailTransaksiRepository,
			PelangganRepository pelangganRepository, IdentitasInstansiRepository identitasInstansiRepository) {
		this.transaksiRepository = transaksiRepository;
		this.detailTransaksiRepository = detailTransaksiRepository;
		this.pelangganRepository = pelangganRepository;
		this.identitasInstansiRepository = identitasInstansiRepository;
	}

	@GetMapping
	public String index(Model model) {
		model.addAttribute("activeMenu", "laporan");
		return "laporan/index";
	}

	// ---- Laporan Transaksi ----
	@GetMapping("/transaksi")
	public String transaksi(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long idPelanggan, Model model) {
		prepareCommon(model, from, to);
		model.addAttribute("idPelanggan", idPelanggan);
		if (from != null && to != null) {
			if (from.isAfter(to)) {
				model.addAttribute("error", "Tanggal awal tidak boleh melebihi tanggal akhir.");
			} else {
				List<Transaksi> rows = transaksiRepository.findReportTransaksi(start(from), end(to), idPelanggan);
				BigDecimal sum = rows.stream().map(Transaksi::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
				model.addAttribute("rows", rows);
				model.addAttribute("sumTotal", sum);
			}
		}
		return "laporan/transaksi";
	}

	@GetMapping("/transaksi/excel")
	public ResponseEntity<byte[]> transaksiExcel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long idPelanggan) throws Exception {
		requireValidRange(from, to);
		List<Transaksi> rows = transaksiRepository.findReportTransaksi(start(from), end(to), idPelanggan);
		BigDecimal sum = rows.stream().map(Transaksi::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		IdentitasInstansi id = identitas();
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sh = wb.createSheet("Laporan Transaksi");
			int r = writeLetterhead(sh, wb, id, "Laporan Transaksi", periode(from, to));
			String[] heads = {"No", "Nomor", "Tanggal", "Pelanggan", "Kasir", "Total"};
			Row hr = sh.createRow(r++);
			headerRow(hr, heads, headerStyle(wb));
			for (int i = 0; i < rows.size(); i++) {
				Transaksi t = rows.get(i);
				Row row = sh.createRow(r++);
				row.createCell(0).setCellValue(i + 1);
				row.createCell(1).setCellValue(t.getNomorTransaksi());
				row.createCell(2).setCellValue(t.getTanggal().format(DT_FMT));
				row.createCell(3).setCellValue(t.getPelanggan().getNama());
				row.createCell(4).setCellValue(t.getUser().getNama());
				row.createCell(5).setCellValue(t.getTotal().doubleValue());
			}
			Row sumRow = sh.createRow(r);
			sumRow.createCell(4).setCellValue("Jumlah/Total");
			sumRow.createCell(5).setCellValue(sum.doubleValue());
			sumRow.getCell(4).setCellStyle(headerStyle(wb));
			autoSize(sh, 6);
			wb.write(out);
			return download(out.toByteArray(), "laporan-transaksi.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		}
	}

	@GetMapping("/transaksi/pdf")
	public ResponseEntity<byte[]> transaksiPdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long idPelanggan) throws Exception {
		requireValidRange(from, to);
		List<Transaksi> rows = transaksiRepository.findReportTransaksi(start(from), end(to), idPelanggan);
		List<Map<String, Object>> data = new ArrayList<>();
		BigDecimal sum = BigDecimal.ZERO;
		int i = 1;
		for (Transaksi t : rows) {
			Map<String, Object> m = new HashMap<>();
			m.put("no", i++);
			m.put("nomor", t.getNomorTransaksi());
			m.put("tanggal", t.getTanggal().format(DT_FMT));
			m.put("pelanggan", t.getPelanggan().getNama());
			m.put("kasir", t.getUser().getNama());
			m.put("total", t.getTotal());
			data.add(m);
			sum = sum.add(t.getTotal());
		}
		Map<String, Object> params = baseParams("Laporan Transaksi", from, to);
		params.put("summary", "Jumlah Transaksi: " + rows.size() + "   Total Omzet: Rp " + formatMoney(sum));
		return pdfResponse("laporan_transaksi.jrxml", params, data, "laporan-transaksi.pdf");
	}

	// ---- Laporan Detail Penjualan ----
	@GetMapping("/detail-penjualan")
	public String detail(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long idPelanggan, @RequestParam(required = false) String q, Model model) {
		prepareCommon(model, from, to);
		model.addAttribute("idPelanggan", idPelanggan);
		model.addAttribute("q", q);
		if (from != null && to != null) {
			if (from.isAfter(to)) {
				model.addAttribute("error", "Tanggal awal tidak boleh melebihi tanggal akhir.");
			} else {
				List<DetailTransaksi> rows = detailTransaksiRepository.findReportDetails(start(from), end(to), idPelanggan, q);
				long sumQty = rows.stream().mapToLong(DetailTransaksi::getJumlah).sum();
				BigDecimal sumSub = rows.stream().map(DetailTransaksi::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
				model.addAttribute("rows", rows);
				model.addAttribute("sumQty", sumQty);
				model.addAttribute("sumSubtotal", sumSub);
			}
		}
		return "laporan/detail-penjualan";
	}

	@GetMapping("/detail-penjualan/excel")
	public ResponseEntity<byte[]> detailExcel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long idPelanggan, @RequestParam(required = false) String q) throws Exception {
		requireValidRange(from, to);
		List<DetailTransaksi> rows = detailTransaksiRepository.findReportDetails(start(from), end(to), idPelanggan, q);
		long sumQty = rows.stream().mapToLong(DetailTransaksi::getJumlah).sum();
		BigDecimal sumSub = rows.stream().map(DetailTransaksi::getSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
		IdentitasInstansi id = identitas();
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sh = wb.createSheet("Detail Penjualan");
			int r = writeLetterhead(sh, wb, id, "Laporan Detail Penjualan", periode(from, to));
			String[] heads = {"No", "Nomor", "Tanggal", "Pelanggan", "Barang", "Jumlah", "Harga Satuan", "Subtotal"};
			headerRow(sh.createRow(r++), heads, headerStyle(wb));
			for (int i = 0; i < rows.size(); i++) {
				DetailTransaksi d = rows.get(i);
				Row row = sh.createRow(r++);
				row.createCell(0).setCellValue(i + 1);
				row.createCell(1).setCellValue(d.getTransaksi().getNomorTransaksi());
				row.createCell(2).setCellValue(d.getTransaksi().getTanggal().format(DT_FMT));
				row.createCell(3).setCellValue(d.getTransaksi().getPelanggan().getNama());
				row.createCell(4).setCellValue(d.getBarang().getNamaBarang());
				row.createCell(5).setCellValue(d.getJumlah());
				row.createCell(6).setCellValue(d.getHargaSatuan().doubleValue());
				row.createCell(7).setCellValue(d.getSubtotal().doubleValue());
			}
			Row sumRow = sh.createRow(r);
			sumRow.createCell(4).setCellValue("Total");
			sumRow.createCell(5).setCellValue(sumQty);
			sumRow.createCell(7).setCellValue(sumSub.doubleValue());
			sumRow.getCell(4).setCellStyle(headerStyle(wb));
			autoSize(sh, 8);
			wb.write(out);
			return download(out.toByteArray(), "laporan-detail-penjualan.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		}
	}

	@GetMapping("/detail-penjualan/pdf")
	public ResponseEntity<byte[]> detailPdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long idPelanggan, @RequestParam(required = false) String q) throws Exception {
		requireValidRange(from, to);
		List<DetailTransaksi> rows = detailTransaksiRepository.findReportDetails(start(from), end(to), idPelanggan, q);
		List<Map<String, Object>> data = new ArrayList<>();
		long sumQty = 0;
		BigDecimal sumSub = BigDecimal.ZERO;
		int i = 1;
		for (DetailTransaksi d : rows) {
			Map<String, Object> m = new HashMap<>();
			m.put("no", i++);
			m.put("nomor", d.getTransaksi().getNomorTransaksi());
			m.put("tanggal", d.getTransaksi().getTanggal().format(DT_FMT));
			m.put("pelanggan", d.getTransaksi().getPelanggan().getNama());
			m.put("barang", d.getBarang().getNamaBarang());
			m.put("jumlah", d.getJumlah());
			m.put("harga", d.getHargaSatuan());
			m.put("subtotal", d.getSubtotal());
			data.add(m);
			sumQty += d.getJumlah();
			sumSub = sumSub.add(d.getSubtotal());
		}
		Map<String, Object> params = baseParams("Laporan Detail Penjualan", from, to);
		params.put("summary", "Total Qty: " + sumQty + "   Total Nilai: Rp " + formatMoney(sumSub));
		return pdfResponse("laporan_detail.jrxml", params, data, "laporan-detail-penjualan.pdf");
	}

	// ---- Laporan Penjualan per Barang ----
	@GetMapping("/penjualan-barang")
	public String barang(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to, Model model) {
		prepareCommon(model, from, to);
		if (from != null && to != null) {
			if (from.isAfter(to)) {
				model.addAttribute("error", "Tanggal awal tidak boleh melebihi tanggal akhir.");
			} else {
				var rows = transaksiRepository.findSalesByBarang(start(from), end(to));
				long sumQty = rows.stream().mapToLong(r -> r.getQty() == null ? 0 : r.getQty()).sum();
				BigDecimal sumNilai = rows.stream().map(r -> r.getNilai() == null ? BigDecimal.ZERO : r.getNilai())
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				model.addAttribute("rows", rows);
				model.addAttribute("sumQty", sumQty);
				model.addAttribute("sumNilai", sumNilai);
			}
		}
		return "laporan/penjualan-barang";
	}

	@GetMapping("/penjualan-barang/excel")
	public ResponseEntity<byte[]> barangExcel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws Exception {
		requireValidRange(from, to);
		var rows = transaksiRepository.findSalesByBarang(start(from), end(to));
		long sumQty = rows.stream().mapToLong(r -> r.getQty() == null ? 0 : r.getQty()).sum();
		BigDecimal sumNilai = rows.stream().map(r -> r.getNilai() == null ? BigDecimal.ZERO : r.getNilai())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		IdentitasInstansi id = identitas();
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sh = wb.createSheet("Penjualan per Barang");
			int r = writeLetterhead(sh, wb, id, "Laporan Penjualan per Barang", periode(from, to));
			String[] heads = {"No", "Kode", "Nama Barang", "Satuan", "Qty Terjual", "Total Nilai"};
			headerRow(sh.createRow(r++), heads, headerStyle(wb));
			for (int i = 0; i < rows.size(); i++) {
				var x = rows.get(i);
				Row row = sh.createRow(r++);
				row.createCell(0).setCellValue(i + 1);
				row.createCell(1).setCellValue(x.getKodeBarang());
				row.createCell(2).setCellValue(x.getNamaBarang());
				row.createCell(3).setCellValue(x.getSatuan());
				row.createCell(4).setCellValue(x.getQty() == null ? 0 : x.getQty());
				row.createCell(5).setCellValue(x.getNilai() == null ? 0 : x.getNilai().doubleValue());
			}
			Row sumRow = sh.createRow(r);
			sumRow.createCell(3).setCellValue("Total");
			sumRow.createCell(4).setCellValue(sumQty);
			sumRow.createCell(5).setCellValue(sumNilai.doubleValue());
			sumRow.getCell(3).setCellStyle(headerStyle(wb));
			autoSize(sh, 6);
			wb.write(out);
			return download(out.toByteArray(), "laporan-penjualan-barang.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		}
	}

	@GetMapping("/penjualan-barang/pdf")
	public ResponseEntity<byte[]> barangPdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) throws Exception {
		requireValidRange(from, to);
		var rows = transaksiRepository.findSalesByBarang(start(from), end(to));
		List<Map<String, Object>> data = new ArrayList<>();
		long sumQty = 0;
		BigDecimal sumNilai = BigDecimal.ZERO;
		int i = 1;
		for (var x : rows) {
			Map<String, Object> m = new HashMap<>();
			m.put("no", i++);
			m.put("kode", x.getKodeBarang());
			m.put("nama", x.getNamaBarang());
			m.put("satuan", x.getSatuan());
			m.put("qty", x.getQty() == null ? 0L : x.getQty());
			m.put("nilai", x.getNilai() == null ? BigDecimal.ZERO : x.getNilai());
			data.add(m);
			sumQty += x.getQty() == null ? 0 : x.getQty();
			sumNilai = sumNilai.add(x.getNilai() == null ? BigDecimal.ZERO : x.getNilai());
		}
		Map<String, Object> params = baseParams("Laporan Penjualan per Barang", from, to);
		params.put("summary", "Total Qty: " + sumQty + "   Total Nilai: Rp " + formatMoney(sumNilai));
		return pdfResponse("laporan_barang.jrxml", params, data, "laporan-penjualan-barang.pdf");
	}

	// ---- Laporan Penjualan per Pelanggan ----
	@GetMapping("/penjualan-pelanggan")
	public String pelanggan(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long idPelanggan, Model model) {
		prepareCommon(model, from, to);
		model.addAttribute("idPelanggan", idPelanggan);
		if (from != null && to != null) {
			if (from.isAfter(to)) {
				model.addAttribute("error", "Tanggal awal tidak boleh melebihi tanggal akhir.");
			} else {
				var rows = transaksiRepository.findSalesByPelanggan(start(from), end(to), idPelanggan);
				long sumTrx = rows.stream().mapToLong(r -> r.getJumlahTransaksi() == null ? 0 : r.getJumlahTransaksi()).sum();
				BigDecimal sumOmzet = rows.stream().map(r -> r.getTotalOmzet() == null ? BigDecimal.ZERO : r.getTotalOmzet())
						.reduce(BigDecimal.ZERO, BigDecimal::add);
				model.addAttribute("rows", rows);
				model.addAttribute("sumTrx", sumTrx);
				model.addAttribute("sumOmzet", sumOmzet);
			}
		}
		return "laporan/penjualan-pelanggan";
	}

	@GetMapping("/penjualan-pelanggan/excel")
	public ResponseEntity<byte[]> pelangganExcel(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long idPelanggan) throws Exception {
		requireValidRange(from, to);
		var rows = transaksiRepository.findSalesByPelanggan(start(from), end(to), idPelanggan);
		long sumTrx = rows.stream().mapToLong(r -> r.getJumlahTransaksi() == null ? 0 : r.getJumlahTransaksi()).sum();
		BigDecimal sumOmzet = rows.stream().map(r -> r.getTotalOmzet() == null ? BigDecimal.ZERO : r.getTotalOmzet())
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		IdentitasInstansi id = identitas();
		try (XSSFWorkbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			Sheet sh = wb.createSheet("Penjualan per Pelanggan");
			int r = writeLetterhead(sh, wb, id, "Laporan Penjualan per Pelanggan", periode(from, to));
			String[] heads = {"No", "Nama Pelanggan", "Jumlah Transaksi", "Total Omzet"};
			headerRow(sh.createRow(r++), heads, headerStyle(wb));
			for (int i = 0; i < rows.size(); i++) {
				var x = rows.get(i);
				Row row = sh.createRow(r++);
				row.createCell(0).setCellValue(i + 1);
				row.createCell(1).setCellValue(x.getNama());
				row.createCell(2).setCellValue(x.getJumlahTransaksi() == null ? 0 : x.getJumlahTransaksi());
				row.createCell(3).setCellValue(x.getTotalOmzet() == null ? 0 : x.getTotalOmzet().doubleValue());
			}
			Row sumRow = sh.createRow(r);
			sumRow.createCell(1).setCellValue("Total");
			sumRow.createCell(2).setCellValue(sumTrx);
			sumRow.createCell(3).setCellValue(sumOmzet.doubleValue());
			sumRow.getCell(1).setCellStyle(headerStyle(wb));
			autoSize(sh, 4);
			wb.write(out);
			return download(out.toByteArray(), "laporan-penjualan-pelanggan.xlsx",
					"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
		}
	}

	@GetMapping("/penjualan-pelanggan/pdf")
	public ResponseEntity<byte[]> pelangganPdf(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
			@RequestParam(required = false) Long idPelanggan) throws Exception {
		requireValidRange(from, to);
		var rows = transaksiRepository.findSalesByPelanggan(start(from), end(to), idPelanggan);
		List<Map<String, Object>> data = new ArrayList<>();
		long sumTrx = 0;
		BigDecimal sumOmzet = BigDecimal.ZERO;
		int i = 1;
		for (var x : rows) {
			Map<String, Object> m = new HashMap<>();
			m.put("no", i++);
			m.put("nama", x.getNama());
			m.put("jumlah", x.getJumlahTransaksi() == null ? 0L : x.getJumlahTransaksi());
			m.put("omzet", x.getTotalOmzet() == null ? BigDecimal.ZERO : x.getTotalOmzet());
			data.add(m);
			sumTrx += x.getJumlahTransaksi() == null ? 0 : x.getJumlahTransaksi();
			sumOmzet = sumOmzet.add(x.getTotalOmzet() == null ? BigDecimal.ZERO : x.getTotalOmzet());
		}
		Map<String, Object> params = baseParams("Laporan Penjualan per Pelanggan", from, to);
		params.put("summary", "Total Transaksi: " + sumTrx + "   Total Omzet: Rp " + formatMoney(sumOmzet));
		return pdfResponse("laporan_pelanggan.jrxml", params, data, "laporan-penjualan-pelanggan.pdf");
	}

	// ---- helpers ----
	private void prepareCommon(Model model, LocalDate from, LocalDate to) {
		model.addAttribute("activeMenu", "laporan");
		model.addAttribute("identitas", identitas());
		model.addAttribute("pelangganList", pelangganRepository.findAll(Sort.by("nama").ascending()));
		model.addAttribute("from", from);
		model.addAttribute("to", to);
		if (from != null && to != null) {
			model.addAttribute("periode", periode(from, to));
		}
	}

	private IdentitasInstansi identitas() {
		return identitasInstansiRepository.findFirstByOrderByIdInstansiAsc().orElse(null);
	}

	private LocalDateTime start(LocalDate d) { return d.atStartOfDay(); }
	private LocalDateTime end(LocalDate d) { return d.plusDays(1).atStartOfDay(); }
	private String periode(LocalDate from, LocalDate to) { return "Periode: " + from.format(PERIODE_FMT) + " s/d " + to.format(PERIODE_FMT); }

	private Map<String, Object> baseParams(String judul, LocalDate from, LocalDate to) {
		IdentitasInstansi id = identitas();
		Map<String, Object> p = new HashMap<>();
		p.put("judul", judul);
		p.put("instansi", id != null ? nvl(id.getNamaInstansi()) : "Sistem Penjualan");
		p.put("alamat", id != null ? nvl(id.getAlamat()) : "");
		String kontak = id == null ? "" : String.join(" · ", nonEmpty(id.getNoTelepon(), id.getEmail()));
		p.put("kontak", kontak);
		p.put("periode", periode(from, to));
		return p;
	}

	private List<String> nonEmpty(String... vals) {
		List<String> r = new ArrayList<>();
		if (vals == null) return r;
		for (String v : vals) if (v != null && !v.isBlank()) r.add(v);
		return r;
	}

	private String nvl(String s) { return s == null ? "" : s; }

	private ResponseEntity<byte[]> download(byte[] data, String filename, String contentType) {
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(filename).build().toString())
				.contentType(MediaType.parseMediaType(contentType))
				.body(data);
	}

	private ResponseEntity<byte[]> pdfResponse(String template, Map<String, Object> params, List<Map<String, Object>> data,
			String filename) throws Exception {
		JasperReport report = compile(template);
		JasperPrint print = JasperFillManager.fillReport(report, params, new JRBeanCollectionDataSource(data));
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		JRPdfExporter exporter = new JRPdfExporter();
		exporter.setExporterInput(new SimpleExporterInput(print));
		exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(out));
		exporter.exportReport();
		return download(out.toByteArray(), filename, "application/pdf");
	}

	private JasperReport compile(String name) {
		return reportCache.computeIfAbsent(name, n -> {
			try (InputStream in = getClass().getResourceAsStream("/reports/" + n)) {
				if (in == null) throw new IllegalStateException("Template tidak ditemukan: " + n);
				return JasperCompileManager.compileReport(in);
			} catch (Exception e) {
				throw new IllegalStateException("Gagal memuat template laporan: " + n, e);
			}
		});
	}

	private void requireValidRange(LocalDate from, LocalDate to) {
		if (from != null && to != null && from.isAfter(to)) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tanggal awal tidak boleh melebihi tanggal akhir.");
		}
	}

	private int writeLetterhead(Sheet sh, XSSFWorkbook wb, IdentitasInstansi id, String judul, String periode) {
		int r = 0;
		Row r0 = sh.createRow(r++); r0.createCell(0).setCellValue(id != null ? nvl(id.getNamaInstansi()) : "Sistem Penjualan");
		r0.getCell(0).setCellStyle(boldStyle(wb));
		Row r1 = sh.createRow(r++); r1.createCell(0).setCellValue(id != null ? nvl(id.getAlamat()) : "");
		Row r2 = sh.createRow(r++); r2.createCell(0).setCellValue(id == null ? "" : String.join(" · ", nonEmpty(id.getNoTelepon(), id.getEmail())));
		Row r3 = sh.createRow(r++); r3.createCell(0).setCellValue(judul); r3.getCell(0).setCellStyle(boldStyle(wb));
		Row r4 = sh.createRow(r++); r4.createCell(0).setCellValue(periode);
		r++;
		return r;
	}

	private CellStyle headerStyle(XSSFWorkbook wb) {
		CellStyle s = wb.createCellStyle();
		Font f = wb.createFont(); f.setBold(true); s.setFont(f); return s;
	}
	private CellStyle boldStyle(XSSFWorkbook wb) {
		CellStyle s = wb.createCellStyle();
		Font f = wb.createFont(); f.setBold(true); s.setFont(f); return s;
	}
	private void headerRow(Row row, String[] heads, CellStyle style) {
		for (int i = 0; i < heads.length; i++) { Cell c = row.createCell(i); c.setCellValue(heads[i]); c.setCellStyle(style); }
	}
	private void autoSize(Sheet sh, int cols) { for (int i = 0; i < cols; i++) sh.autoSizeColumn(i); }

	private String formatMoney(BigDecimal v) {
		if (v == null) v = BigDecimal.ZERO;
		return String.format(new java.util.Locale("id", "ID"), "%,.2f", v);
	}
}