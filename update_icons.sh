#!/bin/bash
ICON="../icon_final_foreground.png"
BG_COLOR="04326E"

# Function to generate icon
# usage: gen_icon "mipmap-folder" "size"
gen_icon() {
  FOLDER=$1
  SIZE=$2
  # Calculate 65% scale for "Safe Zone" look
  ICON_SIZE=$(python3 -c "print(int($SIZE * 0.65))")
  OFFSET=$(python3 -c "print(int(($SIZE - $ICON_SIZE) / 2))")
  
  TARGET_DIR="app/src/main/res/$FOLDER"
  mkdir -p "$TARGET_DIR"
  
  # DELETE OLD FILES to ensure no staleness
  rm -f "$TARGET_DIR/ic_launcher.png" "$TARGET_DIR/ic_launcher_round.png"
  
  echo "Generating $FOLDER ($SIZE x $SIZE)..."
  
  # Composite: White Background + Scaled Icon Centered
  ffmpeg -y -hide_banner -loglevel error \
    -f lavfi -i color=c=$BG_COLOR:s=${SIZE}x${SIZE} \
    -i "$ICON" \
    -filter_complex "[1:v]scale=${ICON_SIZE}:${ICON_SIZE}[icon];[0:v][icon]overlay=${OFFSET}:${OFFSET}" \
    -frames:v 1 \
    "$TARGET_DIR/ic_launcher.png"
    
  cp "$TARGET_DIR/ic_launcher.png" "$TARGET_DIR/ic_launcher_round.png"
}

gen_icon "mipmap-mdpi" 48
gen_icon "mipmap-hdpi" 72
gen_icon "mipmap-xhdpi" 96
gen_icon "mipmap-xxhdpi" 144
gen_icon "mipmap-xxxhdpi" 192

echo "✅ All legacy icons updated!"
