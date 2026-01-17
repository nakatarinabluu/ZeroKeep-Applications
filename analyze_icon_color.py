from PIL import Image
import sys

try:
    path = "/mnt/Project/Android App/Password Saver/android/app/src/main/res/mipmap-xhdpi/ic_launcher.png"
    img = Image.open(path)
    # Get top-left pixel which serves as background for square icons
    rgb = img.getpixel((0, 0))
    # Convert to Hex
    hex_color = '#{:02x}{:02x}{:02x}'.format(rgb[0], rgb[1], rgb[2])
    print(f"Background Hex: {hex_color.upper()}")
except Exception as e:
    print(f"Error: {e}")
