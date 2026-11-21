import { mkdirSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';
import { deflateSync } from 'node:zlib';

const __dirname = dirname(fileURLToPath(import.meta.url));
const assetsDir = join(__dirname, '..', 'assets');

// CRC32 table for valid PNG chunk checksums
const crcTable = new Uint32Array(256);
for (let n = 0; n < 256; n += 1) {
  let c = n;
  for (let k = 0; k < 8; k += 1) {
    c = (c & 1) !== 0 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
  }
  crcTable[n] = c >>> 0;
}

function crc32(buf) {
  let c = 0xffffffff;
  for (let i = 0; i < buf.length; i += 1) {
    c = crcTable[(c ^ buf[i]) & 0xff] ^ (c >>> 8);
  }
  return (c ^ 0xffffffff) >>> 0;
}

function pngChunk(type, data) {
  const typeBytes = Buffer.from(type, 'ascii');
  const len = Buffer.alloc(4);
  len.writeUInt32BE(data.length, 0);
  const body = Buffer.concat([typeBytes, data]);
  const crc = Buffer.alloc(4);
  crc.writeUInt32BE(crc32(body), 0);
  return Buffer.concat([len, body, crc]);
}

function encodePng(width, height, rgba) {
  const signature = Buffer.from([137, 80, 78, 71, 13, 10, 26, 10]);
  const ihdr = Buffer.alloc(13);
  ihdr.writeUInt32BE(width, 0);
  ihdr.writeUInt32BE(height, 4);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 6; // RGBA
  ihdr[10] = 0;
  ihdr[11] = 0;
  ihdr[12] = 0;

  const stride = width * 4;
  const raw = Buffer.alloc((stride + 1) * height);
  for (let y = 0; y < height; y += 1) {
    const rowStart = y * (stride + 1);
    raw[rowStart] = 0; // filter: None
    rgba.copy(raw, rowStart + 1, y * stride, (y + 1) * stride);
  }

  return Buffer.concat([
    signature,
    pngChunk('IHDR', ihdr),
    pngChunk('IDAT', deflateSync(raw, { level: 9 })),
    pngChunk('IEND', Buffer.alloc(0)),
  ]);
}

/**
 * Renders the Studio Manager terracotta pottery vessel mark (matching web/public/favicon.svg)
 * on a warm stoneware cream background or transparent canvas.
 */
function renderMark(size, { background, scale }) {
  const rgba = Buffer.alloc(size * size * 4);
  const fg = [0x9a, 0x4a, 0x2c, 0xff]; // terracotta accent from mobile/src/ui.tsx
  const highlight = [0xb6, 0x62, 0x40, 0xff];
  const rim = [0x7e, 0x3a, 0x20, 0xff];

  const half = size / 2;
  const radius = (size * scale) / 2;

  for (let y = 0; y < size; y += 1) {
    for (let x = 0; x < size; x += 1) {
      const idx = (y * size + x) * 4;
      let color = background;

      // Normalized coordinates in [-1, 1] relative to the vessel box
      const nx = (x + 0.5 - half) / radius;
      const ny = (y + 0.5 - half) / radius;

      // Vessel silhouette: rim + neck + rounded ceramic belly + flat foot
      const inLip = ny >= -0.82 && ny <= -0.66 && Math.abs(nx) <= 0.44;
      const inNeck = ny > -0.66 && ny <= -0.44 && Math.abs(nx) <= 0.34 + ((ny + 0.66) / 0.22) * 0.14;
      const bellyDx = nx / 0.86;
      const bellyDy = (ny - 0.14) / 0.7;
      const inBelly = ny > -0.44 && ny <= 0.78 && bellyDx * bellyDx + bellyDy * bellyDy <= 1.0;
      const inFoot = ny > 0.68 && ny <= 0.84 && Math.abs(nx) <= 0.46;

      if (inLip) {
        color = rim;
      } else if (inNeck || inBelly || inFoot) {
        // Subtle horizontal wheel-thrown ceramic band near upper shoulder
        const inBand = ny > -0.18 && ny < -0.06;
        color = inBand ? highlight : fg;
      }

      rgba[idx] = color[0];
      rgba[idx + 1] = color[1];
      rgba[idx + 2] = color[2];
      rgba[idx + 3] = color[3];
    }
  }
  return encodePng(size, size, rgba);
}

mkdirSync(assetsDir, { recursive: true });

const cream = [0xf7, 0xf4, 0xef, 0xff];
const transparent = [0x00, 0x00, 0x00, 0x00];

writeFileSync(join(assetsDir, 'icon.png'), renderMark(1024, { background: cream, scale: 0.72 }));
writeFileSync(join(assetsDir, 'adaptive-icon.png'), renderMark(1024, { background: transparent, scale: 0.56 }));
writeFileSync(join(assetsDir, 'splash-icon.png'), renderMark(512, { background: transparent, scale: 0.72 }));

console.log('Generated mobile/assets/{icon,adaptive-icon,splash-icon}.png');
