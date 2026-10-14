"use strict";
const assert = require("node:assert/strict");
const fs = require("node:fs");
const path = require("node:path");
const vm = require("node:vm");

class Element {
  constructor() { this.value = ""; this._text = ""; this.children = []; this.listeners = {}; this.dataset = {}; }
  set textContent(value) { this._text = value; this.children = []; }
  get textContent() { return this._text + this.children.map(child => child.textContent || "").join(" "); }
  replaceChildren(...children) { this.children = children; this._text = ""; }
  append(...children) { this.children.push(...children); }
  addEventListener(name, callback) { this.listeners[name] = callback; }
  scrollIntoView() {}
}
const flush = () => new Promise(resolve => setImmediate(resolve));
const response = (status, value) => ({ status, ok: status < 400, json: async () => value });
const auth = username => ({ accessToken: "fake-" + username, user: { username } });

async function setup() {
  const elements = new Map();
  const pending = new Map();
  const html = fs.readFileSync(path.join(__dirname, "../src/main/resources/static/index.html"), "utf8");
  for (const match of html.matchAll(/\bid="([^"]+)"/g)) elements.set(match[1], new Element());
  const context = vm.createContext({
    document: {
      getElementById(id) { if (!elements.has(id)) elements.set(id, new Element()); return elements.get(id); },
      createElement() { return new Element(); }
    },
    fetch: (url) => pending.has(url) ? pending.get(url).promise : Promise.resolve(response(200, [])),
    console
  });
  vm.runInContext(fs.readFileSync(path.join(__dirname, "../src/main/resources/static/app.js"), "utf8"), context);
  await flush();
  return {
    context, elements,
    setAccount(name) { context.nextAuth = name ? auth(name) : null; vm.runInContext("setAccount(nextAuth)", context); },
    delay(url) {
      let resolve;
      const promise = new Promise(callback => { resolve = callback; });
      pending.set(url, { promise });
      return value => { pending.delete(url); resolve(value); };
    }
  };
}

function putPrivateUi(test) {
  vm.runInContext('currentQuiz = {id: 99, title: "ALICE_PRIVATE"}', test.context);
  for (const id of ["feed", "questions", "quiz-title", "quiz-description", "result", "personal-output", "status"]) {
    test.elements.get(id).textContent = "ALICE_PRIVATE";
  }
  test.elements.get("definition").value = '{"title":"ALICE_PRIVATE"}';
  test.elements.get("draft-id").value = "99";
  test.elements.get("play-panel").hidden = false;
}

function assertPrivateCleared(test) {
  assert.equal(vm.runInContext("currentQuiz", test.context), null);
  assert.equal(test.elements.get("play-panel").hidden, true);
  assert.equal(test.elements.get("draft-id").value, "");
  assert.equal(test.elements.get("submit-attempt").disabled, true);
  for (const id of ["feed", "questions", "quiz-title", "quiz-description", "result", "personal-output", "status"]) {
    assert.ok(!test.elements.get(id).textContent.includes("ALICE_PRIVATE"), "Private DOM retained in " + id);
  }
  assert.ok(!test.elements.get("definition").value.includes("ALICE_PRIVATE"), "Private definition retained");
}

async function run() {
  let passed = 0;
  for (const next of [null, "bob"]) {
    const test = await setup();
    test.setAccount("alice");
    await flush();
    putPrivateUi(test);
    test.setAccount(next);
    await flush();
    assertPrivateCleared(test);
    passed++;
  }

  const mine = await setup();
  mine.setAccount("alice");
  await flush();
  const releaseMine = mine.delay("/api/quizzes/mine");
  const mineRequest = mine.elements.get("mine").listeners.click();
  mine.setAccount("bob");
  releaseMine(response(200, [{ id: 99, title: "ALICE_PRIVATE", description: "", type: "EDUCATIONAL",
    questionCount: 1, author: "alice", published: false }]));
  await mineRequest;
  await flush();
  assert.ok(!mine.elements.get("feed").textContent.includes("ALICE_PRIVATE"), "A stale private feed was rendered");
  assert.ok(mine.elements.get("account").textContent.includes("bob"));
  passed++;

  const denied = await setup();
  denied.setAccount("alice");
  await flush();
  const releaseDenied = denied.delay("/api/me/statistics");
  const deniedRequest = denied.elements.get("statistics").listeners.click();
  denied.setAccount("bob");
  releaseDenied(response(401, { detail: "Expired Alice token" }));
  await deniedRequest;
  await flush();
  assert.ok(denied.elements.get("account").textContent.includes("bob"), "Stale 401 logged Bob out");
  assert.equal(vm.runInContext("token", denied.context), "fake-bob");
  passed++;

  const quiz = await setup();
  quiz.setAccount("alice");
  await flush();
  const releaseQuiz = quiz.delay("/api/quizzes/99");
  const quizRequest = vm.runInContext('action(() => openQuiz(99))', quiz.context);
  quiz.setAccount("bob");
  releaseQuiz(response(200, { id: 99, title: "ALICE_PRIVATE", description: "", published: false, questions: [] }));
  await quizRequest;
  await flush();
  assertPrivateCleared(quiz);
  passed++;
  console.log("WhichOne UI auth regression: " + passed + "/5 PASS");
}
run().catch(error => { console.error(error.message); process.exitCode = 1; });
