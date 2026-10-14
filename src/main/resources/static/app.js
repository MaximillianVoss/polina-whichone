"use strict";

// The demo keeps a token in memory only. No passwords or tokens go to localStorage.
let token = "";
let currentQuiz = null;
let authEpoch = 0;
class StaleResponseError extends Error { }
function checkEpoch(epoch) {
  if (epoch !== authEpoch) throw new StaleResponseError();
}
const byId = id => document.getElementById(id);
const protectedButtons = ["seed", "statistics", "history", "mine", "create-draft",
  "update-draft", "publish-draft", "delete-draft", "submit-attempt"];

function showStatus(message, error = false) {
  byId("status").textContent = message;
  byId("status").dataset.error = String(error);
}

function setAccount(auth = null) {
  authEpoch++;
  token = auth ? auth.accessToken : "";
  currentQuiz = null;
  byId("feed").replaceChildren();
  byId("questions").replaceChildren();
  byId("play-panel").hidden = true;
  byId("quiz-title").textContent = "";
  byId("quiz-description").textContent = "";
  byId("result").textContent = "";
  byId("draft-id").value = "";
  fillEditor(educationalTemplate());
  byId("personal-output").textContent = "Запроси результаты текущего аккаунта.";
  showStatus(auth ? "Аккаунт переключён. Загружается общая лента." : "Сеанс страницы завершён.");
  byId("password").value = "";
  byId("username").value = auth ? auth.user.username : "";
  byId("account").textContent = auth ? "Выполнен вход: " + auth.user.username : "Вход ещё не выполнен.";
  byId("logout").hidden = !auth;
  protectedButtons.forEach(id => { byId(id).disabled = !auth; });
  byId("submit-attempt").disabled = !auth || !currentQuiz || !currentQuiz.published;
  if (!auth) {
    byId("password").value = "";
    byId("personal-output").textContent = "Войди, чтобы увидеть свои результаты.";
    byId("result").textContent = "";
  }
  // Clear private DOM immediately, then fetch only the current account's public feed.
  action(refreshFeed);
}

