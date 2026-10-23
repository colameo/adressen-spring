# Pisa — Tutorial: von der Beschreibung zur laufenden Anwendung

Du baust eine kleine **Adressverwaltung** (Personen mit Adresse und Firma) von Grund auf. Du schreibst nur
Prosa; ein LLM schlägt Architektur und Domänenmodell vor, Pisa prüft die Vorschläge und erzeugt daraus ein
lauffähiges Spring-Boot-Projekt. Nach jedem Schritt steht, was du sehen solltest.

> Stand: `pisa 0.13.0`. Befehle im Detail: [User Guide](pisa-user-guide.md). Dauer: etwa eine Stunde, der
> erste Maven-Lauf lädt Abhängigkeiten.

Die Schritte 2 und 4 rufen ein LLM auf (zwei kurze Aufrufe); alle anderen Schritte laufen ohne. Ohne LLM schreibst du die
beiden Dateien selbst, so wie sie in den Schritten gezeigt sind.
Am Ende steht ein **optionaler Schritt** (10, CI mit GitHub Actions); `pisa play --fast` lässt ihn aus.

## 0. Voraussetzungen

| Was | Prüfen |
|---|---|
| Pisa 0.13.0 | `pisa --version` zeigt `pisa 0.13.0`. Sonst installieren: `AISE/projects/install-pisa.sh --dir <Ordner, der im PATH liegt>` (braucht `gh auth login`) |
| Java 25 und Maven | `java -version`, `mvn -v` |
| LLM-Zugang für `pi` | `pi` ist mit einem Modell eingerichtet (siehe Pisa-README, «Model access»); nur für Schritt 2 und 4 nötig |
| Optional: Pisa-Plugin | IntelliJ oder VS Code färbt `.dml`/`.adl` und zeigt Fehler beim Schreiben (User Guide, «Editor und PlantUML einrichten») |

## 1. Ordner anlegen und Pisa vorbereiten

```bash
mkdir adressen-spring && cd adressen-spring
pisa init
```

Du solltest sehen: `Prepared .: doc/ (the user guide and both tutorials) and spec/.` und den Hinweis, welches Tutorial du lesen kannst.
`pisa init` ohne Angaben legt noch **kein Projekt** an, nur das, was für jede Art von Anwendung gleich ist: den Ordner **`doc/`** mit dem User Guide und beiden
Tutorials (Spring Boot und Hono) und einen leeren Ordner `spec/`. Welche Art von Anwendung du willst (Java mit Spring Boot oder TypeScript mit Hono), entscheidest du
jetzt anhand der Tutorials und der Beschreibung in Schritt 2; ein Projekt entsteht erst in Schritt 3. Alle weiteren Befehle laufen in diesem Ordner.

## 2. Architektur beschreiben und vorschlagen lassen

<!-- play: llm -->
```bash
pisa propose-architecture --out .
```

Du solltest sehen: `Created spec/architecture.md with a starting text - describe the architecture there, …`.
Öffne `spec/architecture.md`, **lösche die Zeile `<!-- pisa:starting-text -->`** und ersetze den Text durch
etwa Folgendes:

<!-- play: write spec/architecture.md -->
```markdown
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
```

Dann nochmals:

<!-- play: llm -->
```bash
pisa propose-architecture --out .
```

Du solltest sehen: den Streaming-Text des Modells, am Ende `Architecture proposal complete:
spec/architecture.adl` und die Zeile `Create the project from it: pisa init --target spring-boot --out . --adl …`.

Öffne `spec/architecture.adl` und prüfe es:

<!-- play: write spec/architecture.adl unless-llm -->
```
architecture AdressenSpring {
    container db : database { engine h2 }
    container api : backend {
        package ch.example.adressen
        authentication session
        roles {
            ADMIN
            USER {
                Company: read, update
            }
        }
        uses db
        components layered
    }
    container web : frontend { technology alpine  servedBy api }
    docker { }
}
```

(Die Datei ist auf mehrere Zeilen formatiert; Kommentare am Anfang nennen, was das Modell nicht abdecken konnte.)
Stimmt etwas nicht, ändere die Datei von Hand: sie gehört ab jetzt dir.

Die Rollen aus der Beschreibung stehen im Block `roles` der `.adl`: `ADMIN` ohne Block darf alles, `USER` darf Firmen nur lesen und ändern (`Company: read, update`), alles andere weiter. Daraus schreibt `init` die `security.yml` (Schritt 7b), und `generate` erzeugt die Tests pro Rolle. **Fehlt der Block** in deiner Datei (ein Modell macht nicht immer, was die Beschreibung sagt), schreibe ihn von Hand in den `api`-Container.

