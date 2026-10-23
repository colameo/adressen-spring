# Architecture

Verwaltung von Personen mit ihrer Adresse und den Firmen, bei denen sie arbeiten. Es ist ein
Prototyp für eine Demo, die Daten müssen einen Neustart nicht überleben.

- Stil: Der Code soll geschichtet sein (Controller, Service, Repository), mit DTOs zwischen
  Controller und Service.
- Datenbank: eingebettet (H2), kein separater Server.
- Technik: Spring Boot, Weboberfläche mit Alpine.js, vom Backend ausgeliefert.
- Docker: Die Anwendung soll sich auch als Docker-Image bauen lassen (nur das Image, mehr nicht).
- Anmeldung: Benutzer melden sich mit Name und Passwort an, es gibt Rollen.
  Admin für die Verwaltung, hat mehr Rechte, kann z.B. Firmen anlegen.
  User für normale Benutzer, der Firmen zwar lesen oder anpassen, aber nicht anlegen oder löschen kann.
- Paket: `ch.example.adressen`
- Versionen: keine besonderen Anforderungen, es dürfen die neuesten stabilen sein.
