#!/bin/bash
# Legge changelog.txt (scritto a mano prima di ogni tag) e costruisce il testo del changelog
# per la GitHub Release (Body) e per il messaggio Telegram (TMessage). Sezioni riconosciute:
# "Aggiunto:", "Aggiornato:", "Rimosso:" (tutte opzionali) — ogni riga sotto una sezione
# diventa un punto elenco. Righe vuote ignorate.

CHANGELOG_TXT="changelog.txt"

CHANGELOG=""
while IFS= read -r line || [ -n "$line" ]; do
  trimmed=$(echo "$line" | sed 's/^[[:space:]]*//;s/[[:space:]]*$//')
  [ -z "$trimmed" ] && continue
  case "$trimmed" in
    Aggiunto:|Aggiornato:|Rimosso:)
      CHANGELOG+=$'\n'"$trimmed"$'\n'
      ;;
    -)
      # riga segnaposto vuota (sezione senza voci), ignorata
      ;;
    *)
      CHANGELOG+="$trimmed"$'\n'
      ;;
  esac
done < "$CHANGELOG_TXT"

{
  echo "$VName"
  echo "$CHANGELOG"
} > body.md
echo 'Body<<EOF' >> $GITHUB_ENV
cat body.md >> $GITHUB_ENV
echo 'EOF' >> $GITHUB_ENV

{
  echo "$VName rilasciato!"
  echo "$CHANGELOG"
} > telegram.msg
echo 'TMessage<<EOF' >> $GITHUB_ENV
cat telegram.msg >> $GITHUB_ENV
echo 'EOF' >> $GITHUB_ENV
