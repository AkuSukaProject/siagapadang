import urllib.request
import re
import os
import urllib.error

url = 'https://doi.org/10.47134/ppm.v1i2.202'
try:
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0'})
    res = urllib.request.urlopen(req)
    html = res.read().decode('utf-8')
    
    # Mencari tautan download PDF pada halaman jurnal OJS
    pdf_links = re.findall(r'href="([^"]+/article/download/[^"]+)"', html)
    if not pdf_links:
        pdf_links = re.findall(r'href="([^"]+/article/view/[0-9]+/[0-9]+)"', html)
        
    if pdf_links:
        pdf_url = pdf_links[0]
        pdf_url = pdf_url.replace('/view/', '/download/')
        print(f"Ditemukan URL PDF: {pdf_url}")
        
        out_dir = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "references")
        os.makedirs(out_dir, exist_ok=True)
        out_path = os.path.join(out_dir, "SIG_ArcGIS_NetworkAnalyst_Pubmedia.pdf")
        
        print(f"Mengunduh ke {out_path}...")
        try:
            pdf_req = urllib.request.Request(pdf_url, headers={'User-Agent': 'Mozilla/5.0'})
            with urllib.request.urlopen(pdf_req) as response, open(out_path, 'wb') as out_file:
                out_file.write(response.read())
            print("Unduhan berhasil!")
        except urllib.error.HTTPError as e:
            print(f"Gagal mengunduh langsung ({e}).")
    else:
        print("Tidak menemukan tautan PDF langsung.")
except Exception as e:
    print(f"Error: {e}")
