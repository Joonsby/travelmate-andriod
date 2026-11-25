// HEIC 스티커 → WebP. Android 플랫폼 디코더는 HEIC 의 보조 알파 이미지를 버려 배경이 검게 나오므로
// libheif(wasm) 로 RGBA 까지 풀고 sharp 로 WebP 를 만든다.
//
//   node convert.js <stickers 폴더> <출력 폴더>
//   예) node convert.js ../../../travelmate-api/public/stickers ./out
//
// <묶음>/<이름>.heic → <출력>/<묶음>/<이름>.webp. 이미 있으면 건너뛴다. 모서리 알파가 0 이 아니면 실패로 센다.
import { readdir, readFile, mkdir, stat } from 'node:fs/promises';
import path from 'node:path';
import decode from 'heic-decode';
import sharp from 'sharp';

const [src, out] = process.argv.slice(2);
if (!src || !out) {
  console.error('사용법: node convert.js <stickers 폴더> <출력 폴더>');
  process.exit(1);
}

const QUALITY = 90;
const CONCURRENCY = 8;

const jobs = [];
for (const pack of await readdir(src, { withFileTypes: true })) {
  if (!pack.isDirectory()) continue;
  for (const file of await readdir(path.join(src, pack.name))) {
    if (file.endsWith('.heic')) jobs.push([pack.name, file]);
  }
}

let done = 0;
let skipped = 0;
let inBytes = 0;
let outBytes = 0;
const failures = [];

async function convert([pack, file]) {
  const from = path.join(src, pack, file);
  const to = path.join(out, pack, file.replace(/\.heic$/, '.webp'));
  const inSize = (await stat(from)).size;
  inBytes += inSize;
  if (await stat(to).catch(() => null)) {
    const outSize = (await stat(to)).size;
    outBytes += outSize;
    skipped++;
    return;
  }
  try {
    const { width, height, data } = await decode({ buffer: await readFile(from) });
    const transparent = countTransparent(data, 4);
    if (transparent === 0) throw new Error('원본에 투명한 부분이 없음');
    await mkdir(path.dirname(to), { recursive: true });
    await sharp(Buffer.from(data.buffer, data.byteOffset, data.byteLength), { raw: { width, height, channels: 4 } })
      .webp({ quality: QUALITY, alphaQuality: 100, effort: 6 })
      .toFile(to);
    // 다시 읽어 투명이 남았는지 확인한다
    const check = await sharp(to).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
    // 손실 압축이라 경계에서 몇 픽셀 차이는 날 수 있다. 투명 영역의 98% 이상이 남아야 통과
    if (countTransparent(check.data, check.info.channels) < transparent * 0.98) throw new Error('변환 후 투명 영역이 줄어듦');
    const outSize = (await stat(to)).size;
    outBytes += outSize;
  } catch (error) {
    failures.push(`${pack}/${file}: ${error.message}`);
  }
  done++;
  if (done % 200 === 0) console.log(`${done + skipped}/${jobs.length}`);
}

function countTransparent(rgba, channels) {
  let n = 0;
  for (let i = channels - 1; i < rgba.length; i += channels) if (rgba[i] === 0) n++;
  return n;
}

const queue = [...jobs];
await Promise.all(Array.from({ length: CONCURRENCY }, async () => {
  while (queue.length) await convert(queue.shift());
}));

const mb = (n) => (n / 1024 / 1024).toFixed(1);
console.log(`대상 ${jobs.length} · 변환 ${done - failures.length} · 이미 있음 ${skipped} · 실패 ${failures.length}`);
console.log(`HEIC ${mb(inBytes)}MB → WebP ${mb(outBytes)}MB`);
if (failures.length) {
  console.log(failures.slice(0, 20).join('\n'));
  process.exitCode = 1;
}
