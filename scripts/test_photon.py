import urllib.request
import json
url = "https://photon.komoot.io/api/?q=Hotel+Ibis+Padang&limit=1"
try:
    response = urllib.request.urlopen(url)
    print(json.loads(response.read().decode('utf-8')))
except Exception as e:
    print(e)
