#!/bin/bash
# Reads the pushed tag (e.g. "v1.2.3"), patches app/build.gradle.kts's versionCode/versionName
# to match, and prepares the env vars the rest of release.yml needs (tag name, display name,
# release/Telegram message body). Same pattern as OOS_Theme's bumpVersion.sh, adapted for
# Obsidian (no changelog.xml here, so the message is a simple release announcement instead).

NEWVERNAME=${GITHUB_REF_NAME/v/}
NEWVERCODE=${NEWVERNAME//.}

echo 'VTag<<EOF' >> $GITHUB_ENV
echo ${GITHUB_REF_NAME} >> $GITHUB_ENV
echo 'EOF' >> $GITHUB_ENV

echo 'VName<<EOF' >> $GITHUB_ENV
echo 'Obsidian v'$NEWVERNAME >> $GITHUB_ENV
echo 'EOF' >> $GITHUB_ENV

echo "ApkName=Obsidian-release-$NEWVERNAME.apk" >> $GITHUB_ENV

sed -i 's/versionCode.*/versionCode    = '$NEWVERCODE'/' app/build.gradle.kts
sed -i 's/versionName    =.*/versionName    = "'$NEWVERNAME'"/' app/build.gradle.kts

echo "Obsidian v$NEWVERNAME released! Full changelog on the GitHub release page." > telegram.msg
echo 'TMessage<<EOF' >> $GITHUB_ENV
cat telegram.msg >> $GITHUB_ENV
echo 'EOF' >> $GITHUB_ENV
