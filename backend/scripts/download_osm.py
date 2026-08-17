import osmnx as ox
import os

# Buat folder data jika belum ada
base_dir = os.path.dirname(os.path.dirname(__file__))
data_dir = os.path.join(base_dir, "data")
os.makedirs(data_dir, exist_ok=True)

place_name = "Padang Barat, Padang, Indonesia"
print(f"Mulai mengunduh jaringan jalan khusus pejalan kaki (walk) untuk: {place_name}...")
print("Proses ini menghubungi server OSM, mohon tunggu beberapa saat...\n")

try:
    # Mengunduh graf jaringan jalan pejalan kaki (gang, trotoar, jalan setapak)
    G = ox.graph_from_place(place_name, network_type="walk")
    
    # Hitung jumlah node dan edge
    num_nodes = len(G.nodes)
    num_edges = len(G.edges)
    
    print("========================================")
    print(" EKSTRAKSI BERHASIL")
    print("========================================")
    print(f"Total Nodes (Persimpangan/Titik): {num_nodes}")
    print(f"Total Edges (Ruas Jalan/Gang): {num_edges}")
    print("========================================\n")
    
    # Simpan sebagai gambar untuk dievaluasi visual
    plot_path = os.path.join(data_dir, "padang_barat_walk.png")
    print(f"Menyimpan visualisasi peta ke: {plot_path}")
    fig, ax = ox.plot_graph(G, show=False, save=True, filepath=plot_path, close=True)
    
    # Simpan sebagai GraphML agar nanti bisa dipakai oleh Habib (NetworkX)
    graphml_path = os.path.join(data_dir, "padang_barat_walk.graphml")
    print(f"Menyimpan data graf ke: {graphml_path}")
    ox.save_graphml(G, filepath=graphml_path)
    
    print("\nSelesai! Silakan buka file gambarnya untuk melihat apakah gang-gang kecil muncul.")

except Exception as e:
    print(f"\nTerjadi kesalahan saat mengunduh data: {e}")