Der leere Block `docker { }` (aus dem Satz «Docker» in deiner Beschreibung) lässt `init` ein `Dockerfile` und eine `.dockerignore` schreiben, mehr nicht. Du musst sie nicht benutzen; wie du sie ausprobierst, steht im optionalen Schritt 11. **Fehlt der Block** in deiner Datei, schreibe `docker { }` von Hand vor die letzte Klammer, oder lass ihn weg, wenn du kein Image brauchst.

Die Zeile `authentication session` (Anmeldung mit Benutzern und Rollen, Schritt 7b) kommt aus dem Satz «Anmeldung» in deiner Beschreibung (ab Pisa 0.10.0). **Fehlt sie** in deiner Datei (ältere Versionen schlagen keine Anmeldung vor; auch ein Modell macht nicht immer, was die Beschreibung sagt), schreibe sie von Hand in den `api`-Container, zum Beispiel vor `uses db`.

**Wenn es nicht klappt:** «ended without an accepted architecture» heisst, das Modell hat dreimal ungültig
geantwortet oder gar nicht über das Werkzeug; Beschreibung genauer formulieren und wiederholen. Mit einer
vorhandenen `.adl` tut der Befehl nichts, ausser mit `--force`.

## 3. Das Projekt anlegen

```bash
pisa init --target spring-boot --out . --group ch.example --artifact adressen-spring --adl spec/architecture.adl
```

Du solltest sehen: `Architecture from spec/architecture.adl: style layered, database h2, package
ch.example.adressen.` und die Liste der Dateien (`pom.xml`, `AdressenSpringApplication.java`, `application.yml`, …,
`.pisa/architecture.plan.json`, `Dockerfile`, `.dockerignore`). Neu sind auch `spec/README.md` (Anleitung zu den Specs) und `README.md`
mit dem Rezept des Projekts. `doc/` aus Schritt 1 bleibt, wie es ist.

## 4. Das Domänenmodell beschreiben und vorschlagen lassen

<!-- play: llm -->
```bash
pisa propose-model --out .
```

Du solltest sehen: `Created spec/domain-spec.md with a starting text - describe the domain model there, …`.
Lösche in `spec/domain-spec.md` wieder die Markerzeile und schreibe:

<!-- play: write spec/domain-spec.md -->
```markdown
# Domain model

Eine einfache Adressverwaltung für ein kleines Büro. Sie speichert Personen mit
ihrer Adresse und den Firmen, bei denen sie arbeiten.

- **Person:** Vorname und Nachname sind Pflicht (je höchstens 80 Zeichen). Die
  E-Mail-Adresse ist optional und darf nur einmal vorkommen. Telefon und
  Geburtsdatum sind optional. Jede Person hat einen Status: aktiv, gesperrt
  (Anzeige «Gesperrt») oder archiviert.
- **Adresse:** Strasse und Ort sind Pflicht, die Postleitzahl besteht aus genau
  4 Ziffern. Die Adresse gehört fest zu einer Person (ein Bestandteil), sie hat
  keine eigene Seite und wird mit der Person gespeichert und gelöscht.
- **Firma:** Der Name ist Pflicht und eindeutig. Eine Firma hat viele Personen,
  eine Person arbeitet bei höchstens einer Firma.

Nicht gewünscht: Verlauf der Adressen, Zugriffsrechte.
```

<!-- play: llm -->
```bash
pisa propose-model --out .
```

Du solltest sehen: `Model proposal complete: spec/adressen-spring.dml` (der Dateiname ist der Projektname) und die
Zeile `Check it, then: pisa generate --file spec/adressen-spring.dml --out .`.

**Prüfe `spec/adressen-spring.dml` genau**, das ist der wichtigste Schritt: Sind alle drei Entitäten da? Hat `Person`
einen `status` als `enum`, `Address` die Regel für die Postleitzahl (`~ "[0-9]{4}"`), `address` bei der Person als
`composed`, `employees` bei der Firma als `mappedBy company`? Wünsche, die die Sprache nicht ausdrückt (hier:
Verlauf, Rechte), stehen als `// Not covered: …` über der ersten Zeile. Das Modell ist Vorschlag, nicht Wahrheit:
ändere von Hand, was nicht stimmt; das Plugin zeigt Fehler beim Schreiben. Zum Vergleich, so sieht ein passendes Modell aus:

<!-- play: write spec/adressen-spring.dml unless-llm -->
```
enum PersonStatus { ACTIVE ("Aktiv"), BLOCKED ("Gesperrt"), ARCHIVED ("Archiviert") }

type Plz = String ~ "[0-9]{4}"

entity Person {
    @id id: Long
    firstName: String(1..80)
    lastName: String(1..80)
    email: Email? unique
    phone: Phone?
    birthday: Date?
    status: PersonStatus
    oneToOne address: Address composed
    manyToOne company: Company
}

entity Address {
    @id id: Long
    street: String
    zip: Plz
    city: String
}

entity Company {
    @id id: Long
    name: String unique
    oneToMany employees: Person mappedBy company
}
```

