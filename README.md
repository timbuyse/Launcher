# Launcher

Minimalistische Android launcher in de stijl van Niagara Launcher. Kotlin + Jetpack Compose.

## Nu aanwezig (MVP)
- Zwart startscherm met klok, datum en favorieten als grote tekstlijst
- Veeg omhoog (of tik op "Alle apps") voor de alfabetische app-lijst
- Letterbalk aan de rechterkant om snel te scrollen
- Zoeken onderaan, Enter start het eerste resultaat
- Lang indrukken op een app: favoriet maken, app-info, verwijderen

## Nog te doen
Meldingen-indicatoren, mediabediening en widgets.

## APK krijgen
Elke push draait de GitHub Actions workflow. Download de APK onder Actions > laatste run > Artifacts (`launcher-debug-apk`) en installeer hem op je telefoon (onbekende bronnen toestaan). Stel daarna Launcher in als standaard-startscherm.

Lokaal bouwen kan in Android Studio (open de map, Gradle sync, Run).
