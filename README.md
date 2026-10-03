# Vault Odds (reconstruction propre)

Mod Fabric client pour Minecraft 1.21.11 : affiche les probabilités des vaults
(trial chambers) et peut insérer la clé automatiquement quand l'objet choisi est affiché.

Touches par défaut : K = afficher/masquer le panneau, J = choisir l'objet cible.

## Compiler
Java 21+ requis (`java -version`).

- Avec Gradle 9.x installé : `gradle build` -> `build/libs/vault-odds-1.4.1.jar`
- Sans rien installer : pousser ce dossier sur GitHub, l'onglet Actions compile
  (workflow `.github/workflows/build.yml`) et fournit le .jar en téléchargement.

Aucune dépendance réseau, aucun code d'exécution de commandes : tout est lisible dans `src/`.