## 5. Formulare und Use Cases ableiten (ohne LLM)

```bash
pisa propose-specs --out .
```

Du solltest sehen: `Wrote spec/person.ui.md`, `Wrote spec/company.ui.md`, `Wrote spec/use-cases.uc.md`.
Die Formulare (`.ui.md`, mit einem Salt-Mockup als Layout) und die Use Cases (je ein Browser-Test) kannst du
später anpassen; bestehende Dateien überschreibt der Befehl nie. Bei einer Anmeldung mit Rollen schreibt er als **Akteur** jedes Use Case die Rolle mit den
wenigsten Rechten, die das darf (Firmen anlegen und löschen: `ADMIN`; Firmen ändern und alles bei Personen: `USER`), und `generate` lässt den Browser-Test als diese Rolle laufen.

## 6. Code erzeugen

```bash
pisa generate --file spec/adressen-spring.dml --out .
```

Du solltest sehen: die Liste der erzeugten Dateien (Entitäten, Repositories, Services, DTOs, Controller, Seiten,
Tests) und am Ende `Recorded step N in .pisa/recipe.json and README.md.`

| Datei | Inhalt |
|---|---|
| `src/main/java/…/model` | die Entitäten (JPA) und das `enum` |
| `…/repository`, `…/service`, `…/dto`, `…/mapper`, `…/controller` | Datenzugriff, Service, DTO, Mapper und REST-Schnittstelle (nur für Entitäten mit eigener Seite: `Address` ist ein Bestandteil der Person und hat keine) |
| `…/config` | die Anmeldung (`SecurityStore`, `SecurityConfig`, `EntityAccess`) |
| `src/main/resources/static/*.html`, `app.js`, `app.css` | die Seiten (Alpine.js) |
| `src/test/java/…` | Repository-, Controller-, Service- und ArchUnit-Tests, Tests pro Rolle, Browser-Tests (Profil `ui`) |

## 7. Starten und ausprobieren

<!-- play: server /login.html -->
```bash
mvn spring-boot:run
```

Öffne http://localhost:8080/ **im Browser über den laufenden Server** (nicht die `.html`-Dateien aus `src/main/resources/static` direkt: Alpine.js und die Skripte kommen vom Server, in der Datei selbst sind Buttons und Texte leer): Menü mit Personen und Firmen. Die Überschrift der Startseite ist «AdressenSpring», der Name aus `architecture AdressenSpring {` in der `.adl`; er steht als `app.title` in `application.yml` (ändern, Anwendung neu starten). Die Anwendung leitet auf die Anmeldeseite: melde dich mit `admin` / `admin` an und wähle auf der Seite «Change password» ein neues Passwort (zweimal, mindestens 8 Zeichen); wer angemeldet ist, steht im Hamburger-Menü (☰) rechts oben. Rollen und weitere Benutzer: Schritt 7b. Lege eine Firma an, dann eine Person mit Adresse
und wähle die Firma. Ein Klick auf eine Zeile der Liste zeigt den Eintrag **schreibgeschützt** (Anzeigemodus); wer ändern darf, hat dort «Edit». Probiere die Regeln aus: eine Postleitzahl mit 3 Ziffern, zweimal dieselbe E-Mail, ein
leerer Nachname; das Feld wird rot und der Server meldet 400 mit der Meldung pro Feld. Die Datenbank siehst du
unter http://localhost:8080/h2-console (Abschnitt 4d des User Guides). Mit `Ctrl+C` beenden.

<!-- play: tests -->
```bash
mvn test            # Repository-, Controller-, Service-, ArchUnit-Tests
```

Und zusätzlich die Browser-Tests (sie laden beim ersten Mal Playwright, ca. 200 MB):

<!-- play: slow -->
```bash
mvn test -Pui
```

Du solltest sehen: `BUILD SUCCESS`. (In einem Probelauf dieses Tutorials waren es 60 Tests ohne Fehler, mit `-Pui` 82.)

## 7a. Optional: Operationen einschränken (`@operations`)

Jede Entität erlaubt standardmässig **alle vier Operationen** (anlegen, lesen, ändern, löschen). Wer etwas sperren will, schreibt es ins Modell, mit `@operations` vor `entity`. Beispiel: Firmen sollen angelegt, gelesen und geändert, aber **nicht gelöscht** werden. Ergänze in `spec/adressen-spring.dml`:

```
@operations(create, read, update)
entity Company {
    …
}
```

`read` muss immer dabei sein (Listen und Auswahlfelder brauchen es). Ohne die Zeile ist alles erlaubt; Pisa sperrt nie von sich aus etwas. Dann:

<!-- play: skip -->
```bash
pisa generate --file spec/adressen-spring.dml --out .
```

Du solltest sehen: einen **Abbruch**, es wird nichts geschrieben:

