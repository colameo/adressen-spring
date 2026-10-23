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
