const $ = id => document.getElementById(id)
const apiRoot = '/api/v2'
const exercisePicker = $('exercise-picker')
const authorPicker = $('author-picker')
const source = $('source')
const language = $('language')
const form = $('author-form')
const cases = $('cases')

const templates = {
  JAVA: 'import java.util.Scanner;\n\npublic class Main {\n    public static void main(String[] args) {\n        Scanner input = new Scanner(System.in);\n        // 在这里读取输入并输出结果。\n    }\n}\n',
  C: '#include <stdio.h>\n\nint main(void) {\n    /* 在这里读取输入并输出结果。 */\n    return 0;\n}\n',
  CPP: '#include <iostream>\n\nint main() {\n    // 在这里读取输入并输出结果。\n    return 0;\n}\n',
}
const verdicts = {
  ACCEPTED: '通过', WRONG_OUTPUT: '输出不匹配', COMPILE_ERROR: '编译失败',
  TIME_LIMIT: '运行超时', OUTPUT_LIMIT: '输出超限', RUNTIME_ERROR: '运行错误',
}
const drafts = new Map()
let draftKey = ''
let currentExercise = ''
let authorExercise = ''
let busy = false
let navigated = 0
let authorRevision = 0
let historyRevision = 0

async function request(path, { method = 'GET', data } = {}) {
  const response = await fetch(apiRoot + path, {
    method,
    headers: data === undefined ? {} : { 'Content-Type': 'application/json' },
    body: data === undefined ? undefined : JSON.stringify(data),
  })
  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || '请求失败（HTTP ' + response.status + '）')
  }
  return response.json()
}

function node(tag, text, className) {
  const item = document.createElement(tag)
  if (text !== undefined) item.textContent = text
  if (className) item.className = className
  return item
}

function error(message = '') {
  $('app-error').textContent = message
  $('app-error').hidden = !message
}

function guarded(task) {
  return async () => {
    error()
    try { await task() } catch (failure) { error(failure.message) }
  }
}

function updateButtons() {
  $('try').disabled = $('submit').disabled = busy || !currentExercise
}

function setSource(slug) {
  if (draftKey) drafts.set(draftKey, source.value)
  draftKey = slug ? slug + '/' + language.value : ''
  source.value = draftKey ? drafts.get(draftKey) ?? templates[language.value] : ''
  source.disabled = !slug
}

function options(picker, entries, placeholder, preferred) {
  picker.replaceChildren(new Option(placeholder, ''))
  for (const entry of entries) {
    picker.add(new Option(entry.title + (entry.archived ? '（已归档）' : ''), entry.slug))
  }
  picker.value = entries.some(entry => entry.slug === preferred) ? preferred : ''
}

async function catalog(preferred = exercisePicker.value) {
  const entries = await request('/exercises')
  options(exercisePicker, entries, '暂无题目', preferred)
  if (!exercisePicker.value && entries.length) exercisePicker.value = entries[0].slug
  await openExercise()
}

async function openExercise() {
  const revision = ++navigated
  const slug = exercisePicker.value
  currentExercise = ''
  $('exercise-title').textContent = slug ? '正在加载题目…' : '暂无题目'
  $('limits').textContent = ''
  $('statement').textContent = ''
  $('examples').replaceChildren()
  $('result').replaceChildren()
  $('trial-input').value = ''
  $('history').replaceChildren()
  setSource('')
  updateButtons()
  if (!slug) return
  const exercise = await request('/exercises/' + encodeURIComponent(slug))
  if (revision !== navigated) return
  currentExercise = slug
  $('exercise-title').textContent = exercise.title
  $('limits').textContent = exercise.policy.timeMs + ' ms / 用例 · ' +
    (exercise.policy.comparison === 'TOKENS' ? '忽略分隔空白差异' : '逐字符比较')
  $('statement').textContent = exercise.statement
  for (const [i, entry] of exercise.examples.entries()) {
    const box = node('section', undefined, 'example')
    box.append(node('h3', '示例 ' + (i + 1)), node('span', '输入', 'muted'), node('pre', entry.input),
      node('span', '输出', 'muted'), node('pre', entry.expected))
    $('examples').append(box)
  }
  $('trial-input').value = exercise.examples[0]?.input || ''
  setSource(slug)
  updateButtons()
  await refreshHistory()
}

function showResult(report, trial = false, id) {
  const host = $('result')
  host.replaceChildren()
  const caption = trial && report.verdict === 'ACCEPTED' ? '自测运行成功' : verdicts[report.verdict]
  host.append(node('h3', (id ? '提交 #' + id + ' · ' : '') + caption))
  if (report.compilerMessage) host.append(node('pre', report.compilerMessage))
  for (const check of report.checks) {
    const section = node('section', undefined, 'check')
    const label = trial ? '自测' : check.example ? '示例用例' : '隐藏用例'
    section.append(node('p', label + ' #' + check.position + ' · ' +
      (trial && check.verdict === 'ACCEPTED' ? '运行成功' : verdicts[check.verdict]) + ' · ' + check.elapsedMs + ' ms'))
    if (check.example && check.output) section.append(node('pre', check.output))
    host.append(section)
  }
}