```
UC6 (spec/use-cases.uc.md): Company doesn't allow "delete" (@operations(create, read, update)) but the test line says "delete"
Nothing was written. Allow the operation in @operations of the entity, or change the Test: line, or delete the use case.
```

Das ist gewollt: der Use Case «Firma löschen» (UC6) verlangt etwas, das die Firma jetzt nicht mehr erlaubt, ein Widerspruch zwischen zwei Specs. Drei Auswege: `delete` wieder erlauben, die `Test:`-Zeile ändern, oder (hier) den Use Case streichen. Lösche in `spec/use-cases.uc.md` den Abschnitt `## UC6 …` und im PlantUML-Diagramm die beiden Zeilen mit `UC6`, dann nochmals `pisa generate`. Pisa löscht nie Dateien: der alte Browser-Test des Use Cases bleibt liegen und würde fehlschlagen, lösche ihn von Hand (`generate` nennt ihn unter `Info: browser tests of use cases that are no longer in spec/*.uc.md`): `src/test/java/ch/example/adressen/ui/UC6UiTest.java`.

Starte die Anwendung neu (`mvn spring-boot:run`) und prüfe:

- Die Seite «Companies» hat keinen Delete-Button mehr; Anlegen und Ändern gehen weiter.
- Die Schnittstelle sperrt das Löschen ebenfalls: `curl -i -X DELETE http://localhost:8080/api/companies/1` antwortet **405** (Method Not Allowed) mit einem `Allow`-Header, die Firma bleibt (`GET` gibt weiter 200). Personen lassen sich weiter löschen.
- `mvn test` prüft das für dich: der erzeugte Test `answers405ForADelete` erwartet 405, und der Test «löschen» der Firma entfällt.

Mehr dazu (Namen, Fehlermeldungen, Wirkung in beiden Zielen, Zusammenspiel mit Rollen): User Guide, Abschnitt «Domänenmodell (`.dml`)», `@operations`. Ändere die Zeile in `@operations(create, read, update, delete)` oder lösche sie, wenn du die Beschränkung wieder aufheben willst.

## 7b. Rechte und Benutzer

Die Anmeldung hast du mit der Zeile `authentication session` in der `.adl` schon eingeschaltet (Details: User Guide, Abschnitt 4g). `init` hat dafür `security.yml` (Rollen und Benutzer), `login.html` und `change-password.html` geschrieben, `generate` die Dateien der Anmeldung (`SecurityStore`, `SecurityConfig`, `EntityAccess`, `SecurityController`, `SecurityTest`). Die `.dml` und die Mockups kennen keine Rollen: sie stehen nicht im Domänenmodell. Schreibst du die Zeile erst später ein, braucht es danach `init … --force` und `generate`.

- Melde dich mit **`admin` / `admin`** an (falls noch nicht geschehen): Es kommt die Seite «Change password», auf der du das neue Passwort **zweimal** eingibst (mindestens 8 Zeichen, nicht der Benutzername). Danach darf `ADMIN` alles, was das Modell erlaubt: Firmen löschen auch er nicht, wenn du 7a gemacht hast (Rechte einer Rolle geschnitten mit `@operations`). Wer angemeldet ist, steht im Hamburger-Menü (☰) rechts oben, mit «Change password» und «Sign out».
- Melde dich ab und mit **`user` / `user`** an (wieder neues Passwort wählen): `USER` darf Personen ganz bearbeiten, Firmen nur lesen und ändern (Block `roles`, siehe unten). Ein Schreibzugriff über `curl` ohne Anmeldung antwortet **401**; angemeldet, aber ohne Recht, **403**.

**Rechte für dieses Beispiel:** Sie stehen schon in der `.adl` (Block `roles`, Schritt 2) und damit in `security.yml`: `USER` bearbeitet Personen (mit ihrer Adresse) ganz, Firmen darf er nur lesen und ändern (so steht es in der Beschreibung der Architektur). `init` hat die Datei aus dem Block geschrieben:

```yaml
  USER:
    "*": [create, read, update, delete]
    "Company": [read, update]            # die Liste einer Entität ersetzt "*"
```

Melde dich als `user` an: Die Seite «Companies» hat keine Buttons zum Anlegen und Löschen mehr (Ändern geht), und ein `curl`-`POST` oder `DELETE` auf `/api/companies` antwortet **403**. Personen kannst du weiter bearbeiten. Ein Klick auf eine Firma zeigt sie schreibgeschützt, mit «Edit» zum Ändern; wer nur lesen dürfte, hätte dort keinen «Edit».
Ändern kannst du die Rechte auf zwei Wegen: von Hand in `security.yml` (Anwendung neu starten, kein `generate`), oder dauerhaft im Block `roles` der `.adl` (dann die `security.yml` löschen, `init … --adl … --force` und `generate`).
Die Tests und die Use Cases folgen der `.adl`: `RolesTest` prüft für jede Rolle und jede Entität, was erlaubt ist (200 statt 403) und was nicht (403), und die Use Cases mit `**Akteur:** USER` oder `ADMIN` (Schritt 5) laufen im Browser als diese Rolle.

