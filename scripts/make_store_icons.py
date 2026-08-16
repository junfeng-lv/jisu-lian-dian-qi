import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import store_common as C

C.OUT.mkdir(parents=True, exist_ok=True)
icon512 = C.icon_image(512)
icon1024 = C.icon_image(1024)
icon512.save(C.OUT / 'icon_512.png')
icon1024.save(C.OUT / 'icon_1024.png')
print(C.OUT)
print(icon512.size, icon1024.size)
