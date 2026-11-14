(() => {
	const rows = document.getElementById('itemRows');
	const template = document.getElementById('itemRowTemplate');
	const addButton = document.getElementById('addRow');
	const totalEl = document.getElementById('grandTotal');
	const formatter = new Intl.NumberFormat('id-ID', {
		style: 'currency',
		currency: 'IDR',
		minimumFractionDigits: 2,
		maximumFractionDigits: 2
	});

	function reindex() {
		[...rows.querySelectorAll('.item-row')].forEach((row, index) => {
			row.querySelectorAll('[data-name]').forEach(field => {
				field.name = `items[${index}].${field.dataset.name}`;
			});
		});
	}

	function selectedOption(row) {
		const select = row.querySelector('.barang-select');
		return select.selectedOptions[0];
	}

	function updateRow(row) {
		const option = selectedOption(row);
		const quantity = Number(row.querySelector('.jumlah-input').value) || 0;
		const price = option && option.dataset.harga ? Number(option.dataset.harga) : null;
		const stock = option && option.dataset.stok ? Number(option.dataset.stok) : null;

		row.querySelector('.harga-satuan').textContent = price === null ? '-' : formatter.format(price);
		row.querySelector('.subtotal').textContent = price === null ? '-' : formatter.format(price * quantity);
		row.querySelector('.stock-info').textContent = stock === null
			? ''
			: `Stok tersedia: ${stock} ${option.dataset.satuan || ''}`.trim();
		updateTotal();
	}

	function updateTotal() {
		let total = 0;
		rows.querySelectorAll('.item-row').forEach(row => {
			const option = selectedOption(row);
			const price = option && option.dataset.harga ? Number(option.dataset.harga) : 0;
			const quantity = Number(row.querySelector('.jumlah-input').value) || 0;
			total += price * quantity;
		});
		totalEl.textContent = formatter.format(total);
	}

	function bind(row) {
		row.querySelector('.barang-select').addEventListener('change', () => updateRow(row));
		row.querySelector('.jumlah-input').addEventListener('input', () => updateRow(row));
		row.querySelector('.remove-row').addEventListener('click', () => {
			if (rows.querySelectorAll('.item-row').length === 1) {
				return;
			}
			row.remove();
			reindex();
			updateTotal();
		});
		updateRow(row);
	}

	addButton.addEventListener('click', () => {
		const row = template.content.firstElementChild.cloneNode(true);
		rows.appendChild(row);
		reindex();
		bind(row);
	});

	reindex();
	rows.querySelectorAll('.item-row').forEach(bind);
})();