Zu den Passwörtern: Das Startpasswort eines Benutzers ist sein Name (`admin/admin`, `user/user`); in `security.yml` steht kein Passwort. Das neue Passwort steht nie im Klartext, der Hash landet in der Tabelle `usr_password` der Datenbank der Anwendung; die Anwendung ändert `security.yml` nie. Mit H2 im Speicher ist die Tabelle nach jedem Neustart leer, dann gilt wieder das Startpasswort und die erzwungene Änderung; mit PostgreSQL bleibt das Passwort.

Eigene Rollen und Benutzer: in `security.yml` eintragen (der Hash kommt von `pisa hash-password`, das Passwort fragt es zweimal ohne Echo ab) und die Anwendung neu starten; ein `generate` ist nicht nötig. `admin/admin` und `user/user` sind öffentlich bekannt, nur für die Entwicklung.

`mvn test` meldet sich in den erzeugten Tests als Testbenutzer an (stehen in `src/test/resources/security-test.yml`, nicht in deiner `security.yml`) und prüft zusätzlich 401, 403 und das Setzen des Passworts.

## 8. Eine Änderung durchziehen

Ändere die Beschreibung, nicht den Code. Zwei Wege:

- **Von Hand im Modell:** in `spec/adressen-spring.dml` ein Feld ergänzen, etwa `title: String(..40)?` bei `Person`.
  **Das Formular muss es auch kennen:** `spec/person.ui.md` ist ein Layout, in dem jedes Feld genau einmal vorkommt;
  ergänze im Mockup eine Zeile, etwa `Title | "title"` unter dem Nachnamen. Dann
  `pisa generate --file spec/adressen-spring.dml --out .` und `mvn test`. Der Code, die Seiten und die Tests folgen.
  Ohne die Mockup-Zeile bricht `generate` ab und schreibt nichts: `The Salt mockup in spec/person.ui.md can't be
  used: … missing "title"`. (Alternativ `spec/person.ui.md` löschen und `pisa propose-specs --out .` neu laufen
  lassen: das Standard-Mockup enthält alle Felder.)
- **Neu vorschlagen lassen:** `domain-spec.md` ändern und `pisa propose-model --out . --force` (überschreibt
  `spec/adressen-spring.dml`, also deine Handänderungen dort).

## 9. Reproduzieren

<!-- play: slow -->
```bash
cd ..
pisa replay --from adressen-spring --out adressen-spring-copy
```

Du solltest sehen: `… All N generated file(s) match the hashes recorded in the recipe.` Pisa führt die
aufgezeichneten Schritte (`init`, `generate`) in einem neuen Ordner erneut aus und vergleicht Dateien mit den
Prüfsummen im Rezept (`adressen-spring/.pisa/recipe.json`). Die LLM-Schritte werden nicht wiederholt: deren Ergebnis
(`spec/architecture.adl`, `spec/adressen-spring.dml`) wird aus dem Quellprojekt kopiert.

## 10. Optional: bei jedem Push bauen und testen (CI)

Das ist ein Zusatz; für alles andere brauchst du ihn nicht. Soll GitHub das Projekt bei jedem Push bauen und die Tests ausführen
(GitHub Actions), ergänze in `spec/architecture.adl` vor der letzten Klammer den Block `pipeline` und wende die `.adl` neu an:

```
    pipeline {
        ci github
    }
```

Die Befehle tun beides statt deiner (die letzte Zeile der `.adl` ersetzen, dann `init --force`). Führe den **ganzen** Block aus: `pisa init … --force` allein ändert nichts, solange der Block `pipeline` nicht in `spec/architecture.adl` steht, und ohne ihn legt Pisa weder ein Repository noch einen Commit an.

<!-- play: optional -->
```bash
cd adressen-spring
sed -i.bak '$ d' spec/architecture.adl && rm spec/architecture.adl.bak
printf '    pipeline {\n        ci github\n    }\n}\n' >> spec/architecture.adl
pisa init --target spring-boot --out . --group ch.example --artifact adressen-spring --adl spec/architecture.adl --force
```

Du solltest sehen: die neue Datei `.github/workflows/ci.yml` in der Liste und `Git repository created, first commit made (branch main …)`.
Pisa legt das Repository an und macht den ersten Commit (`Initial project (pisa init)`), nur lokal; es kennt dein GitHub-Konto nicht,
das übernimmst du. Es gibt drei Fälle, je einen Absatz:

**Normalfall: Pisa hat den Commit gemacht.** Dann legst du das Repository auf GitHub an und lädst es hoch (braucht die
GitHub-CLI `gh`, einmal `gh auth login`):

