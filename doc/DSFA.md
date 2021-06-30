# Datenschutzfolgenabschätzung

Diese Datenschutzfolgenabschätzung bezieht sich nur auf den in diesem Repository vorhanden Funktionsumfang zur Erstellung von Impf- und Genesenenzertifikate.

Es werden nur die Datenflüsse beim Einsatz in der PU beschrieben.

## Beschreibung der Art der Datenverarbeitung

Es werden personenbezogene Daten erhoben und verarbeitet. Diese werden dem System bekannt gemacht und an die Schnittstelle übergeben, welche dann das eigentliche Zertifikat erstellt.

Ebenfalls übergeben wird der Schnittstelle eine Authentifizierung des Arztes in Form eines JWT (_Token_). Diese Authentifizierung kann Daten des Arztes enthalten. Der konkrete Inhalt wird jedoch durch den Authentifizierungsservice `id.impfnachweis.info` bestimmt.

Die Personenbezogenen Daten werden anschließend an `api.impfnachweis.info` übermittelt.

Das jeweilig erstellte Zertifikat wird zurück auf das System übertragen in Form der Antwort eines HTTP-Requests.

## Begründung für die Datenerhebung

Zum Zwecke der Ausstellung eines personenindividuellen Impfzertifikates ist es erforderlich die genauen Impfdaten einer Person zu erheben.

Des weiteren ist es zur Authentifizierung gegenüber der Zertifikatserstellungsschnittstelle (welche das eigentliche Zertifikat erstellt) erforderlich. Darüber wird die Legitimierung des Zertifikatsanfragers (z.B. der Arzt) durchgeführt.

## Umfang der erfassten Daten

Es werden ausschließlich die folgenden Daten der geimpften Person erfasst und verarbeitet:
* Name
* Vorname
* Geburtsdatum
* Krankheit (in der Regel sollte das die Identifizierung für COVID-19 sein)
* Impfdatum
* Impfstoff
* Impfstoffproduzent
* Impfstoffprodukt
* Dosennummer
* Gesamtdosen

Bei einer genesenen Person werden ausschließlich folgende Daten erfasst:
* Name
* Vorname
* Geburtsdatum
* Krankheit (in der Regel sollte das die Identifizierung für COVID-19 sein)
* Datum des ersten positiven Befundes
* Startdatum des Genesenenzertifikates
* Enddatum des Genesenenzertifikates

Sowohl bei der Erstellung des Impfzertifikates, als auch bei der Erstellung des Genesenenzertifikates wird die Identifizierung des Arztes in Form einer Betriebsstättennummer übertragen.

Die übertragenen Daten zur Identifizierung des Arztes werden im Wesentlichen durch `id.impfnachweis.info` festgelegt. Zum Erhalt der Authentifizierung werden folgende Daten übertragen:
* SMC-B Zertifikat der Betriebsstätte des Arztes


## Datenspeicherung

Die beschriebenen Daten werden nicht gespeichert. Daher erübrigt sich die Notwendigkeit einer Datenlöschung.

## Maßnahmen zur Wahrung der Vertraulichkeit

Die Verwendung dieses Systems in der PU erfordert einen Konnektor, welcher mit der TI verbunden ist. Dies stellt die erste Sicherheitsebene dar.

Bei regelgerechter Konfiguration wird der Datenverkehr über den TI-Konnektor geleitet. Darüber hinaus ist - bei Angabe der korrekten Konfiguration - sowohl die Erstellung der Arztlegitimierung, als auch die Datenübertragung zur Erstellung und zum Abruf der Zertifikate via HTTPS transportverschlüsselt.

Der Zugriff auf den Konnektor kann so konfiguriert werden, dass dieser ausschließlich authentifiziert erfolgt. Zur Auswahl stehen hier eine Clientzertifikatauthentifizierung sowie eine Benutzername-Passwort-Authentifizierung (sog. _Basic auth_).

## Risiken

Es bestehen die bei jeder Datenübertragung in einem Netzwerk üblichen Risiken.

### Risiko 1
Insbesondere besteht die Möglichkeit, dass der Token, welcher die Authentifizierung des Arztes enthält, verloren geht oder an die falsche Stelle übertragen wird (z.B. wenn eine Fehlkonfiguration durch in der PVS-Integration vorliegt). Mit dieser Authentifizierung könnten dann Dritte, welche Zugriff auf die TI haben, für eine gewisse Zeit im Namen des Arztes Impf- und Genesenenzertifikate erstellen. Außerdem werden dann die zusätzlichen Metadaten, welche ebenfalls im Token enthalten sind, diesen Dritten bekannt gemacht.

### Risiko 2
Wird die Antwort vom Zertifikatserstellungsdienst nicht korrekt übertragen, oder von diesem System verworfen, ist ein entsprechendes Zertifikat erstellt, aber nicht an den Patienten ausgeliefert worden.

### Risiko 3
Wird das System falsch konfiguriert (wenn der Endpunkt zur Zertifikatserstellung falsch angegeben wurde) kann, neben dem Arzt-Token auch die Patientendaten, wie oben beschrieben, Dritten am angegebenen Endpunkt bekannt gemacht werden.

### Risiko 4
Durch eine fehlerhafte Konfiguration ist es möglich, die Transportverschlüsselung via HTTPS zu deaktivieren oder eine Zertifikatsüberprüfung (des TLS-Zertifikates) zu deaktivieren. In diesem Fall besteht die Möglichkeit durch Dritte, alle übertragenen Daten abzugreifen und zu verändern. Die Nutzung der so abgefangenen Daten ist oben in diesem Abschnitt beschrieben. Eine Manipulation der Daten auf dem Transportweg ergibt die Möglichkeit der Änderung der zur Zertifikatserstellung verwendeten Daten.

### Risiko 5
In der Standardkonfiguration horcht das System auf allen IP-Adressen des Rechners, auf dem es läuft. Dies ermöglicht einen Zugriff von anderen Rechnern über das Netzwerk. Da dieses System keine Authentifizierung hat, wäre es in diesem Fall möglich, dass sich beliebige Nutzer, die via Netzwerk auf den Rechner zugreifen können, beliebig viele Impfzertifikate erstellen. Analoges gilt für einen Zugriff direkt vom Rechner auf dem das System installiert ist, unabhängig von der "Horchkonfiguration".

## Bewertung der Risiken

| Risiko | Eintrittswahrscheinlichkeit | Auswirkungen | Prävention |
| ----- | ----- | ------ | ------ |
| Risiko 1 | gering | sehr hoch | Kontrolle der Konfiguration; vorgeschlagene Werte für PU verwenden |
| Risiko 2 | mittel | niedrig | keine möglich; Zertifikatserstellung muss dann erneut gestartet werden |
| Risiko 3 | gering | sehr hoch | Kontrolle der Konfiguration; vorgeschlagene Werte für PU verwenden |
| Risiko 4 | gering | sehr hoch | Kontrolle der Konfiguration; vorgeschlagene Werte für PU verwenden; Zertifikatsüberprüfung in Konfiguration aktivieren |
| Risiko 5 | mittel | mittel | Netzwerktechnische Isolation des Systems (beispielsweise nur auf `127.0.0.1` horchen); zusätzlich lokale Zugriffsbeschränkungen (z.B. durch Sperren der Arbeitsstation) |
