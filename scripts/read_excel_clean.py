import pandas as pd
import json
import os

file_path = r"c:\Users\asus\Downloads\LAMP 1 - TABEL IDENTIFIKASI KETERSEDIAAN DAN KEBUTUHAN FASILITAS EVAKUASI TINGKAT ZONA KOTA PADANG (CETAK).xlsx"

# Kita baca seluruh row tanpa header khusus dulu
df = pd.read_excel(file_path, engine='openpyxl')

# Cari kolom yang mengandung kata 'SEKTOR' dan isi nilai NaN dengan ffill
df.ffill(inplace=True)

# Filter string
def contains_sector(row):
    return any(isinstance(val, str) and ('SEKTOR E' in val or 'SEKTOR G' in val) for val in row)

mask = df.apply(contains_sector, axis=1)
filtered_df = df[mask]

out_file = os.path.join("data", "sektor_eg.csv")
filtered_df.to_csv(out_file, index=False)
print(f"Berhasil mengekstrak Sektor E dan G ke {out_file}")