<!-- play: skip -->
```bash
gh repo create adressen-spring --private --source . --push
```

**Pisa hat kein Repository oder keinen Commit gemacht.** Das sind die vier Fälle:

- Der Block `pipeline` fehlt in `spec/architecture.adl` (nur die letzte Zeile ausgeführt): **weder Repository noch Commit**.
- `pisa play --fast` hat den ganzen Schritt ausgelassen: **weder Repository noch Commit**.
- Der Ordner liegt schon in einem Git-Repository: Pisa fasst es nicht an, es macht **keinen** Commit.
- In deiner Git-Konfiguration fehlen Name und E-Mail: das Repository ist da, aber **kein Commit**.

`git status` im Projektordner zeigt es: «not a git repository» heisst, es gibt keines. Dann machst du das Repository und den Commit selbst
und gehst danach weiter wie im Normalfall (`git init` schadet nicht, wenn das Repository schon da ist):

<!-- play: skip -->
```bash
git init -b main
git add -A
git commit -m "Initial project"
```

**Ohne `gh`.** Lege auf github.com ein leeres Repository an und verbinde es mit deinem Ordner (statt `gh repo create`):

<!-- play: skip -->
```bash
git remote add origin git@github.com:<dein-name>/adressen-spring.git
git push -u origin main
```

Danach läuft die Pipeline auf GitHub (Reiter «Actions»): der Job `tests` und der Job `browser-tests`. Die Datei gehört dir,
auch `init --force` überschreibt sie nicht. Pisa hat sie noch nicht auf einem echten GitHub-Lauf geprüft; wird ein Job rot, ist
das ein Befund für Pisa. Mehr dazu: User Guide, Abschnitt 4e (`pipeline`). `pisa play --fast` lässt diesen Schritt aus.

## 11. Optional: mit Docker und der echten Datenbank ausprobieren

Auch das ist ein Zusatz. Bisher läuft alles auf H2 im Speicher. Das `Dockerfile` aus Schritt 3 liegt schon da; mit Docker kannst du die Anwendung
als Image bauen und gegen ein **echtes PostgreSQL** laufen lassen, quasi wie später in der Cloud. Du brauchst es für nichts anderes.
`pisa play` spielt diesen Schritt **nie** ab (alles `skip`, auch ohne `--fast`): probiere ihn von Hand aus. Voraussetzung: Docker läuft
(`docker version`), die Ports 8080 und 5432 sind frei.

**1. Nur das Image, mit Port.** Das `Dockerfile` baut die Anwendung als Image. Ein Dockerfile veröffentlicht keinen Port von selbst (`EXPOSE` ist nur ein
Hinweis), darum gehört `-p 8080:8080` in den Befehl, sonst erreichst du die Anwendung von aussen nicht (in IntelliJ: im Run-Konfig «Bind ports» setzen):

<!-- play: skip -->
```bash
docker build -t adressen-spring .
docker run --rm --name adressen-spring -p 8080:8080 adressen-spring
```

Du solltest sehen: die Anwendung auf http://localhost:8080/login.html (H2 im Container; Benutzer `admin/admin` und `user/user`, das gewählte Passwort ist mit dem Stoppen weg, weil H2 im Speicher liegt). Stoppen mit Ctrl-C.

**2. Auf PostgreSQL umstellen.** In `spec/architecture.adl` die Zeile `engine h2` durch `engine postgresql` ersetzen, die `.adl` neu anwenden und
den Code neu erzeugen. Mit PostgreSQL schreibt Pisa die `compose.yml` mit der Anwendung (`app`) und der Datenbank (`db`) auch ohne die Zeile
`compose` im Block `docker`.

<!-- play: skip -->
```bash
sed -i.bak 's/engine h2/engine postgresql/' spec/architecture.adl && rm spec/architecture.adl.bak
pisa init --target spring-boot --out . --group ch.example --artifact adressen-spring --adl spec/architecture.adl --force
pisa generate --file spec/adressen-spring.dml --out .
```

Die alte `H2TcpServerConfig.java` (der H2-TCP-Server) bricht den Bau gegen PostgreSQL. `pisa init … --force` löscht sie selbst, wenn du sie nicht geändert hast (`Info: deleted, no longer used …`); hast du sie geändert, steht sie mit einer Zeile `Delete them with: rm …` in der Meldung, dann führe den Befehl aus.

**3. Anwendung und Datenbank starten.**

<!-- play: skip -->
```bash
docker compose up --build
```

Du solltest sehen: Maven baut das Image (beim ersten Mal einige Minuten), dann `Started AdressenSpringApplication`. Die Anwendung läuft auf
http://localhost:8080/login.html (Benutzer `admin/admin` und `user/user`; das gewählte Passwort liegt in der Tabelle `usr_password` von PostgreSQL und bleibt). Stoppen mit Ctrl-C, aufräumen mit
`docker compose down` (mit `-v` auch die Daten).