async function refreshHistory() {
  const revision = ++historyRevision
  const slug = currentExercise
  $('history').replaceChildren()
  if (!slug) return
  const list = await request('/attempts?exercise=' + encodeURIComponent(slug))
  if (revision !== historyRevision || currentExercise !== slug) return
  if (!list.length) $('history').append(node('li', '这道题还没有提交。', 'muted'))
  for (const entry of list) {
    const row = node('li')
    const button = node('button', '#' + entry.id + ' · ' + entry.language + ' · ' + verdicts[entry.verdict])
    button.type = 'button'
    button.addEventListener('click', guarded(async () => {
      const detail = await request('/attempts/' + entry.id)
      if (currentExercise !== slug) return
      showResult(detail.result, false, detail.summary.id)
      const oldSource = node('details')
      oldSource.append(node('summary', '查看当时提交的源代码'), node('pre', detail.source))
      $('result').append(oldSource)
    }))
    row.append(button, node('time', new Date(entry.submittedAt).toLocaleString()))
    $('history').append(row)
  }
}

async function run(trial) {
  if (busy || !currentExercise) return
  const slug = currentExercise
  const revision = navigated
  const body = { language: language.value, source: source.value }
  if (trial) body.input = $('trial-input').value
  busy = true
  updateButtons()
  $('result').replaceChildren(node('p', trial ? '正在运行自测…' : '正在编译和评测…'))
  try {
    const response = await request('/exercises/' + encodeURIComponent(slug) + (trial ? '/trials' : '/attempts'),
      { method: 'POST', data: body })
    if (revision === navigated) {
      showResult(trial ? response : response.result, trial, response.summary?.id)
      if (!trial) await refreshHistory()
    }
  } catch (failure) {
    if (revision === navigated) $('result').replaceChildren(node('p', failure.message, 'error'))
  } finally {
    busy = false
    updateButtons()
  }
}

function field(name) {
  return form.elements.namedItem(name)
}

function addCase(data = {}) {
  if (cases.children.length >= 20) return
  const row = $('case-template').content.firstElementChild.cloneNode(true)
  row.querySelector('[name=input]').value = data.input ?? ''
  row.querySelector('[name=expected]').value = data.expected ?? ''
  row.querySelector('[name=example]').checked = data.example ?? false
  row.querySelector('[data-remove]').addEventListener('click', () => {
    if (cases.children.length > 1) row.remove()
  })
  cases.append(row)
}

async function openAuthor() {
  const revision = ++authorRevision
  const slug = authorPicker.value
  $('save').disabled = true
  $('author-status').textContent = ''
  try {
    const draft = slug ? await request('/authoring/exercises/' + encodeURIComponent(slug)) : null
    if (revision !== authorRevision) return
    authorExercise = slug
    form.reset()
    field('slug').readOnly = !!slug
    $('archive-label').hidden = !slug
    cases.replaceChildren()
    if (draft) {
      for (const key of ['slug', 'title', 'statement']) field(key).value = draft[key]
      field('archived').checked = draft.archived
      field('time').value = draft.policy.timeMs
      field('output').value = draft.policy.outputBytes
      field('comparison').value = draft.policy.comparison
      for (const entry of draft.cases) addCase(entry)
    } else {
      addCase({ example: true })
    }
    $('save').disabled = false
  } catch (failure) {
    if (revision === authorRevision) throw failure
  }
}

async function authorCatalog(preferred = authorPicker.value) {
  options(authorPicker, await request('/authoring/exercises'), '新建题目', preferred)
  await openAuthor()
}

form.addEventListener('submit', async event => {
  event.preventDefault()
  const payload = {
    slug: field('slug').value.trim(), title: field('title').value.trim(), statement: field('statement').value,
    archived: field('archived').checked,
    policy: { timeMs: Number(field('time').value), outputBytes: Number(field('output').value),
      comparison: field('comparison').value },
    cases: Array.from(cases.children, row => ({ input: row.querySelector('[name=input]').value,
      expected: row.querySelector('[name=expected]').value, example: row.querySelector('[name=example]').checked })),
  }
  $('save').disabled = true
  authorPicker.disabled = true
  try {
    await request('/authoring/exercises' + (authorExercise ? '/' + encodeURIComponent(authorExercise) : ''),
      { method: authorExercise ? 'PUT' : 'POST', data: payload })
    await Promise.all([authorCatalog(payload.slug), catalog(payload.slug)])
    $('author-status').textContent = '已保存。'
  } catch (failure) {
    $('author-status').textContent = failure.message
  } finally {
    $('save').disabled = false
    authorPicker.disabled = false
  }
})

for (const button of document.querySelectorAll('[data-pane]')) {
  button.addEventListener('click', guarded(async () => {
    for (const tab of document.querySelectorAll('[data-pane]')) {
      tab.setAttribute('aria-pressed', String(tab === button))
      $(tab.dataset.pane).hidden = tab !== button
    }
    if (button.dataset.pane === 'author' && !cases.children.length) await authorCatalog()
  }))
}
exercisePicker.addEventListener('change', guarded(openExercise))
authorPicker.addEventListener('change', guarded(openAuthor))
language.addEventListener('change', () => setSource(currentExercise))
$('try').addEventListener('click', () => run(true))
$('submit').addEventListener('click', () => run(false))
$('refresh').addEventListener('click', guarded(refreshHistory))
$('add-case').addEventListener('click', () => addCase())
guarded(() => catalog())()