async function api(path, method = "GET", body = undefined, epoch = authEpoch) {
  checkEpoch(epoch);
  const headers = {};
  if (token) headers.Authorization = "Bearer " + token;
  if (body !== undefined) headers["Content-Type"] = "application/json";
  const response = await fetch(path, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  const data = response.status === 204 ? null : await response.json();
  // A late Alice response must not render data or log Bob out after an account switch.
  checkEpoch(epoch);
  if (!response.ok) {
    if (response.status === 401 && token) {
      setAccount(null);
      showStatus("Сессия завершилась. Войди снова.", true);
    }
    const fields = data.errors ? " " + JSON.stringify(data.errors) : "";
    throw new Error("HTTP " + response.status + ": " + (data.detail || "Запрос отклонён.") + fields);
  }
  return data;
}

async function action(work) {
  const epoch = authEpoch;
  try { await work(epoch); } catch (error) {
    if (!(error instanceof StaleResponseError) && epoch === authEpoch) showStatus(error.message, true);
  }
}

const educationalTemplate = () => ({
  title: "Основы Java",
  description: "Два вопроса. Максимум — 5 баллов.",
  type: "EDUCATIONAL", outcomes: [],
  questions: [
    { text: "Чему равно 2 + 2?", points: 3, options: [
      { text: "4", correct: true, weight: 0 }, { text: "5", correct: false, weight: 0 }
    ] },
    { text: "Какое слово объявляет класс?", points: 2, options: [
      { text: "class", correct: true, weight: 0 }, { text: "table", correct: false, weight: 0 }
    ] }
  ]
});

const personalityTemplate = () => ({
  title: "Как ты учишься?",
  description: "Учебный пример с двумя исходами. При равенстве побеждает первый объявленный исход.",
  type: "PERSONALITY",
  outcomes: [
    { code: "PLANNER", label: "Планировщик", description: "Тебе нравится сначала составить план." },
    { code: "EXPLORER", label: "Исследователь", description: "Тебе нравится исследовать через практику." }
  ],
  questions: [
    { text: "С чего ты начинаешь новую тему?", points: 0, options: [
      { text: "Составляю план", correct: false, outcomeCode: "PLANNER", weight: 2 },
      { text: "Пробую пример", correct: false, outcomeCode: "EXPLORER", weight: 2 }
    ] },
    { text: "Как тебе удобнее закреплять знания?", points: 0, options: [
      { text: "По расписанию", correct: false, outcomeCode: "PLANNER", weight: 2 },
      { text: "Через эксперименты", correct: false, outcomeCode: "EXPLORER", weight: 2 }
    ] }
  ]
});

function fillEditor(definition) { byId("definition").value = JSON.stringify(definition, null, 2); }

function element(tag, text, className) {
  const item = document.createElement(tag);
  if (text !== undefined) item.textContent = text;
  if (className) item.className = className;
  return item;
}

function quizCards(quizzes, target, isMine = false) {
  target.replaceChildren();
  if (!quizzes.length) { target.append(element("p", "Пока нет квизов. Создай свой или два готовых примера.")); }
  for (const quiz of quizzes) {
    const card = element("article", undefined, "quiz-card");
    card.append(element("p", quiz.type === "EDUCATIONAL" ? "Обучающий" : "Личностный", "tag"));
    card.append(element("h3", quiz.title));
    card.append(element("p", quiz.description));
    card.append(element("p", quiz.questionCount + " вопроса · автор " + quiz.author));
    const open = element("button", quiz.published ? "Открыть квиз" : "Посмотреть черновик");
    open.addEventListener("click", () => action(async epoch => {
      await openQuiz(quiz.id, epoch);
      checkEpoch(epoch);
      if (isMine && !quiz.published) {
        byId("draft-id").value = quiz.id;
        showStatus("Выбран черновик #" + quiz.id + ". Для изменения введи полный JSON определения.");
      }
    }));
    card.append(open);
    if (isMine) card.append(element("p", "ID " + quiz.id + " · " + (quiz.published ? "опубликован" : "черновик")));
    target.append(card);
  }
}

async function refreshFeed(epoch = authEpoch) {
  const quizzes = await api("/api/quizzes", "GET", undefined, epoch);
  checkEpoch(epoch);
  quizCards(quizzes, byId("feed"));
}

async function openQuiz(id, epoch = authEpoch) {
  const quiz = await api("/api/quizzes/" + id, "GET", undefined, epoch);
  checkEpoch(epoch);
  currentQuiz = quiz;
  byId("quiz-title").textContent = currentQuiz.title;
  byId("quiz-description").textContent = currentQuiz.description;
  byId("questions").replaceChildren();
  byId("result").textContent = "";
  for (const [index, question] of currentQuiz.questions.entries()) {
    const fieldset = element("fieldset");
    fieldset.append(element("legend", (index + 1) + ". " + question.text));
    for (const option of question.options) {
      const label = element("label", undefined, "option");
      const radio = element("input");
      radio.type = "radio";
      radio.name = "question-" + question.id;
      radio.value = String(option.id);
      radio.required = true;
      label.append(radio, element("span", option.text));
      fieldset.append(label);
    }
    byId("questions").append(fieldset);
  }
  byId("play-panel").hidden = false;
  byId("submit-attempt").disabled = !token || !currentQuiz.published;
  byId("play-panel").scrollIntoView({ behavior: "smooth", block: "start" });
}

async function authenticate(path, epoch = authEpoch) {
  const auth = await api(path, "POST", {
    username: byId("username").value, password: byId("password").value
  }, epoch);
  checkEpoch(epoch);
  setAccount(auth);
  byId("password").value = "";
  showStatus("Готово. Ты вошёл как " + auth.user.username + ".");
}

byId("auth-form").addEventListener("submit", event => {
  event.preventDefault();
  action(epoch => authenticate("/api/auth/login", epoch));
});
byId("register").addEventListener("click", () => action(epoch => authenticate("/api/auth/register", epoch)));
byId("logout").addEventListener("click", () => {
  setAccount(null);
  showStatus("Токен удалён из памяти страницы. Для нового запроса можно войти снова.");
});
byId("seed").addEventListener("click", () => action(async epoch => {
  byId("seed").disabled = true;
  try {
    for (const definition of [educationalTemplate(), personalityTemplate()]) {
      const quiz = await api("/api/quizzes", "POST", definition, epoch);
      await api("/api/quizzes/" + quiz.id + "/publish", "POST", undefined, epoch);
    }
    await refreshFeed(epoch);
    checkEpoch(epoch);
    showStatus("Два примера опубликованы. Выбери квиз и отправь ответы.");
  } finally { if (epoch === authEpoch) byId("seed").disabled = !token; }
}));
byId("refresh").addEventListener("click", () => action(refreshFeed));
byId("statistics").addEventListener("click", () => action(async epoch => {
  const result = await api("/api/me/statistics", "GET", undefined, epoch);
  checkEpoch(epoch);
  byId("personal-output").textContent = JSON.stringify(result, null, 2);
  showStatus("Получена статистика твоих сохранённых попыток.");
}));
byId("history").addEventListener("click", () => action(async epoch => {
  const result = await api("/api/me/attempts", "GET", undefined, epoch);
  checkEpoch(epoch);
  byId("personal-output").textContent = JSON.stringify(result, null, 2);
  showStatus("Получена история твоих попыток.");
}));
byId("attempt-form").addEventListener("submit", event => {
  event.preventDefault();
  action(async epoch => {
    const quiz = currentQuiz;
    if (!quiz) throw new Error("Сначала открой квиз текущего аккаунта.");
    const answers = quiz.questions.map(question => {
      const selected = document.querySelector('input[name="question-' + question.id + '"]:checked');
      if (!selected) throw new Error("Выбери один ответ на каждый вопрос.");
      return { questionId: question.id, optionId: Number(selected.value) };
    });
    const result = await api("/api/quizzes/" + quiz.id + "/attempts", "POST", { answers }, epoch);
    checkEpoch(epoch);
    byId("result").textContent = result.type === "EDUCATIONAL"
      ? "Твой результат: " + result.score + " из " + result.maxScore + " баллов."
      : result.outcomeLabel + ". " + result.outcomeDescription;
    showStatus("Попытка #" + result.id + " сохранена. Статистика обновится при запросе.");
  });
});
byId("educational-template").addEventListener("click", () => fillEditor(educationalTemplate()));
byId("personality-template").addEventListener("click", () => fillEditor(personalityTemplate()));
byId("mine").addEventListener("click", () => action(async epoch => {
  const quizzes = await api("/api/quizzes/mine", "GET", undefined, epoch);
  checkEpoch(epoch);
  quizCards(quizzes, byId("feed"), true);
  showStatus("Показаны твои черновики и опубликованные квизы.");
}));
byId("create-draft").addEventListener("click", () => action(async epoch => {
  const quiz = await api("/api/quizzes", "POST", JSON.parse(byId("definition").value), epoch);
  checkEpoch(epoch);
  byId("draft-id").value = quiz.id;
  showStatus("Создан черновик #" + quiz.id + ". Его можно изменить, затем опубликовать.");
}));
function draftId() {
  const id = Number(byId("draft-id").value);
  if (!Number.isSafeInteger(id) || id < 1) throw new Error("Укажи положительный ID своего черновика.");
  return id;
}
byId("update-draft").addEventListener("click", () => action(async epoch => {
  await api("/api/quizzes/" + draftId(), "PUT", JSON.parse(byId("definition").value), epoch);
  checkEpoch(epoch);
  showStatus("Черновик заменён. ID вопросов и вариантов обновились.");
}));
byId("publish-draft").addEventListener("click", () => action(async epoch => {
  await api("/api/quizzes/" + draftId() + "/publish", "POST", undefined, epoch);
  await refreshFeed(epoch);
  checkEpoch(epoch);
  showStatus("Квиз опубликован окончательно. Для изменений создай новый квиз.");
}));
byId("delete-draft").addEventListener("click", () => action(async epoch => {
  await api("/api/quizzes/" + draftId(), "DELETE", undefined, epoch);
  checkEpoch(epoch);
  byId("draft-id").value = "";
  await refreshFeed(epoch);
  checkEpoch(epoch);
  showStatus("Твой черновик удалён.");
}));

fillEditor(educationalTemplate());
action(refreshFeed);