**4. Nur die Datenbank (zum Entwickeln).** `docker compose up -d db` startet nur PostgreSQL, die Anwendung startest du wie gewohnt mit `mvn spring-boot:run`
(sie nimmt die Datenbank aus `application.yml`):

<!-- play: skip -->
```bash
docker compose up -d db
mvn spring-boot:run
```

**5. Tests gegen ein echtes PostgreSQL (Testcontainers).** Mit PostgreSQL erzeugt Pisa zu jeder Entität einen Repository-Test und einen Test, der die ganze
Anwendung startet, beide gegen ein PostgreSQL in einem Container, den Testcontainers selbst startet und wieder löscht (eigener, freier Port; die
`compose`-Datenbank muss nicht laufen). Sie laufen nur mit dem Maven-Profil `testcontainers` (JUnit-Tag `testcontainers`), `mvn test` allein lässt sie aus und braucht kein Docker:

<!-- play: skip -->
```bash
mvn test -Ptestcontainers
```

Du solltest sehen: zusätzlich zu den normalen Tests die Klassen `…RepositoryPostgresTest` und `…PostgresApplicationTest`, `BUILD SUCCESS`. Profile
lassen sich kombinieren (`-Pui,testcontainers`). Die erzeugte CI-Pipeline (Schritt 10) führt bei PostgreSQL `mvn -B test -Ptestcontainers` aus.

**Wenn es nicht klappt:**

- `port is already allocated`: ein anderes PostgreSQL oder eine andere Anwendung belegt 5432 oder 8080. Lege in der `compose.yml` andere Ports fest
  (`"5433:5432"`) oder setze `database.port` in `.pisa/architecture.plan.json` und wende die `.adl` neu an.
- `docker compose` hängt bei `docker-credential-desktop`: der Credential-Helper von Docker Desktop antwortet nicht. Starte Docker Desktop neu oder nutze
  eine `DOCKER_CONFIG` ohne `credsStore`.
- Testcontainers findet Docker nicht: Docker Desktop läuft nicht oder der Socket liegt woanders (`DOCKER_HOST`).

## 12. Optional: in die Cloud (Render)

