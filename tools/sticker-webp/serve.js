// 변환 결과를 에뮬레이터에서 확인하기 위한 임시 서버. 서버 반영 전 시험용.
//   node serve.js <stickers 폴더> <webp 출력 폴더> [포트]
// /stickers/catalog.json 은 원본 폴더의 묶음별 catalog.json 을 합쳐서, 그림은 webp 출력 → 원본 순으로 찾는다.
// 에뮬레이터에서는 http://10.0.2.2:<포트> 를 API 주소로 넣는다.
import { createServer } from 'node:http';
import { readdirSync, readFileSync, existsSync } from 'node:fs';
import path from 'node:path';

const [src, out, port = '8090'] = process.argv.slice(2);
const types = { '.webp': 'image/webp', '.heic': 'image/heic', '.json': 'application/json' };

const packs = readdirSync(src, { withFileTypes: true })
  .filter((d) => d.isDirectory())
  .flatMap((d) => JSON.parse(readFileSync(path.join(src, d.name, 'catalog.json'), 'utf8')).packs);
const catalog = JSON.stringify({ packs });

createServer((req, res) => {
  const url = decodeURIComponent(new URL(req.url, 'http://x').pathname);
  if (url === '/stickers/catalog.json') return res.writeHead(200, { 'content-type': types['.json'] }).end(catalog);
  const rel = url.replace(/^\/stickers\//, '');
  if (rel.includes('..')) return res.writeHead(400).end();
  const file = [path.join(out, rel), path.join(src, rel)].find(existsSync);
  if (!file) return res.writeHead(404).end();
  res.writeHead(200, { 'content-type': types[path.extname(file)] ?? 'application/octet-stream' }).end(readFileSync(file));
}).listen(Number(port), () => console.log(`http://localhost:${port}/stickers/catalog.json`));
