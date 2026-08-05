import pandas as pd

file_path = r"c:\Users\asus\Downloads\LAMP 1 - TABEL IDENTIFIKASI KETERSEDIAAN DAN KEBUTUHAN FASILITAS EVAKUASI TINGKAT ZONA KOTA PADANG (CETAK).xlsx"

# Membaca data excel
try:
    df = pd.read_excel(file_path, engine='openpyxl')
    print("Kolom yang tersedia:", df.columns.tolist())
    
    # Mencoba mencari data Sektor E dan G
    # Pandas biasanya membaca merged cells dengan menaruh nilai di baris pertama dan NaN di baris selanjutnya
    print("\nSample Data:")
    print(df.head(20).to_string())
except Exception as e:
    print(f"Error membaca Excel: {e}")