Auch das ist ein Zusatz und braucht ein GitHub-Repository (Schritt 10) und ein Konto bei [Render](https://render.com). Render lässt das Docker-Image der Anwendung
auf einem **kostenlosen Web-Dienst** laufen: er schläft nach 15 Minuten ohne Zugriffe (das Aufwachen dauert etwa eine Minute) und hat keinen dauerhaften
Datenträger, `H2` und die gewählten Passwörter sind nach jedem Neustart weg. Die kostenlose PostgreSQL von Render läuft nach 30 Tagen ab: wenn du Schritt 11 gemacht hast,
setze `engine postgresql` in `spec/architecture.adl` wieder auf `engine h2`. `pisa play` spielt den Schritt **nie** ab (alles `skip`).

**1. Die Zeilen in die `.adl`.** Der Block `pipeline` (Schritt 10) bekommt `release github` und `deploy render`; `deploy` braucht den Block `docker`, den du schon hast
(Schritt 2). Hast du Schritt 10 übersprungen, schreibe den ganzen Block vor die letzte Klammer:

```
    pipeline {
        ci github
        release github
        deploy render
    }
```

Dann die `.adl` neu anwenden (`render.yml` und `.github/workflows/cd.yml` entstehen, einmal, danach gehören sie dir):

<!-- play: skip -->
```bash
pisa init --target spring-boot --out . --group ch.example --artifact adressen-spring --adl spec/architecture.adl --force
```

Du solltest sehen: `render.yml` und `.github/workflows/cd.yml` in der Liste und `Deploy to Render: the comments at the top of render.yml say how`.

**2. Bei Render einrichten (im Browser).** Schiebe das Projekt nach GitHub (Schritt 10). Auf dashboard.render.com **New ▸ Blueprint**, das Repository wählen und im Feld
**Blueprint Path** `render.yml` eintragen (Render sucht standardmässig `render.yaml`). Danach im Dienst **Settings ▸ Deploy Hook** die URL kopieren und im GitHub-Repository
unter **Settings ▸ Secrets and variables ▸ Actions** als Secret `RENDER_DEPLOY_HOOK_URL` speichern. Die URL ist geheim: nie in eine Datei schreiben.

**3. Deployen.** Es deployt nur der Deploy-Hook, also nur ein Tag `v*` (das Release-Workflow legt das Release an, `cd.yml` ruft den Hook):

<!-- play: skip -->
```bash
git add -A && git commit -m "Deploy to Render"
git tag v0.1.0
git push origin main v0.1.0
```

Du solltest sehen: im Repository unter *Actions* den Lauf **Deploy**, bei Render einen neuen Deploy und nach einigen Minuten die Anwendung unter der Adresse des Dienstes
(`https://<name>.onrender.com/login.html`). Der erste Zugriff nach dem Schlaf dauert etwa eine Minute. Mehr dazu: User Guide, Abschnitt 4e («Cloud: Render»).
Pisa hat das noch nicht auf einem echten Render-Konto geprüft: ob 512 MB für Spring reichen und ob der Dienst den `healthCheckPath` annimmt, ist ungeprüft.

## Was du jetzt hast

```
adressen-spring/
  spec/            architecture.md, architecture.adl, domain-spec.md, adressen-spring.dml,
                   person.ui.md, company.ui.md, use-cases.uc.md, domain-model.puml/.md
  .pisa/           recipe.json (Rezept), architecture.plan.json (Plan, aus der .adl abgeleitet)
  src/             erzeugter Code und Tests (nie von Hand ändern, `generate` schreibt ihn neu)
  doc/             User Guide und beide Tutorials
  .github/workflows/ci.yml   die Pipeline (nur mit Schritt 10), .git (Repository, nur mit Schritt 10)
  Dockerfile, .dockerignore  das Image (aus dem Block `docker`); compose.yml (nur mit Schritt 11)
  render.yml                 der Blueprint für Render (nur mit Schritt 12)
  pom.xml, README.md, project.spec.json, application.yml (Name der Anwendung), security.yml (Rollen und Benutzer)
```

Du schreibst Prosa und prüfst Vorschläge; **die Specs** (`.adl`, `.dml`, `.ui.md`, `.uc.md`) sind die Quelle
der Wahrheit, der Code ist daraus ableitbar. Dieselben Specs ergeben mit `runtime hono` ein TypeScript-Projekt: siehe das
[Hono-TypeScript-Tutorial](pisa-tutorial-hono-typescript.md).

## Wenn etwas nicht klappt

| Meldung | Bedeutung |
|---|---|
| `… is still the starting text` | Die Markerzeile `<!-- pisa:starting-text -->` steht noch in der Beschreibung. |
| `ended without an accepted …` | Das Modell hat nicht gültig über das Werkzeug geantwortet; Beschreibung präzisieren, wiederholen. |
| `The Salt mockup in spec/<entity>.ui.md can't be used: … missing "x"` | Ein Feld im `.dml` fehlt im Mockup (oder umgekehrt). Zeile `Label | "x"` ins Mockup einfügen, `generate` schreibt vorher nichts. |
| `Package org.h2.tools ist nicht vorhanden` nach dem Wechsel auf PostgreSQL | Die alte `src/main/java/…/config/H2TcpServerConfig.java` (vom H2-Betrieb) braucht H2. `init --force` löscht sie, wenn sie unverändert ist; sonst meldet es `Info: no longer used … changed by you` samt Zeile `Delete them with: rm …`. Befehl ausführen. |
| `Package lombok / org.mapstruct ist nicht vorhanden` nach dem Wechsel `layered` → `crud` | `init --adl … --force` und `generate` löschen die unveränderten Dateien des alten Stils selbst; geänderte bleiben und stehen unter `Info: no longer used … changed by you` mit `rm`-Befehl: `src/main/java/…/dto`, `…/mapper`, `…/service` und `src/test/java/…/service`. |
| Seite zeigt leere Buttons oder Überschriften | Die Seite läuft nicht über den Server. `mvn spring-boot:run` starten und http://localhost:8080/ öffnen; Alpine.js setzt die Texte erst im Browser. |
| `Company doesn't allow "delete" (@operations(…)) but the test line says "delete"` | Ein Use Case verlangt eine Operation, die `@operations` der Entität sperrt. `delete` wieder erlauben, die `Test:`-Zeile ändern oder den Use Case streichen (Schritt 7a). |
| Nach dem Streichen eines Use Cases schlägt dessen Browser-Test fehl | Pisa löscht nie: den alten Test von Hand löschen (`test/ui/UC<n>.spec.ts` bzw. `…/ui/UC<n>UiTest.java`). |
| `Info: the plan has authentication session, but there is no security.yml` | `security.yml` fehlt. `pisa init … --adl … --force` schreibt sie neu. |
| Anmeldung funktioniert nach dem Einschalten nicht (Seiten offen) | `init … --force` vergessen: erst damit kommen `security.yml` und die Seiten, dann `generate` und Neustart. |
| `No project.spec.json in .` | `generate` braucht zuerst `pisa init` (Schritt 3). |
| `… exists already` | Die Zieldatei gibt es; `--force` schlägt neu vor und überschreibt sie. |
| Das Projekt kompiliert nicht nach einem Modellvorschlag | Prüfe das `.dml`: etwa eine Beziehung auf einen `composed`-Bestandteil einer anderen Entität wird heute nicht abgelehnt (bekannte Lücke, `doc/state/findings.md`). |
