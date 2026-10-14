import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const project = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const packet = path.resolve(project, '../..');
const docs = path.join(project, 'docs');
const check = process.argv.includes('--check');
const intro = fs.readFileSync(path.join(docs, 'MANUAL-INTRO.md'), 'utf8').trimEnd();
const files = [
  'pom.xml', 'Dockerfile', 'compose.yaml', '.env.example', '.gitignore', '.dockerignore',
  '.mvn/wrapper/maven-wrapper.properties',
].filter(file => fs.existsSync(path.join(project, file)));
function visit(relative) {
  for (const entry of fs.readdirSync(path.join(project, relative), { withFileTypes: true })) {
    const file = relative + '/' + entry.name;
    if (entry.isDirectory()) visit(file);
    else if (entry.isFile()) files.push(file);
    else throw new Error('Unexpected non-file in own source tree: ' + file);
  }
}
for (const directory of ['src', 'examples', 'scripts']) visit(directory);
const ordered = [...new Set(files)].sort();
const language = file => {
  const extension = path.extname(file);
  if (file.endsWith('.env.example')) return 'dotenv';
  if (file === 'Dockerfile') return 'dockerfile';
  return { '.java': 'java', '.xml': 'xml', '.yml': 'yaml', '.yaml': 'yaml',
    '.json': 'json', '.js': 'javascript', '.cjs': 'javascript', '.mjs': 'javascript',
    '.html': 'html', '.css': 'css', '.sql': 'sql', '.sh': 'bash', '.ps1': 'powershell',
    '.properties': 'properties' }[extension] ?? 'text';
};
// Four backticks keep code that itself contains Markdown fences intact.
const fence = '`'.repeat(4);
const blocks = ordered.map(file => {
  const code = fs.readFileSync(path.join(project, file), 'utf8').trimEnd();
  if (code.split(/\r?\n/).some(line => line.startsWith(fence))) throw new Error('Nested long fence: ' + file);
  return '### ' + file + '\n\n' + fence + language(file) + '\n' + code + '\n' + fence + '\n';
}).join('\n');
const appendix = '\n\n## Полное содержимое файлов проекта\n\n'
  + 'Ниже непосредственно содержимое проверяемых файлов, без многоточий и пропущенных строк. '
  + 'Пути относительны корню WhichOne. Стандартный Maven Wrapper входит в архив; '
  + 'его большой неизменённый служебный код здесь не повторяется. Рабочие БД, зависимости, '
  + 'сборки, исходный каркас Initializr и временные браузерные отчёты не относятся к этим листингам.\n\n'
  + blocks;
function content(navigation) {
  return (intro.includes('{{NAVIGATION}}') ? intro.replaceAll('{{NAVIGATION}}', navigation)
    : navigation + '\n\n' + intro) + appendix;
}
const destinations = [[path.join(docs, 'MANUAL.md'), content(
  '[Условие](ASSIGNMENT.md) · [README](../README.md) · [Теория](THEORY.md) · '
  + '[Задания без ответов](PRACTICE.md) · [Решения](SOLUTIONS.md) · [Проверка](../VERIFICATION.md)')]];
if (path.relative(packet, project).replaceAll('\\', '/') === 'solutions/whichone' &&
    fs.existsSync(path.join(packet, 'assignments/final-backend-whichone.md'))) {
  destinations.push([path.join(packet, 'guides/final-backend-whichone.md'), content(
    '[Условие](../assignments/final-backend-whichone.md) · [Проект](../solutions/whichone/README.md) · '
    + '[Общий запуск Mac](../GROUP-RUN.md) · [Проверки](../solutions/whichone/VERIFICATION.md)')]);
}
for (const [file, material] of destinations) {
  if (check) {
    if (!fs.existsSync(file) || fs.readFileSync(file, 'utf8') !== material)
      throw new Error('Manual differs from current source/intro: ' + file);
  } else fs.writeFileSync(file, material, 'utf8');
  const actual = fs.readFileSync(file, 'utf8');
  for (const source of ordered) {
    const code = fs.readFileSync(path.join(project, source), 'utf8').trimEnd();
    if (!actual.includes(fence + language(source) + '\n' + code + '\n' + fence))
      throw new Error('Incomplete source listing: ' + source);
  }
}
console.log(`${check ? 'Verified' : 'Generated'} full manuals: ${ordered.length} own files, ${destinations.length} documents, exact source match.`);
