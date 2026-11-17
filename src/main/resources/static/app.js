"use strict";

// The demo keeps a token in memory only. No passwords or tokens go to localStorage.
let token = "";
let username = "";
let currentQuiz = null;
let authEpoch = 0;
let showingMine = false;
let feedRequest = 0;
let feedBusy = false;
let ownPublished = false;
let cardDeleteButtons = [];
class StaleResponseError extends Error { }
function checkEpoch(epoch) {
  if (epoch !== authEpoch) throw new StaleResponseError();
}
const byId = id => document.getElementById(id);
const protectedButtons = ["statistics", "history", "mine", "create-draft",
  "update-draft", "publish-draft", "delete-draft", "submit-attempt"];

function showStatus(message, error = false) {
  byId("status").textContent = message;
  byId("status").dataset.error = String(error);
}

function showFeedStatus(message, error = false) {
  byId("feed-status").textContent = message;
  byId("feed-status").dataset.error = String(error);
}

function updateFeedButtons() {
  byId("seed").disabled = !token || feedBusy;
  byId("delete-all").disabled = !token || !ownPublished || feedBusy;
  cardDeleteButtons.forEach(button => { button.disabled = feedBusy; });
}

function closeQuiz() {
  currentQuiz = null;
  byId("questions").replaceChildren();
  byId("play-panel").hidden = true;
  byId("quiz-title").textContent = "";
  byId("quiz-description").textContent = "";
  byId("result").textContent = "";
  byId("submit-attempt").disabled = true;
}

function setAccount(auth = null) {
  authEpoch++;
  token = auth ? auth.accessToken : "";
  username = auth ? auth.user.username : "";
  showingMine = false;
  feedBusy = false;
  ownPublished = false;
  cardDeleteButtons = [];
  byId("feed").replaceChildren();
  closeQuiz();
  showFeedStatus("");
  byId("feed-note").textContent = auth
    ? "Удаление из ленты сохраняет историю прохождений."
    : "Войди, чтобы добавить примеры или удалить свои квизы из ленты.";
  updateFeedButtons();
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

async function action(work, reportError = showStatus) {
  const epoch = authEpoch;
  try { await work(epoch); } catch (error) {
    if (!(error instanceof StaleResponseError) && epoch === authEpoch) reportError(error.message, true);
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
  cardDeleteButtons = [];
  ownPublished = quizzes.some(quiz => quiz.published && quiz.author === username);
  if (!quizzes.length) { target.append(element("p", "Пока нет квизов. Создай свой или добавь тестовые данные.")); }
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
    const actions = element("div", undefined, "card-actions");
    actions.append(open);
    if (username && quiz.author === username) {
      const remove = element("button", "Удалить", "danger");
      remove.type = "button";
      remove.setAttribute("aria-label", "Удалить квиз «" + quiz.title + "»");
      remove.addEventListener("click", () => {
        const message = "Удалить квиз «" + quiz.title + "»?" +
          (quiz.published ? " Сохранённые попытки останутся в истории." : "");
        if (window.confirm(message)) manageFeed(epoch => deleteQuiz(quiz, epoch));
      });
      cardDeleteButtons.push(remove);
      actions.append(remove);
    }
    card.append(actions);
    if (isMine) card.append(element("p", "ID " + quiz.id + " · " + (quiz.published ? "опубликован" : "черновик")));
    target.append(card);
  }
  updateFeedButtons();
}

async function refreshFeed(epoch = authEpoch, mine = showingMine) {
  const request = ++feedRequest;
  const quizzes = await api(mine ? "/api/quizzes/mine" : "/api/quizzes", "GET", undefined, epoch);
  checkEpoch(epoch);
  if (request !== feedRequest) return false;
  showingMine = mine;
  quizCards(quizzes, byId("feed"), mine);
  return true;
}

function manageFeed(work) {
  if (!token || feedBusy) return;
  return action(async epoch => {
    feedBusy = true;
    updateFeedButtons();
    showFeedStatus("");
    try { await work(epoch); } finally {
      if (epoch === authEpoch) {
        feedBusy = false;
        updateFeedButtons();
      }
    }
  }, showFeedStatus);
}

async function deleteQuiz(quiz, epoch) {
  const path = "/api/quizzes/" + quiz.id;
  await api(quiz.published ? path + "/archive" : path, quiz.published ? "POST" : "DELETE", undefined, epoch);
  if (currentQuiz?.id === quiz.id) closeQuiz();
  if (byId("draft-id").value === String(quiz.id)) byId("draft-id").value = "";
  await refreshFeed(epoch);
  showFeedStatus(quiz.published ? "Квиз удалён из ленты." : "Черновик удалён.");
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
byId("seed").addEventListener("click", () => manageFeed(async epoch => {
  const { count } = await api("/api/quizzes/demo", "POST", undefined, epoch);
  await refreshFeed(epoch);
  showFeedStatus(count ? "Добавлено тестовых квизов: " + count + "." : "Оба тестовых квиза уже в ленте.");
}));
byId("delete-all").addEventListener("click", () => {
  if (!window.confirm("Удалить из ленты все твои опубликованные квизы? Черновики и история прохождений сохранятся.")) return;
  manageFeed(async epoch => {
    const { count } = await api("/api/quizzes/mine/archive", "POST", undefined, epoch);
    if (currentQuiz?.published && currentQuiz.author === username) closeQuiz();
    await refreshFeed(epoch);
    showFeedStatus(count ? "Удалено из ленты: " + count + "." : "У тебя нет опубликованных квизов.");
  });
});
byId("refresh").addEventListener("click", () => action(epoch => refreshFeed(epoch, false), showFeedStatus));
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
  if (await refreshFeed(epoch, true)) showStatus("Показаны твои черновики и опубликованные квизы.");
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
