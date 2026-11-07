# SmartResolve web console

The React interface for creating and managing SmartResolve support tickets.

## Local development

Run the backend from the repository root, then start the web console:

```powershell
npm install
npm run dev
```

The development server runs on http://localhost:5173 and forwards `/api` requests to the Spring Boot service on port 8080.

## Checks

```powershell
npm run lint
npm run build
```
