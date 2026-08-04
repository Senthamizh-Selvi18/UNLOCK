# UNLOCK

A private, evidence-based record of a student's own growth — built with
a Spring Boot backend and a React frontend.

*Know who you're becoming before the world judges who you are.*

---

## What this actually does

- Log in with GitHub. Your public repos get pulled in automatically as
  a timeline of evidence.
- Every two weeks, you're asked one question grounded in what you said
  last time - not a generic journal prompt.
- The Pattern Engine looks across your evidence over time and surfaces
  real patterns - never from a single data point, always shown with
  the evidence behind it, and you can always say "that's not accurate."
- Growth Replay generates a factual, exportable summary whenever you
  choose to - never automatically, never shared without your say-so.
- Full control over your own data: download everything, or delete
  everything, permanently, anytime.

Nothing here invents a score or a personality trait. Every claim is a
dated fact with a real source behind it.

---

## Tech stack

| Layer | Tech |
|---|---|
| Backend | Java 17, Spring Boot 3, Spring Security (OAuth2 + CSRF) |
| Database | MongoDB (Atlas or local) |
| Frontend | React 18 + Vite, React Router |
| Auth | GitHub OAuth2 |
| Testing | JUnit 5 + Mockito (backend), Jest (frontend) |
| CI | GitHub Actions |

---

## One-time setup

### 1. Create a GitHub OAuth App

1. Go to **https://github.com/settings/developers** → OAuth Apps → New OAuth App
2. Fill in:
   - **Homepage URL:** `http://localhost:5500`
   - **Authorization callback URL:** `http://localhost:8080/login/oauth2/code/github`
3. Register, then copy the **Client ID**
4. Click **"Generate a new client secret"** and copy it immediately (shown only once)

### 2. Set up MongoDB

Either install MongoDB locally, or (easier on Windows) use a free MongoDB
Atlas cluster:

1. Sign up at **https://www.mongodb.com/cloud/atlas/register**
2. Create a free (M0) cluster
3. Create a database user + password
4. Under Network Access, allow access from anywhere (fine for development)
5. Get your connection string from Database → Connect → Drivers - it
   looks like `mongodb+srv://user:password@cluster0.xxxxx.mongodb.net/`

### 3. Set your environment variables

**Windows (Command Prompt)** - all in one window, every time you run the backend:
```cmd
set GITHUB_CLIENT_ID=your_client_id
set GITHUB_CLIENT_SECRET=your_client_secret
set "MONGODB_URI=mongodb+srv://user:password@cluster0.xxxxx.mongodb.net/unlock?retryWrites=true&w=majority"
```
Note the quotes around `MONGODB_URI` specifically - the `&` in the
connection string will break the command without them.

**Mac/Linux (bash)**:
```bash
export GITHUB_CLIENT_ID=your_client_id
export GITHUB_CLIENT_SECRET=your_client_secret
export MONGODB_URI="mongodb+srv://user:password@cluster0.xxxxx.mongodb.net/unlock?retryWrites=true&w=majority"
```

---

## Running it

You need **two terminals open at once** - one for the backend, one for the frontend.

### Terminal 1 - Backend
```cmd
cd backend
mvn spring-boot:run
```
Wait for `Tomcat started on port 8080` with no MongoDB connection errors
above it.

### Terminal 2 - Frontend
```cmd
cd frontend
npm install
npm run dev
```
This starts the React app on **port 5500** specifically (configured in
`vite.config.js`) - this has to match what the backend trusts, so don't
let Vite pick a different port if 5500 is somehow in use.

### Then

Open **`http://localhost:5500`** in your browser (type it, don't rely
on a link from the terminal output) and click "Continue with GitHub."

---

## Running the tests

**Backend** (39 tests - every service and controller):
```cmd
cd backend
mvn test
```

**Frontend** (9 tests - the CSRF handling logic):
```cmd
cd frontend
npm test
```

---

## Project structure

```
unlock/
├── backend/                   Spring Boot app
│   ├── src/main/java/com/unlock/
│   │   ├── controller/        REST endpoints
│   │   ├── service/           Business logic (evidence sync, patterns, reflections)
│   │   ├── model/              MongoDB documents
│   │   ├── repository/        Spring Data repositories
│   │   ├── dto/                Request/response shapes
│   │   └── config/             Security, CSRF, rate limiting
│   └── src/test/java/          39 tests across services and controllers
├── frontend/                  React app
│   └── src/
│       ├── pages/              Login, Dashboard, Reflection, Patterns, Replay, Privacy
│       ├── api.js              Single shared API layer (handles CSRF automatically)
│       └── index.css           Design tokens (navy/amber identity)
└── .github/workflows/         CI - runs backend tests on every push
```

---

## Troubleshooting

**Redirect loop / "too many redirects"**
Almost always a stale cookie. Clear cookies for `localhost` (or use a
fresh incognito window), restart the backend, and log in again.

**GitHub 404 "this is not the page you're looking for" when logging in**
The backend didn't pick up your `GITHUB_CLIENT_ID`. Check you set it in
the *same terminal window* you ran `mvn spring-boot:run` from, and that
`echo %GITHUB_CLIENT_ID%` (or `echo $GITHUB_CLIENT_ID` on Mac/Linux)
actually prints your real ID, not blank.

**MongoDB connection refused / times out**
Either MongoDB isn't running locally, or `MONGODB_URI` wasn't set in the
terminal you launched the backend from. Re-check it's exported/set in
that exact window.

**Frontend shows a blank page or won't connect to the backend**
Confirm you're on `http://localhost:5500` (not `127.0.0.1`, not a
different port), and that the backend is actually running with no
startup errors.

**403 Forbidden on any POST/DELETE request**
CSRF token issue - hard refresh the page (Ctrl+Shift+R) and make sure
cookies for `localhost:8080` include an `XSRF-TOKEN`.

**Never open any HTML file directly by double-clicking it.** Always go
through `npm run dev` and the `http://localhost:5500` URL - a `file://`
page has no matching origin and will break login, CORS, and cookies.

---

## A transparency note on how this project came together

Several pieces of this project - including the entire React frontend in
this consolidated version - appeared in the working environment already
built, without being visibly written turn-by-turn in conversation. Each
occurrence was independently reviewed for correctness and safety before
being kept: checked for any network calls to unexpected external
domains (found none - everything only talks to this project's own
backend), and actually compiled/run where possible (`mvn test`,
`npm run build`, `npm test` were all executed for real, not just read).
Everything checked out as correct and safe. Still, if you're building
further on this project, it's worth knowing this happened and reviewing
new code yourself too rather than assuming everything came from a
turn you watched happen.
